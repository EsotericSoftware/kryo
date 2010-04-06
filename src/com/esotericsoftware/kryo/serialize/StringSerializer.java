
package com.esotericsoftware.kryo.serialize;

import static com.esotericsoftware.minlog.Log.*;

import java.nio.ByteBuffer;
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
		int charCount = value.length();
		IntSerializer.put(buffer, charCount, true);
		int c;
		for (int i = 0; i < charCount; i++) {
			c = value.charAt(i);
			if (c <= 0x007F) {
				buffer.put((byte)c);
			} else if (c > 0x07FF) {
				buffer.put((byte)(0xE0 | c >> 12 & 0x0F));
				buffer.put((byte)(0x80 | c >> 6 & 0x3F));
				buffer.put((byte)(0x80 | c >> 0 & 0x3F));
			} else {
				buffer.put((byte)(0xC0 | c >> 6 & 0x1F));
				buffer.put((byte)(0x80 | c >> 0 & 0x3F));
			}
		}
	}

	static public String get (ByteBuffer buffer) {
		int charCount = IntSerializer.get(buffer, true);
		char[] chars = Kryo.getContext().getCharArray(charCount);
		int c, charIndex = 0;
		while (charIndex < charCount) {
			c = buffer.get() & 0xff;
			switch (c >> 4) {
			case 0:
			case 1:
			case 2:
			case 3:
			case 4:
			case 5:
			case 6:
			case 7:
				chars[charIndex++] = (char)c;
				break;
			case 12:
			case 13:
				chars[charIndex++] = (char)((c & 0x1F) << 6 | buffer.get() & 0x3F);
				break;
			case 14:
				chars[charIndex++] = (char)((c & 0x0F) << 12 | (buffer.get() & 0x3F) << 6 | (buffer.get() & 0x3F) << 0);
				break;
			}
		}
		return new String(chars, 0, charCount);
	}
}
