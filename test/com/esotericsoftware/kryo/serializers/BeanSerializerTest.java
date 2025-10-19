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

package com.esotericsoftware.kryo.serializers;

import com.esotericsoftware.kryo.KryoTestCase;

import org.junit.jupiter.api.Test;

/** @author Nathan Sweet */
class BeanSerializerTest extends KryoTestCase {
	{
		supportsCopy = true;
	}

	@Test
	void testBeanSerializer () {
		kryo.register(TestClass.class, new BeanSerializer(kryo, TestClass.class));

		TestClass test = new TestClass();
		test.setOptional(12);
		test.setNullField("value");
		test.setText("123");
		test.setChild(new TestClass());
		roundTrip(37, test);
		test.setNullField(null);
		roundTrip(33, test);
	}

	public static class TestClass {
		private String text = "something";
		private String nullField;
		private TestClass child;
		private TestClass child2;
		private float abc = 1.2f;
		private int optional;

		public String getText () {
			return text;
		}

		public void setText (String text) {
			this.text = text;
		}

		public String getNullField () {
			return nullField;
		}

		public void setNullField (String nullField) {
			this.nullField = nullField;
		}

		public TestClass getChild () {
			return child;
		}

		public void setChild (TestClass child) {
			this.child = child;
		}

		public TestClass getChild2 () {
			return child2;
		}

		public void setChild2 (TestClass child2) {
			this.child2 = child2;
		}

		public float getAbc () {
			return abc;
		}

		public void setAbc (float abc) {
			this.abc = abc;
		}

		public int getOptional () {
			return optional;
		}

		public void setOptional (int optional) {
			this.optional = optional;
		}

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			TestClass other = (TestClass)obj;
			if (Float.floatToIntBits(abc) != Float.floatToIntBits(other.abc)) return false;
			if (child == null) {
				if (other.child != null) return false;
			} else if (child != this && !child.equals(other.child)) return false;
			if (child2 == null) {
				if (other.child2 != null) return false;
			} else if (child2 != this && !child2.equals(other.child2)) return false;
			if (nullField == null) {
				if (other.nullField != null) return false;
			} else if (!nullField.equals(other.nullField)) return false;
			if (text == null) {
				if (other.text != null) return false;
			} else if (!text.equals(other.text)) return false;
			return true;
		}
	}
}
