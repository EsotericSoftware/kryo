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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.KryoTestCase;
import com.esotericsoftware.kryo.SerializerFactory.CompatibleFieldSerializerFactory;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.jupiter.api.Test;

/** @author Nathan Sweet */
class CompatibleFieldSerializerTest extends KryoTestCase {
	{
		supportsCopy = true;
	}

	@Test
	void testCompatibleFieldSerializer () {
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
	void testAddedField () {
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
	void testAddedFieldToClassWithManyFields () {
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
	void testRemovedField () {
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
	void testChangeFieldTypeWithChunkedEncodingEnabled () {
		testChangeFieldType(16, true);
	}

	@Test
	void testChangeFieldTypeWithChunkedEncodingDisabled () {
		assertThrows(KryoException.class, () -> testChangeFieldType(14, false),
				"Read type is incompatible with the field type: String -> Long");
	}

	private void testChangeFieldType(int length, boolean chunked) {
		CompatibleFieldSerializer<ClassWithStringField> serializer = new CompatibleFieldSerializer<>(kryo, ClassWithStringField.class);
		serializer.getCompatibleFieldSerializerConfig().setChunkedEncoding(chunked);
		kryo.setReferences(false);
		kryo.register(ClassWithStringField.class, serializer);

		roundTrip(length, new ClassWithStringField("Hacker"));

		final Kryo otherKryo = new Kryo();
		CompatibleFieldSerializer<AnotherClass> otherSerializer = new CompatibleFieldSerializer<>(kryo, ClassWithLongField.class);
		otherSerializer.getCompatibleFieldSerializerConfig().setChunkedEncoding(chunked);
		otherKryo.setReferences(false);
		otherKryo.register(ClassWithLongField.class, otherSerializer);

		final ClassWithLongField o = (ClassWithLongField) otherKryo.readClassAndObject(input);
		assertNull(o.value);
	}

	@Test
	void testChangePrimitiveAndWrapperFieldTypes () {
		testChangePrimitiveAndWrapperFieldTypes(22, true);
		testChangePrimitiveAndWrapperFieldTypes(18, false);
	}

	private void testChangePrimitiveAndWrapperFieldTypes (int length, boolean chunked) {
		CompatibleFieldSerializer<ClassWithPrimitiveAndWrapper> serializer = new CompatibleFieldSerializer<>(kryo, ClassWithPrimitiveAndWrapper.class);
		serializer.getCompatibleFieldSerializerConfig().setChunkedEncoding(chunked);
		kryo.setReferences(false);
		kryo.register(ClassWithPrimitiveAndWrapper.class, serializer);

		roundTrip(length, new ClassWithPrimitiveAndWrapper(1, 1L));

		final Kryo otherKryo = new Kryo();
		CompatibleFieldSerializer<ClassWithWrapperAndPrimitive> otherSerializer = new CompatibleFieldSerializer<>(kryo, ClassWithWrapperAndPrimitive.class);
		otherSerializer.getCompatibleFieldSerializerConfig().setChunkedEncoding(chunked);
		otherKryo.setReferences(false);
		otherKryo.register(ClassWithWrapperAndPrimitive.class, otherSerializer);

		ClassWithWrapperAndPrimitive o = (ClassWithWrapperAndPrimitive) otherKryo.readClassAndObject(input);
		assertEquals(1L, o.value1, 0);
		assertEquals(1, o.value2);
	}

	@Test
	void testRemovedFieldFromClassWithManyFields () {
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
		assertNull(((ClassWithManyFields)object2).bAdd, "the bAdd field should be null");
		// update the field in order to verify the remainder of the object was deserialized correctly
		((ClassWithManyFields)object2).bAdd = object1.bAdd;
		assertEquals(object1, object2);
	}

	@Test
	void testRemovedMultipleFieldsFromClassWithManyFields () {
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
		assertNull(object2.bb, "bb should be null");
		assertNull(object2.cc, "cc should be null");
		assertNull(object2.dd, "dd should be null");
		// update the fields to verify the remainder of the object was deserialized correctly
		object2.bb = object1.bb;
		object2.cc = object1.cc;
		object2.dd = object1.dd;
		assertEquals(object1, object2);
	}

	@Test
	void testRemoveAllFieldsFromClassWithManyFields () {
		CompatibleFieldSerializer serializer = new CompatibleFieldSerializer<>(kryo, ClassWithManyFields.class);
		kryo.register(ClassWithManyFields.class, serializer);

		ClassWithManyFields object1 = new ClassWithManyFields();
		roundTrip(118, object1);

		for (FieldSerializer.CachedField field : serializer.getFields()) {
			serializer.removeField(field.getName());
		}

		ClassWithManyFields object2 = (ClassWithManyFields)kryo.readClassAndObject(input);
		assertEquals(object1, object2);
	}

	@Test
	void testExtendedClass () {
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

	@Test
	void testClassWithSuperTypeFields() {
		kryo.setReferences(false);
		kryo.setRegistrationRequired(false);

		CompatibleFieldSerializer<ClassWithSuperTypeFields> serializer = new CompatibleFieldSerializer<>(kryo,
			ClassWithSuperTypeFields.class);
		CompatibleFieldSerializer.CompatibleFieldSerializerConfig config = serializer.getCompatibleFieldSerializerConfig();
		config.setChunkedEncoding(true);
		config.setReadUnknownFieldData(true);
		kryo.register(ClassWithSuperTypeFields.class, serializer);

		roundTrip(71, new ClassWithSuperTypeFields("foo", Arrays.asList("bar"), "baz"));
	}

	// https://github.com/EsotericSoftware/kryo/issues/774
	@Test
	void testClassWithObjectField() {
		CompatibleFieldSerializer<ClassWithObjectField> serializer = new CompatibleFieldSerializer<>(kryo, ClassWithObjectField.class);
		CompatibleFieldSerializer.CompatibleFieldSerializerConfig config = serializer.getCompatibleFieldSerializerConfig();
		config.setChunkedEncoding(true);
		config.setReadUnknownFieldData(true);
		kryo.register(ClassWithObjectField.class, serializer);

		Output output1 = new Output(4096, Integer.MAX_VALUE);
		final ClassWithObjectField o1 = new ClassWithObjectField(123);
		kryo.writeClassAndObject(output1, o1);

		Output output2 = new Output(4096, Integer.MAX_VALUE);
		final ClassWithObjectField o2 = new ClassWithObjectField("foo");
		kryo.writeClassAndObject(output2, o2);

		assertEquals(o1, kryo.readClassAndObject(new Input(output1.getBuffer())));
		assertEquals(o2, kryo.readClassAndObject(new Input(output2.getBuffer())));
	}

	// https://github.com/EsotericSoftware/kryo/issues/821
	@Test
	void testClassWithLambdaField () {
		CompatibleFieldSerializer.CompatibleFieldSerializerConfig config = new CompatibleFieldSerializer.CompatibleFieldSerializerConfig();
		kryo.setDefaultSerializer(new CompatibleFieldSerializerFactory(config));
		kryo.register(ClassWithLambdaField.class);
		kryo.register(ClosureSerializer.Closure.class, new ClosureSerializer());

		roundTrip(236, new ClassWithLambdaField());
	}

	// https://github.com/EsotericSoftware/kryo/issues/840
	@Test
	void testClassWithGenericField () {
		CompatibleFieldSerializer.CompatibleFieldSerializerConfig config = new CompatibleFieldSerializer.CompatibleFieldSerializerConfig();
		kryo.setDefaultSerializer(new CompatibleFieldSerializerFactory(config));
		kryo.register(ClassWithGenericField.class);

		roundTrip(9, new ClassWithGenericField<>(1));
	}

	public static class TestClass {
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

	public static class ExtendedTestClass extends TestClass {
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

	public static class AnotherClass {
		String value;

		public AnotherClass () {
		}

		public AnotherClass (String value) {
			this.value = value;
		}

		@Override
		public boolean equals (Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			final AnotherClass that = (AnotherClass)o;
			return Objects.equals(value, that.value);
		}

		@Override
		public int hashCode () {
			return Objects.hash(value);
		}
	}

	public static class ClassWithManyFields {
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

	public static class ClassWithPrimitiveAndWrapper {
		long value1;
		Long value2;

		public ClassWithPrimitiveAndWrapper () {
		}

		public ClassWithPrimitiveAndWrapper (long value1, Long value2) {
			this.value1 = value1;
			this.value2 = value2;
		}

		@Override
		public boolean equals (Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			final ClassWithPrimitiveAndWrapper that = (ClassWithPrimitiveAndWrapper)o;
			return value1 == that.value1 && Objects.equals(value2, that.value2);
		}
	}

	public static class ClassWithWrapperAndPrimitive {
		Long value1;
		long value2;

		public ClassWithWrapperAndPrimitive() {
		}

		@Override
		public boolean equals (Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			final ClassWithWrapperAndPrimitive that = (ClassWithWrapperAndPrimitive)o;
			return value1.equals(that.value1) && value2 == that.value2;
		}
	}

	public static class ClassWithSuperTypeFields {
		private Object value;
		private Iterable<?> list;
		private Serializable serializable;

		public ClassWithSuperTypeFields () {
		}

		public ClassWithSuperTypeFields (Object value, List<?> list, Serializable serializable) {
			this.value = value;
			this.list = list;
			this.serializable = serializable;
		}

		@Override
		public boolean equals (Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			ClassWithSuperTypeFields that = (ClassWithSuperTypeFields)o;
			return Objects.equals(value, that.value) && Objects.equals(list, that.list)
				&& Objects.equals(serializable, that.serializable);
		}

		@Override
		public int hashCode () {
			return Objects.hash(value, list, serializable);
		}
	}

	public static class ClassWithObjectField {
		Object value;

		public ClassWithObjectField() { }

		public ClassWithObjectField(Object value) {
			this.value = value;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			ClassWithObjectField wrapper = (ClassWithObjectField) o;
			return Objects.equals(value, wrapper.value);
		}
	}

	public static class ClassWithStringField {
		String value;

		public ClassWithStringField() {
		}

		public ClassWithStringField(String value) {
			this.value = value;
		}

		@Override
		public boolean equals (Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			final ClassWithStringField that = (ClassWithStringField)o;
			return Objects.equals(value, that.value);
		}
	}

	public static class ClassWithLongField {
		Long value;

		public ClassWithLongField() {
		}

		public ClassWithLongField(Long value) {
			this.value = value;
		}

		@Override
		public boolean equals (Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			final ClassWithLongField that = (ClassWithLongField)o;
			return Objects.equals(value, that.value);
		}
	}

	public static class ClassWithLambdaField {

		@FunctionalInterface
		public interface Callback extends Serializable {
			int call();
		}

		private final Callback callback;

		public ClassWithLambdaField () {
			callback = () -> 1;
		}

		public boolean equals (Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			ClassWithLambdaField that = (ClassWithLambdaField)o;
			return Objects.equals(callback.call(), that.callback.call());
		}
	}

	public static class ClassWithGenericField<T extends Comparable<T>> {

		private T value;

		public ClassWithGenericField() {
		}

		public ClassWithGenericField(T value) {
			this.value = value;
		}

		public boolean equals (Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			ClassWithGenericField<?> that = (ClassWithGenericField<?>)o;
			return Objects.equals(value, that.value);
		}
	}
}
