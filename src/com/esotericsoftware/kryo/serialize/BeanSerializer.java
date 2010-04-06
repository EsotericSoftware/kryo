
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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.SerializationException;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.reflectasm.MethodAccess;

/**
 * Serializes Java beans using bean accessor methods. Only bean properties with both a getter and setter are serialized. This
 * class is not as fast as {@link FieldSerializer} but is much faster and more efficient than Java serialization. Bytecode
 * generation is used to invoke the bean propert methods, if possible.
 * <p>
 * BeanSerializer does not write header data, only the object data is stored. If the type of a bean property is not final (note
 * primitives are final) then an extra byte is written for that property.
 * @see Serializer
 * @see Kryo#register(Class, Serializer)
 * @author Nathan Sweet <misc@n4te.com>
 */
public class BeanSerializer extends Serializer {
	static final Object[] noArgs = {};

	private final Kryo kryo;
	private CachedProperty[] properties;
	Object access;

	public BeanSerializer (Kryo kryo, Class type) {
		this.kryo = kryo;

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
		ArrayList<CachedProperty> cachedProperties = new ArrayList(descriptors.length);
		for (int i = 0, n = descriptors.length; i < n; i++) {
			PropertyDescriptor property = descriptors[i];
			String name = property.getName();
			if (name.equals("class")) continue;
			Method getMethod = property.getReadMethod();
			Method setMethod = property.getWriteMethod();
			if (getMethod == null || setMethod == null) continue; // Require both a getter and setter.

			// Always use the same serializer for this property if the properties' class is final.
			Serializer serializer = null;
			Class returnType = getMethod.getReturnType();
			if (Modifier.isFinal(returnType.getModifiers())) serializer = kryo.getRegisteredClass(returnType).getSerializer();

			CachedProperty cachedProperty = new CachedProperty();
			cachedProperty.name = name;
			cachedProperty.getMethod = getMethod;
			cachedProperty.setMethod = setMethod;
			cachedProperty.serializer = serializer;
			cachedProperty.setMethodType = setMethod.getParameterTypes()[0];
			cachedProperties.add(cachedProperty);
		}

		properties = cachedProperties.toArray(new CachedProperty[cachedProperties.size()]);

		try {
			access = MethodAccess.get(type);
			for (int i = 0, n = properties.length; i < n; i++) {
				CachedProperty property = properties[i];
				property.getterAccessIndex = ((MethodAccess)access).getIndex(property.getMethod.getName());
				property.setterAccessIndex = ((MethodAccess)access).getIndex(property.setMethod.getName());
			}
		} catch (Throwable ignored) {
			// ReflectASM is not available on Android.
		}
	}

	public void writeObjectData (ByteBuffer buffer, Object object) {
		Class type = object.getClass();
		try {
			for (int i = 0, n = properties.length; i < n; i++) {
				CachedProperty property = properties[i];
				if (TRACE) trace("kryo", "Writing property: " + property + " (" + object.getClass() + ")");
				Object value = property.get(object);
				Serializer serializer = property.serializer;
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
		return readObjectData(newInstance(kryo, type), buffer, type);
	}

	protected <T> T readObjectData (T object, ByteBuffer buffer, Class<T> type) {
		try {
			for (int i = 0, n = properties.length; i < n; i++) {
				CachedProperty property = properties[i];
				if (TRACE) trace("kryo", "Reading property: " + property + " (" + object.getClass() + ")");
				Object value;
				Serializer serializer = property.serializer;
				if (serializer != null)
					value = serializer.readObject(buffer, property.setMethodType);
				else
					value = kryo.readClassAndObject(buffer);
				property.set(object, value);
			}
		} catch (IllegalAccessException ex) {
			throw new SerializationException("Error accessing setter method in class: " + type.getName(), ex);
		} catch (InvocationTargetException ex) {
			throw new SerializationException("Error invoking setter method in class: " + type.getName(), ex);
		}
		if (TRACE) trace("kryo", "Read bean: " + object);
		return object;
	}

	class CachedProperty {
		String name;
		Method getMethod, setMethod;
		Class setMethodType;
		Serializer serializer;
		int getterAccessIndex, setterAccessIndex;

		public String toString () {
			return name;
		}

		Object get (Object object) throws IllegalAccessException, InvocationTargetException {
			if (access != null) return ((MethodAccess)access).invoke(object, getterAccessIndex);
			return getMethod.invoke(object, noArgs);
		}

		void set (Object object, Object value) throws IllegalAccessException, InvocationTargetException {
			if (access != null) {
				((MethodAccess)access).invoke(object, setterAccessIndex, value);
				return;
			}
			setMethod.invoke(object, new Object[] {value});
		}
	}
}
