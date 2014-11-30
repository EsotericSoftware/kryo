package com.esotericsoftware.kryo.serializers;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.InputChunked;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.io.OutputChunked;
import com.esotericsoftware.kryo.util.ObjectMap;

import java.util.HashMap;
import java.util.Map;

import static com.esotericsoftware.minlog.Log.*;

/**
 * Serializes objects using direct field assignment, with limited support for
 * forward and backward compatibility. Fields can be added or removed without
 * invalidating previously serialized bytes. Note that changing the type of a
 * field is not supported.
 * <p>
 * There is additional overhead compared to {@link FieldSerializer}. A header is
 * output the first time an object of a given type is serialized. The header
 * consists of an int for the number of fields, then a String for each field
 * name. Also, to support skipping the bytes for a field that no longer exists,
 * for each field value an int is written that is the length of the value in
 * bytes.
 * <p>
 * Note that the field data is identified by name. The situation where a super
 * class and its sub class have same field name,super class's  field which conflicted
 * will be ignored. this will avoid conflicted field data being set to null when deserialize.
 * @author Nathan Sweet <misc@n4te.com>
 * @author Bohr Qiu <bohr.qiu@gmail.com>
 */
public class CompatibleFieldSerializer<T> extends FieldSerializer<T> {
	
	private CachedField[] fields;
	
	public CompatibleFieldSerializer(Kryo kryo, Class type) {
		super(kryo, type);
	}
	
	public void write(Kryo kryo, Output output, T object) {
		CachedField[] fields = getFields();
		ObjectMap context = kryo.getGraphContext();
		if (!context.containsKey(this)) {
			context.put(this, null);
			if (TRACE)
				trace("kryo", "Write " + fields.length + " field names.");
			output.writeVarInt(fields.length, true);
			for (int i = 0, n = fields.length; i < n; i++)
				output.writeString(fields[i].field.getName());
		}
		
		OutputChunked outputChunked = new OutputChunked(output, 1024);
		for (int i = 0, n = fields.length; i < n; i++) {
			fields[i].write(outputChunked, object);
			outputChunked.endChunks();
		}
	}
	
	public T read(Kryo kryo, Input input, Class<T> type) {
		T object = create(kryo, input, type);
		kryo.reference(object);
		ObjectMap context = kryo.getGraphContext();
		CachedField[] fields = (CachedField[]) context.get(this);
		if (fields == null) {
			int length = input.readVarInt(true);
			if (TRACE)
				trace("kryo", "Read " + length + " field names.");
			String[] names = new String[length];
			for (int i = 0; i < length; i++)
				names[i] = input.readString();
			
			fields = new CachedField[length];
			CachedField[] allFields = getFields();
			outer: for (int i = 0, n = names.length; i < n; i++) {
				String schemaName = names[i];
				for (int ii = 0, nn = allFields.length; ii < nn; ii++) {
					if (allFields[ii].field.getName().equals(schemaName)) {
						fields[i] = allFields[ii];
						continue outer;
					}
				}
				if (TRACE)
					trace("kryo", "Ignore obsolete field: " + schemaName);
			}
			context.put(this, fields);
		}
		
		InputChunked inputChunked = new InputChunked(input, 1024);
		boolean hasGenerics = getGenerics() != null;
		for (int i = 0, n = fields.length; i < n; i++) {
			CachedField cachedField = fields[i];
			if(cachedField != null && hasGenerics) {
				// Generic type used to instantiate this field could have 
				// been changed in the meantime. Therefore take the most 
				// up-to-date definition of a field
				cachedField = getField(cachedField.field.getName());
			}
			if (cachedField == null) {
				if (TRACE)
					trace("kryo", "Skip obsolete field.");
				inputChunked.nextChunks();
				continue;
			}
			cachedField.read(inputChunked, object);
			inputChunked.nextChunks();
		}
		return object;
	}
	
	/**
	 * ignore the duplicated fields in the super class
	 */
	protected void initializeCachedFields() {
		//get all cached fields form super class
		CachedField[] fields = super.getFields();
		Map<String, CachedField> cachedFiledMap = new HashMap<String, CachedField>();
		for (CachedField field : fields) {
			String fieldName = field.getField().getName();
			if (!cachedFiledMap.containsKey(fieldName)) {
				cachedFiledMap.put(fieldName, field);
			} else {
				//field belong to this.getType() will be save to cachedFiledMap
				if (field.getField().getDeclaringClass().equals(this.getType())) {
					cachedFiledMap.put(fieldName, field);
				}
				//field belong to super class will be ignored
			}
		}
		//set cached fields, the fields order will be ignored because of read field don't depends on its order
		setFields(cachedFiledMap.values().toArray(new CachedField[cachedFiledMap.size()]));
	}
	
	public void removeField(String fieldName) {
		for (int i = 0; i < fields.length; i++) {
			CachedField cachedField = fields[i];
			if (cachedField.field.getName().equals(fieldName)) {
				CachedField[] newFields = new CachedField[fields.length - 1];
				System.arraycopy(fields, 0, newFields, 0, i);
				System.arraycopy(fields, i + 1, newFields, i, newFields.length - i);
				fields = newFields;
				super.removeField(fieldName);
				return;
			}
		}
		throw new IllegalArgumentException("Field \"" + fieldName + "\" not found on class: "
											+ type.getName());
	}
	
	/**
	 * override the
	 * {@link com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer#getFields}
	 */
	public CachedField[] getFields() {
		return fields;
	}
	
	public void setFields(CachedField[] fields) {
		this.fields = fields;
	}
}
