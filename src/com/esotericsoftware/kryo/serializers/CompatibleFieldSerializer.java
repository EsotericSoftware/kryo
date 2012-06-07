
package com.esotericsoftware.kryo.serializers;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.InputChunked;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.io.OutputChunked;
import com.esotericsoftware.kryo.util.ObjectMap;

import static com.esotericsoftware.minlog.Log.*;

/** Serializes objects using direct field assignment, with limited support for forward and backward compatibility. Fields can be
 * added or removed without invalidating previously serialized bytes. Note that changing the type of a field is not supported.
 * <p>
 * There is additional overhead compared to {@link FieldSerializer}. A header is output the first time an object of a given type
 * is serialized. The header consists of an int for the number of fields, then a String for each field name. Also, to support
 * skipping the bytes for a field that no longer exists, for each field value an int is written that is the length of the value in
 * bytes.
 * <p>
 * Note that the field data is identified by name. The situation where a super class has a field with the same name as a subclass
 * must be avoided.
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
				if (cachedField.valueClass == null) {
					Registration registration = kryo.writeClass(outputChunked, value.getClass());
					if (serializer == null) serializer = registration.getSerializer();
					if (cachedField.generics != null) serializer.setGenerics(kryo, cachedField.generics);
					kryo.writeObject(outputChunked, value, serializer);
				} else {
					if (serializer == null) cachedField.serializer = serializer = kryo.getSerializer(cachedField.valueClass);
					if (cachedField.generics != null) serializer.setGenerics(kryo, cachedField.generics);
					if (cachedField.canBeNull)
						kryo.writeObjectOrNull(outputChunked, value, serializer);
					else {
						if (value == null) {
							throw new KryoException("Field value is null but canBeNull is false: " + cachedField + " ("
								+ object.getClass().getName() + ")");
						}
						kryo.writeObject(outputChunked, value, serializer);
					}
				}

				outputChunked.endChunks();
			} catch (IllegalAccessException ex) {
				throw new KryoException("Error accessing field: " + cachedField + " (" + object.getClass().getName() + ")", ex);
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

				Class concreteType = cachedField.valueClass;
				Serializer serializer = cachedField.serializer;
				if (concreteType == null) {
					Registration registration = kryo.readClass(inputChunked);
					if (registration == null)
						value = null;
					else {
						if (serializer == null) serializer = registration.getSerializer();
						if (cachedField.generics != null) serializer.setGenerics(kryo, cachedField.generics);
						value = kryo.readObject(inputChunked, registration.getType(), serializer);
					}
				} else {
					if (serializer == null) cachedField.serializer = serializer = kryo.getSerializer(concreteType);
					if (cachedField.generics != null) serializer.setGenerics(kryo, cachedField.generics);
					if (cachedField.canBeNull)
						value = kryo.readObjectOrNull(inputChunked, concreteType, serializer);
					else
						value = kryo.readObject(inputChunked, concreteType, serializer);
				}

				cachedField.set(object, value);

				inputChunked.nextChunks();
			} catch (IllegalAccessException ex) {
				throw new KryoException("Error accessing field: " + cachedField + " (" + getType().getName() + ")", ex);
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
