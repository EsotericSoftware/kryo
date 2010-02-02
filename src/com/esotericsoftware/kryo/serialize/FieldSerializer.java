
package com.esotericsoftware.kryo.serialize;

import static com.esotericsoftware.minlog.Log.*;
import static org.objectweb.asm.Opcodes.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.esotericsoftware.kryo.CustomSerialization;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.NotNull;
import com.esotericsoftware.kryo.SerializationException;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.Kryo.RegisteredClass;

/**
 * Serializes objects using direct field assignment. This is a very fast mechanism for serializing objects, often as good as
 * {@link CustomSerialization}. FieldSerializer is many times smaller and faster than Java serialization. The fields should be
 * public for optimal performance, which allows bytecode generation to be used instead of reflection.
 * <p>
 * FieldSerializer does not write header data, only the object data is stored. If the type of a field is not final (note
 * primitives are final) then an extra byte is written for that field.
 * @see Serializer
 * @see Kryo#register(Class, Serializer)
 * @author Nathan Sweet <misc@n4te.com>
 */
public class FieldSerializer extends Serializer {
	final Kryo kryo;
	final Class type;
	private CachedField[] fields;
	private final AccessLoader accessLoader = new AccessLoader();
	private boolean fieldsCanBeNull = true, setFieldsAsAccessible = true;

	public FieldSerializer (Kryo kryo, Class type) {
		this.kryo = kryo;
		this.type = type;
		rebuildCachedFields();
	}

	private void rebuildCachedFields () {
		if (type.isInterface()) {
			fields = new CachedField[0]; // No fields to serialize.
			return;
		}

		// Collect all fields.
		ArrayList<Field> allFields = new ArrayList();
		Class nextClass = type;
		while (nextClass != Object.class) {
			Collections.addAll(allFields, nextClass.getDeclaredFields());
			nextClass = nextClass.getSuperclass();
		}

		ArrayList<CachedField> publicFields = new ArrayList();
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
			if (setFieldsAsAccessible)
				field.setAccessible(true);
			else if (Modifier.isPrivate(modifiers)) {
				continue;
			}

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
				cachedField.serializer = kryo.getRegisteredClass(fieldClass).getSerializer();
			}

