/* Copyright (c) 2008-2026, Nathan Sweet
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 * - Neither the name of Esoteric Software nor the names of its contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. */

package com.esotericsoftware.kryo.serializers;

import static com.esotericsoftware.kryo.util.Log.*;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.Util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Comparator;

/** Serializer for record classes.
 * @author Julia Boes {@literal <julia.boes@oracle.com>}
 * @author Chris Hegarty {@literal <chris.hegarty@oracle.com>}
 * @deprecated FieldSerializer and its subclasses serialize records by default and are faster. Use this serializer only to read
 *             records written by Kryo 5, see {@link com.esotericsoftware.kryo.Kryo5Compatibility}. */
@Deprecated
public class RecordSerializer<T> extends ImmutableSerializer<T> {
	private static final ClassValue<Constructor<?>> CONSTRUCTOR = new ClassValue<Constructor<?>>() {
		protected Constructor<?> computeValue (Class<?> type) {
			return getCanonicalConstructor(type);
		}
	};
	private static final ClassValue<Component[]> COMPONENTS = new ClassValue<Component[]>() {
		protected Component[] computeValue (Class<?> type) {
			return components(type);
		}
	};

	private boolean fixedFieldTypes = false;

	public RecordSerializer (Class<T> clazz) {
		if (!Util.isRecord(clazz)) throw new KryoException(clazz + " is not a record");
	}

	@Override
	public void write (Kryo kryo, Output output, T object) {
		for (Component component : COMPONENTS.get(object.getClass())) {
			final Class<?> type = component.type;
			final String name = component.name;
			try {
				if (TRACE) trace("kryo", "Write property: " + name + " (" + type.getName() + ")");
				if (type.isPrimitive()) {
					kryo.writeObject(output, component.getValue(object));
				} else {
					if (fixedFieldTypes || kryo.isFinal(type)) {
						kryo.writeObjectOrNull(output, component.getValue(object), type);
					} else {
						kryo.writeClassAndObject(output, component.getValue(object));
					}
				}
			} catch (KryoException ex) {
				ex.addTrace(name + " (" + type.getName() + ")");
				throw ex;
			} catch (Throwable t) {
				KryoException ex = new KryoException(t);
				ex.addTrace(name + " (" + type.getName() + ")");
				throw ex;
			}
		}
	}

	@Override
	public T read (Kryo kryo, Input input, Class<? extends T> type) {
		final Component[] components = COMPONENTS.get(type);
		final Object[] values = new Object[components.length];
		for (Component component : components) {
			final String name = component.name;
			final Class<?> componentType = component.type;
			try {
				if (TRACE) trace("kryo", "Read property: " + name + " (" + type.getName() + ")");
				// Populate values in the order required by the canonical constructor
				if (componentType.isPrimitive()) {
					values[component.index] = kryo.readObject(input, componentType);
				} else {
					if (fixedFieldTypes || kryo.isFinal(componentType)) {
						values[component.index] = kryo.readObjectOrNull(input, componentType);
					} else {
						values[component.index] = kryo.readClassAndObject(input);
					}
				}
			} catch (KryoException ex) {
				ex.addTrace(name + " (" + type.getName() + ")");
				throw ex;
			} catch (Throwable t) {
				KryoException ex = new KryoException(t);
				ex.addTrace(name + " (" + type.getName() + ")");
				throw ex;
			}
		}
		return invokeCanonicalConstructor(type, values);
	}

	/** A record component with its index in the canonical constructor and its accessor. */
	private static final class Component {
		final String name;
		final Class<?> type;
		final int index;
		private final Method accessor;

		Component (RecordComponent component, int index) {
			name = component.getName();
			type = component.getType();
			this.index = index;
			accessor = component.getAccessor();
			try {
				accessor.setAccessible(true);
			} catch (Exception t) {
				KryoException ex = new KryoException(t);
				ex.addTrace("Could not retrieve record component accessor (" + component.getDeclaringRecord().getName() + ")");
				throw ex;
			}
		}

		Object getValue (Object record) {
			try {
				return accessor.invoke(record);
			} catch (Exception t) {
				KryoException ex = new KryoException(t);
				ex.addTrace("Could not retrieve record component value (" + record.getClass().getName() + ")");
				throw ex;
			}
		}
	}

	/** Returns the components of the given record class, sorted by name. The data is written in this order. */
	private static Component[] components (Class<?> type) {
		RecordComponent[] recordComponents = type.getRecordComponents();
		if (recordComponents == null) throw new KryoException("Not a record: " + type.getName());
		Component[] components = new Component[recordComponents.length];
		for (int i = 0; i < recordComponents.length; i++)
			components[i] = new Component(recordComponents[i], i);
		Arrays.sort(components, Comparator.comparing(component -> component.name));
		return components;
	}

	/** Invokes the canonical constructor of a record class with the given argument values. */
	private T invokeCanonicalConstructor (Class<? extends T> recordType, Object[] args) {
		try {
			return (T)CONSTRUCTOR.get(recordType).newInstance(args);
		} catch (Throwable t) {
			KryoException ex = new KryoException(t);
			ex.addTrace("Could not construct type (" + recordType.getName() + ")");
			throw ex;
		}
	}

	private static Constructor<?> getCanonicalConstructor (Class<?> recordType) {
		try {
			Class<?>[] paramTypes = Arrays.stream(recordType.getRecordComponents())
				.map(RecordComponent::getType)
				.toArray(Class<?>[]::new);
			Constructor<?> canonicalConstructor = recordType.getDeclaredConstructor(paramTypes);
			canonicalConstructor.setAccessible(true);
			return canonicalConstructor;
		} catch (Throwable t) {
			KryoException ex = new KryoException(t);
			ex.addTrace("Could not retrieve record canonical constructor (" + recordType.getName() + ")");
			throw ex;
		}
	}

	/** Tells the RecordSerializer that all field types are effectively final. This allows the serializer to be more efficient,
	 * since it knows field values will not be a subclass of their declared type. Default is false. */
	public void setFixedFieldTypes (boolean fixedFieldTypes) {
		this.fixedFieldTypes = fixedFieldTypes;
	}
}
