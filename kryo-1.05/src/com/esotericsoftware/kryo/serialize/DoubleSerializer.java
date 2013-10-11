
package com.esotericsoftware.kryo.serialize;

import java.nio.ByteBuffer;

import com.esotericsoftware.kryo.Serializer;

import static com.esotericsoftware.minlog.Log.*;

/**
 * Writes a 1-10 byte double.
 * @author Nathan Sweet <misc@n4te.com>
 */
public class DoubleSerializer extends Serializer {
	private final double precision;
	private final boolean optimizePositive;

	/**
	 * Creates a DoubleSerializer that allows uses 8 bytes to represent a double, with no loss of precision.
	 */
	public DoubleSerializer () {
		precision = 0;
		optimizePositive = false;
	}

	/**
	 * Creates a DoubleSerializer that allows uses 1-10 bytes to represent a double, with a loss of precision. The double is
	 * multiplied by the specified precision, then cast to a long and serialized with {@link LongSerializer}. LongSerializer uses
	 * 1-8 bytes from 0 to 7205794037927935 with "optimize positive" and -3602897018963968 to 3602897018963967 without. If a double
	 * multiplied by the precision would fall out these values, it will take 9 or 10 bytes to serialize and it may be better to use
	 * the other DoubleSerializer constructor.
	 */
	public DoubleSerializer (double precision, boolean optimizePositive) {
		this.precision = precision;
		this.optimizePositive = optimizePositive;
	}

	public Double readObjectData (ByteBuffer buffer, Class type) {
		double d;
		if (precision == 0)
			d = buffer.getDouble();
		else
			d = LongSerializer.get(buffer, optimizePositive) / (double)precision;
		if (TRACE) trace("kryo", "Read double: " + d);
		return d;
	}

	public void writeObjectData (ByteBuffer buffer, Object object) {
		if (precision == 0)
			buffer.putDouble((Double)object);
		else
			LongSerializer.put(buffer, (long)((Double)object * precision), optimizePositive);
		if (TRACE) trace("kryo", "Wrote double: " + object);
	}
}
