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

import static com.esotericsoftware.kryo.util.Log.*;

import java.lang.System.Logger.Level;

/** Routes Kryo's logging to a {@link System.Logger}, so it can be configured with any logging framework that supports it, eg
 * SLF4J with slf4j-jdk-platform-logging or Log4j with log4j-jpl. Without such an adapter, the logging goes to java.util.logging.
 * @see #install() */
@IgnoreAndroid
public class SystemLogger extends Log.Logger {
	private final System.Logger logger;

	public SystemLogger (System.Logger logger) {
		this.logger = logger;
	}

	/** Routes Kryo's logging to the System.Logger named "com.esotericsoftware.kryo" and sets Kryo's {@link Log} level to the most
	 * detailed level that is enabled for that logger. Kryo only creates log messages for that level, so call this again after
	 * changing the level of the logger. */
	public static void install () {
		SystemLogger logger = new SystemLogger(System.getLogger("com.esotericsoftware.kryo"));
		Log.setLogger(logger);
		Log.set(logger.level());
	}

	/** Returns the most detailed {@link Log} level that is enabled for the System.Logger. */
	int level () {
		if (logger.isLoggable(Level.TRACE)) return LEVEL_TRACE;
		if (logger.isLoggable(Level.DEBUG)) return LEVEL_DEBUG;
		if (logger.isLoggable(Level.INFO)) return LEVEL_INFO;
		if (logger.isLoggable(Level.WARNING)) return LEVEL_WARN;
		if (logger.isLoggable(Level.ERROR)) return LEVEL_ERROR;
		return LEVEL_NONE;
	}

	public void log (int level, String category, String message, Throwable ex) {
		Level systemLevel = switch (level) {
		case LEVEL_ERROR -> Level.ERROR;
		case LEVEL_WARN -> Level.WARNING;
		case LEVEL_INFO -> Level.INFO;
		case LEVEL_DEBUG -> Level.DEBUG;
		default -> Level.TRACE;
		};
		if (category != null) message = "[" + category + "] " + message;
		if (ex == null)
			logger.log(systemLevel, message);
		else
			logger.log(systemLevel, message, ex);
	}
}
