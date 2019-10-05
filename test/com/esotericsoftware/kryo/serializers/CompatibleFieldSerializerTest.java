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

package com.esotericsoftware.kryo.serializers;

import static org.junit.Assert.*;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoTestCase;
import com.esotericsoftware.kryo.SerializerFactory.CompatibleFieldSerializerFactory;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.junit.Test;

/** @author Nathan Sweet */
public class CompatibleFieldSerializerTest extends KryoTestCase {
	{
		supportsCopy = true;
	}

	@Test
	public void testCompatibleFieldSerializer () {
		testCompatibleFieldSerializer(83, false, false);
		testCompatibleFieldSerializer(116, false, true);
		testCompatibleFieldSerializer(80, true, false);
		testCompatibleFieldSerializer(113, true, true);
	}

	private void testCompatibleFieldSerializer (int length, boolean references, final boolean chunked) {
		kryo.setReferences(references);

		CompatibleFieldSerializerFactory factory = new CompatibleFieldSerializerFactory() {
			public CompatibleFieldSerializer newSerializer (Kryo kryo, Class type) {
				CompatibleFieldSerializer serializer = super.newSerializer(kryo, type);
				serializer.getCompatibleFieldSerializerConfig().setChunkedEncoding(chunked);
				return serializer;
			}
		};
		kryo.register(TestClass.class, factory.newSerializer(kryo, TestClass.class));
		kryo.register(AnotherClass.class, factory.newSerializer(kryo, AnotherClass.class));

		TestClass object1 = new TestClass();
		object1.child = new TestClass();
		object1.other = new AnotherClass();
		object1.other.value = "meow";
		roundTrip(length, object1);
	}

	@Test
	public void testAddedField () {
		testAddedField(59, false, false);
		testAddedField(87, false, true);
		testAddedField(63, true, false);
		testAddedField(91, true, true);
	}

	private void testAddedField (int length, boolean references, boolean chunked) {
		kryo.setReferences(references);

		CompatibleFieldSerializer serializer = new CompatibleFieldSerializer(kryo, AnotherClass.class);
		serializer.getCompatibleFieldSerializerConfig().setChunkedEncoding(chunked);
		kryo.register(AnotherClass.class, serializer);

		serializer = new CompatibleFieldSerializer(kryo, TestClass.class);
		serializer.getCompatibleFieldSerializerConfig().setChunkedEncoding(chunked);
		kryo.register(TestClass.class, serializer);

		TestClass object1 = new TestClass();
		object1.child = new TestClass();
		object1.other = new AnotherClass();
		object1.other.value = "meow";

		serializer.removeField("text");
		roundTrip(length, object1);

		serializer.updateFields();
		Object object2 = kryo.readClassAndObject(input);
		assertEquals(object1, object2);
	}

	@Test
	public void testAddedFieldToClassWithManyFields () {
		testAddedFieldToClassWithManyFields(189, false, false, true);
		testAddedFieldToClassWithManyFields(152, false, false, false);

		testAddedFieldToClassWithManyFields(263, false, true, true);
		testAddedFieldToClassWithManyFields(226, false, true, false);

		testAddedFieldToClassWithManyFields(227, true, false, true);
		testAddedFieldToClassWithManyFields(190, true, false, false);

		testAddedFieldToClassWithManyFields(301, true, true, true);
		testAddedFieldToClassWithManyFields(264, true, true, false);
	}

