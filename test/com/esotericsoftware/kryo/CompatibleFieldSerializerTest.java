/* Copyright (c) 2008, Nathan Sweet
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

package com.esotericsoftware.kryo;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import org.apache.commons.lang.builder.EqualsBuilder;

/** @author Nathan Sweet <misc@n4te.com> */
public class CompatibleFieldSerializerTest extends KryoTestCase {
	{
		supportsCopy = true;
	}

	public void testCompatibleFieldSerializer () throws FileNotFoundException {
		TestClass object1 = new TestClass();
		object1.child = new TestClass();
		object1.other = new AnotherClass();
		object1.other.value = "meow";
		kryo.setDefaultSerializer(CompatibleFieldSerializer.class);
		kryo.register(TestClass.class);
		kryo.register(AnotherClass.class);
		roundTrip(107, 107, object1);
	}

	public void testAddedField () throws FileNotFoundException {
		TestClass object1 = new TestClass();
		object1.child = new TestClass();
		object1.other = new AnotherClass();
		object1.other.value = "meow";

		CompatibleFieldSerializer serializer = new CompatibleFieldSerializer(kryo, TestClass.class);
		serializer.removeField("text");
		kryo.register(TestClass.class, serializer);
		kryo.register(AnotherClass.class, new CompatibleFieldSerializer(kryo, AnotherClass.class));
		roundTrip(80, 80, object1);

		kryo.register(TestClass.class, new CompatibleFieldSerializer(kryo, TestClass.class));
		Object object2 = kryo.readClassAndObject(input);
		assertEquals(object1, object2);
	}

	public void testAddedFieldToClassWithManyFields () throws FileNotFoundException {
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

		CompatibleFieldSerializer serializer = new CompatibleFieldSerializer(kryo, ClassWithManyFields.class);
		serializer.removeField("bAdd");
		kryo.register(ClassWithManyFields.class, serializer);
		roundTrip(226, 226, object1);

		kryo.register(ClassWithManyFields.class, new CompatibleFieldSerializer(kryo, ClassWithManyFields.class));
		Object object2 = kryo.readClassAndObject(input);
		assertEquals(object1, object2);
	}

	public void testRemovedField () throws FileNotFoundException {
		TestClass object1 = new TestClass();
		object1.child = new TestClass();

		kryo.register(TestClass.class, new CompatibleFieldSerializer(kryo, TestClass.class));
		roundTrip(94, 94, object1);

		CompatibleFieldSerializer serializer = new CompatibleFieldSerializer(kryo, TestClass.class);
		serializer.removeField("text");
		kryo.register(TestClass.class, serializer);
		Object object2 = kryo.readClassAndObject(input);
		assertEquals(object1, object2);
	}

	public void testRemovedFieldFromClassWithManyFields () throws FileNotFoundException {
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

		kryo.register(ClassWithManyFields.class, new CompatibleFieldSerializer(kryo, ClassWithManyFields.class));
		roundTrip(236, 236, object1);

		CompatibleFieldSerializer serializer = new CompatibleFieldSerializer(kryo, ClassWithManyFields.class);
		serializer.removeField("bAdd");
		kryo.register(ClassWithManyFields.class, serializer);
		Object object2 = kryo.readClassAndObject(input);
		assertTrue(object2 instanceof ClassWithManyFields);
		assertNull("the bAdd field should be null", ((ClassWithManyFields)object2).bAdd);
		// update the field in order to verify the remainder of the object was deserialized correctly
		((ClassWithManyFields)object2).bAdd = object1.bAdd;
		assertEquals(object1, object2);
	}

	public void testRemovedMultipleFieldsFromClassWithManyFields () throws FileNotFoundException {
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

		kryo.register(ClassWithManyFields.class, new CompatibleFieldSerializer(kryo, ClassWithManyFields.class));
		roundTrip(220, 220, object1);

		CompatibleFieldSerializer serializer = new CompatibleFieldSerializer(kryo, ClassWithManyFields.class);
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

	public void testExtendedClass () throws FileNotFoundException {
		ExtendedTestClass extendedObject = new ExtendedTestClass();

		// this test would fail with DEFAULT field name strategy
		kryo.getFieldSerializerConfig().setCachedFieldNameStrategy(FieldSerializer.CachedFieldNameStrategy.EXTENDED);

		CompatibleFieldSerializer serializer = new CompatibleFieldSerializer(kryo, ExtendedTestClass.class);
		kryo.register(ExtendedTestClass.class, serializer);
		roundTrip(286, 286, extendedObject);

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

		@Override
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
