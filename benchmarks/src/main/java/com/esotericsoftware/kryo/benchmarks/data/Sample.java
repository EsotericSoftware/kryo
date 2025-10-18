/* Copyright (c) 2008-2025, Nathan Sweet
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 * - Neither the name of Esoteric Software nor the names of its contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. */

package com.esotericsoftware.kryo.benchmarks.data;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;

import java.util.Arrays;

public class Sample {
	@Tag(0) public int intValue;
	@Tag(1) public long longValue;
	@Tag(2) public float floatValue;
	@Tag(3) public double doubleValue;
	@Tag(4) public short shortValue;
	@Tag(5) public char charValue;
	@Tag(6) public boolean booleanValue;

	@Tag(7) public Integer IntValue;
	@Tag(8) public Long LongValue;
	@Tag(9) public Float FloatValue;
	@Tag(10) public Double DoubleValue;
	@Tag(11) public Short ShortValue;
	@Tag(12) public Character CharValue;
	@Tag(13) public Boolean BooleanValue;

	@Tag(14) public int[] intArray;
	@Tag(15) public long[] longArray;
	@Tag(16) public float[] floatArray;
	@Tag(17) public double[] doubleArray;
	@Tag(18) public short[] shortArray;
	@Tag(19) public char[] charArray;
	@Tag(20) public boolean[] booleanArray;

	@Tag(21) public String string; // Can be null.
	@Tag(22) public Sample sample; // Can be null.

	public Sample () {
	}

	public Sample populate (boolean circularReference) {
		intValue = 123;
		longValue = 1230000;
		floatValue = 12.345f;
		doubleValue = 1.234567;
		shortValue = 12345;
		charValue = '!';
		booleanValue = true;

		IntValue = 321;
		LongValue = 3210000l;
		FloatValue = 54.321f;
		DoubleValue = 7.654321;
		ShortValue = 32100;
		CharValue = '$';
		BooleanValue = Boolean.FALSE;

		intArray = new int[] {-1234, -123, -12, -1, 0, 1, 12, 123, 1234};
		longArray = new long[] {-123400, -12300, -1200, -100, 0, 100, 1200, 12300, 123400};
		floatArray = new float[] {-12.34f, -12.3f, -12, -1, 0, 1, 12, 12.3f, 12.34f};
		doubleArray = new double[] {-1.234, -1.23, -12, -1, 0, 1, 12, 1.23, 1.234};
		shortArray = new short[] {-1234, -123, -12, -1, 0, 1, 12, 123, 1234};
		charArray = "asdfASDF".toCharArray();
		booleanArray = new boolean[] {true, false, false, true};

		string = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		if (circularReference) sample = this;
		return this;
	}

	public int hashCode () {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((BooleanValue == null) ? 0 : BooleanValue.hashCode());
		result = prime * result + ((CharValue == null) ? 0 : CharValue.hashCode());
		result = prime * result + ((DoubleValue == null) ? 0 : DoubleValue.hashCode());
		result = prime * result + ((FloatValue == null) ? 0 : FloatValue.hashCode());
		result = prime * result + ((IntValue == null) ? 0 : IntValue.hashCode());
		result = prime * result + ((LongValue == null) ? 0 : LongValue.hashCode());
		result = prime * result + ((ShortValue == null) ? 0 : ShortValue.hashCode());
		result = prime * result + Arrays.hashCode(booleanArray);
		result = prime * result + (booleanValue ? 1231 : 1237);
		result = prime * result + Arrays.hashCode(charArray);
		result = prime * result + charValue;
		result = prime * result + Arrays.hashCode(doubleArray);
		long temp;
		temp = Double.doubleToLongBits(doubleValue);
		result = prime * result + (int)(temp ^ (temp >>> 32));
		result = prime * result + Arrays.hashCode(floatArray);
		result = prime * result + Float.floatToIntBits(floatValue);
		result = prime * result + Arrays.hashCode(intArray);
		result = prime * result + intValue;
		result = prime * result + Arrays.hashCode(longArray);
		result = prime * result + (int)(longValue ^ (longValue >>> 32));
		result = prime * result + ((sample == null) ? 0 : sample.hashCode());
		result = prime * result + Arrays.hashCode(shortArray);
		result = prime * result + shortValue;
		result = prime * result + ((string == null) ? 0 : string.hashCode());
		return result;
	}

	public boolean equals (Object object) {
		if (this == object) return true;
		if (object == null) return false;
		if (getClass() != object.getClass()) return false;
		Sample other = (Sample)object;
		if (BooleanValue == null) {
			if (other.BooleanValue != null) return false;
		} else if (!BooleanValue.equals(other.BooleanValue)) return false;
		if (CharValue == null) {
			if (other.CharValue != null) return false;
		} else if (!CharValue.equals(other.CharValue)) return false;
		if (DoubleValue == null) {
			if (other.DoubleValue != null) return false;
		} else if (!DoubleValue.equals(other.DoubleValue)) return false;
		if (FloatValue == null) {
			if (other.FloatValue != null) return false;
		} else if (!FloatValue.equals(other.FloatValue)) return false;
		if (IntValue == null) {
			if (other.IntValue != null) return false;
		} else if (!IntValue.equals(other.IntValue)) return false;
		if (LongValue == null) {
			if (other.LongValue != null) return false;
		} else if (!LongValue.equals(other.LongValue)) return false;
		if (ShortValue == null) {
			if (other.ShortValue != null) return false;
		} else if (!ShortValue.equals(other.ShortValue)) return false;
		if (!Arrays.equals(booleanArray, other.booleanArray)) return false;
		if (booleanValue != other.booleanValue) return false;
		if (!Arrays.equals(charArray, other.charArray)) return false;
		if (charValue != other.charValue) return false;
		if (!Arrays.equals(doubleArray, other.doubleArray)) return false;
		if (Double.doubleToLongBits(doubleValue) != Double.doubleToLongBits(other.doubleValue)) return false;
		if (!Arrays.equals(floatArray, other.floatArray)) return false;
		if (Float.floatToIntBits(floatValue) != Float.floatToIntBits(other.floatValue)) return false;
		if (!Arrays.equals(intArray, other.intArray)) return false;
		if (intValue != other.intValue) return false;
		if (!Arrays.equals(longArray, other.longArray)) return false;
		if (longValue != other.longValue) return false;
		if (sample == null) {
			if (other.sample != null) return false;
		} else if (sample != this && !sample.equals(other.sample)) return false;
		if (!Arrays.equals(shortArray, other.shortArray)) return false;
		if (shortValue != other.shortValue) return false;
		if (string == null) {
			if (other.string != null) return false;
		} else if (!string.equals(other.string)) return false;
		return true;
	}
}
