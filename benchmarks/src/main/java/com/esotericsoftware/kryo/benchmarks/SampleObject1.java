/* Copyright (c) 2008-2018, Nathan Sweet
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

package com.esotericsoftware.kryo.benchmarks;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;

import java.util.Arrays;

public class SampleObject1 {
	@Tag(1) int intValue;
	@Tag(2) float floatValue;
	@Tag(3) Short shortValue;
	@Tag(4) long[] longArray;
	@Tag(5) double[] doubleArray;
	@Tag(6) String stringValue;

	public SampleObject1 () {
	}

	SampleObject1 (int intVal, float floatVal, short shortVal, long[] longArr, double[] dblArr, String str) {
		this.intValue = intVal;
		this.floatValue = floatVal;
		this.shortValue = shortVal;
		this.longArray = longArr;
		this.doubleArray = dblArr;
		this.stringValue = str;
	}

	static SampleObject1 createSample () {
		long[] longArray = new long[10];
		for (int i = 0; i < longArray.length; i++)
			longArray[i] = i;

		double[] doubleArray = new double[10];
		for (int i = 0; i < doubleArray.length; i++)
			doubleArray[i] = 0.1 * i;

		return new SampleObject1(123, 123.456f, (short)321, longArray, doubleArray, "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789");
	}

	public boolean equals (Object other) {
		if (this == other) return true;
		if (other == null || getClass() != other.getClass()) return false;
		SampleObject1 obj = (SampleObject1)other;
		return intValue == obj.intValue //
			&& floatValue == obj.floatValue //
			&& shortValue.equals(obj.shortValue) //
			&& Arrays.equals(doubleArray, obj.doubleArray) //
			&& Arrays.equals(longArray, obj.longArray) //
			&& (stringValue == null ? obj.stringValue == null : stringValue.equals(obj.stringValue));
	}
}