			cachedFields.add(cachedField);
			if (Modifier.isPublic(modifiers)) publicFields.add(cachedField);
		}

		// Generate an access class for any public fields.
		if (!publicFields.isEmpty()) {
			Access access = accessLoader.createAccess(type, publicFields);
			for (int i = 0, n = publicFields.size(); i < n; i++) {
				CachedField cachedField = publicFields.get(i);
				cachedField.access = access;
				cachedField.accessIndex = i;
			}
		}

		int fieldCount = cachedFields.size();
		fields = new CachedField[fieldCount];
		for (int i = 0; i < fieldCount; i++)
			fields[i] = cachedFields.poll();
	}

	/**
	 * Sets the default value for {@link CachedField#setCanBeNull(boolean)}.
	 * @param fieldsCanBeNull False if none of the fields are null. Saves 1 byte per field. True if it is not known (default).
	 */
	public void setFieldsCanBeNull (boolean fieldsCanBeNull) {
		this.fieldsCanBeNull = fieldsCanBeNull;
		rebuildCachedFields();
	}

	/**
	 * Controls which fields are accessed.
	 * @param setFieldsAsAccessible If true, all non-transient fields (inlcuding private fields) will be serialized and
	 *           {@link Field#setAccessible(boolean) set as accessible} (default). If false, only fields in the public API will be
	 *           serialized.
	 */
	public void setFieldsAsAccessible (boolean setFieldsAsAccessible) {
		this.setFieldsAsAccessible = setFieldsAsAccessible;
		rebuildCachedFields();
	}

	public void writeObjectData (ByteBuffer buffer, Object object) {
		Class type = object.getClass();
		try {
			for (int i = 0, n = fields.length; i < n; i++) {
				CachedField cachedField = fields[i];
				if (TRACE) trace("kryo", "Writing field: " + cachedField + " (" + type.getName() + ")");

				Object value = cachedField.get(object);

				Serializer serializer = cachedField.serializer;
				if (cachedField.fieldClass == null) {
					if (value == null) {
						kryo.writeClass(buffer, null);
						continue;
					}
					RegisteredClass registeredClass = kryo.writeClass(buffer, value.getClass());
					if (serializer == null) serializer = registeredClass.getSerializer();
					serializer.writeObjectData(buffer, value);
				} else {
					if (!cachedField.canBeNull)
						serializer.writeObjectData(buffer, value);
					else
						serializer.writeObject(buffer, value);
				}
			}
		} catch (IllegalAccessException ex) {
			throw new SerializationException("Error accessing field in class: " + type.getName(), ex);
		}
		if (TRACE) trace("kryo", "Wrote object: " + object);
	}

	public <T> T readObjectData (ByteBuffer buffer, Class<T> type) {
		return readObjectData(newInstance(type), buffer, type);
	}

	protected <T> T readObjectData (T object, ByteBuffer buffer, Class<T> type) {
		try {
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
						concreteType = registeredClass.getType();
						if (serializer == null) serializer = registeredClass.getSerializer();
						value = serializer.readObjectData(buffer, concreteType);
					}
				} else {
					if (!cachedField.canBeNull)
						value = serializer.readObjectData(buffer, concreteType);
					else
						value = serializer.readObject(buffer, concreteType);
				}

				cachedField.set(object, value);
			}
		} catch (IllegalAccessException ex) {
			throw new SerializationException("Error accessing field in class: " + type.getName(), ex);
		}
		if (TRACE) trace("kryo", "Read object: " + object);
		return object;
	}

	/**
	 * Allows specific fields to be optimized.
	 */
	public CachedField getField (String fieldName) {
		for (CachedField cachedField : fields)
			if (cachedField.field.getName().equals(fieldName)) return cachedField;
		throw new IllegalArgumentException("Field \"" + fieldName + "\" not found on class: " + type.getName());
	}

	/**
	 * Removes a field so that it won't be serialized.
	 */
	public void removeField (String fieldName) {
		for (int i = 0; i < fields.length; i++) {
			CachedField cachedField = fields[i];
			if (cachedField.field.getName().equals(fieldName)) {
				CachedField[] newFields = new CachedField[fields.length - 1];
				System.arraycopy(fields, 0, newFields, 0, i);
				System.arraycopy(fields, i + 1, newFields, i, newFields.length - i);
				fields = newFields;
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
		Access access;
		int accessIndex;

		/**
		 * @param fieldClass The concrete class of the values for this field. This saves 1-2 bytes. The serializer registered for
		 *           the specified class will be used. Set to null if the field type in the class definition is final or the values
		 *           for this field vary (default).
		 */
		public void setClass (Class fieldClass) {
			this.fieldClass = fieldClass;
			this.serializer = fieldClass == null ? null : kryo.getRegisteredClass(fieldClass).getSerializer();
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

		Object get (Object object) throws IllegalAccessException {
			if (access != null) return access.get(object, accessIndex);
			return field.get(object);
		}

		void set (Object object, Object value) throws IllegalAccessException {
			if (access != null)
				access.set(object, accessIndex, value);
			else
				field.set(object, value);
		}
	}

	class AccessLoader extends ClassLoader {
		private HashMap<Class, Access> classToAccess = new HashMap();

		protected Class<?> findClass (String name) throws ClassNotFoundException {
			if (name.equals(Access.class.getName())) return Access.class;
			if (name.equals(type.getName())) return type;
			return super.findClass(name);
		}

		public Access createAccess (Class type, ArrayList<CachedField> publicFields) {
			int fieldCount = publicFields.size();
			Access access = classToAccess.get(type);
			if (access != null) return access;

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
					"com/esotericsoftware/kryo/serialize/FieldSerializer$Access", null);
				cw.visitInnerClass("com/esotericsoftware/kryo/serialize/FieldSerializer$Access",
					"com/esotericsoftware/kryo/serialize/FieldSerializer", "Access", ACC_PUBLIC + ACC_STATIC + ACC_ABSTRACT);
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

					Label[] labels = new Label[fieldCount];
					for (int i = 0, n = publicFields.size(); i < n; i++)
						labels[i] = new Label();
					Label defaultLabel = new Label();
					mv.visitTableSwitchInsn(0, fieldCount - 1, defaultLabel, labels);

					for (int i = 0; i < fieldCount; i++) {
						Field field = publicFields.get(i).field;
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

					Label[] labels = new Label[fieldCount];
					for (int i = 0, n = fieldCount; i < n; i++)
						labels[i] = new Label();
					Label defaultLabel = new Label();
					mv.visitTableSwitchInsn(0, fieldCount - 1, defaultLabel, labels);

					for (int i = 0; i < fieldCount; i++) {
						Field field = publicFields.get(i).field;

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
				classToAccess.put(type, access);
				return access;
			} catch (Exception ex) {
				throw new SerializationException("Error constructing ASM access class: " + accessClassName, ex);
			}
		}
	}

	static public abstract class Access {
		abstract public void set (Object object, int fieldIndex, Object value);

		abstract public Object get (Object object, int fieldIndex);
	}
}
