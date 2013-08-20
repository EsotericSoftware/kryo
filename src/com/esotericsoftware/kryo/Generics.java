package com.esotericsoftware.kryo;

import static com.esotericsoftware.minlog.Log.TRACE;
import static com.esotericsoftware.minlog.Log.trace;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;

/***
 * Helper class to map type name variables to concrete classes that are used during instantiation

 * @author Roman Levenstein <romixlev@gmail.com>
 *
 */
public class Generics {
	private Map<String, Class> typeVar2class;

	private Generics parentScope;
	
	public Generics() {
		typeVar2class = new HashMap<String, Class>();
		parentScope = null;
	}
	
	public Generics(Map<String, Class> mappings) {
		typeVar2class = new HashMap<String, Class>(mappings);
		parentScope = null;
	}
	
	public Generics(Generics parentScope) {
		typeVar2class = new HashMap<String, Class>();
		this.parentScope = parentScope;
	}
	
	public void add(String typeVar, Class clazz) {
		typeVar2class.put(typeVar, clazz);
	}
	
	public Class getConcreteClass(String typeVar) {
		Class clazz = typeVar2class.get(typeVar);
		if(clazz == null && parentScope != null)
			return parentScope.getConcreteClass(typeVar);
		return clazz;
	}

	public void setParentScope(Generics scope) {
		if(parentScope != null)
			throw new RuntimeException("Parent scope can be set just once");
		parentScope = scope;
	}

	public Generics getParentScope() {
		return parentScope;
	}
	
	public String toString() {
		return typeVar2class.toString();
	}

	public void resetParentScope() {
		parentScope = null;
	}
	
	/** Returns the first level of classes or interfaces for a generic type.
	 * @return null if the specified type is not generic or its generic types are not classes. */
	public static Class[] getGenerics (Type genericType, Kryo kryo) {
		if (genericType instanceof GenericArrayType) {
			Type componentType = ((GenericArrayType)genericType).getGenericComponentType();
			if (componentType instanceof Class)
				return new Class[] {(Class)componentType};
			else
				return getGenerics(componentType, kryo);
		}
		if (!(genericType instanceof ParameterizedType)) return null;
		if (TRACE) trace("kryo", "Processing generic type " + genericType);
		Type[] actualTypes = ((ParameterizedType)genericType).getActualTypeArguments();
		Class[] generics = new Class[actualTypes.length];
		int count = 0;
		for (int i = 0, n = actualTypes.length; i < n; i++) {
			Type actualType = actualTypes[i];
			if (TRACE) trace("kryo", "Processing actual type " + actualType + " (" + actualType.getClass().getName() + ")");
			generics[i] = Object.class;
			if (actualType instanceof Class)
				generics[i] = (Class)actualType;
			else if (actualType instanceof ParameterizedType)
				generics[i] = (Class)((ParameterizedType)actualType).getRawType();
			else if (actualType instanceof TypeVariable) {
				Generics scope = kryo.getGenericsScope();
				if (scope != null) {
					Class clazz = scope.getConcreteClass(((TypeVariable)actualType).getName());
					if (clazz != null) {
						generics[i] = clazz;
					} else
						continue;
				} else
					continue;
			} else if (actualType instanceof GenericArrayType) {
				Type componentType = ((GenericArrayType)actualType).getGenericComponentType();
				if (componentType instanceof Class)
					generics[i] = Array.newInstance((Class)componentType, 0).getClass();
				else if (componentType instanceof TypeVariable) {
					Generics scope = kryo.getGenericsScope();
					if (scope != null) {
						Class clazz = scope.getConcreteClass(((TypeVariable)componentType).getName());
						if (clazz != null) {
							generics[i] = Array.newInstance(clazz, 0).getClass();
						}
					}
				} else {
					Class[] componentGenerics = getGenerics(componentType, kryo);
					if (componentGenerics != null) generics[i] = componentGenerics[0];
				}
			} else
				continue;
			count++;
		}
		if (count == 0) return null;
		return generics;
	}
}
