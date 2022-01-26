/* Copyright (c) 2008-2022, Nathan Sweet
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

package com.esotericsoftware.kryo.util;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.io.Input;

import static com.esotericsoftware.kryo.util.Util.*;
import static com.esotericsoftware.minlog.Log.*;

/** This is enhanced resolver from {@link DefaultClassResolver} for fast deserialization of collections only in
 * {@link Kryo#registrationRequired} == true. You can specify the mappings between class and ID by
 * {@link Kryo#register(Class, int)}, But don't specify huge ID like 20000000 because this resolver uses array internally. This
 * resolver internally reconstructs {@link #idToRegistrationArray} whenever the mappings are updated, by
 * {@link #updateIdToRegistrationArray()}. Therefore, it is not suitable in terms of performance if the mappings are updated
 * frequently at peaktime of application. In terms of functionality, {@link ArrayClassResolver} is completely equivalent to
 * {@link DefaultClassResolver}. So output binary of {@link ArrayClassResolver} is equivalent to that of
 * {@link DefaultClassResolver}.
 *
 * @author lifeinwild1@gmail.com */
public final class ArrayClassResolver extends DefaultClassResolver {
	/** array variant of {@link DefaultClassResolver#idToRegistration} for fast lookup. */
	private Registration[] idToRegistrationArray = new Registration[0];

	private void updateIdToRegistrationArray () {
		int maxId = 0;
		for (Registration e : idToRegistration.values()) {
			if (e.getId() > maxId)
				maxId = e.getId();
		}

		Registration[] updated = new Registration[maxId + 1];
		for (Registration e : idToRegistration.values()) {
			updated[e.getId()] = e;
		}

		idToRegistrationArray = updated;
	}

	@Override
	public final Registration getRegistration (int classID) {
		if (classID >= idToRegistrationArray.length)
			return null;
		return idToRegistrationArray[classID];
	}

	@Override
	public Registration readClass (Input input) {
		int classID = input.readVarInt(true);
		switch (classID) {
		case Kryo.NULL:
			if (TRACE || (DEBUG && kryo.getDepth() == 1)) log("Read", null, input.position());
			return null;
		case NAME + 2: // Offset for NAME and NULL.
			return readName(input);
		}
		int index = classID - 2;
		Registration registration = getRegistration(index);
		if (registration == null) throw new KryoException("Encountered unregistered class ID: " + (classID - 2));
		if (TRACE) trace("kryo", "Read class " + (classID - 2) + ": " + className(registration.getType()) + pos(input.position()));
		return registration;
	}

	/*
	 * @Override public void reset() { super.reset(); //This method does not reset the entire state of ClassResolver, but resets
	 * states that is created per object graph. //So the mappings between class and id is not reset. //The semantic is actually
	 * like resetPerUse(). }
	 */

	@Override
	public Registration unregister (int classID) {
		Registration r = super.unregister(classID);
		updateIdToRegistrationArray();
		return r;
	}

	@Override
	public Registration register (Registration registration) {
		Registration r = super.register(registration);
		updateIdToRegistrationArray();
		return r;
	}

	@Override
	public Registration registerImplicit (Class type) {
		Registration r = super.registerImplicit(type);
		updateIdToRegistrationArray();
		return r;
	}
}