	private void testAddedFieldToClassWithManyFields (int length, boolean references, boolean chunked,
		boolean readUnknownTagData) {
		kryo.setReferences(references);

		CompatibleFieldSerializer serializer = new CompatibleFieldSerializer(kryo, ClassWithManyFields.class);
		serializer.getCompatibleFieldSerializerConfig().setReadUnknownFieldData(readUnknownTagData);
		serializer.getCompatibleFieldSerializerConfig().setChunkedEncoding(chunked);
		kryo.register(ClassWithManyFields.class, serializer);

		// class must have more than CompatibleFieldSerializer#THRESHOLD_BINARY_SEARCH number of fields
		ClassWithManyFields object1 = new ClassWithManyFields();
		object1.aa = "aa";
		object1.a0 = "a0";
		object1.bb = "bb";
		object1.b0 = "b0";
		object1.cc = "cc";
		object1.c0 = "c0";
		object1.dd = "dd";
		object1.d0 = "d0";
		object1.ee = "ee";
		object1.e0 = "e0";
		object1.ff = "ff";
		object1.f0 = "f0";
		object1.gg = "gg";
		object1.g0 = "g0";
		object1.hh = "hh";
		object1.h0 = "h0";
		object1.ii = "ii";
		object1.i0 = "i0";
		object1.jj = "jj";
		object1.j0 = "j0";
		object1.kk = "kk";
		object1.k0 = "k0";
		object1.ll = "ll";
		object1.mm = "mm";
		object1.nn = "nn";
		object1.oo = "oo";
		object1.pp = "pp";
		object1.qq = "qq";
		object1.rr = "rr";
		object1.ss = "ss";
		object1.tt = "tt";
		object1.uu = "uu";
		object1.vv = "vv";
		object1.ww = "ww";
		object1.xx = "xx";
		object1.yy = "yy";
		object1.zz = "zzaa";

		serializer.removeField("bAdd");
		kryo.register(ClassWithManyFields.class, serializer);
		roundTrip(length, object1);

		serializer.updateFields();
		Object object2 = kryo.readClassAndObject(input);
		assertEquals(object1, object2);
	}

	@Test
	public void testRemovedField () {
		testRemovedField(92, false, false);
		testRemovedField(125, false, true);
		testRemovedField(87, true, false);
		testRemovedField(120, true, true);
	}

	private void testRemovedField (int length, boolean references, boolean chunked) {
		kryo.setReferences(references);

		CompatibleFieldSerializer serializer = new CompatibleFieldSerializer(kryo, AnotherClass.class);
		serializer.getCompatibleFieldSerializerConfig().setChunkedEncoding(chunked);
		kryo.register(AnotherClass.class, serializer);

		serializer = new CompatibleFieldSerializer(kryo, TestClass.class);
		serializer.getCompatibleFieldSerializerConfig().setChunkedEncoding(chunked);
		kryo.register(TestClass.class, serializer);

		TestClass object1 = new TestClass();
		object1.text = "so much fun";
		object1.child = new TestClass();
		object1.other = new AnotherClass();
		object1.other.value = object1.text;

		TestClass object2 = roundTrip(length, object1);
		assertEquals("so much fun", object2.text);
		assertEquals("so much fun", object1.other.value);

		serializer.removeField("text");
		kryo.register(TestClass.class, serializer);
		object2 = (TestClass)kryo.readClassAndObject(input);
		assertEquals("so much fun", object2.other.value);
		assertEquals("something", object2.text);
		object2.text = "so much fun";
		assertEquals(object1, object2);
	}

	@Test
	public void testRemovedFieldFromClassWithManyFields () {
		testRemovedFieldFromClassWithManyFields(198, false, false, true);
		// testRemovedFieldFromClassWithManyFields(0, false, false, false); // Doesn't support remove.

		testRemovedFieldFromClassWithManyFields(274, false, true, true);
		testRemovedFieldFromClassWithManyFields(236, false, true, false);

		testRemovedFieldFromClassWithManyFields(237, true, false, true);
		// testRemovedFieldFromClassWithManyFields(0, true, false, false); // Doesn't support remove.

		testRemovedFieldFromClassWithManyFields(313, true, true, true);
		testRemovedFieldFromClassWithManyFields(275, true, true, false);
	}

