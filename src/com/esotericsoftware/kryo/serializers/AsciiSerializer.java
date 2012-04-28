
package com.esotericsoftware.kryo.serializers;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/** Serializes strings using ASCII.
 * @see Output#writeAscii(String)
 * @author Nathan Sweet <misc@n4te.com> */
public class AsciiSerializer extends Serializer<String> {
	{
		setImmutable(true);
		setAcceptsNull(true);
	}

	public void write (Kryo kryo, Output output, String object) {
		output.writeAscii(object);
	}

	public String create (Kryo kryo, Input input, Class<String> type) {
		return input.readAscii();
	}
	
	public static void main (String[] args) throws Exception {
		Object moo = new Integer[]{123,2,3,4};
		Object[] cow = (Object[])moo;
		System.out.println(cow[0]);
	}
}
