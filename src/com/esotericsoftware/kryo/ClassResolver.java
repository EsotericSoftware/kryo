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

package com.esotericsoftware.kryo;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/** Handles class registration, writing class identifiers to bytes, and reading class identifiers from bytes.
 * @author Nathan Sweet */
public interface ClassResolver {
	/** Sets the Kryo instance that this ClassResolver will be used for. This is called automatically by Kryo. */
	public void setKryo (Kryo kryo);

	/** Stores the specified registration.
	 * @see Kryo#register(Registration) */
	public Registration register (Registration registration);

	/** Removes the registration for the specified ID.
	 * @return May be null if the class ID was not registered. */
	public Registration unregister (int classID);

	/** Called when an unregistered type is encountered and {@link Kryo#setRegistrationRequired(boolean)} is false. */
	public Registration registerImplicit (Class type);

	/** Returns the registration for the specified class, or null if the class is not registered. */
	public Registration getRegistration (Class type);

	/** Returns the registration for the specified ID, or null if no class is registered with that ID. */
	public Registration getRegistration (int classID);

	/** Writes a class and returns its registration.
	 * @param type May be null.
	 * @return Will be null if type is null. */
	public Registration writeClass (Output output, Class type);

	/** Reads a class and returns its registration.
	 * @return May be null. */
	public Registration readClass (Input input);

	/** Called by {@link Kryo#reset()}. */
	public void reset ();

	/** Starts deferring class names: a class name that is written the first time in the current object graph is written only as a
	 * name ID, and the name is written later by {@link #endDeferredNames(Output, int)}. Chunked encoding uses this to write the
	 * class names before the data, so they are known even if a field is skipped. Calls can be nested. The default implementation
	 * doesn't defer class names.
	 * @return The mark for {@link #endDeferredNames(Output, int)}. */
	public default int beginDeferredNames () {
		return 0;
	}

	/** Writes the class names deferred since the mark and ends deferring for the matching {@link #beginDeferredNames()}. */
	public default void endDeferredNames (Output output, int mark) {
		output.writeVarInt(0, true);
	}

	/** Reads class names written by {@link #endDeferredNames(Output, int)} and remembers those that are not known yet. */
	public default void readDeferredNames (Input input) {
		for (int i = 0, n = input.readVarInt(true); i < n; i++) {
			input.readVarInt(true);
			input.readString();
		}
	}
}
