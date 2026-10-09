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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer.CompatibleFieldSerializerConfig;
import com.esotericsoftware.kryo.serializers.FieldSerializer.FieldAccessType;
import com.esotericsoftware.kryo.serializers.FieldSerializer.FieldSerializerConfig;
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.TaggedFieldSerializerConfig;
import com.esotericsoftware.kryo.serializers.VersionFieldSerializer.Since;
import com.esotericsoftware.kryo.serializers.VersionFieldSerializer.VersionFieldSerializerConfig;
import com.esotericsoftware.kryo.util.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

/** All ways to access fields write the same bytes: Unsafe, VarHandles with and without hidden classes, reflection and generated
 * code, for each FieldSerializer subclass and with the settings that change how fields are written. */
class FieldAccessEquivalenceTest {
	/** @param intField The implementation of an int field, null with generated code.
	 * @param objectField The implementation of an Object field, null with generated code. */
	record Mode (String name, FieldAccessType fieldAccess, boolean hiddenFields, boolean codeGeneration, String intField,
		String objectField) {
	}

	static List<Mode> modes () {
		List<Mode> modes = new ArrayList<>();
		if (Util.unsafe) modes.add(new Mode("Unsafe", FieldAccessType.UNSAFE, false, false, "IntUnsafeField", "UnsafeField"));
		if (CachedFields.hiddenFields) {
			modes.add(new Mode("VarHandle hidden classes", FieldAccessType.VARHANDLE, true, false, "IntHiddenField",
				"ObjectHiddenField"));
		}
		modes.add(new Mode("VarHandle", FieldAccessType.VARHANDLE, false, false, "IntVarHandleField", "VarHandleField"));
		modes.add(new Mode("reflection", FieldAccessType.REFLECTION, false, false, "IntReflectField", "ReflectField"));
		if (CachedFields.codeGeneration) modes.add(new Mode("code generation", FieldAccessType.VARHANDLE, true, true, null, null));
		return modes;
	}

	@Test
	void testSameBytes () {
		List<Mode> modes = modes();
		assertTrue(modes.size() >= 2, modes.toString());
		for (String serializer : new String[] {"field", "compatible", "compatible chunked", "tagged", "tagged chunked", "version"}) {
			for (boolean references : new boolean[] {false, true}) {
				for (boolean varEncoding : new boolean[] {true, false}) {
					String setup = serializer + ", references: " + references + ", varEncoding: " + varEncoding;
					byte[] expected = null;
					String expectedMode = null;
					for (Mode mode : modes) {
						byte[] bytes = write(mode, serializer, references, varEncoding);
						if (expected == null) {
							expected = bytes;
							expectedMode = mode.name;
						} else
							assertArrayEquals(expected, bytes, mode.name + " vs " + expectedMode + ", " + setup);
					}
				}
			}
		}
	}

	/** Writes the data, then checks that reading it and writing it again gives the same bytes. */
	static byte[] write (Mode mode, String serializer, boolean references, boolean varEncoding) {
		boolean hiddenFields = CachedFields.hiddenFields;
		CachedFields.hiddenFields = mode.hiddenFields;
		try {
			Kryo kryo = new Kryo();
			kryo.setReferences(references);
			kryo.register(ArrayList.class);
			kryo.register(HashMap.class);
			kryo.register(int[].class);
			kryo.register(Kind.class);
			kryo.register(Data.class, serializer(kryo, Data.class, mode, serializer, varEncoding));
			kryo.register(Inner.class, serializer(kryo, Inner.class, mode, serializer, varEncoding));

			Output output = new Output(256, -1);
			kryo.writeObject(output, new Data(true));
			byte[] bytes = output.toBytes();

			// The mode is used, it didn't fall back to another one. Checked after the first use, hidden classes are defined then.
			if (!mode.codeGeneration) {
				FieldSerializer dataSerializer = (FieldSerializer)kryo.getSerializer(Data.class);
				String setup = mode.name + ", " + serializer;
				assertEquals(mode.intField, CachedFields.implementationName(dataSerializer.getField("intValue")), setup);
				assertEquals(mode.objectField, CachedFields.implementationName(dataSerializer.getField("object")), setup);
			}

			Data read = kryo.readObject(new Input(bytes), Data.class);
			assertEquals(Long.MIN_VALUE, read.longValue, mode.name);
			assertEquals("final", read.finalString, mode.name);
			assertEquals(-7, read.inner.finalValue, mode.name);
			output.reset();
			kryo.writeObject(output, read);
			assertArrayEquals(bytes, output.toBytes(), "read and written again, " + mode.name);
			return bytes;
		} finally {
			CachedFields.hiddenFields = hiddenFields;
		}
	}

