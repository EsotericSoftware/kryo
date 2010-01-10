
package com.esotericsoftware.kryo.serialize;

import static com.esotericsoftware.minlog.Log.*;

import java.nio.ByteBuffer;
import java.util.Date;

import com.esotericsoftware.kryo.Serializer;

/**
 * Serializes instances of java.util.Date.
 */
public class DateSerializer extends Serializer {
	static private LongSerializer longSerializer = new LongSerializer(true);

	public Date readObjectData (ByteBuffer buffer, Class type) {
		Date date = new Date(LongSerializer.get(buffer, true));
		if (TRACE) trace("kryo", "Read date: " + date);
		return date;
	}

	public void writeObjectData (ByteBuffer buffer, Object object) {
		LongSerializer.put(buffer, ((Date)object).getTime(), true);
		if (TRACE) trace("kryo", "Wrote date: " + object);
	}
}
