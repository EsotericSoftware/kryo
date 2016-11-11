/* Copyright (c) 2016, Martin Grotzke
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

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/** Serializers for {@link Optional}, {@link OptionalInt}, {@link OptionalLong} and {@link OptionalDouble}. Are added as default
 * serializers for java >= 1.8. */
public final class OptionalSerializers {

	public static void addDefaultSerializers (Kryo kryo) {
		if (isClassAvailable("java.util.Optional")) kryo.addDefaultSerializer(Optional.class, new OptionalSerializer());
		if (isClassAvailable("java.util.OptionalInt")) kryo.addDefaultSerializer(OptionalInt.class, new OptionalIntSerializer());
		if (isClassAvailable("java.util.OptionalLong")) kryo.addDefaultSerializer(OptionalLong.class, new OptionalLongSerializer());
		if (isClassAvailable("java.util.OptionalDouble"))
			kryo.addDefaultSerializer(OptionalDouble.class, new OptionalDoubleSerializer());
	}

	private static class OptionalSerializer extends Serializer<Optional> {

		{
			setAcceptsNull(false);
		}

		@Override
		@SuppressWarnings("unchecked")
		public void write (Kryo kryo, Output output, Optional object) {
			Object nullable = object.isPresent() ? object.get() : null;
			kryo.writeClassAndObject(output, nullable);
		}

		@Override
		public Optional read (Kryo kryo, Input input, Class<Optional> type) {
			return Optional.ofNullable(kryo.readClassAndObject(input));
		}

		@Override
		public Optional copy (Kryo kryo, Optional original) {
			if (original.isPresent()) {
				return Optional.of(kryo.copy(original.get()));
			}
			return original;
		}
	}

	private static class OptionalIntSerializer extends Serializer<OptionalInt> {
		{
			setImmutable(true);
		}

		public void write (Kryo kryo, Output output, OptionalInt object) {
			output.writeBoolean(object.isPresent());
			if (object.isPresent()) output.writeInt(object.getAsInt());
		}

		public OptionalInt read (Kryo kryo, Input input, Class<OptionalInt> type) {
			boolean present = input.readBoolean();
			return present ? OptionalInt.of(input.readInt()) : OptionalInt.empty();
		}
	}

	private static class OptionalLongSerializer extends Serializer<OptionalLong> {
		{
			setImmutable(true);
		}

		public void write (Kryo kryo, Output output, OptionalLong object) {
			output.writeBoolean(object.isPresent());
			if (object.isPresent()) output.writeLong(object.getAsLong());
		}

		public OptionalLong read (Kryo kryo, Input input, Class<OptionalLong> type) {
			boolean present = input.readBoolean();
			return present ? OptionalLong.of(input.readLong()) : OptionalLong.empty();
		}
	}

	private static class OptionalDoubleSerializer extends Serializer<OptionalDouble> {
		{
			setImmutable(true);
		}

		public void write (Kryo kryo, Output output, OptionalDouble object) {
			output.writeBoolean(object.isPresent());
			if (object.isPresent()) output.writeDouble(object.getAsDouble());
		}

		public OptionalDouble read (Kryo kryo, Input input, Class<OptionalDouble> type) {
			boolean present = input.readBoolean();
			return present ? OptionalDouble.of(input.readDouble()) : OptionalDouble.empty();
		}
	}

}