	private void testRemovedFieldFromClassWithManyFields (int length, boolean references, boolean chunked,
		boolean readUnknownTagData) {
		kryo.setReferences(references);

		CompatibleFieldSerializer serializer = new CompatibleFieldSerializer(kryo, ClassWithManyFields.class);
		serializer.getCompatibleFieldSerializerConfig().setReadUnknownFieldData(readUnknownTagData);
		serializer.getCompatibleFieldSerializerConfig().setChunkedEncoding(chunked);
		kryo.register(ClassWithManyFields.class, serializer);

		// class must have more than CompatibleFieldSerializer#THRESHOLD_BINARY_SEARCH number of fields
		ClassWithManyFields object1 = new ClassWithManyFields();
		object1.aa = "aa";
		object1.a0 = "a0";
		object1.bAdd = "bAdd";
		object1.bb = "bb";
		object1.b0 = "b0";
		object1.cc = "cc";
		object1.c0 = "c0";
		object1.dd = "dd";
		object1.d0 = "d0";
		object1.ee = "ee";
		object1.e0 = "e0";
		object1.ff = "ff";
		object1.f0 = "f0";
		object1.gg = "gg";
		object1.g0 = "g0";
		object1.hh = "hh";
		object1.h0 = "h0";
		object1.ii = "ii";
		object1.i0 = "i0";
		object1.jj = "jj";
		object1.j0 = "j0";
		object1.kk = "kk";
		object1.k0 = "k0";
		object1.ll = "ll";
		object1.mm = "mm";
		object1.nn = "nn";
		object1.oo = "oo";
		object1.pp = "pp";
		object1.qq = "qq";
		object1.rr = "rr";
		object1.ss = "ss";
		object1.tt = "tt";
		object1.uu = "uu";
		object1.vv = "vv";
		object1.ww = "ww";
		object1.xx = "xx";
		object1.yy = "yy";
		object1.zz = "zzaa";

		roundTrip(length, object1);

		serializer.removeField("bAdd");
		kryo.register(ClassWithManyFields.class, serializer);
		Object object2 = kryo.readClassAndObject(input);
		assertTrue(object2 instanceof ClassWithManyFields);
		assertNull("the bAdd field should be null", ((ClassWithManyFields)object2).bAdd);
		// update the field in order to verify the remainder of the object was deserialized correctly
		((ClassWithManyFields)object2).bAdd = object1.bAdd;
		assertEquals(object1, object2);
	}

	@Test
	public void testRemovedMultipleFieldsFromClassWithManyFields () {
		testRemovedMultipleFieldsFromClassWithManyFields(170, false, false, true);
		// testRemovedMultipleFieldsFromClassWithManyFields(0, false, false, false); // Doesn't support remove.

		testRemovedMultipleFieldsFromClassWithManyFields(246, false, true, true);
		testRemovedMultipleFieldsFromClassWithManyFields(220, false, true, false);

		testRemovedMultipleFieldsFromClassWithManyFields(197, true, false, true);
		// testRemovedMultipleFieldsFromClassWithManyFields(0, true, false, false); // Doesn't support remove.

		testRemovedMultipleFieldsFromClassWithManyFields(273, true, true, true);
		testRemovedMultipleFieldsFromClassWithManyFields(247, true, true, false);
	}

	private void testRemovedMultipleFieldsFromClassWithManyFields (int length, boolean references, boolean chunked,
		boolean readUnknownTagData) {
		kryo.setReferences(references);

		CompatibleFieldSerializer serializer = new CompatibleFieldSerializer(kryo, ClassWithManyFields.class);
		serializer.getCompatibleFieldSerializerConfig().setReadUnknownFieldData(readUnknownTagData);
		serializer.getCompatibleFieldSerializerConfig().setChunkedEncoding(chunked);
		kryo.register(ClassWithManyFields.class, serializer);

		// class must have more than CompatibleFieldSerializer#THRESHOLD_BINARY_SEARCH number of fields
		ClassWithManyFields object1 = new ClassWithManyFields();
		object1.aa = "aa";
		object1.bb = "bb";
		object1.cc = "cc";
		object1.dd = "dd";
		object1.ee = "ee";
		object1.ff = "ff";
		object1.gg = "gg";
		object1.hh = "hh";
		object1.ii = "ii";
		object1.jj = "jj";
		object1.kk = "kk";
		object1.ll = "ll";
		object1.mm = "mm";
		object1.nn = "nn";
		object1.oo = "oo";
		object1.pp = "pp";
		object1.qq = "qq";
		object1.rr = "rr";
		object1.ss = "ss";
		object1.tt = "tt";
		object1.uu = "uu";
		object1.vv = "vv";
		object1.ww = "ww";
		object1.xx = "xx";
		object1.yy = "yy";
		object1.zz = "zz";

		roundTrip(length, object1);

		serializer.removeField("bb");
		serializer.removeField("cc");
		serializer.removeField("dd");

		kryo.register(ClassWithManyFields.class, serializer);
		ClassWithManyFields object2 = (ClassWithManyFields)kryo.readClassAndObject(input);
		assertNull("bb should be null", object2.bb);
		assertNull("cc should be null", object2.cc);
		assertNull("dd should be null", object2.dd);
		// update the fields to verify the remainder of the object was deserialized correctly
		object2.bb = object1.bb;
		object2.cc = object1.cc;
		object2.dd = object1.dd;
		assertEquals(object1, object2);
	}

