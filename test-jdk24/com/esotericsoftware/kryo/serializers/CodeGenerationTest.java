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

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.KryoTestCase;
import com.esotericsoftware.kryo.SerializerFactory.FieldSerializerFactory;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.FieldSerializer.Bind;
import com.esotericsoftware.kryo.serializers.FieldSerializer.NotNull;
import com.esotericsoftware.kryo.util.MapReferenceResolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests {@link CodeGeneration}, which needs Java 24+. */
class CodeGenerationTest extends KryoTestCase {
	{
		supportsCopy = true;
	}

	@BeforeEach
	public void setUp () throws Exception {
		assumeTrue(CachedFields.codeGeneration, "Code generation is not supported on this platform.");
		super.setUp();
		kryo.setDefaultSerializer(codeGeneration());
	}

	static private FieldSerializerFactory codeGeneration () {
		FieldSerializerFactory factory = new FieldSerializerFactory();
		factory.getConfig().setCodeGeneration(true);
		return factory;
	}

	@Test
	void testAllFieldKinds () {
		kryo.register(AllKinds.class);
		kryo.register(Nested.class);
		kryo.register(Color.class);
		kryo.register(ArrayList.class);
		kryo.register(HashMap.class);
		kryo.register(int[].class);
		AllKinds object = AllKinds.create();
		assertGenerated(AllKinds.class);
		roundTrip(83, object);

		object.nested = null;
		object.string = null;
		object.list = null;
		object.integer = null;
		roundTrip(57, object);
	}

	@Test
	void testSameBytesAsCachedFields () {
		Kryo cachedFields = new Kryo();
		FieldSerializerFactory factory = new FieldSerializerFactory();
		factory.getConfig().setCodeGeneration(false); // In case the system property enables it.
		cachedFields.setDefaultSerializer(factory);
		for (Kryo k : new Kryo[] {kryo, cachedFields}) {
			k.register(AllKinds.class);
			k.register(Nested.class);
			k.register(Color.class);
			k.register(ArrayList.class);
			k.register(HashMap.class);
			k.register(int[].class);
		}
		assertNull(((FieldSerializer)cachedFields.getSerializer(AllKinds.class)).generated);
		assertGenerated(AllKinds.class);

		AllKinds object = AllKinds.create();
		assertArrayEquals(write(cachedFields, object), write(kryo, object));

		// Read with each, write with the other.
		assertEquals(object, read(kryo, write(cachedFields, object), AllKinds.class));
		assertEquals(object, read(cachedFields, write(kryo, object), AllKinds.class));
	}

	@Test
	void testWithoutVarEncodingAndReferences () {
		kryo.setReferences(false);
		FieldSerializerFactory factory = codeGeneration();
		factory.getConfig().setVariableLengthEncoding(false);
		kryo.setDefaultSerializer(factory);
		kryo.register(AllKinds.class);
		kryo.register(Nested.class);
		kryo.register(Color.class);
		kryo.register(ArrayList.class);
		kryo.register(HashMap.class);
		kryo.register(int[].class);
		assertGenerated(AllKinds.class);
		roundTrip(102, AllKinds.create());
	}

	@Test
	void testStringFieldWithReferences () {
		kryo.register(Strings.class);
		assertGenerated(Strings.class);
		assertFalse(((FieldSerializer)kryo.getSerializer(Strings.class)).getField("a") instanceof ReflectField);
		Strings object = new Strings();
		object.a = "same";
		object.b = "same";
		object.c = "other";
		roundTrip(14, object);

		// With references for strings, a String field is an object field: written with the StringSerializer and references.
		kryo.setReferenceResolver(new MapReferenceResolver() {
			public boolean useReferences (Class type) {
				return type == String.class || super.useReferences(type);
			}
		});
		kryo.register(Strings.class, new FieldSerializer(kryo, Strings.class, codeGeneration().getConfig()));
		assertGenerated(Strings.class);
		assertTrue(((FieldSerializer)kryo.getSerializer(Strings.class)).getField("a") instanceof ReflectField);
		roundTrip(14, object); // 1 class, 1 not null, 5 + 1 (reference) + 6 strings.
	}

	@Test
	void testSharedByKryoInstances () {
		Kryo kryo2 = new Kryo();
		kryo2.setDefaultSerializer(codeGeneration());
		kryo.register(Nested.class);
		kryo2.register(Nested.class);
		GeneratedFields generated1 = assertGenerated(Nested.class);
		GeneratedFields generated2 = ((FieldSerializer)kryo2.getSerializer(Nested.class)).generated;
		assertNotNull(generated2);
		assertNotSame(generated1, generated2);
		assertSame(generated1.getClass(), generated2.getClass());
		assertTrue(generated1.getClass().isHidden());
	}

	@Test
	void testRemoveFieldAndUpdateFields () {
		kryo.register(Nested.class);
		FieldSerializer serializer = (FieldSerializer)kryo.getSerializer(Nested.class);
		GeneratedFields generated = serializer.generated;
		assertNotNull(generated);

		serializer.removeField("value");
		assertNotNull(serializer.generated);
		assertNotSame(generated, serializer.generated);
		assertNotSame(generated.getClass(), serializer.generated.getClass());
		Nested object = new Nested();
		object.value = 123;
		object.name = "name";
		Nested read = read(kryo, write(kryo, object), Nested.class);
		assertEquals(0, read.value);
		assertEquals("name", read.name);

		// The removed field stays removed.
		serializer.getFieldSerializerConfig().setVariableLengthEncoding(false);
		serializer.updateFields();
		assertNotNull(serializer.generated);
		assertEquals(4, write(kryo, object).length);

		serializer.getFieldSerializerConfig().setCodeGeneration(false);
		serializer.updateFields();
		assertNull(serializer.generated);
		assertEquals(4, write(kryo, object).length);
	}

