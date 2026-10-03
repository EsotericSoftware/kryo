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

package com.esotericsoftware.kryo.util;

import static org.junit.jupiter.api.Assertions.*;

import com.esotericsoftware.minlog.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

class SystemLoggerTest {
	// Without an adapter for System.Logger, the logging goes to java.util.logging.
	@Test
	void testInstall () {
		Logger logger = Logger.getLogger("com.esotericsoftware.kryo");
		List<LogRecord> records = new ArrayList<>();
		Handler handler = new Handler() {
			public void publish (LogRecord record) {
				records.add(record);
			}

			public void flush () {
			}

			public void close () {
			}
		};
		logger.addHandler(handler);
		logger.setLevel(Level.FINE); // DEBUG for System.Logger.
		try {
			SystemLogger.install();
			assertTrue(Log.DEBUG);
			assertFalse(Log.TRACE);

			Log.debug("kryo", "message");
			RuntimeException ex = new RuntimeException();
			Log.warn("kryo", "warning", ex);
			assertEquals(2, records.size());
			assertEquals(Level.FINE, records.get(0).getLevel());
			assertEquals("[kryo] message", records.get(0).getMessage());
			assertEquals(Level.WARNING, records.get(1).getLevel());
			assertSame(ex, records.get(1).getThrown());
		} finally {
			logger.removeHandler(handler);
			logger.setLevel(null);
			Log.setLogger(new Log.Logger());
			Log.set(Log.LEVEL_INFO);
		}
	}
}
