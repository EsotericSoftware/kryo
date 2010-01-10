
package com.esotericsoftware.kryo.serialize;

import static com.esotericsoftware.minlog.Log.*;
import static org.objectweb.asm.Opcodes.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.PriorityQueue;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.NotNull;
import com.esotericsoftware.kryo.SerializationException;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.Kryo.RegisteredClass;

/**
 * Serializes objects using direct field assignment through bytecode generation. This serializer is faster than
 * {@link FieldSerializer} but can only serializer public fields, otherwise it behaves the same.
 * @see FieldSerializer
 */
public class AsmFieldSerializer extends Serializer {
	final Kryo kryo;
	private final AccessLoader loader = new AccessLoader();
	final IdentityHashMap<Class, CachedField[]> fieldCache = new IdentityHashMap();
	private boolean fieldsCanBeNull = true;

	public AsmFieldSerializer (Kryo kryo) {
		this.kryo = kryo;
	}

	/**
	 * Sets the default value for {@link CachedField#setCanBeNull(boolean)}. Should not be called after any objects are serialized
	 * or {@link #getField(Class, String)} is used.
	 * @param fieldsCanBeNull False if none of the fields are null. Saves 1 byte per field. True if it is not known (default).
	 */
	public void setFieldsCanBeNull (boolean fieldsCanBeNull) {
		this.fieldsCanBeNull = fieldsCanBeNull;
	}

	CachedField[] cache (Class type) {
		if (type.isInterface()) return new CachedField[0]; // No fields to serialize.
		ArrayList<Field> allFields = new ArrayList();
		Class nextClass = type;
		while (nextClass != Object.class) {
			Collections.addAll(allFields, nextClass.getDeclaredFields());
			nextClass = nextClass.getSuperclass();
		}
		PriorityQueue<CachedField> cachedFields = new PriorityQueue(Math.max(1, allFields.size()), new Comparator<CachedField>() {
			public int compare (CachedField o1, CachedField o2) {
				// Fields are sorted by alpha so the order of the data is known.
				return o1.field.getName().compareTo(o2.field.getName());
			}
		});
		for (int i = 0, n = allFields.size(); i < n; i++) {
			Field field = allFields.get(i);
			int modifiers = field.getModifiers();
			if (Modifier.isTransient(modifiers)) continue;
			if (Modifier.isFinal(modifiers)) continue;
			if (Modifier.isStatic(modifiers)) continue;
			if (field.isSynthetic()) continue;
			if (Modifier.isPrivate(modifiers)) continue;

			CachedField cachedField = new CachedField();
			cachedField.field = field;
			if (fieldsCanBeNull)
				cachedField.canBeNull = !field.isAnnotationPresent(NotNull.class);
			else
				cachedField.canBeNull = false;

			// Always use the same serializer for this field if the field's class is final.
			Class fieldClass = field.getType();
			if (Modifier.isFinal(fieldClass.getModifiers())) {
				cachedField.fieldClass = fieldClass;
				cachedField.serializer = kryo.getRegisteredClass(fieldClass).serializer;
			}

			cachedFields.add(cachedField);
		}

		int n = cachedFields.size();
		CachedField[] cachedFieldArray = new CachedField[n];
		for (int i = 0; i < n; i++)
			cachedFieldArray[i] = cachedFields.poll();
		fieldCache.put(type, cachedFieldArray);
		return cachedFieldArray;
	}

	public void writeObjectData (ByteBuffer buffer, Object object) {
		Class type = object.getClass();
		Access access = loader.getAccess(type);
		CachedField[] fields = access.cachedFields;
		for (int i = 0, n = fields.length; i < n; i++) {
			CachedField cachedField = fields[i];
			if (TRACE) trace("kryo", "Writing field: " + cachedField + " (" + type.getName() + ")");

			Object value = access.get(object, i);

			Serializer serializer = cachedField.serializer;
			if (cachedField.fieldClass == null) {
				if (value == null) {
					kryo.writeClass(buffer, null);
					continue;
				}
				RegisteredClass registeredClass = kryo.writeClass(buffer, value.getClass());
				if (serializer == null) serializer = registeredClass.serializer;
				serializer.writeObjectData(buffer, value);
			} else {
				if (!cachedField.canBeNull)
					serializer.writeObjectData(buffer, value);
				else
					serializer.writeObject(buffer, value);
			}
		}
		if (TRACE) trace("kryo", "Wrote object: " + object);
	}

	public <T> T readObjectData (ByteBuffer buffer, Class<T> type) {
		T object = newInstance(type);
		Access access = loader.getAccess(type);
		CachedField[] fields = access.cachedFields;
		for (int i = 0, n = fields.length; i < n; i++) {
			CachedField cachedField = fields[i];
			if (TRACE) trace("kryo", "Reading field: " + cachedField + " (" + type.getName() + ")");

			Object value;

			Class concreteType = cachedField.fieldClass;
			Serializer serializer = cachedField.serializer;
			if (concreteType == null) {
				RegisteredClass registeredClass = kryo.readClass(buffer);
				if (registeredClass == null)
					value = null;
				else {
					concreteType = registeredClass.type;
					if (serializer == null) serializer = registeredClass.serializer;
					value = serializer.readObjectData(buffer, concreteType);
				}
			} else {
				if (!cachedField.canBeNull)
					value = serializer.readObjectData(buffer, concreteType);
				else
					value = serializer.readObject(buffer, concreteType);
			}

			access.set(object, i, value);
		}
		if (TRACE) trace("kryo", "Read object: " + object);
		return object;
	}

