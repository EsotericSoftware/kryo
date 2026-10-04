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
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import com.esotericsoftware.kryo.serializers.VersionFieldSerializer.Since;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;

import java.io.Serializable;
import java.util.List;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;
import org.objenesis.strategy.StdInstantiatorStrategy;

/** Final fields of serializable classes are set with the method handles of the JDK if available, also if final field mutation
 * is denied, eg with {@code --illegal-final-field-mutation=deny}. */
class FinalFieldsTest {
	@Test
	void testFinalFields () {
		List<BiFunction<Kryo, Class, Serializer>> serializers = List.of(FieldSerializer::new, CompatibleFieldSerializer::new,
			TaggedFieldSerializer::new, VersionFieldSerializer::new);
		for (BiFunction<Kryo, Class, Serializer> factory : serializers) {
			Kryo kryo = new Kryo();
			kryo.setReferences(true);
			kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
			kryo.register(Base.class, factory.apply(kryo, Base.class));
			kryo.register(Node.class, factory.apply(kryo, Node.class));
			Node node = new Node(42, "name", 7);
			node.mutable = "mutable";

			Output output = new Output(1024, -1);
			kryo.writeObject(output, node);
			Node read = kryo.readObject(new Input(output.toBytes()), Node.class);
			assertNode(read);

			assertNode(kryo.copy(node));
		}
	}

	private void assertNode (Node node) {
		assertEquals(42, node.number);
		assertEquals("name", node.name);
		assertEquals(7L, node.base);
		assertEquals("mutable", node.mutable);
		assertSame(node, node.self); // A final field that refers to the object itself.
	}

	public static class Base implements Serializable {
		@Tag(1) final long base;

		Base (long base) {
			this.base = base;
		}
	}

	public static class Node extends Base {
		@Tag(2) final int number;
		@Tag(3) final String name;
		@Tag(4) final Node self;
		@Tag(5) @Since(0) String mutable;

		Node (int number, String name, long base) {
			super(base);
			this.number = number;
			this.name = name;
			self = this;
		}
	}
}
