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
import com.esotericsoftware.kryo.serializers.GenericsTest.A.DontPassToSuper;
import com.esotericsoftware.kryo.serializers.GenericsTest.ClassWithMap.MapKey;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GenericsTest extends KryoTestCase {
	{
		supportsCopy = true;
	}

	@BeforeEach
	public void setUp () throws Exception {
		super.setUp();
	}

	@Test
	void testGenericClassWithGenericFields () {
		kryo.setReferences(true);
		kryo.setRegistrationRequired(false);
		kryo.register(BaseGeneric.class);

		List list = Arrays.asList(new SerializableObjectFoo("one"), new SerializableObjectFoo("two"),
			new SerializableObjectFoo("three"));
		BaseGeneric<SerializableObjectFoo> bg1 = new BaseGeneric(list);

		roundTrip(117, bg1);
	}

	@Test
	void testNonGenericClassWithGenericSuperclass () {
		kryo.setReferences(true);
		kryo.setRegistrationRequired(false);
		kryo.register(BaseGeneric.class);
		kryo.register(ConcreteClass.class);

		List list = Arrays.asList(new SerializableObjectFoo("one"), new SerializableObjectFoo("two"),
			new SerializableObjectFoo("three"));
		ConcreteClass cc1 = new ConcreteClass(list);

		roundTrip(117, cc1);
	}

	// Test for/from https://github.com/EsotericSoftware/kryo/issues/377
	@Test
	void testDifferentTypeArguments () {
		LongHolder o1 = new LongHolder(1L);
		LongListHolder o2 = new LongListHolder(Arrays.asList(1L));

		kryo.setRegistrationRequired(false);

		roundTrip(65, o1);
		roundTrip(99, o2);
	}

	// https://github.com/EsotericSoftware/kryo/issues/611
	@Test
	void testSuperGenerics () {
		kryo.register(SuperGenerics.Root.class);
		kryo.register(SuperGenerics.Value.class);

		SuperGenerics.Root root = new SuperGenerics.Root();
		root.rootSuperField = new SuperGenerics.Value();

		roundTrip(4, root);
	}

	// https://github.com/EsotericSoftware/kryo/issues/648
	@Test
	void testMapTypeParams () {
		ClassWithMap hasMap = new ClassWithMap();
		MapKey key = new MapKey();
		key.field1 = "foo";
		key.field2 = "bar";
		HashSet set = new HashSet();
		set.add("one");
		set.add("two");
		hasMap.values.put(key, set);

		kryo.register(ClassWithMap.class);
		kryo.register(MapKey.class);
		kryo.register(HashMap.class);
		kryo.register(HashSet.class);

		roundTrip(18, hasMap);
	}

	// https://github.com/EsotericSoftware/kryo/issues/622
	@Test
	void testNotPassingToSuper () {
		kryo.register(DontPassToSuper.class);
		kryo.copy(new DontPassToSuper<>());
	}

	// Test for https://github.com/EsotericSoftware/kryo/issues/654
	@Test
	void testFieldWithGenericInterface () {
		ClassWithGenericInterfaceField.A o = new ClassWithGenericInterfaceField.A();

		kryo.setRegistrationRequired(false);

		roundTrip(170, o);
	}

	// Test for https://github.com/EsotericSoftware/kryo/issues/655
	@Test
	void testFieldWithGenericArrayType() {
		ClassArrayHolder o = new ClassArrayHolder(new Class[] {});

		kryo.setRegistrationRequired(false);

		roundTrip(70, o);
	}

	// Test for https://github.com/EsotericSoftware/kryo/issues/940
	@Test
	void testFieldWithStringArrayType () {
		StringArray array = new StringArray(new String[] {"1"});
		
		kryo.setRegistrationRequired(false);

		roundTrip(67, array);
	}

	// Test for https://github.com/EsotericSoftware/kryo/issues/940
	@Test
	void testFieldWithNumberArrayType () {
		NumberArray<Integer> array = new NumberArray<>(new Integer[] {1});
		NumberArrayHolder container = new NumberArrayHolder(array);
		
		kryo.setRegistrationRequired(false);

		roundTrip(137, container);
	}

	// Test for https://github.com/EsotericSoftware/kryo/issues/940
	@Test
	void testFieldWithObjectArrayType () {
		ObjectArray<TestObject> array = new ObjectArray<>(new TestObject[] {new TestObject(1)});
		ObjectArrayHolder container = new ObjectArrayHolder(array);
		
		kryo.setRegistrationRequired(false);

		roundTrip(265, container);
	}

	// Test for https://github.com/EsotericSoftware/kryo/issues/655
	@Test
	void testClassWithMultipleGenericTypes() {
		HolderWithAdditionalGenericType<String, Integer> o = new HolderWithAdditionalGenericType<>(1);

		kryo.setRegistrationRequired(false);

		roundTrip(87, o);
	}

	// Test for https://github.com/EsotericSoftware/kryo/issues/655
	@Test
	void testClassHierarchyWithChangingGenericTypeVariables () {
		ClassHierarchyWithChangingTypeVariableNames.A<?> o = new ClassHierarchyWithChangingTypeVariableNames.A<>(Enum.class);

		kryo.setRegistrationRequired(false);

		roundTrip(131, o);
	}

	// Test for https://github.com/EsotericSoftware/kryo/issues/655
	@Test
	void testClassHierarchyWithMultipleTypeVariables () {
		ClassHierarchyWithMultipleTypeVariables.A<Integer, ?> o = new ClassHierarchyWithMultipleTypeVariables.A<>(Enum.class);

		kryo.setRegistrationRequired(false);

		roundTrip(110, o);
	}

	// Test for https://github.com/EsotericSoftware/kryo/issues/721
	@Test
	void testClassHierarchyWithMissingTypeVariables () {
		ClassWithMissingTypeVariable.A o = new ClassWithMissingTypeVariable.A(
				new ClassWithMissingTypeVariable.B<>(1));

		kryo.setRegistrationRequired(false);

		roundTrip(168, o);
	}

	interface Holder<V> {
		V getValue ();
	}

	abstract static class AbstractValueHolder<V> implements Holder<V> {
		private final V value;

		AbstractValueHolder (V value) {
			this.value = value;
		}

		public V getValue () {
			return value;
		}

		@Override
		public boolean equals (Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			final AbstractValueHolder<?> that = (AbstractValueHolder<?>)o;
			return Objects.deepEquals(value, that.value);
		}
	}

	abstract static class AbstractValueListHolder<V> extends AbstractValueHolder<List<V>> {
		AbstractValueListHolder (List<V> value) {
			super(value);
		}
	}

	static class LongHolder extends AbstractValueHolder<Long> {
		/** Kryo Constructor */
		LongHolder () {
			super(null);
		}

		LongHolder (Long value) {
			super(value);
		}
	}

	static class LongListHolder extends AbstractValueListHolder<Long> {
		/** Kryo Constructor */
		LongListHolder () {
			super(null);
		}

		LongListHolder (java.util.List<Long> value) {
			super(value);
		}
	}

	static class ClassArrayHolder extends AbstractValueHolder<Class<?>[]> {
		/** Kryo Constructor */
		ClassArrayHolder () {
			super(null);
		}

		ClassArrayHolder (Class<?>[] value) {
			super(value);
		}
	}

	static class HolderWithAdditionalGenericType<BT, OT> extends AbstractValueHolder<OT> {
		private BT value;

		/** Kryo Constructor */
		HolderWithAdditionalGenericType () {
			super(null);
		}

		HolderWithAdditionalGenericType(OT value) {
			super(value);
		}

		@Override
		public boolean equals (Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			if (!super.equals(o)) return false;
			final HolderWithAdditionalGenericType<?, ?> that = (HolderWithAdditionalGenericType<?, ?>)o;
			return Objects.equals(value, that.value);
		}
	}

	// A simple serializable class.
	public static class SerializableObjectFoo implements Serializable {
		String name;

		SerializableObjectFoo (String name) {
			this.name = name;
		}

		public SerializableObjectFoo () {
			name = "Default";
		}

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			SerializableObjectFoo other = (SerializableObjectFoo)obj;
			if (name == null) {
				if (other.name != null) return false;
			} else if (!name.equals(other.name)) return false;
			return true;
		}
	}

	static class BaseGeneric<T extends Serializable> {
		// The type of this field cannot be derived from the context.
		// Therefore, Kryo should consider it to be Object.
		private final List<T> listPayload;

		/** Kryo Constructor */
		BaseGeneric () {
			super();
			this.listPayload = null;
		}

		BaseGeneric (final List<T> listPayload) {
			super();
			// Defensive copy, listPayload is mutable
			this.listPayload = new ArrayList(listPayload);
		}

		public final List<T> getPayload () {
			return this.listPayload;
		}

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			BaseGeneric other = (BaseGeneric)obj;
			if (listPayload == null) {
				if (other.listPayload != null) return false;
			} else if (!listPayload.equals(other.listPayload)) return false;
			return true;
		}

	}

	// This is a non-generic class with a generic superclass.
	static class ConcreteClass2 extends BaseGeneric<SerializableObjectFoo> {
		/** Kryo Constructor */
		ConcreteClass2 () {
			super();
		}

		public ConcreteClass2 (final List listPayload) {
			super(listPayload);
		}
	}

	static class ConcreteClass1 extends ConcreteClass2 {
		/** Kryo Constructor */
		ConcreteClass1 () {
			super();
		}

		public ConcreteClass1 (final List listPayload) {
			super(listPayload);
		}
	}

	static class ConcreteClass extends ConcreteClass1 {
		/** Kryo Constructor */
		ConcreteClass () {
			super();
		}

		public ConcreteClass (final List listPayload) {
			super(listPayload);
		}
	}

	static class SuperGenerics {
		public static class RootSuper<RS> {
			public ValueSuper<RS> rootSuperField;

			@Override
			public boolean equals (Object o) {
				if (this == o) return true;
				if (o == null || getClass() != o.getClass()) return false;
				final RootSuper<?> rootSuper = (RootSuper<?>)o;
				return Objects.equals(rootSuperField, rootSuper.rootSuperField);
			}
		}

		public static class Root extends RootSuper<String> {
		}

		public static class ValueSuper<VS> extends ValueSuperSuper<Integer> {
			VS superField;
		}

		public static class ValueSuperSuper<VSS> {
			VSS superSuperField;
		}

		public static class Value extends ValueSuper<String> {
			@Override
			public boolean equals (Object o) {
				if (this == o) return true;
				return (o != null && getClass() == o.getClass());
			}
		}
	}

	static class ClassWithMap {
		public final Map<MapKey, Set<String>> values = new HashMap();

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			ClassWithMap other = (ClassWithMap)obj;
			if (values == null) {
				if (other.values != null) return false;
			} else if (!values.toString().equals(other.values.toString())) return false;
			return true;
		}

		public static class MapKey {
			public String field1, field2;

			public String toString () {
				return field1 + ":" + field2;
			}
		}
	}

	static class A<X> {
		public static class B<Y> extends A {
		}

		public static class DontPassToSuper<Z> extends B {
			B<Z> b;
		}
	}

	static class ClassWithGenericInterfaceField {
		static class A extends B<String> {
			A () {
				super(new C());
			}
		}

		static class B<T> {
			Supplier<T> s;

			B (Supplier<T> s) {
				this.s = s;
			}

			@Override
			public boolean equals(Object o) {
				if (this == o) return true;
				if (o == null || getClass() != o.getClass()) return false;
				final B<?> b = (B<?>) o;
				return Objects.equals(s.get(), b.s.get());
			}

			@Override
			public int hashCode() {
				return Objects.hash(s);
			}
		}

		static class C implements Supplier<String>, Serializable {
			@Override
			public String get () {
				return null;
			}
		}
	}

	static class ClassHierarchyWithChangingTypeVariableNames {
		static final class A<T> extends B<T> {
			T d;

			/** Kryo Constructor */
			A () {
			}

			A (T d) {
				this.d = d;
			}

			@Override
			public boolean equals (Object o) {
				if (this == o) return true;
				if (o == null || getClass() != o.getClass()) return false;
				final A<?> a = (A<?>)o;
				return Objects.equals(d, a.d);
			}
		}

		static class B<E> extends C<E> {
		}

		static class C<E> {
		}
	}

	static class ClassHierarchyWithMultipleTypeVariables {
		static class A<T, S> extends B<T> {
			Class<S> s;

			/** Kryo Constructor */
			A () {
			}

			A (Class<S> s) {
				this.s = s;
			}

			@Override
			public boolean equals (Object o) {
				if (this == o) return true;
				if (o == null || getClass() != o.getClass()) return false;
				final A<?, ?> a = (A<?, ?>)o;
				return Objects.equals(s, a.s);
			}
		}

		static class B<T> extends C<T> {
		}

		public static class C<T> {
		}
	}

	static class ClassWithMissingTypeVariable {
		static final class A {
			C<String> c;

			/** Kryo Constructor */
			A () {
			}

			A (C<String> c) {
				this.c = c;
			}

			@Override
			public boolean equals (Object o) {
				if (this == o) return true;
				if (o == null || getClass() != o.getClass()) return false;
				final A a = (A)o;
				return Objects.equals(c, a.c);
			}
		}

		static class B<R, V> implements C<V> {
			R r;

			/** Kryo Constructor */
			B () {
			}

			B (R r) {
				this.r = r;
			}

			@Override
			public boolean equals(Object o) {
				if (this == o) return true;
				if (o == null || getClass() != o.getClass()) return false;
				final B<?, ?> b = (B<?, ?>) o;
				return Objects.equals(r, b.r);
			}
		}

		interface C<T> {
		}
	}

	public static class StringArray {

		private String[] values;

		public StringArray() {
		}

		public StringArray(String[] array) {
			this.values = array;
		}

		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			StringArray that = (StringArray) o;
			return Arrays.equals(values, that.values);
		}
	}

	public static class NumberArray<V extends Number> {

		private V[] values;

		public NumberArray() {
		}

		public NumberArray(V[] array) {
			this.values = array;
		}

		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			NumberArray<?> that = (NumberArray<?>) o;
			return Arrays.equals(values, that.values);
		}
	}

	public static class NumberArrayHolder {

		private NumberArray<Integer> field;

		public NumberArrayHolder() {
		}

		public NumberArrayHolder(NumberArray<Integer> array) {
			this.field = array;
		}

		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			NumberArrayHolder that = (NumberArrayHolder) o;
			return Objects.equals(field, that.field);
		}
	}

	public static class ObjectArray<V> {

		private V[] values;

		public ObjectArray() {
		}

		public ObjectArray(V[] array) {
			this.values = array;
		}

		public boolean equals (Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			ObjectArray<?> that = (ObjectArray<?>)o;
			return Arrays.equals(values, that.values);
		}
	}

	public static class ObjectArrayHolder {

		private ObjectArray<TestObject> field;

		public ObjectArrayHolder() {
		}

		public ObjectArrayHolder(ObjectArray<TestObject> array) {
			this.field = array;
		}

		public boolean equals (Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			ObjectArrayHolder that = (ObjectArrayHolder)o;
			return Objects.equals(field, that.field);
		}
	}

	public static class TestObject {
		private int i;

		public TestObject() {
		}

		public TestObject(int i) {
			this.i = i;
		}

		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			TestObject that = (TestObject) o;
			return i == that.i;
		}
	}
}
