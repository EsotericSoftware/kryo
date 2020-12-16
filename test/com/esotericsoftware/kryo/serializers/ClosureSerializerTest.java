/* Copyright (c) 2008-2020, Nathan Sweet
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

import com.esotericsoftware.kryo.KryoTestCase;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.lang.invoke.SerializedLambda;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Test for java 8 closures. */
class ClosureSerializerTest extends KryoTestCase {
	@BeforeEach
	public void setUp () throws Exception {
		super.setUp();
		// kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
		kryo.register(Object[].class);
		kryo.register(Class.class);
		kryo.register(getClass()); // The closure's capturing class must be registered.
		kryo.register(SerializedLambda.class);
		kryo.register(ClosureSerializer.Closure.class, new ClosureSerializer());
	}

	@Test
	void testSerializableClosure () {
		Callable<Integer> closure1 = (Callable<Integer> & java.io.Serializable)( () -> 72363);

		// The length cannot be checked reliable, as it can vary based on the JVM.
		roundTrip(Integer.MIN_VALUE, closure1);

		Output output = new Output(1024, -1);
		kryo.writeObject(output, closure1);

		Input input = new Input(output.getBuffer(), 0, output.position());
		Callable<Integer> closure2 = (Callable<Integer>)kryo.readObject(input, ClosureSerializer.Closure.class);

		doAssertEquals(closure1, closure2);
	}

	protected void doAssertEquals (Object object1, Object object2) {
		try {
			assertEquals(((Callable)object1).call(), ((Callable)object2).call());
		} catch (Exception ex) {
			throw new RuntimeException(ex.getMessage());
		}
	}
}
