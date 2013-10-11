
package com.esotericsoftware.kryo.serialize;

import static com.esotericsoftware.minlog.Log.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;

import com.esotericsoftware.kryo.Serializer;

/**
 * Serializes instances of {@link BigDecimal}.
 * @author Nathan Sweet <misc@n4te.com>
 */
public class BigDecimalSerializer extends Serializer {
	private BigIntegerSerializer bigIntegerSerializer = new BigIntegerSerializer();

	public BigDecimal readObjectData (ByteBuffer buffer, Class type) {
		BigInteger unscaledValue = bigIntegerSerializer.readObjectData(buffer, null);
		int scale = IntSerializer.get(buffer, false);
		BigDecimal value = new BigDecimal(unscaledValue, scale);
		if (TRACE) trace("kryo", "Read BigDecimal: " + value);
		return value;
	}

	public void writeObjectData (ByteBuffer buffer, Object object) {
		BigDecimal value = (BigDecimal)object;
		bigIntegerSerializer.writeObjectData(buffer, value.unscaledValue());
		IntSerializer.put(buffer, value.scale(), false);
		if (TRACE) trace("kryo", "Wrote BigDecimal: " + value);
	}
}
