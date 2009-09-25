
package com.esotericsoftware.kryo.serialize;

import static com.esotericsoftware.log.Log.*;

import java.nio.ByteBuffer;

import com.esotericsoftware.kryo.Context;
import com.esotericsoftware.kryo.Serializer;

/**
 * Writes a 2 byte char.
 * @author Nathan Sweet <misc@n4te.com>
 */
public class CharSerializer extends Serializer {
	public Character readObjectData (ByteBuffer buffer, Class type) {
		char ch = buffer.getChar();
		if (level <= TRACE) trace("kryo", "Read char: " + ch);
		return ch;
	}

	public void writeObjectData (ByteBuffer buffer, Object object) {
		buffer.putChar((Character)object);
		if (level <= TRACE) trace("kryo", "Wrote char: " + object);
	}
}
