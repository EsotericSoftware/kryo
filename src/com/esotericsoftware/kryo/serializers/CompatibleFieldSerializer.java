
package com.esotericsoftware.kryo.serializers;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.NotNull;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.Util;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.InputChunked;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.io.OutputChunked;
import com.esotericsoftware.kryo.serializers.FieldSerializer.CachedField;
import com.esotericsoftware.kryo.serializers.FieldSerializer.Optional;
import com.esotericsoftware.kryo.util.ObjectMap;
import com.esotericsoftware.reflectasm.FieldAccess;

import static com.esotericsoftware.minlog.Log.*;

/** Serializes objects using direct field assignment, with limited support for forward and backward compatibility. Fields can be
 * added or removed without invalidating previously serialized bytes. Note that changing the type of a field is not supported.
 * <p>
 * There is additional overhead compared to {@link FieldSerializer}. A header is output the first time an object of a given type
 * is serialized. The header consists of an int for the number of fields, then a String for each field name. Also, to support
 * skipping the bytes for a field that no longer exists, for each field value an int is written that is the length of the value in
 * bytes.
 * @author Nathan Sweet <misc@n4te.com> */
public class CompatibleFieldSerializer<T> extends FieldSerializer<T> {
	public CompatibleFieldSerializer (Kryo kryo, Class type) {
		super(kryo, type);
	}

	public void write (Kryo kryo, Output output, T object) {
		CachedField[] fields = getFields();
		ObjectMap context = kryo.getGraphContext();
		if (!context.containsKey(this)) {
			context.put(this, null);
			if (TRACE) trace("kryo", "Write " + fields.length + " field names.");
			output.writeInt(fields.length, true);
			for (int i = 0, n = fields.length; i < n; i++)
				output.writeString(fields[i].field.getName());
		}

		OutputChunked outputChunked = new OutputChunked(output, 1024);
		for (int i = 0, n = fields.length; i < n; i++) {
			CachedField cachedField = fields[i];
			try {
				if (TRACE) trace("kryo", "Write field: " + cachedField + " (" + object.getClass().getName() + ")");

				Object value = cachedField.get(object);
				if (value == null) {
					kryo.writeClass(outputChunked, null);
					outputChunked.endChunks();
					continue;
				}

				Serializer serializer = cachedField.serializer;
				if (cachedField.fieldClass == null) {
					Registration registration = kryo.writeClass(outputChunked, value.getClass());
					if (serializer == null) serializer = registration.getSerializer();
					kryo.writeObject(outputChunked, value, serializer);
				} else {
					if (serializer == null)
						cachedField.serializer = serializer = kryo.getRegistration(cachedField.fieldClass).getSerializer();
					if (!cachedField.canBeNull)
						kryo.writeObject(outputChunked, value, serializer);
					else
						kryo.writeObjectOrNull(outputChunked, value, serializer);
				}

				outputChunked.endChunks();
			} catch (IllegalAccessException ex) {
				throw new KryoException("Error accessing field in class: " + object.getClass().getName(), ex);
			} catch (KryoException ex) {
				ex.addTrace(cachedField + " (" + object.getClass().getName() + ")");
				throw ex;
			} catch (RuntimeException runtimeEx) {
				KryoException ex = new KryoException(runtimeEx);
				ex.addTrace(cachedField + " (" + object.getClass().getName() + ")");
				throw ex;
			}
		}
	}

	public void read (Kryo kryo, Input input, T object) {
		ObjectMap context = kryo.getGraphContext();
		CachedField[] fields = (CachedField[])context.get(this);
		if (fields == null) {
			int length = input.readInt(true);
			if (TRACE) trace("kryo", "Read " + length + " field names.");
			String[] names = new String[length];
			for (int i = 0; i < length; i++)
				names[i] = input.readString();

			fields = new CachedField[length];
			CachedField[] allFields = getFields();
			outer:
			for (int i = 0, n = names.length; i < n; i++) {
				String schemaName = names[i];
				for (int ii = 0, nn = allFields.length; ii < nn; ii++) {
					if (allFields[ii].field.getName().equals(schemaName)) {
						fields[i] = allFields[ii];
						continue outer;
					}
				}
				if (TRACE) trace("kryo", "Ignore obsolete field: " + schemaName);
			}
			context.put(this, fields);
		}

		InputChunked inputChunked = new InputChunked(input, 1024);
		for (int i = 0, n = fields.length; i < n; i++) {
			CachedField cachedField = fields[i];
			try {
				if (cachedField == null) {
					if (TRACE) trace("kryo", "Skip obsolete field.");
					inputChunked.nextChunks();
					continue;
				}

				if (TRACE) trace("kryo", "Read field: " + cachedField + " (" + getType().getName() + ")");

				Object value;

				Class concreteType = cachedField.fieldClass;
				Serializer serializer = cachedField.serializer;
				if (concreteType == null) {
					Registration registration = kryo.readClass(inputChunked);
					if (registration == null)
						value = null;
					else {
						concreteType = registration.getType();
						if (serializer == null) serializer = registration.getSerializer();
						value = kryo.readObject(inputChunked, concreteType, serializer);
					}
				} else {
					if (serializer == null) cachedField.serializer = serializer = kryo.getRegistration(concreteType).getSerializer();
					if (!cachedField.canBeNull)
						value = kryo.readObject(inputChunked, concreteType, serializer);
					else
						value = kryo.readObjectOrNull(inputChunked, concreteType, serializer);
				}

				cachedField.set(object, value);

				inputChunked.nextChunks();
			} catch (IllegalAccessException ex) {
				throw new KryoException("Error accessing field in class: " + getType().getName(), ex);
			} catch (KryoException ex) {
				ex.addTrace(cachedField + " (" + getType().getName() + ")");
				throw ex;
			} catch (RuntimeException runtimeEx) {
				KryoException ex = new KryoException(runtimeEx);
				ex.addTrace(cachedField + " (" + getType().getName() + ")");
				throw ex;
			}
		}
	}
}
