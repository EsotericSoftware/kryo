
package com.esotericsoftware.kryo.serializers;

import static com.esotericsoftware.minlog.Log.TRACE;
import static com.esotericsoftware.minlog.Log.trace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.InputChunked;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.ObjectMap;

public class FieldMappingCompatibleSerializer<T> extends CompatibleFieldSerializer<T> {
	public FieldMappingCompatibleSerializer (Kryo kryo, Class<?> type) {
		super(kryo, type);
	}

	private class DummyCachedField extends CachedField {
		private Kryo kryo;

		private DummyCachedField (Kryo kryo) {
			this.kryo = kryo;
		}

		@Override
		public void write (Output output, Object object) {
		}

		@Override
		public void read (Input input, Object object) {
			////////////////////////////////////////////////////////////////////////////////////
			// I expect Kryo#readClassAndObject will read proper bytes for current field,
			// and this DummyCachedField is set for the fields who are not existing in the DTO we have to fill.
			// So, by calling the method below, the Input will keep the right index of inputStream.
			////////////////////////////////////////////////////////////////////////////////////
			kryo.readClassAndObject(input);
		}

		@Override
		public void copy (Object original, Object copy) {
		}
	};

	public T read (Kryo kryo, Input input, Class<T> type) {
		T object = create(kryo, input, type);
		kryo.reference(object);

		ObjectMap context = kryo.getGraphContext();
		CachedField[] fieldsOfDTOToMap = getCachedFields(kryo, input, context);
		InputChunked inputChunked = new InputChunked(input, 1024);
		boolean hasGenerics = getGenerics() != null;
		for (CachedField cachedField : fieldsOfDTOToMap) {
			if (hasGenerics && cachedField.getField() != null) {
				cachedField = getField(cachedField.getField().getName());
			}

			cachedField.read(inputChunked, object);
			inputChunked.nextChunks();
		}
		return object;
	}

	private CachedField[] getCachedFields (Kryo kryo, Input input, ObjectMap context) {
		CachedField[] fields = (CachedField[])context.get(this);
		if (fields != null) {
			return fields;
		}

		int cachedDTOFieldsLength = input.readVarInt(true);
		if (TRACE) {
			trace("kryo", "Read " + cachedDTOFieldsLength + " field names.");
		}

		String[] fieldNamesFromInput = getFieldNamesFromInput(input, cachedDTOFieldsLength);
		List<CachedField> newFieldsToCacheBuffer = new ArrayList<CachedField>();

		////////////////////////////////////////////////////////////////////////////////////
		// Because, CompatibleFieldSerializer is comparing the fields between Input's and DTO's with 2 arrays and one index of one of the them,
		// it throws IndexOutOfBoundsException when the pivot index is larger than the size of the other array.
		// And, it was using 2depth loop for it. (normally n^2 operation)
		// 
		// So, just by making a Map for mapping I can avoid IndexOutOfBoundsException and make the complexity to 2n from n^2.
		// Of course under the assuming that HashMap's hashing is fast enough.
		////////////////////////////////////////////////////////////////////////////////////
		Map<String, CachedField> allFieldsOfEntity = getAllFieldsOfEntityMap();
		for (int i = 0, n = fieldNamesFromInput.length; i < n; i++) {
			newFieldsToCacheBuffer.add(findMatchedField(kryo, fieldNamesFromInput[i], allFieldsOfEntity));
		}

		CachedField[] result = newFieldsToCacheBuffer.toArray(new CachedField[0]);
		context.put(this, result);

		return result;
	}

	private Map<String, CachedField> getAllFieldsOfEntityMap () {
		Map<String, CachedField> allFieldsOfEntity = new HashMap<String, CachedField>();
		for (CachedField field : getFields()) {
			allFieldsOfEntity.put(field.getField().getName(), field);
		}
		return allFieldsOfEntity;
	}

	private CachedField findMatchedField (Kryo kryo, String findingField, Map<String, CachedField> allFieldsOfEntity) {
		if (!allFieldsOfEntity.containsKey(findingField)) {
			if (TRACE) {
				trace("kryo", "Ignore obsolete field: " + findingField);
			}

			return new DummyCachedField(kryo);
		}

		return allFieldsOfEntity.get(findingField);
	}

	private String[] getFieldNamesFromInput (Input input, int length) {
		List<String> fieldNamesBuffer = new ArrayList<String>();
		for (int i = 0; i < length; i++) {
			fieldNamesBuffer.add(input.readString());
		}

		return fieldNamesBuffer.toArray(new String[0]);
	}
}
