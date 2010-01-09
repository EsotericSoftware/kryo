
package com.esotericsoftware.kryo.serialize;

import static com.esotericsoftware.minlog.Log.TRACE;
import static com.esotericsoftware.minlog.Log.trace;

import java.nio.ByteBuffer;
import java.util.Date;

import com.esotericsoftware.kryo.Serializer;

/**
 * Serializes instances of java.util.Date.
 */
public class DateSerializer extends Serializer {
	static private LongSerializer longSerializer = new LongSerializer(true);

	public Date readObjectData (ByteBuffer buffer, Class type) {
		long l = longSerializer.readObjectData(buffer, long.class);
		if (TRACE) trace("kryo", "Read date: " + l);
		return new Date(l);
	}

	public void writeObjectData (ByteBuffer buffer, Object object) {
		longSerializer.writeObjectData(buffer, ((Date)object).getTime());
		if (TRACE) trace("kryo", "Wrote date: " + object);
	}
}
