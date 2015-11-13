package com.esotericsoftware.kryo.serializers;

import static com.esotericsoftware.minlog.Log.TRACE;
import static com.esotericsoftware.minlog.Log.trace;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.InputChunked;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.io.OutputChunked;
import com.esotericsoftware.kryo.util.ObjectMap;

/** 
 * Serializes objects similar to {@link CompatibleFieldSerializer}. The only difference is it doesn't store null field values.
 * 
 * Note:- Similar to {@link CompatibleFieldSerializer} the field data is identified by name. The situation where a super class has a field with the same name as a subclass
 * must be avoided.
 * 
 * @author Mitesh Pathak <miteshpathak05@gmail.com> 
 * @see CompatibleFieldSerializer
 * 
 * */
public class NonNullCompatibleFieldSerializer<T> extends FieldSerializer<T> {
	/* For object with more than BINARY_SEARCH_THRESHOLD fields, use binary search instead of iterative search */
	private static final int THRESHOLD_BINARY_SEARCH = 32;

	public NonNullCompatibleFieldSerializer (Kryo kryo, Class<?> type) {
		super(kryo, type);
	}

	@Override
	public void write (Kryo kryo, Output output, T object) {
		CachedField[] fields = getFields();
		ObjectMap context = kryo.getGraphContext();
		if (!context.containsKey(this)) {
			context.put(this, null);
			if (TRACE) trace("kryo", "Write " + fields.length + " field names.");
			output.writeVarInt(fields.length, true);
			for (int i = 0, n = fields.length; i < n; i++)
				output.writeString(fields[i].field.getName());
		}

		// bit set for null check
		BitSet bitSet = new BitSet(fields.length);
		List<CachedField> filtered = new ArrayList<>();
		for (int i = 0; i < fields.length; i++) {
			if (isDefaultValueField(fields[i].getField(), object)) {
				bitSet.clear(i);
			} else {
				bitSet.set(i);
				filtered.add(fields[i]);
			}
		}
		byte[] bitSetData = bitSet.toByteArray();

		OutputChunked outputChunked = new OutputChunked(output, 1024);
		// write bitset
		outputChunked.writeVarInt(bitSetData.length, true);
		outputChunked.write(bitSetData);
		outputChunked.endChunks();
		for (CachedField f : filtered) {
			f.write(outputChunked, object);
			outputChunked.endChunks();
		}
	}

	@Override
	public T read (Kryo kryo, Input input, Class<T> type) {
		T object = create(kryo, input, type);
		kryo.reference(object);
		ObjectMap context = kryo.getGraphContext();

		CachedField[] fields = (CachedField[])context.get(this);
		if (fields == null) {
			int length = input.readVarInt(true);
			if (TRACE) trace("kryo", "Read " + length + " field names.");
			String[] names = new String[length];
			for (int i = 0; i < length; i++)
				names[i] = input.readString();

			fields = new CachedField[length];
			CachedField[] allFields = getFields();

			if (length < THRESHOLD_BINARY_SEARCH) {
				outer:
					for (int i = 0; i < length; i++) {
						String schemaName = names[i];
						for (int ii = 0, nn = allFields.length; ii < nn; ii++) {
							if (allFields[ii].field.getName().equals(schemaName)) {
								fields[i] = allFields[ii];
								continue outer;
							}
						}
						if (TRACE) trace("kryo", "Ignore obsolete field: " + schemaName);
					}
			} else {
				// binary search for schemaName
				int low, mid, high;
				int compare;
				outerBinarySearch:
					for (int i = 0; i < length; i++) {
						String schemaName = names[i];

						low = 0;
						high = length - 1;

						while (low <= high) {
							mid = (low + high) >>> 1;
					String midVal = allFields[mid].field.getName();
					compare = schemaName.compareTo(midVal);

					if (compare < 0) {
						high = mid - 1;
					}
					else if (compare > 0) {
						low = mid + 1;
					}
					else {
						fields[i] = allFields[mid];
						continue outerBinarySearch;
					}
						}
						if (TRACE) trace("kryo", "Ignore obsolete field: " + schemaName);
					}
			}

			context.put(this, fields);
		}

		InputChunked inputChunked = new InputChunked(input, 1024);

		// read bitSet
		int bitSetLen = inputChunked.readVarInt(true);
		byte[] bitSetData = inputChunked.readBytes(bitSetLen);
		BitSet bitSet = BitSet.valueOf(bitSetData);
		inputChunked.nextChunks();

		boolean hasGenerics = getGenerics() != null;

		for (int i = 0, n = fields.length; i < n; i++) {
			if (!bitSet.get(i)) {
				continue; // no chunks saved
			}
			CachedField cachedField = fields[i];
			if (cachedField == null) {
				if (TRACE) trace("kryo", "Skip obsolete field.");
				inputChunked.nextChunks();
				continue; // field removed in new version
			}

			if (cachedField != null && hasGenerics) {
				// Generic type used to instantiate this field could have
				// been changed in the meantime. Therefore take the most
				// up-to-date definition of a field
				cachedField = getField(cachedField.field.getName());
			}
			cachedField.read(inputChunked, object);
			inputChunked.nextChunks();
		}
		return object;
	}

	/**
	 * Check if the field either initialized or has default value (only primitive types).
	 *
	 * @param field
	 * @param object
	 * @return true if the object is null or has default value. Returns false otherwise
	 */
	private static boolean isDefaultValueField(Field field, Object object) {
		field.setAccessible(true);

		try {
			Class<?> clazz = field.getType();
			Object fieldValue = field.get(object);
			if (true) return fieldValue == null;

			if (clazz == byte.class) {
				return (Byte) fieldValue == 0;
			}
			if (clazz == short.class) {
				return (Short)fieldValue == 0;
			}
			if (clazz == int.class) {
				return (Integer)fieldValue == 0;
			}
			if (clazz == long.class) {
				return (Long)fieldValue == 0L;
			}
			if (clazz == float.class) {
				return (Float)fieldValue == 0.0f;
			}
			if (clazz == double.class) {
				return (Double)fieldValue == 0.0d;
			}
			if (clazz == char.class) {
				return (Character)fieldValue == '\u0000';
			}
			if (clazz == boolean.class) {
				return (Boolean)fieldValue == false;
			}
			return fieldValue == null;
		} catch (IllegalArgumentException | IllegalAccessException e) {
			return true;
		}
	}

}
