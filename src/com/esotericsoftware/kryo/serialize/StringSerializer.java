
package com.esotericsoftware.kryo.serialize;

import static com.esotericsoftware.minlog.Log.*;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

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
		ByteBuffer outputBuffer = Kryo.getContext().getBuffer(value.length() * maxBytesPerChar);
		encoder.encode(CharBuffer.wrap(value), outputBuffer, true);
		outputBuffer.flip();

		int bytesWritten = outputBuffer.limit();
		IntSerializer.put(buffer, bytesWritten, true);
		buffer.put(outputBuffer);
	}

	static public String get (ByteBuffer buffer) {
		int bytesToRead = IntSerializer.get(buffer, true);
		char[] outputArray = Kryo.getContext().getCharArray(bytesToRead);
		CharBuffer outputBuffer = CharBuffer.wrap(outputArray);

		int oldLimit = buffer.limit();
		buffer.limit(buffer.position() + bytesToRead);
		decoder.decode(buffer, outputBuffer, true);
		buffer.limit(oldLimit);

		return new String(outputArray, 0, outputBuffer.position());
	}
}
