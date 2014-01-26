package com.esotericsoftware.kryo.serializers;

import com.esotericsoftware.kryo.Kryo;

import java.util.HashMap;
import java.util.Map;

/**
 * Serializes objects using direct field assignment, with limited support for
 * forward and backward compatibility. Fields can be added or removed without
 * invalidating previously serialized bytes. Note that changing the type of a
 * field is not supported.
 * <p>
 * Features like
 * {@link com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer},<b>but
 * The situation where a super class has a field with the same name as a
 * subclass will be accepted.</b>
 * 
 * @author bohr.qiu <bohr.qiu@gmail.com>
 */
public class DuplicateFieldNameAcceptedCompatibleFieldSerializer<T> extends CompatibleFieldSerializer<T> {
	
	private CachedField[] fields;
	
	public DuplicateFieldNameAcceptedCompatibleFieldSerializer(Kryo kryo, Class type) {
		super(kryo, type);
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
