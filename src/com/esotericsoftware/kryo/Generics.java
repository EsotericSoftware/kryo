
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

/*** Helper class to map type name variables to concrete classes that are used during instantiation
 * 
 * @author Roman Levenstein <romixlev@gmail.com> */
public class Generics {
	private Map<String, Class> typeVar2class;

	private Generics parentScope;

	public Generics () {
		typeVar2class = new HashMap<String, Class>();
		parentScope = null;
	}

	public Generics (Map<String, Class> mappings) {
		typeVar2class = new HashMap<String, Class>(mappings);
		parentScope = null;
	}

	public Generics (Generics parentScope) {
		typeVar2class = new HashMap<String, Class>();
		this.parentScope = parentScope;
	}

	public void add (String typeVar, Class clazz) {
		typeVar2class.put(typeVar, clazz);
	}

	public Class getConcreteClass (String typeVar) {
		Class clazz = typeVar2class.get(typeVar);
		if (clazz == null && parentScope != null) return parentScope.getConcreteClass(typeVar);
		return clazz;
	}

	public void setParentScope (Generics scope) {
		if (parentScope != null) throw new IllegalStateException("Parent scope can be set just once");
		parentScope = scope;
	}

	public Generics getParentScope () {
		return parentScope;
	}
	
	public Map<String, Class> getMappings() {
		return typeVar2class;
	}

	public String toString () {
		return typeVar2class.toString();
	}

	public void resetParentScope () {
		parentScope = null;
	}
}
