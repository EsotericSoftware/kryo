/* Copyright (c) 2008-2026, Nathan Sweet
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

import static com.esotericsoftware.kryo.util.Util.*;
import static org.junit.jupiter.api.Assertions.*;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoTestCase;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.GenericsTest.A.DontPassToSuper;
import com.esotericsoftware.kryo.serializers.FieldSerializer.CachedField;
import com.esotericsoftware.kryo.serializers.GenericsTest.ClassWithMap.MapKey;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objenesis.strategy.StdInstantiatorStrategy;

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

		roundTrip(114, bg1);
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

		roundTrip(114, cc1);
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
	// A parameterized type without type arguments of its own.
	@Test
	void testNonGenericInnerClassOfGenericClass () {
		kryo.setRegistrationRequired(false);
		kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
		roundTrip(Integer.MIN_VALUE, new DeclaredTypes.HolderInner());
	}

	// The super class of an inner class passes a type parameter of the enclosing class to the declared type.
	@Test
	void testTypeParameterOfEnclosingClass () {
		kryo.setRegistrationRequired(false);
		kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
		roundTrip(242, new DeclaredTypes.HolderEnclosing()); // Includes the enclosing instance.
	}

	// The type parameter of the class is not passed to the declared interface.
	@Test
	void testTypeParameterNotPassedToDeclaredType () {
		kryo.setRegistrationRequired(false);
		roundTrip(Integer.MIN_VALUE, new DeclaredTypes.HolderC());
	}

	// The type parameters of the class are passed to the declared super class in a different order.
	@Test
	void testTypeParametersInDifferentOrder () {
		kryo.setRegistrationRequired(false);
		roundTrip(Integer.MIN_VALUE, new DeclaredTypes.HolderBase());
	}

	// The same class is held by fields with different declared types.
	@Test
	void testDifferentDeclaredTypes () {
		kryo.setRegistrationRequired(false);
		roundTrip(Integer.MIN_VALUE, new DeclaredTypes.HolderMulti());
	}

	// A class that extends a generic class as a raw type uses the type arguments of the declared type, like Kryo 5.
	@Test
	void testRawSubclassOfDeclaredType () {
		kryo.setRegistrationRequired(false);
		// Written by Kryo 5.
		byte[] bytes = Base64.getDecoder().decode(
			"AQBjb20uZXNvdGVyaWNzb2Z0d2FyZS5rcnlvLnNlcmlhbGl6ZXJzLkdlbmVyaWNzVGVzdCREZWNsYXJlZFR5cGVzJFJhd1N14gEBamF2YS51dGlsLkFycmF5TGlz9AKCYg==");
		assertEquals(new DeclaredTypes.HolderRaw(), kryo.readObject(new Input(bytes), DeclaredTypes.HolderRaw.class));
		Output output = new Output(1024);
		kryo.writeObject(output, new DeclaredTypes.HolderRaw());
		assertArrayEquals(bytes, output.toBytes());
	}

	// Each type variable needs two entries, so nested generic objects must not overflow the type variable storage.
	@Test
	void testNestedTypeVariables () {
		kryo.setRegistrationRequired(false);
		roundTrip(Integer.MIN_VALUE, new DeclaredTypes.HolderNode());
	}

	// A type argument that can't be resolved must not shift the following type arguments.
	@Test
	void testUnresolvedTypeArgument () {
		kryo.setRegistrationRequired(false);
		roundTrip(Integer.MIN_VALUE, new DeclaredTypes.HolderPair());
	}

	@Test
	void testClassHierarchyWithMissingTypeVariables () {
		ClassWithMissingTypeVariable.A o = new ClassWithMissingTypeVariable.A(
				new ClassWithMissingTypeVariable.B<>(1));

		kryo.setRegistrationRequired(false);

		roundTrip(168, o);
	}

	@Test
	void testSharedGenericTypes () throws Exception {
		// The generic types of fields are shared by all Kryo instances. Many instances in many threads resolve the same generic
		// types at the same time, including the type arguments of a super type (IntMap<String> as a Map).
		int threadCount = 8;
		Thread[] threads = new Thread[threadCount];
		Throwable[] failures = new Throwable[threadCount];
		for (int i = 0; i < threadCount; i++) {
			int index = i;
			threads[i] = new Thread(() -> {
				try {
					for (int ii = 0; ii < 100; ii++) {
						Kryo kryo = new Kryo();
						kryo.setReferences(true);
						kryo.register(SharedGenerics.class);
						kryo.register(SharedGenerics.IntMap.class);
						kryo.register(ArrayList.class);
						kryo.register(HashMap.class);
						SharedGenerics object = new SharedGenerics(index * 1000 + ii);
						Output output = new Output(1024, -1);
						kryo.writeObject(output, object);
						SharedGenerics read = kryo.readObject(new Input(output.toBytes()), SharedGenerics.class);
						assertEquals(object, read);
						assertEquals("" + (index * 1000 + ii), read.map.get(1));
					}
				} catch (Throwable ex) {
					failures[index] = ex;
				}
			});
			threads[i].start();
		}
		for (Thread thread : threads)
			thread.join();
		for (Throwable failure : failures)
			if (failure != null) throw new AssertionError(failure);

		// Unless on Android, where ClassValue is not available, two Kryo instances use the same generic type for a field.
		if (!isAndroid) {
			CachedField field1 = new FieldSerializer(new Kryo(), SharedGenerics.class).getField("map");
			CachedField field2 = new FieldSerializer(new Kryo(), SharedGenerics.class).getField("map");
			assertSame(((ReflectField)field1).genericType, ((ReflectField)field2).genericType);
		}
	}

	static public class SharedGenerics {
		static public class IntMap<V> extends HashMap<Integer, V> {
		}

		IntMap<String> map = new IntMap<>();
		List<String> list = new ArrayList<>();
		Map<String, List<Integer>> nested = new HashMap<>();

		public SharedGenerics () {
		}

		SharedGenerics (int value) {
			map.put(1, "" + value);
			list.add("" + value);
			nested.put("" + value, new ArrayList<>(List.of(value)));
		}

		public boolean equals (Object other) {
			return other instanceof SharedGenerics o && map.equals(o.map) && list.equals(o.list) && nested.equals(o.nested);
		}

		public int hashCode () {
			return map.hashCode();
		}
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
			return Objects.equals(values, other.values);
		}

		@Override
		public int hashCode() {
			return Objects.hash(values);
		}

		public static class MapKey {
			public String field1, field2;

			@Override
			public boolean equals(Object obj) {
				if (this == obj) return true;
				if (!(obj instanceof MapKey)) return false;
				MapKey other = (MapKey) obj;
				return Objects.equals(field1, other.field1) &&
					Objects.equals(field2, other.field2);
			}

			@Override
			public int hashCode() {
				return Objects.hash(field1, field2);
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

	static class DeclaredTypes {
		interface C<T> {
		}

		public static class B<R> implements C<String> {
			public R r;

			public B () {
			}

			B (R r) {
				this.r = r;
			}

			public boolean equals (Object o) {
				return o instanceof B b && Objects.equals(r, b.r);
			}
		}

		public static class HolderC {
			public C<String> c = new B<>(1);

			public boolean equals (Object o) {
				return o instanceof HolderC h && Objects.equals(c, h.c);
			}
		}

		public static class Base<K, V> {
		}

		public static class Sub<A, B> extends Base<B, A> {
			public A a;
			public B b;

			public Sub () {
			}

			Sub (A a, B b) {
				this.a = a;
				this.b = b;
			}

			public boolean equals (Object o) {
				return o instanceof Sub s && Objects.equals(a, s.a) && Objects.equals(b, s.b);
			}
		}

		public static class HolderBase {
			public Base<String, Integer> base = new Sub<>(1, "x");

			public boolean equals (Object o) {
				return o instanceof HolderBase h && Objects.equals(base, h.base);
			}
		}

		public static class Pair<X, Y> {
			public X x;
			public Y y;

			public Pair () {
			}

			Pair (X x, Y y) {
				this.x = x;
				this.y = y;
			}

			public boolean equals (Object o) {
				return o instanceof Pair p && Objects.equals(x, p.x) && Objects.equals(y, p.y);
			}
		}

		public static class MultiBase<M> {
		}

		public interface MultiInterface<I> {
		}

		public static class Multi<T> extends MultiBase<T> implements MultiInterface<T> {
			public List<T> list = new ArrayList<>();

			Multi<T> add (T value) {
				list.add(value);
				return this;
			}

			public boolean equals (Object o) {
				return o instanceof Multi m && Objects.equals(list, m.list);
			}
		}

		public static class HolderMulti {
			public MultiBase<String> base = new Multi<String>().add("a");
			public MultiInterface<Integer> interfaceType = new Multi<Integer>().add(1);
			public Multi<Long> direct = new Multi<Long>().add(2L);
			public MultiBase<Integer> base2 = new Multi<Integer>().add(3);

			public boolean equals (Object o) {
				return o instanceof HolderMulti h && Objects.equals(base, h.base) && Objects.equals(interfaceType, h.interfaceType)
					&& Objects.equals(direct, h.direct) && Objects.equals(base2, h.base2);
			}
		}

		public static class GenericBase<T> {
			public List<T> list = new ArrayList<>();

			public boolean equals (Object o) {
				return o != null && o.getClass() == getClass() && Objects.equals(list, ((GenericBase)o).list);
			}
		}

		public static class RawSub extends GenericBase {
		}

		public static class HolderRaw {
			public GenericBase<String> raw = new RawSub();

			HolderRaw () {
				raw.list.add("b");
			}

			public boolean equals (Object o) {
				return o instanceof HolderRaw h && Objects.equals(raw, h.raw);
			}
		}

		public static class Node<A, B, C> {
			public A a;
			public B b;
			public C c;
			public Node<A, B, C> child;

			public boolean equals (Object o) {
				return o instanceof Node n && Objects.equals(a, n.a) && Objects.equals(b, n.b) && Objects.equals(c, n.c)
					&& Objects.equals(child, n.child);
			}
		}

		public static class HolderNode {
			public Node<String, Integer, Long> node = new Node<>();

			HolderNode () {
				Node<String, Integer, Long> current = node;
				for (int i = 0; i < 5; i++) {
					current.a = "a" + i;
					current.b = i;
					current.c = (long)i;
					current.child = new Node<>();
					current = current.child;
				}
			}

			public boolean equals (Object o) {
				return o instanceof HolderNode h && Objects.equals(node, h.node);
			}
		}

		public static class Outer<T> {
			public class Inner {
				public int value = 1;

				public boolean equals (Object o) {
					return o instanceof Outer.Inner i && value == i.value;
				}
			}
		}

		public static class HolderInner {
			public Outer<String>.Inner inner = new Outer<String>().new Inner();

			public boolean equals (Object o) {
				return o instanceof HolderInner h && Objects.equals(inner, h.inner);
			}
		}

		public static class Enclosing<X> {
			public class Mid<Y> extends Base<X, Y> {
			}

			public class Inner<Z> extends Mid<Z> {
			}
		}

		public static class HolderEnclosing {
			public Base<String, Integer> base = new Enclosing<String>().new Inner<Integer>();

			public boolean equals (Object o) {
				return o instanceof HolderEnclosing h && base.getClass() == h.base.getClass();
			}
		}

		public static class HolderPair<T> {
			public Pair<T, String> pair = new Pair(1, "s");

			public boolean equals (Object o) {
				return o instanceof HolderPair h && Objects.equals(pair, h.pair);
			}
		}
	}
}