	@Test
	void testFinalField () {
		kryo.register(FinalField.class);
		assertGenerated(FinalField.class);
		roundTrip(6, new FinalField(7, "name"));
	}

	@Test
	void testNotSupported () {
		// Records use the cached fields.
		kryo.register(Point.class);
		assertNull(((FieldSerializer)kryo.getSerializer(Point.class)).generated);
		roundTrip(3, new Point(1, 2));

		// Other field serializers don't use generated code.
		kryo.register(Nested.class, new CompatibleFieldSerializer(kryo, Nested.class));
		assertNull(((FieldSerializer)kryo.getSerializer(Nested.class)).generated);
	}

	@Test
	void testErrors () {
		kryo.register(Nested.class);
		kryo.register(NotNullField.class);
		assertGenerated(NotNullField.class);
		NotNullField object = new NotNullField();
		object.nested = new Nested();
		KryoException ex = assertThrows(KryoException.class, () -> write(kryo, object));
		assertTrue(ex.getMessage().contains("required (" + NotNullField.class.getName() + ")"), ex.getMessage());

		// A String where a Nested is expected: the VarHandle rejects the value.
		Output output = new Output(64);
		kryo.writeClass(output, String.class);
		output.writeString("not a Nested");
		output.writeString("required");
		Input input = new Input(output.toBytes());
		ex = assertThrows(KryoException.class, () -> kryo.readObject(input, NotNullField.class));
		assertTrue(ex.getMessage().contains("Error reading " + NotNullField.class.getName()), ex.getMessage());
		assertTrue(ex.getCause() instanceof ClassCastException, String.valueOf(ex.getCause()));
	}

	private GeneratedFields assertGenerated (Class type) {
		FieldSerializer serializer = (FieldSerializer)kryo.getSerializer(type);
		assertNotNull(serializer.generated, "No code generated for: " + type);
		return serializer.generated;
	}

	static private byte[] write (Kryo kryo, Object object) {
		Output output = new Output(1024, -1);
		kryo.writeObject(output, object);
		return output.toBytes();
	}

	static private <T> T read (Kryo kryo, byte[] bytes, Class<T> type) {
		return kryo.readObject(new Input(bytes), type);
	}

	static public class AllKinds {
		int i;
		long l;
		double d;
		float f;
		boolean z;
		short s;
		char c;
		byte b;
		String string;
		Integer integer;
		Color color;
		int[] ints;
		Nested nested;
		Object polymorphic;
		List<Nested> list;
		Map<String, Nested> map;
		@Bind(serializer = DefaultSerializers.StringSerializer.class) String bound;

		static AllKinds create () {
			AllKinds o = new AllKinds();
			o.i = -1234;
			o.l = 1L << 40;
			o.d = 1.5;
			o.f = -2.5f;
			o.z = true;
			o.s = -3;
			o.c = 'x';
			o.b = 5;
			o.string = "string";
			o.integer = 99;
			o.color = Color.green;
			o.ints = new int[] {1, 2, 3};
			o.nested = new Nested();
			o.nested.value = 7;
			o.nested.name = "nested";
			o.polymorphic = new Nested();
			o.list = new ArrayList<>(Arrays.asList(new Nested(), o.nested));
			o.map = new HashMap<>();
			o.map.put("key", o.nested);
			o.bound = "bound";
			return o;
		}

		public boolean equals (Object obj) {
			AllKinds other = (AllKinds)obj;
			return i == other.i && l == other.l && d == other.d && f == other.f && z == other.z && s == other.s && c == other.c
				&& b == other.b && Objects.equals(string, other.string) && Objects.equals(integer, other.integer) && color == other.color
				&& Arrays.equals(ints, other.ints) && Objects.equals(nested, other.nested) && Objects.equals(polymorphic, other.polymorphic)
				&& Objects.equals(list, other.list) && Objects.equals(map, other.map) && Objects.equals(bound, other.bound);
		}
	}

	static public class Nested {
		int value;
		String name;

		public boolean equals (Object obj) {
			Nested other = (Nested)obj;
			return value == other.value && Objects.equals(name, other.name);
		}

		public int hashCode () {
			return value;
		}
	}

	static public class Strings {
		String a, b, c;

		public boolean equals (Object obj) {
			Strings other = (Strings)obj;
			return Objects.equals(a, other.a) && Objects.equals(b, other.b) && Objects.equals(c, other.c);
		}
	}

	static public class NotNullField {
		Nested nested;
		@NotNull String required;
	}

	static public class FinalField {
		final int value;
		final String name;

		public FinalField () {
			this(0, null);
		}

		public FinalField (int value, String name) {
			this.value = value;
			this.name = name;
		}

		public boolean equals (Object obj) {
			return value == ((FinalField)obj).value && Objects.equals(name, ((FinalField)obj).name);
		}
	}

	public record Point (int x, int y) {
	}

	public enum Color {
		red, green, blue
	}
}
