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

import static org.junit.jupiter.api.Assertions.*;

import com.esotericsoftware.kryo.KryoTestCase;
import com.esotericsoftware.kryo.serializers.VersionFieldSerializer.Since;

import org.junit.jupiter.api.Test;

class VersionedFieldSerializerTest extends KryoTestCase {
	{
		supportsCopy = true;
	}

	@Test
	void testVersionFieldSerializer () {
		TestClass object1 = new TestClass();
		object1.moo = 2;
		object1.child = null;
		object1.other = new AnotherClass();
		object1.other.value = "meow";

		kryo.setDefaultSerializer(VersionFieldSerializer.class);
		kryo.register(AnotherClass.class);

		// Make VersionFieldSerializer handle "child" field being null.
		VersionFieldSerializer serializer = new VersionFieldSerializer(kryo, TestClass.class);
		serializer.getField("child").setValueClass(TestClass.class, serializer);
		kryo.register(TestClass.class, serializer);

		TestClass object2 = roundTrip(25, object1);

		assertEquals(object2.moo, object1.moo);
		assertEquals(object2.other.value, object1.other.value);
	}

	public static class TestClass {
		@Since(1) public String text = "something";
		@Since(1) public int moo = 120;
		@Since(2) public long moo2 = 1234120;
		@Since(2) public TestClass child;
		@Since(3) public int zzz = 123;
		@Since(3) public AnotherClass other;

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			TestClass other = (TestClass)obj;
			if (child == null) {
				if (other.child != null) return false;
			} else if (!child.equals(other.child)) return false;
			if (moo != other.moo) return false;
			if (moo2 != other.moo2) return false;
			if (text == null) {
				if (other.text != null) return false;
			} else if (!text.equals(other.text)) return false;
			if (zzz != other.zzz) return false;
			return true;
		}
	}

	public static class AnotherClass {
		@Since(1) String value;
	}

	private static class FutureClass {
		@Since(0) public Integer value;
		@Since(1) public FutureClass2 futureClass2;
		@Since(2) public String futureString = "unchanged";

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			FutureClass other = (FutureClass)obj;
			if (futureString == null) {
				if (other.futureString != null) return false;
			} else if (!futureString.equals(other.futureString)) return false;
			if (futureClass2 == null) {
				if (other.futureClass2 != null) return false;
			} else if (!futureClass2.equals(other.futureClass2)) return false;
			if (value == null) {
				if (other.value != null) return false;
			} else if (!value.equals(other.value)) return false;
			return true;
		}

		/** What equals(Object) would have been before the chunked fields were added to the class. */
		public boolean pastEquals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			FutureClass other = (FutureClass)obj;
			if (futureClass2 == null) {
				if (other.futureClass2 != null) return false;
			} else if (!futureClass2.pastEquals(other.futureClass2)) return false;
			if (value == null) {
				if (other.value != null) return false;
			} else if (!value.equals(other.value)) return false;
			return true;
		}
	}

	private static class FutureClass2 {
		@Since(0) public String text = "something";
		@Since(1) public int moo = 120;
		@Since(2) public long moo2 = 1234120;
		@Since(value = 3) public int zzz = 123;
		@Since(value = 4) public FutureClass2 fc2;

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			FutureClass2 other = (FutureClass2)obj;
			if (fc2 == null) {
				if (other.fc2 != null) return false;
			} else if (!fc2.equals(other.fc2)) return false;
			if (moo != other.moo) return false;
			if (moo2 != other.moo2) return false;
			if (text == null) {
				if (other.text != null) return false;
			} else if (!text.equals(other.text)) return false;
			if (zzz != other.zzz) return false;
			return true;
		}

		/** What equals(Object) would have been before the chunked fields were added to the class. */
		public boolean pastEquals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			FutureClass2 other = (FutureClass2)obj;
			if (moo != other.moo) return false;
			if (moo2 != other.moo2) return false;
			if (text == null) {
				if (other.text != null) return false;
			} else if (!text.equals(other.text)) return false;
			return true;
		}
	}
}