	/**
	 * Allows specific fields to be optimized.
	 */
	public CachedField getField (Class type, String fieldName) {
		CachedField[] fields = fieldCache.get(type);
		if (fields == null) fields = cache(type);
		for (CachedField cachedField : fields)
			if (cachedField.field.getName().equals(fieldName)) return cachedField;
		throw new IllegalArgumentException("Field \"" + fieldName + "\" not found on class: " + type.getName());
	}

	/**
	 * Removes a field so that it won't be serialized.
	 */
	public void removeField (Class type, String fieldName) {
		CachedField[] fields = fieldCache.get(type);
		if (fields == null) fields = cache(type);
		for (int i = 0; i < fields.length; i++) {
			CachedField cachedField = fields[i];
			if (cachedField.field.getName().equals(fieldName)) {
				CachedField[] newFields = new CachedField[fields.length - 1];
				System.arraycopy(fields, 0, newFields, 0, i);
				System.arraycopy(fields, i + 1, newFields, i, newFields.length - i);
				fieldCache.put(type, newFields);
				return;
			}
		}
		throw new IllegalArgumentException("Field \"" + fieldName + "\" not found on class: " + type.getName());
	}

	/**
	 * Controls how a field will be serialized.
	 */
	public class CachedField {
		Field field;
		Class fieldClass;
		Serializer serializer;
		boolean canBeNull;

		/**
		 * @param fieldClass The concrete class of the values for this field. This saves 1-2 bytes. The serializer registered for
		 *           the specified class will be used. Set to null if the field type in the class definition is final or the values
		 *           for this field vary (default).
		 */
		public void setClass (Class fieldClass) {
			this.fieldClass = fieldClass;
			this.serializer = fieldClass == null ? null : kryo.getRegisteredClass(fieldClass).serializer;
		}

		/**
		 * @param fieldClass The concrete class of the values for this field. This saves 1-2 bytes. The serializer registered for
		 *           the specified class will be used. Set to null if the field type in the class definition is final or the values
		 *           for this field vary (default).
		 */
		public void setClass (Class fieldClass, Serializer serializer) {
			this.fieldClass = fieldClass;
			this.serializer = serializer;
		}

		public void setCanBeNull (boolean canBeNull) {
			this.canBeNull = canBeNull;
		}

		public String toString () {
			return field.getName();
		}
	}

	class AccessLoader extends ClassLoader {
		private IdentityHashMap<Class, Access> classToAccess = new IdentityHashMap();

