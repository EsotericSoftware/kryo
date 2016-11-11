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

import java.lang.invoke.SerializedLambda;
import java.util.concurrent.Callable;

import org.junit.Assert;
import org.objenesis.strategy.StdInstantiatorStrategy;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoTestCase;

/** Test for java 8 closures.
 *
 * For jdk < 1.8 excluded from surefire tests via the "until-java8" profile in pom.xml which excludes "Java8*Tests". */
public class Java8ClosureSerializerTest extends KryoTestCase {

	public void setUp () throws Exception {
		super.setUp();
		kryo.setInstantiatorStrategy(new Kryo.DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
		// the following registrations are needed because registration is required
		kryo.register(Object[].class);
		kryo.register(java.lang.Class.class);
		kryo.register(getClass()); // closure capturing class (in this test `this`), it would usually already be registered
		kryo.register(SerializedLambda.class);
		// always needed for closure serialization, also if registrationRequired=false
		kryo.register(ClosureSerializer.Closure.class, new ClosureSerializer());
	}

	public void testSerializeSerializableLambdaWithKryo () throws Exception {
		Callable<Boolean> doNothing = (Callable<Boolean> & java.io.Serializable)( () -> true);
		roundTrip(222, 225, doNothing);
	}

	// we must override equals as lambdas have no equals check built in...
	@Override
	protected void doAssertEquals (Object object1, Object object2) {
		try {
			Assert.assertEquals(((Callable<?>)object1).call(), ((Callable<?>)object2).call());
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		}
	}

}