	static Serializer serializer (Kryo kryo, Class type, Mode mode, String serializer, boolean varEncoding) {
		Supplier<FieldSerializerConfig> config = switch (serializer) {
		case "compatible", "compatible chunked" -> () -> {
			CompatibleFieldSerializerConfig compatible = new CompatibleFieldSerializerConfig();
			compatible.setChunkedEncoding(serializer.endsWith("chunked"));
			return compatible;
		};
		case "tagged", "tagged chunked" -> () -> {
			TaggedFieldSerializerConfig tagged = new TaggedFieldSerializerConfig();
			tagged.setChunkedEncoding(serializer.endsWith("chunked"));
			return tagged;
		};
		case "version" -> VersionFieldSerializerConfig::new;
		default -> FieldSerializerConfig::new;
		};
		FieldSerializerConfig c = config.get();
		c.setFieldAccess(mode.fieldAccess);
		c.setCodeGeneration(mode.codeGeneration);
		c.setVariableLengthEncoding(varEncoding);
		FieldSerializer fieldSerializer = switch (serializer) {
		case "compatible", "compatible chunked" -> new CompatibleFieldSerializer(kryo, type, (CompatibleFieldSerializerConfig)c);
		case "tagged", "tagged chunked" -> new TaggedFieldSerializer(kryo, type, (TaggedFieldSerializerConfig)c);
		case "version" -> new VersionFieldSerializer(kryo, type, (VersionFieldSerializerConfig)c);
		default -> new FieldSerializer(kryo, type, c);
		};
		// The mode is used, it didn't fall back to another one.
		String setup = mode.name + ", " + serializer;
		if (mode.codeGeneration)
			assertNotNull(fieldSerializer.generated(), setup);
		else
			assertNull(fieldSerializer.generated(), setup);
		return fieldSerializer;
	}

	enum Kind {
		A, B {
		}
	}

	/** Every field type the field accessors and the generated code handle differently, with values at the edges of the
	 * encodings. Tags for TaggedFieldSerializer, a version for VersionFieldSerializer. */
	public static class Data {
		@Tag(1) public boolean booleanValue;
		@Tag(2) public byte byteValue;
		@Tag(3) public char charValue;
		@Tag(4) public short shortValue;
		@Tag(5) public int intValue;
		@Tag(6) public long longValue;
		@Tag(7) public float floatValue;
		@Tag(8) public double doubleValue;
		@Tag(9) private int privateValue;
		@Tag(10) public Integer integer;
		@Tag(11) public Long nullLong;
		@Tag(12) public String string;
		@Tag(13) public String nullString;
		@Tag(14) public String unicode;
		@Tag(15) public Kind kind;
		@Tag(16) public Kind kindWithBody;
		@Tag(17) public int[] ints;
		@Tag(18) public List<String> list;
		@Tag(19) public Map<String, Integer> map;
		@Tag(20) public Object object;
		@Tag(21) public Inner inner;
		@Tag(22) public Inner sameInner;
		@Tag(23) @Since(1) public final String finalString;
		@Tag(24) @Since(1) public final long finalLong;
		@Tag(25) public transient int transientValue = 9;

		public Data () {
			finalString = null;
			finalLong = 0;
		}

		Data (boolean values) {
			booleanValue = true;
			byteValue = Byte.MIN_VALUE;
			charValue = '￿';
			shortValue = Short.MIN_VALUE;
			intValue = -64; // Varint with zigzag: 1 byte.
			longValue = Long.MIN_VALUE;
			floatValue = Float.NaN;
			doubleValue = -0.0;
			privateValue = Integer.MAX_VALUE;
			integer = 128; // Varint: 2 bytes.
			string = "string";
			unicode = "ä中😀";
			kind = Kind.A;
			kindWithBody = Kind.B;
			ints = new int[] {0, -1, Integer.MIN_VALUE};
			list = new ArrayList<>(Arrays.asList("a", null, "b"));
			map = new HashMap<>();
			map.put("one", 1);
			map.put("null", null);
			object = new Inner(3);
			inner = new Inner(-7);
			sameInner = inner;
			finalString = "final";
			finalLong = 1L << 40;
		}
	}

	public static class Inner {
		@Tag(1) public final int finalValue;
		@Tag(2) public Object back;

		public Inner () {
			finalValue = 0;
		}

		Inner (int value) {
			finalValue = value;
		}
	}
}
