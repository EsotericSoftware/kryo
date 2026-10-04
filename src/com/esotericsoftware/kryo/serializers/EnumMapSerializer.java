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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.EnumMap;

/** Serializer for {@link EnumMap}. The default serializer writes the enum type of the keys. With
 * {@link #EnumMapSerializer(Class)}, the enum type is known and not written.
 * @author Nathan Sweet */
public class EnumMapSerializer extends MapSerializer<EnumMap> {
	private final Class<? extends Enum> enumType;

	/** Writes the enum type of the keys. */
	public EnumMapSerializer () {
		enumType = null;
	}

	/** @param enumType The enum type of the keys, which is not written. */
	public EnumMapSerializer (Class<? extends Enum> enumType) {
		this.enumType = enumType;
	}

	protected void writeHeader (Kryo kryo, Output output, EnumMap map) {
		if (enumType == null) kryo.writeClass(output, keyType(map));
	}

	protected EnumMap create (Kryo kryo, Input input, Class<? extends EnumMap> type, int size) {
		if (enumType != null) return new EnumMap(enumType);
		Registration registration = kryo.readClass(input);
		if (registration == null || !registration.getType().isEnum())
			throw new KryoException("Invalid EnumMap key type: " + (registration == null ? null : registration.getType().getName()));
		return new EnumMap(registration.getType());
	}

	protected EnumMap createCopy (Kryo kryo, EnumMap original) {
		return new EnumMap(original);
	}

	/** Returns the enum type of the keys. It is not public API, but part of the serialized form of {@link EnumMap}, so for an
	 * empty map it is taken from Java serialization. */
	static Class keyType (EnumMap map) {
		if (!map.isEmpty()) return ((Enum)map.keySet().iterator().next()).getDeclaringClass();
		try (KeyTypeOutput output = new KeyTypeOutput()) {
			output.writeObject(map);
			if (output.keyType == null) throw new KryoException("EnumMap key type not found.");
			return output.keyType;
		} catch (IOException ex) {
			throw new KryoException("Unable to find the EnumMap key type.", ex);
		}
	}

	/** Discards the data and remembers the first enum class written, which is the key type of an empty {@link EnumMap}. */
	static class KeyTypeOutput extends ObjectOutputStream {
		Class keyType;

		KeyTypeOutput () throws IOException {
			super(new OutputStream() { // OutputStream.nullOutputStream needs Android API level 33.
				public void write (int b) {
				}

				public void write (byte[] bytes, int offset, int length) {
				}
			});
		}

		protected void annotateClass (Class type) {
			if (keyType == null && type.isEnum()) keyType = type;
		}
	}
}
