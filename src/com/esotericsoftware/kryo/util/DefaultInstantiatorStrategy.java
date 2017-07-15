
package com.esotericsoftware.kryo.util;

import static com.esotericsoftware.kryo.util.Util.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import org.objenesis.instantiator.ObjectInstantiator;
import org.objenesis.strategy.InstantiatorStrategy;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.reflectasm.ConstructorAccess;

public class DefaultInstantiatorStrategy implements org.objenesis.strategy.InstantiatorStrategy {
	private InstantiatorStrategy fallbackStrategy;

	public DefaultInstantiatorStrategy () {
	}

	public DefaultInstantiatorStrategy (InstantiatorStrategy fallbackStrategy) {
		this.fallbackStrategy = fallbackStrategy;
	}

	public void setFallbackInstantiatorStrategy (final InstantiatorStrategy fallbackStrategy) {
		this.fallbackStrategy = fallbackStrategy;
	}

	public InstantiatorStrategy getFallbackInstantiatorStrategy () {
		return fallbackStrategy;
	}

	public ObjectInstantiator newInstantiatorOf (final Class type) {
		if (!Util.isAndroid) {
			// Use ReflectASM if the class is not a non-static member class.
			Class enclosingType = type.getEnclosingClass();
			boolean isNonStaticMemberClass = enclosingType != null && type.isMemberClass()
				&& !Modifier.isStatic(type.getModifiers());
			if (!isNonStaticMemberClass) {
				try {
					final ConstructorAccess access = ConstructorAccess.get(type);
					return new ObjectInstantiator() {
						public Object newInstance () {
							try {
								return access.newInstance();
							} catch (Exception ex) {
								throw new KryoException("Error constructing instance of class: " + className(type), ex);
							}
						}
					};
				} catch (Exception ignored) {
				}
			}
		}

		// Reflection.
		try {
			Constructor ctor;
			try {
				ctor = type.getConstructor((Class[])null);
			} catch (Exception ex) {
				ctor = type.getDeclaredConstructor((Class[])null);
				ctor.setAccessible(true);
			}
			final Constructor constructor = ctor;
			return new ObjectInstantiator() {
				public Object newInstance () {
					try {
						return constructor.newInstance();
					} catch (Exception ex) {
						throw new KryoException("Error constructing instance of class: " + className(type), ex);
					}
				}
			};
		} catch (Exception ignored) {
		}

		if (fallbackStrategy == null) {
			if (type.isMemberClass() && !Modifier.isStatic(type.getModifiers()))
				throw new KryoException("Class cannot be created (non-static member class): " + className(type));
			else {
				StringBuilder message = new StringBuilder("Class cannot be created (missing no-arg constructor): " + className(type));
				if (type.getSimpleName().equals("")) {
					message.append("\nThis is an anonymous class, which is not serializable by default in Kryo. Possible solutions:\n")
						.append("1. Remove uses of anonymous classes, including double brace initialization, from the containing\n")
						.append(
							"class. This is the safest solution, as anonymous classes don't have predictable names for serialization.\n")
						.append("2. Register a FieldSerializer for the containing class and call FieldSerializer\n")
						.append("setIgnoreSyntheticFields(false) on it. This is not safe but may be sufficient temporarily.");
				}
				throw new KryoException(message.toString());
			}
		}
		// InstantiatorStrategy.
		return fallbackStrategy.newInstantiatorOf(type);
	}
}
