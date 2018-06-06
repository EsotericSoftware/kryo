/* Copyright (c) 2018, Nathan Sweet
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
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.esotericsoftware.kryo;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;

import java.util.Arrays;

public class SampleObject1 {

	@Tag(1)
	int intVal;
	@Tag(2)
	float floatVal;
	@Tag(3)
	Short shortVal;
	@Tag(4)
	long[] longArr;
	@Tag(5)
	double[] dblArr;
	@Tag(6)
	String str;

	public SampleObject1 () {
	}

	SampleObject1 (int intVal, float floatVal, Short shortVal, long[] longArr, double[] dblArr, String str) {
		this.intVal = intVal;
		this.floatVal = floatVal;
		this.shortVal = shortVal;
		this.longArr = longArr;
		this.dblArr = dblArr;
		this.str = str;
	}

	static SampleObject1 createSample() {
		long[] longArr = new long[10];
		for (int i = 0; i < longArr.length; i++)
			longArr[i] = i;

		double[] dblArr = new double[10];
		for (int i = 0; i < dblArr.length; i++)
			dblArr[i] = 0.1 * i;

		return new SampleObject1(123, 123.456f, (short)321, longArr, dblArr, "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789");
	}

	@Override
	public boolean equals (Object other) {
		if (this == other) return true;

		if (other == null || getClass() != other.getClass()) return false;

		SampleObject1 obj = (SampleObject1)other;

		return intVal == obj.intVal && floatVal == obj.floatVal && shortVal.equals(obj.shortVal)
				&& Arrays.equals(dblArr, obj.dblArr) && Arrays.equals(longArr, obj.longArr)
				&& (str == null ? obj.str == null : str.equals(obj.str));
	}

}