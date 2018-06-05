/* Copyright (c) 2008-2017, Nathan Sweet
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

import static com.esotericsoftware.minlog.Log.*;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.serializers.FieldSerializer.CachedField;

import sun.misc.Unsafe;

/** Configuration for FieldSerializer instances. */
public class FieldSerializerConfig implements Cloneable {
	boolean fieldsCanBeNull = true;
	boolean setFieldsAsAccessible = true;
	boolean ignoreSyntheticFields = true;
	boolean fixedFieldTypes;
	boolean copyTransient = true;
	boolean serializeTransient;
	boolean varEncoding = true;
	boolean extendedFieldNames;
	boolean unsafe;

	public FieldSerializerConfig clone () {
		try {
			return (FieldSerializerConfig)super.clone(); // Clone is ok as we have only primitive fields.
		} catch (CloneNotSupportedException ex) {
			throw new RuntimeException(ex);
		}
	}

	/** Sets the default value for {@link FieldSerializer.CachedField#setCanBeNull(boolean)}.
	 * @param fieldsCanBeNull False if none of the fields are null. Saves 0-1 byte per field. True if it is not known (default). */
	public void setFieldsCanBeNull (boolean fieldsCanBeNull) {
		this.fieldsCanBeNull = fieldsCanBeNull;
		if (TRACE) trace("kryo", "FieldSerializerConfig fieldsCanBeNull: " + fieldsCanBeNull);
	}

	public boolean getFieldsCanBeNull () {
		return fieldsCanBeNull;
	}

	/** Controls which fields are serialized.
	 * @param setFieldsAsAccessible If true, all non-transient fields (inlcuding private fields) will be serialized and
	 *           {@link java.lang.reflect.Field#setAccessible(boolean) set as accessible} if necessary (default). If false, only
	 *           fields in the public API will be serialized. */
	public void setFieldsAsAccessible (boolean setFieldsAsAccessible) {
		this.setFieldsAsAccessible = setFieldsAsAccessible;
		if (TRACE) trace("kryo", "FieldSerializerConfig setFieldsAsAccessible: " + setFieldsAsAccessible);
	}

	public boolean getSetFieldsAsAccessible () {
		return setFieldsAsAccessible;
	}

	/** Controls if synthetic fields are serialized. Default is true.
	 * @param ignoreSyntheticFields If true, only non-synthetic fields will be serialized. */
	public void setIgnoreSyntheticFields (boolean ignoreSyntheticFields) {
		this.ignoreSyntheticFields = ignoreSyntheticFields;
		if (TRACE) trace("kryo", "FieldSerializerConfig ignoreSyntheticFields: " + ignoreSyntheticFields);
	}

	public boolean getIgnoreSyntheticFields () {
		return ignoreSyntheticFields;
	}

	/** Sets the default value for {@link FieldSerializer.CachedField#setClass(Class)} to the field's declared type. This allows
	 * FieldSerializer to be more efficient, since it knows field values will not be a subclass of their declared type. Default is
	 * false. */
	public void setFixedFieldTypes (boolean fixedFieldTypes) {
		this.fixedFieldTypes = fixedFieldTypes;
		if (TRACE) trace("kryo", "FieldSerializerConfig fixedFieldTypes: " + fixedFieldTypes);
	}

	public boolean getFixedFieldTypes () {
		return fixedFieldTypes;
	}

	/** If false, when {@link Kryo#copy(Object)} is called all transient fields that are accessible will be ignored from being
	 * copied. Default is true. */
	public void setCopyTransient (boolean copyTransient) {
		this.copyTransient = copyTransient;
		if (TRACE) trace("kryo", "FieldSerializerConfig copyTransient: " + copyTransient);
	}

	public boolean getCopyTransient () {
		return copyTransient;
	}

	/** If set, transient fields will be serialized. Default is false. */
	public void setSerializeTransient (boolean serializeTransient) {
		this.serializeTransient = serializeTransient;
		if (TRACE) trace("kryo", "FieldSerializerConfig serializeTransient: " + serializeTransient);
	}

	public boolean getSerializeTransient () {
		return serializeTransient;
	}

	/** When true, variable length values are used for int and long fields. Default is true.
	 * @see CachedField#setVariableLengthEncoding(boolean) */
	public void setVariableLengthEncoding (boolean varEncoding) {
		this.varEncoding = varEncoding;
		if (TRACE) trace("kryo", "FieldSerializerConfig variable length encoding: " + varEncoding);
	}

	public boolean getVariableLengthEncoding () {
		return varEncoding;
	}

	/** When true, field names are prefixed by their declaring class. This can avoid conflicts when a subclass has a field with the
	 * same name as a super class. Default is false. */
	public void setExtendedFieldNames (boolean extendedFieldNames) {
		this.extendedFieldNames = extendedFieldNames;
		if (TRACE) trace("kryo", "FieldSerializerConfig extendedFieldNames: " + extendedFieldNames);
	}

	public boolean getExtendedFieldNames () {
		return extendedFieldNames;
	}

	public boolean getUnsafe () {
		return unsafe;
	}

	/** When true, fields will be read using {@link Unsafe}, if possible. */
	public void setUnsafe (boolean unsafe) {
		this.unsafe = unsafe;
	}
}
