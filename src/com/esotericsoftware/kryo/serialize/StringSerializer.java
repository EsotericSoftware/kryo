
package com.esotericsoftware.kryo.serialize;

import static com.esotericsoftware.log.Log.*;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

import com.esotericsoftware.kryo.Context;
import com.esotericsoftware.kryo.Serializer;

/**
 * Writes a String as UTF-8 bytes.
 * @author Nathan Sweet <misc@n4te.com>
 */
public class StringSerializer extends Serializer {
	static private final CharsetEncoder encoder;
	static private final CharsetDecoder decoder;
	static {
		Charset charset = Charset.forName("UTF-8");
		encoder = charset.newEncoder();
		decoder = charset.newDecoder();
	}

	public String readObjectData (ByteBuffer buffer, Class type) {
		String s = get(buffer);
		if (level <= TRACE) trace("kryo", "Read string: " + s);
		return s;
	}

	public void writeObjectData (ByteBuffer buffer, Object object) {
		String s = (String)object;
		put(buffer, s);
		if (level <= TRACE) trace("kryo", "Wrote string: " + object);
	}

	static public void put (ByteBuffer buffer, String value) {
		// Essentially what the JDK does internally.
		byte[] bytes = new byte[(int)(value.length() * encoder.maxBytesPerChar())];
		ByteBuffer tempBuffer = ByteBuffer.wrap(bytes);
		encoder.encode(CharBuffer.wrap(value), tempBuffer, true);
		int length = tempBuffer.position();
		IntSerializer.put(buffer, length, true);
		buffer.put(bytes, 0, length);
	}

	static public String get (ByteBuffer buffer) {
		// Essentially what the JDK does internally.
		int length = IntSerializer.get(buffer, true);
		char[] chars = new char[length];
		decoder.decode(buffer, CharBuffer.wrap(chars), true);
		return new String(chars);
	}
}
