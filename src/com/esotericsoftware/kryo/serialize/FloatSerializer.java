
package com.esotericsoftware.kryo.serialize;

import static com.esotericsoftware.minlog.Log.*;

import java.nio.ByteBuffer;

import javax.print.attribute.IntegerSyntax;

import com.esotericsoftware.kryo.Serializer;

/**
 * Writes a 1-5 byte float.
 * @author Nathan Sweet <misc@n4te.com>
 */
public class FloatSerializer extends Serializer {
	private final float precision;
	private final boolean optimizePositive;

	/**
	 * Creates a FloatSerializer that allows uses 4 bytes to represent a float, with no loss of precision.
	 */
	public FloatSerializer () {
		precision = 0;
		optimizePositive = false;
	}

	/**
	 * Creates a FloatSerializer that allows uses 1-5 bytes to represent a float, with a loss of precision. The float is multiplied
	 * by the specified precision, then cast to an int and serialized with {@link IntSerializer}. IntSerializer uses 1-4 bytes from
	 * 0 to 268,435,455 with "optimize positive" and -134,217,728 to 134,217,727 without. If a float multiplied by the precision
	 * would fall outside these values, it will take 5 bytes to serialize and it may be better to use the other FloatSerializer
	 * constructor.
	 */
	public FloatSerializer (float precision, boolean optimizePositive) {
		this.precision = precision;
		this.optimizePositive = optimizePositive;
	}

	public Float readObjectData (ByteBuffer buffer, Class type) {
		float f;
		if (precision == 0)
			f = buffer.getFloat();
		else
			f = IntSerializer.get(buffer, optimizePositive) / (float)precision;
		if (TRACE) trace("kryo", "Read float: " + f);
		return f;
	}

	public void writeObjectData (ByteBuffer buffer, Object object) {
		if (precision == 0)
			buffer.putFloat((Float)object);
		else
			IntSerializer.put(buffer, (int)((Float)object * precision), optimizePositive);
		if (TRACE) trace("kryo", "Wrote float: " + object);
	}
}