	@Test
	public void testExtendedClass () {
		testExtendedClass(270, false, false);
		testExtendedClass(294, false, true);
		testExtendedClass(273, true, false);
		testExtendedClass(297, true, true);
	}

	private void testExtendedClass (int length, boolean references, boolean chunked) {
		kryo.setReferences(references);

		CompatibleFieldSerializer serializer = new CompatibleFieldSerializer(kryo, ExtendedTestClass.class);
		serializer.getCompatibleFieldSerializerConfig().setChunkedEncoding(chunked);
		// this test would fail with DEFAULT field name strategy
		serializer.getCompatibleFieldSerializerConfig().setExtendedFieldNames(true);
		serializer.updateFields();
		kryo.register(ExtendedTestClass.class, serializer);

		ExtendedTestClass extendedObject = new ExtendedTestClass();
		roundTrip(length, extendedObject);

		ExtendedTestClass object2 = (ExtendedTestClass)kryo.readClassAndObject(input);
		assertEquals(extendedObject, object2);
	}

	static public class TestClass {
		public String text = "something";
		public int moo = 120;
		public long moo2 = 1234120;
		public TestClass child;
		public int zzz = 123;
		public AnotherClass other;

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

	static public class ExtendedTestClass extends TestClass {
		// keep the same names of attributes like TestClass
		public String text = "extendedSomething";
		public int moo = 127;
		public long moo2 = 5555;
		public TestClass child;
		public int zzz = 222;
		public AnotherClass other;

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			ExtendedTestClass other = (ExtendedTestClass)obj;

			if (!super.equals(obj)) return false;
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

	static public class AnotherClass {
		String value;
	}

	static public class ClassWithManyFields {
		public String aa;
		public String bb;
		public String bAdd;
		public String cc;
		public String dd;
		public String ee;
		public String ff;
		public String gg;
		public String hh;
		public String ii;
		public String jj;
		public String kk;
		public String ll;
		public String mm;
		public String nn;
		public String oo;
		public String pp;
		public String qq;
		public String rr;
		public String ss;
		public String tt;
		public String uu;
		public String vv;
		public String ww;
		public String xx;
		public String yy;
		public String zz;
		public String a0;
		public String b0;
		public String c0;
		public String d0;
		public String e0;
		public String f0;
		public String g0;
		public String h0;
		public String i0;
		public String j0;
		public String k0;

		public boolean equals (Object obj) {
			if (obj instanceof ClassWithManyFields) {
				ClassWithManyFields other = (ClassWithManyFields)obj;
				return new EqualsBuilder().append(aa, other.aa).append(a0, other.a0).append(bb, other.bb).append(b0, other.b0)
					.append(cc, other.cc).append(c0, other.c0).append(dd, other.dd).append(d0, other.d0).append(ee, other.ee)
					.append(e0, other.e0).append(ff, other.ff).append(f0, other.f0).append(gg, other.gg).append(g0, other.g0)
					.append(hh, other.hh).append(h0, other.h0).append(ii, other.ii).append(i0, other.i0).append(jj, other.jj)
					.append(j0, other.j0).append(kk, other.kk).append(k0, other.k0).append(ll, other.ll).append(mm, other.mm)
					.append(nn, other.nn).append(oo, other.oo).append(pp, other.pp).append(qq, other.qq).append(rr, other.rr)
					.append(ss, other.ss).append(tt, other.tt).append(uu, other.uu).append(vv, other.vv).append(xx, other.xx)
					.append(yy, other.yy).append(zz, other.zz).append(bAdd, other.bAdd).isEquals();
			}
			return false;
		}
	}
}
