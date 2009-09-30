
package com.esotericsoftware.kryo.serialize;

import static com.esotericsoftware.minlog.Log.*;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.SerializationException;
import com.esotericsoftware.kryo.Serializer;

/**
 * Serializes Java beans using bean accessor methods. Only bean properties with both a getter and setter are serialized. This
 * class is not as fast as {@link FieldSerializer} but is much faster and more efficient than Java serialization.
 * <p>
 * BeanSerializer does not write header data, only the object data is stored. If the type of a bean property is not final (note
 * primitives are final) then an extra byte is written for that property.
 * @see Serializer
 * @see Kryo#register(Class, Serializer)
 * @author Nathan Sweet <misc@n4te.com>
 */
public class BeanSerializer extends Serializer {
	static private final HashMap<Class, CachedMethod[]> setterMethodCache = new HashMap();
	static private final HashMap<Class, CachedMethod[]> getterMethodCache = new HashMap();

	private final Kryo kryo;

	public BeanSerializer (Kryo kryo) {
		this.kryo = kryo;
	}

	/**
	 * Stores the getter and setter methods for each bean property in the specified class.
	 */
	private void cache (Class type) {
		BeanInfo info;
		try {
			info = Introspector.getBeanInfo(type);
		} catch (IntrospectionException ex) {
			throw new SerializationException("Error getting bean info.", ex);
		}
		// Methods are sorted by alpha so the order of the data is known.
		PropertyDescriptor[] descriptors = info.getPropertyDescriptors();
		Arrays.sort(descriptors, new Comparator<PropertyDescriptor>() {
			public int compare (PropertyDescriptor o1, PropertyDescriptor o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		ArrayList<CachedMethod> getterMethods = new ArrayList(descriptors.length);
		ArrayList<CachedMethod> setterMethods = new ArrayList(descriptors.length);
		for (int i = 0, n = descriptors.length; i < n; i++) {
			PropertyDescriptor property = descriptors[i];
			if (property.getName().equals("class")) continue;
			Method getMethod = property.getReadMethod();
			Method setMethod = property.getWriteMethod();
			// Require both a getter and setter.
			if (getMethod == null || setMethod == null) continue;
			System.out.println(getMethod.getName());

			// Always use the same serializer for this property if the properties's class is final.
			Serializer serializer = null;
			Class returnType = getMethod.getReturnType();
			if (Modifier.isFinal(returnType.getModifiers())) serializer = kryo.getRegisteredClass(returnType).serializer;

			CachedMethod cachedGetMethod = new CachedMethod();
			cachedGetMethod.method = getMethod;
			cachedGetMethod.serializer = serializer;
			getterMethods.add(cachedGetMethod);

			CachedMethod cachedSetMethod = new CachedMethod();
			cachedSetMethod.method = setMethod;
			cachedSetMethod.serializer = serializer;
			cachedSetMethod.type = setMethod.getParameterTypes()[0];
			setterMethods.add(cachedSetMethod);
		}
		getterMethodCache.put(type, getterMethods.toArray(new CachedMethod[getterMethods.size()]));
		setterMethodCache.put(type, setterMethods.toArray(new CachedMethod[setterMethods.size()]));
	}

	private CachedMethod[] getGetterMethods (Class type) {
		CachedMethod[] getterMethods = getterMethodCache.get(type);
		if (getterMethods != null) return getterMethods;
		cache(type);
		return getterMethodCache.get(type);
	}

	private CachedMethod[] getSetterMethods (Class type) {
		CachedMethod[] setterMethods = setterMethodCache.get(type);
		if (setterMethods != null) return setterMethods;
		cache(type);
		return setterMethodCache.get(type);
	}

	public void writeObjectData (ByteBuffer buffer, Object object) {
		Class type = object.getClass();
		Object[] noArgs = {};
		try {
			CachedMethod[] getterMethods = getGetterMethods(type);
			for (int i = 0, n = getterMethods.length; i < n; i++) {
				CachedMethod cachedMethod = getterMethods[i];
				if (TRACE) trace("kryo", "Writing property: " + cachedMethod + "(" + object.getClass() + ")");
				Object value = cachedMethod.method.invoke(object, noArgs);
				Serializer serializer = cachedMethod.serializer;
				if (serializer != null)
					serializer.writeObject(buffer, value);
				else
					kryo.writeClassAndObject(buffer, value);
			}
		} catch (IllegalAccessException ex) {
			throw new SerializationException("Error accessing getter method in class: " + type.getName(), ex);
		} catch (InvocationTargetException ex) {
			throw new SerializationException("Error invoking getter method in class: " + type.getName(), ex);
		}
		if (TRACE) trace("kryo", "Wrote bean: " + object);
	}

	public <T> T readObjectData (ByteBuffer buffer, Class<T> type) {
		T object = newInstance(type);
		try {
			CachedMethod[] setterMethods = getSetterMethods(object.getClass());
			for (int i = 0, n = setterMethods.length; i < n; i++) {
				CachedMethod cachedMethod = setterMethods[i];
				if (TRACE) trace("kryo", "Reading property: " + cachedMethod + "(" + object.getClass() + ")");
				Object value;
				Serializer serializer = cachedMethod.serializer;
				if (serializer != null)
					value = serializer.readObject(buffer, cachedMethod.type);
				else
					value = kryo.readClassAndObject(buffer);
				cachedMethod.method.invoke(object, new Object[] {value});
			}
		} catch (IllegalAccessException ex) {
			throw new SerializationException("Error accessing setter method in class: " + type.getName(), ex);
		} catch (InvocationTargetException ex) {
			throw new SerializationException("Error invoking setter method in class: " + type.getName(), ex);
		}
		if (TRACE) trace("kryo", "Read bean: " + object);
		return object;
	}

	static class CachedMethod {
		public Method method;
		public Serializer serializer;
		public Class type; // Only populated for setter methods.

		public String toString () {
			return method.getName();
		}
	}
}
