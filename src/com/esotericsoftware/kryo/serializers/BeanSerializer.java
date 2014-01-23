
package com.esotericsoftware.kryo.serializers;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.reflectasm.MethodAccess;

import static com.esotericsoftware.minlog.Log.*;

/** Serializes Java beans using bean accessor methods. Only bean properties with both a getter and setter are serialized. This
 * class is not as fast as {@link FieldSerializer} but is much faster and more efficient than Java serialization. Bytecode
 * generation is used to invoke the bean propert methods, if possible.
 * <p>
 * BeanSerializer does not write header data, only the object data is stored. If the type of a bean property is not final (note
 * primitives are final) then an extra byte is written for that property.
 * @see Serializer
 * @see Kryo#register(Class, Serializer)
 * @author Nathan Sweet <misc@n4te.com> */
public class BeanSerializer<T> extends Serializer<T> {
	static final Object[] noArgs = {};
	private CachedProperty[] properties;
	Object access;

	public BeanSerializer (Kryo kryo, Class type) {
		BeanInfo info;
		try {
			info = Introspector.getBeanInfo(type);
		} catch (IntrospectionException ex) {
			throw new KryoException("Error getting bean info.", ex);
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
			if (kryo.isFinal(returnType)) serializer = kryo.getRegistration(returnType).getSerializer();

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
				property.getterAccessIndex = ((MethodAccess)access).getIndex(property.getMethod.getName(),
					property.getMethod.getParameterTypes());
				property.setterAccessIndex = ((MethodAccess)access).getIndex(property.setMethod.getName(),
					property.setMethod.getParameterTypes());
			}
		} catch (Throwable ignored) {
			// ReflectASM is not available on Android.
		}
	}

	public void write (Kryo kryo, Output output, T object) {
		Class type = object.getClass();
		for (int i = 0, n = properties.length; i < n; i++) {
			CachedProperty property = properties[i];
			try {
				if (TRACE) trace("kryo", "Write property: " + property + " (" + type.getName() + ")");
				Object value = property.get(object);
				Serializer serializer = property.serializer;
				if (serializer != null)
					kryo.writeObjectOrNull(output, value, serializer);
				else
					kryo.writeClassAndObject(output, value);
			} catch (IllegalAccessException ex) {
				throw new KryoException("Error accessing getter method: " + property + " (" + type.getName() + ")", ex);
			} catch (InvocationTargetException ex) {
				throw new KryoException("Error invoking getter method: " + property + " (" + type.getName() + ")", ex);
			} catch (KryoException ex) {
				ex.addTrace(property + " (" + type.getName() + ")");
				throw ex;
			} catch (RuntimeException runtimeEx) {
				KryoException ex = new KryoException(runtimeEx);
				ex.addTrace(property + " (" + type.getName() + ")");
				throw ex;
			}
		}
	}

	public T read (Kryo kryo, Input input, Class<T> type) {
		T object = kryo.newInstance(type);
		kryo.reference(object);
		for (int i = 0, n = properties.length; i < n; i++) {
			CachedProperty property = properties[i];
			try {
				if (TRACE) trace("kryo", "Read property: " + property + " (" + object.getClass() + ")");
				Object value;
				Serializer serializer = property.serializer;
				if (serializer != null)
					value = kryo.readObjectOrNull(input, property.setMethodType, serializer);
				else
					value = kryo.readClassAndObject(input);
				property.set(object, value);
			} catch (IllegalAccessException ex) {
				throw new KryoException("Error accessing setter method: " + property + " (" + object.getClass().getName() + ")", ex);
			} catch (InvocationTargetException ex) {
				throw new KryoException("Error invoking setter method: " + property + " (" + object.getClass().getName() + ")", ex);
			} catch (KryoException ex) {
				ex.addTrace(property + " (" + object.getClass().getName() + ")");
				throw ex;
			} catch (RuntimeException runtimeEx) {
				KryoException ex = new KryoException(runtimeEx);
				ex.addTrace(property + " (" + object.getClass().getName() + ")");
				throw ex;
			}
		}
		return object;
	}

	public T copy (Kryo kryo, T original) {
		T copy = (T)kryo.newInstance(original.getClass());
		for (int i = 0, n = properties.length; i < n; i++) {
			CachedProperty property = properties[i];
			try {
				Object value = property.get(original);
				property.set(copy, value);
			} catch (KryoException ex) {
				ex.addTrace(property + " (" + copy.getClass().getName() + ")");
				throw ex;
			} catch (RuntimeException runtimeEx) {
				KryoException ex = new KryoException(runtimeEx);
				ex.addTrace(property + " (" + copy.getClass().getName() + ")");
				throw ex;
			} catch (Exception ex) {
				throw new KryoException("Error copying bean property: " + property + " (" + copy.getClass().getName() + ")", ex);
			}
		}
		return copy;
	}

	class CachedProperty<X> {
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
