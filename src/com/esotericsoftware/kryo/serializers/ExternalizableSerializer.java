package com.esotericsoftware.kryo.serializers;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.KryoObjectInput;
import com.esotericsoftware.kryo.io.KryoObjectOutput;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.ObjectMap;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Method;

/**
 * Writes using the objects externalizable interface if it can reliably do so. Typically, a object can
 * be efficiently written with Kryo and Java's externalizable interface. However, there may be behavior
 * problems if the class uses either the 'readResolve' or 'writeReplace' methods. We will fall back onto
 * the standard {@link JavaSerializer} if we detect either of these methods.
 * <p/>
 * Note that this class does not specialize the type on {@code Externalizable}. That is because if
 * we fall back on the {@code JavaSerializer} it may have an {@code readResolve} method that returns
 * an object of a different type.
 *
 * @author Robert DiFalco <robert.difalco@gmail.com>
 */
public class ExternalizableSerializer extends Serializer {

	 private ObjectMap<Class, JavaSerializer> javaSerializerByType;

	 private KryoObjectInput objectInput = null;
	 private KryoObjectOutput objectOutput = null;

	 @Override
	 public void write (Kryo kryo, Output output, Object object) {
		  JavaSerializer serializer = getJavaSerializerIfRequired(object.getClass());
		  if (serializer == null) {
				writeExternal(kryo, output, object);
		  } else {
				serializer.write(kryo, output, object);
		  }
	 }

	 @Override
	 public Object read (Kryo kryo, Input input, Class type) {
		  JavaSerializer serializer = getJavaSerializerIfRequired(type);
		  if (serializer == null) {
				return readExternal(kryo, input, type);
		  } else {
				return serializer.read(kryo, input, type);
		  }
	 }

	 private void writeExternal (Kryo kryo, Output output, Object object) {
		  try {
				((Externalizable)object).writeExternal(getObjectOutput(kryo, output));
		  } catch (ClassCastException e) {
				throw new KryoException(e);
		  } catch (IOException e) {
				throw new KryoException(e);
		  }
	 }

	 private Object readExternal (Kryo kryo, Input input, Class type) {
		  try {
				Externalizable object = (Externalizable)type.newInstance();
				object.readExternal(getObjectInput(kryo, input));
				return object;
		  } catch (ClassCastException e) {
				throw new KryoException(e);
		  } catch (ClassNotFoundException e) {
				throw new KryoException(e);
		  } catch (IOException e) {
				throw new KryoException(e);
		  } catch (InstantiationException e) {
				throw new KryoException(e);
		  } catch (IllegalAccessException e) {
				throw new KryoException(e);
		  }
	 }

	 @SuppressWarnings("unchecked")
	 private ObjectOutput getObjectOutput (Kryo kryo, Output output) {
		  if (objectOutput == null) {
				objectOutput = new KryoObjectOutput(kryo, output);
		  } else {
				objectOutput.setOutput(output);
		  }

		  return objectOutput;
	 }

	 @SuppressWarnings("unchecked")
	 private ObjectInput getObjectInput (Kryo kryo, Input input) {
		  if (objectInput == null) {
				objectInput = new KryoObjectInput(kryo, input);
		  } else {
				objectInput.setInput(input);
		  }

		  return objectInput;
	 }

	 /**
	  * Determines if this class requires the fall-back {@code JavaSerializer}. If the class does
	  * not require any specialized Java serialization features then null will be returned.
	  *
	  * @param type the type we wish to externalize
	  * @return a {@code JavaSerializer} if the type requires more than simple externalization.
	  */
	 private JavaSerializer getJavaSerializerIfRequired (Class type) {
		  JavaSerializer javaSerializer = getCachedSerializer(type);
		  if (javaSerializer == null && isJavaSerializerRequired(type)) {
				javaSerializer = new JavaSerializer();
		  }

		  return javaSerializer;
	 }

	 private JavaSerializer getCachedSerializer (Class type) {
		  if (javaSerializerByType == null) {
				javaSerializerByType = new ObjectMap<Class, JavaSerializer>();
				return null;
		  }

		  return javaSerializerByType.get(type);
	 }

	 private boolean isJavaSerializerRequired (Class type) {
		  return (hasInheritableReplaceMethod(type, "writeReplace")
			  || hasInheritableReplaceMethod(type, "readResolve"));
	 }

	 /* find out if there are any pesky serialization extras on this class */
	 private static boolean hasInheritableReplaceMethod (Class type, String methodName) {
		  Method method = null;
		  Class<?> current = type;
		  while (current != null) {
				try {
					 method = current.getDeclaredMethod(methodName);
					 break;
				} catch (NoSuchMethodException ex) {
					 current = current.getSuperclass();
				}
		  }

		  return ((method != null) && (method.getReturnType() == Object.class));
	 }
}