		Access getAccess (Class type) {
			Access access = classToAccess.get(type);
			if (access != null) return access;

			CachedField[] cachedFields = fieldCache.get(type);
			if (cachedFields == null) cachedFields = cache(type);

			String className = type.getName();
			String accessClassName = className + "Access";
			Class accessClass = null;
			try {
				accessClass = loadClass(accessClassName);
			} catch (ClassNotFoundException ignored) {
			}
			if (accessClass == null) {
				String targetClassName = className.replace('.', '/');

				ClassWriter cw = new ClassWriter(0);
				FieldVisitor fv;
				MethodVisitor mv;
				AnnotationVisitor av0;
				cw.visit(V1_6, ACC_PUBLIC + ACC_SUPER, accessClassName.replace('.', '/'), null,
					"com/esotericsoftware/kryo/serialize/AsmFieldSerializer$Access", null);
				cw.visitInnerClass("com/esotericsoftware/kryo/serialize/AsmFieldSerializer$Access",
					"com/esotericsoftware/kryo/serialize/AsmFieldSerializer", "Access", ACC_PUBLIC + ACC_STATIC + ACC_ABSTRACT);
				{
					mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
					mv.visitCode();
					mv.visitVarInsn(ALOAD, 0);
					mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
					mv.visitInsn(RETURN);
					mv.visitMaxs(1, 1);
					mv.visitEnd();
				}
				{
					mv = cw.visitMethod(ACC_PUBLIC, "set", "(Ljava/lang/Object;ILjava/lang/Object;)V", null, null);
					mv.visitCode();
					mv.visitVarInsn(ILOAD, 2);

					Label[] labels = new Label[cachedFields.length];
					for (int i = 0, n = cachedFields.length; i < n; i++)
						labels[i] = new Label();
					Label defaultLabel = new Label();
					mv.visitTableSwitchInsn(0, cachedFields.length - 1, defaultLabel, labels);

					for (int i = 0, n = cachedFields.length; i < n; i++) {
						Field field = cachedFields[i].field;
						Type fieldType = Type.getType(field.getType());

						mv.visitLabel(labels[i]);
						mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
						mv.visitVarInsn(ALOAD, 1);
						mv.visitTypeInsn(CHECKCAST, targetClassName);
						mv.visitVarInsn(ALOAD, 3);

						switch (fieldType.getSort()) {
						case Type.BOOLEAN:
							mv.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
							mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z");
							break;
						case Type.BYTE:
							mv.visitTypeInsn(CHECKCAST, "java/lang/Byte");
							mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B");
							break;
						case Type.CHAR:
							mv.visitTypeInsn(CHECKCAST, "java/lang/Character");
							mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C");
							break;
						case Type.SHORT:
							mv.visitTypeInsn(CHECKCAST, "java/lang/Short");
							mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S");
							break;
						case Type.INT:
							mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
							mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I");
							break;
						case Type.FLOAT:
							mv.visitTypeInsn(CHECKCAST, "java/lang/Float");
							mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F");
							break;
						case Type.LONG:
							mv.visitTypeInsn(CHECKCAST, "java/lang/Long");
							mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J");
							break;
						case Type.DOUBLE:
							mv.visitTypeInsn(CHECKCAST, "java/lang/Double");
							mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D");
							break;
						case Type.ARRAY:
							mv.visitTypeInsn(CHECKCAST, fieldType.getDescriptor());
							break;
						case Type.OBJECT:
							mv.visitTypeInsn(CHECKCAST, fieldType.getInternalName());
							break;
						}

						mv.visitFieldInsn(PUTFIELD, targetClassName, field.getName(), fieldType.getDescriptor());
						mv.visitInsn(RETURN);
					}

					mv.visitLabel(defaultLabel);
					mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
					mv.visitTypeInsn(NEW, "java/lang/IllegalArgumentException");
					mv.visitInsn(DUP);
					mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
					mv.visitInsn(DUP);
					mv.visitLdcInsn("Field not found: ");
					mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");
					mv.visitVarInsn(ILOAD, 2);
					mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;");
					mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
					mv.visitMethodInsn(INVOKESPECIAL, "java/lang/IllegalArgumentException", "<init>", "(Ljava/lang/String;)V");
					mv.visitInsn(ATHROW);
					mv.visitMaxs(5, 4);
					mv.visitEnd();
				}
				{
					mv = cw.visitMethod(ACC_PUBLIC, "get", "(Ljava/lang/Object;I)Ljava/lang/Object;", null, null);
					mv.visitCode();
					mv.visitVarInsn(ILOAD, 2);

					Label[] labels = new Label[cachedFields.length];
					for (int i = 0, n = cachedFields.length; i < n; i++)
						labels[i] = new Label();
					Label defaultLabel = new Label();
					mv.visitTableSwitchInsn(0, cachedFields.length - 1, defaultLabel, labels);

					for (int i = 0, n = cachedFields.length; i < n; i++) {
						Field field = cachedFields[i].field;

						mv.visitLabel(labels[i]);
						mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
						mv.visitVarInsn(ALOAD, 1);
						mv.visitTypeInsn(CHECKCAST, targetClassName);
						mv.visitFieldInsn(GETFIELD, targetClassName, field.getName(), Type.getDescriptor(field.getType()));

						Type fieldType = Type.getType(field.getType());
						switch (fieldType.getSort()) {
						case Type.BOOLEAN:
							mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;");
							break;
						case Type.BYTE:
							mv.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;");
							break;
						case Type.CHAR:
							mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;");
							break;
						case Type.SHORT:
							mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;");
							break;
						case Type.INT:
							mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
							break;
						case Type.FLOAT:
							mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;");
							break;
						case Type.LONG:
							mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;");
							break;
						case Type.DOUBLE:
							mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;");
							break;
						}

						mv.visitInsn(ARETURN);
					}

					mv.visitLabel(defaultLabel);
					mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
					mv.visitTypeInsn(NEW, "java/lang/IllegalArgumentException");
					mv.visitInsn(DUP);
					mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
					mv.visitInsn(DUP);
					mv.visitLdcInsn("Field not found: ");
					mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");
					mv.visitVarInsn(ILOAD, 2);
					mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;");
					mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
					mv.visitMethodInsn(INVOKESPECIAL, "java/lang/IllegalArgumentException", "<init>", "(Ljava/lang/String;)V");
					mv.visitInsn(ATHROW);
					mv.visitMaxs(5, 3);
					mv.visitEnd();
				}
				cw.visitEnd();
				byte[] data = cw.toByteArray();
				accessClass = super.defineClass(className + "Access", data, 0, data.length);
			}
			try {
				access = (Access)accessClass.newInstance();
				access.cachedFields = cachedFields;
				classToAccess.put(type, access);
				return access;
			} catch (Exception ex) {
				throw new SerializationException("Error constructing ASM access class: " + accessClassName, ex);
			}
		}
	}

	static public abstract class Access {
		public CachedField[] cachedFields;

		abstract public void set (Object object, int fieldIndex, Object value);

		abstract public Object get (Object object, int fieldIndex);
	}
}
