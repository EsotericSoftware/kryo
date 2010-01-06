
package com.esotericsoftware.kryo.serialize;

import static com.esotericsoftware.minlog.Log.TRACE;
import static com.esotericsoftware.minlog.Log.trace;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

import com.esotericsoftware.kryo.Context;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;

/**
 * Writes a String as UTF-8 bytes.
 * @author Nathan Sweet <misc@n4te.com>
 */
public class StringSerializer extends Serializer {
	static private final CharsetEncoder encoder;
	static private final CharsetDecoder decoder;
	static private final int maxBytesPerChar;
	static {
		Charset charset = Charset.forName("UTF-8");
		encoder = charset.newEncoder();
		maxBytesPerChar = (int)Math.ceil(encoder.maxBytesPerChar());
		decoder = charset.newDecoder();
	}

	public String readObjectData (ByteBuffer buffer, Class type) {
		String s = get(buffer);
		if (TRACE) trace("kryo", "Read string: " + s);
		return s;
	}

	public void writeObjectData (ByteBuffer buffer, Object object) {
		String s = (String)object;
		put(buffer, s);
		if (TRACE) trace("kryo", "Wrote string: " + object);
	}

	static public void put (ByteBuffer buffer, String value) {
		Context context = Kryo.getContext();
		ByteBuffer tempBuffer = context.getBuffer(value.length() * maxBytesPerChar);
		encoder.encode(CharBuffer.wrap(value), tempBuffer, true);
		tempBuffer.flip();
		int length = tempBuffer.limit();
		IntSerializer.put(buffer, length, true);
		buffer.put(tempBuffer);
	}

	static public String get (ByteBuffer buffer) {
		int length = IntSerializer.get(buffer, true);
		Context context = Kryo.getContext();
		char[] chars = (char[])context.get("charArray");
		if (chars == null || chars.length < length) {
			chars = new char[length];
			context.put("charArray", chars);
		}
		CharBuffer tempBuffer = CharBuffer.wrap(chars);
		tempBuffer.limit(length);
		decoder.decode(buffer, tempBuffer, true);
		return new String(chars, 0, tempBuffer.position());
	}
}
