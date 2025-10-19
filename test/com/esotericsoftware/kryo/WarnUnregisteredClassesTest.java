/* Copyright (c) 2008-2025, Nathan Sweet
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

import static org.junit.jupiter.api.Assertions.*;

import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.minlog.Log;
import com.esotericsoftware.minlog.Log.Logger;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** @author Tiago Albineli Motta <timotta@gmail.com> */
class WarnUnregisteredClassesTest {
	LoggerStub log;

	@BeforeEach
	public void setUp () throws Exception {
		log = new LoggerStub();
		Log.setLogger(log);
		Log.INFO();
	}

	@Test
	void testLogOnlyOneTimePerClass () {
		Kryo kryo = new Kryo();
		kryo.setRegistrationRequired(false);
		kryo.setWarnUnregisteredClasses(true);

		write(kryo, new UnregisteredClass());
		assertEquals(1, log.messages.size());

		write(kryo, new UnregisteredClass());
		assertEquals(1, log.messages.size());

		write(kryo, new UnregisteredClass2());
		assertEquals(2, log.messages.size());

		write(kryo, new UnregisteredClass2());
		assertEquals(2, log.messages.size());
	}

	@Test
	void testDontLogIfNotRequired () {
		Kryo kryo = new Kryo();
		kryo.setRegistrationRequired(false);
		kryo.setWarnUnregisteredClasses(false);

		write(kryo, new UnregisteredClass());
		assertEquals(0, log.messages.size());

		write(kryo, new UnregisteredClass2());
		assertEquals(0, log.messages.size());
	}

	@Test
	void testDontLogClassIsRegistered () {
		Kryo kryo = new Kryo();
		kryo.setRegistrationRequired(false);
		kryo.setWarnUnregisteredClasses(true);
		kryo.register(RegisteredClass.class);

		write(kryo, new RegisteredClass());
		assertEquals(0, log.messages.size());
	}

	@Test
	void testLogShouldBeWarn () {
		Kryo kryo = new Kryo();
		kryo.setRegistrationRequired(false);
		kryo.setWarnUnregisteredClasses(true);

		write(kryo, new UnregisteredClass());
		assertEquals(Log.LEVEL_WARN, log.levels.get(0).intValue());
	}

	@Test
	void testLogMessageShouldContainClassName () {
		Kryo kryo = new Kryo();
		kryo.setRegistrationRequired(false);
		kryo.setWarnUnregisteredClasses(true);

		write(kryo, new UnregisteredClass());
		assertTrue(log.messages.get(0).contains(UnregisteredClass.class.getName()));
	}

	@Test
	void testLogMessageShouldContainRegistrationHintWithCanonicalName () {
		Kryo kryo = new Kryo();
		kryo.setRegistrationRequired(false);
		kryo.setWarnUnregisteredClasses(true);

		write(kryo, new UnregisteredClass());
		assertTrue(log.messages.get(0).contains(
				"kryo.register(com.esotericsoftware.kryo.WarnUnregisteredClassesTest.UnregisteredClass.class)"
		));
	}

	public void write (Kryo kryo, Object object) {
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		Output output = new Output(outStream, 4096);
		kryo.writeClassAndObject(output, object);
		output.flush();
	}

	class LoggerStub extends Logger {
		public List<Integer> levels = new ArrayList();
		public List<String> messages = new ArrayList();

		public void log (int level, String category, String message, Throwable ex) {
			levels.add(level);
			messages.add(message);
		}
	}

	static class UnregisteredClass {
		public UnregisteredClass () {
		}
	}

	static class UnregisteredClass2 {
		public UnregisteredClass2 () {
		}
	}

	static class RegisteredClass {
		public RegisteredClass () {
		}
	}
}
