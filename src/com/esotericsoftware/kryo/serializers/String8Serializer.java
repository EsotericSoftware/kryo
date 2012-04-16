
package com.esotericsoftware.kryo.serializers;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/** @see Output#writeString8(String)
 * @author Nathan Sweet <misc@n4te.com> */
public class String8Serializer extends Serializer<String> {
	{
		setAcceptsNull(true);
	}

	public void write (Kryo kryo, Output output, String object) {
		output.writeString8(object);
	}

	public String create (Kryo kryo, Input input, Class<String> type) {
		return input.readString8();
	}
}
