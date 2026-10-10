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

import java.io.PrintWriter;
import java.io.StringWriter;

/** Kryo's low overhead, lightweight logging, a copy of <a href="https://github.com/EsotericSoftware/minlog">MinLog</a> with the
 * same API, so Kryo has no dependency on it. The level and logger are Kryo's only, they are not shared with MinLog. The level is
 * set with {@link #set(int)} or the methods named after the levels, eg {@link #DEBUG()}, the logger with
 * {@link #setLogger(Logger)}, eg a {@link SystemLogger}.
 * @author Nathan Sweet */
public class Log {
	/** No logging at all. */
	static public final int LEVEL_NONE = 6;
	/** Critical errors. The application may no longer work correctly. */
	static public final int LEVEL_ERROR = 5;
	/** Important warnings. The application will continue to work correctly. */
	static public final int LEVEL_WARN = 4;
	/** Informative messages. Typically used for deployment. */
	static public final int LEVEL_INFO = 3;
	/** Debug messages. This level is useful during development. */
	static public final int LEVEL_DEBUG = 2;
	/** Trace messages. A lot of information is logged, so this level is usually only needed when debugging a problem. */
	static public final int LEVEL_TRACE = 1;

	/** The level of messages that will be logged. */
	static private int level = LEVEL_INFO;

	/** True when the ERROR level will be logged. */
	static public boolean ERROR = level <= LEVEL_ERROR;
	/** True when the WARN level will be logged. */
	static public boolean WARN = level <= LEVEL_WARN;
	/** True when the INFO level will be logged. */
	static public boolean INFO = level <= LEVEL_INFO;
	/** True when the DEBUG level will be logged. */
	static public boolean DEBUG = level <= LEVEL_DEBUG;
	/** True when the TRACE level will be logged. */
	static public boolean TRACE = level <= LEVEL_TRACE;

	/** Sets the level to log. */
	static public void set (int level) {
		Log.level = level;
		ERROR = level <= LEVEL_ERROR;
		WARN = level <= LEVEL_WARN;
		INFO = level <= LEVEL_INFO;
		DEBUG = level <= LEVEL_DEBUG;
		TRACE = level <= LEVEL_TRACE;
	}

	static public void NONE () {
		set(LEVEL_NONE);
	}

	static public void ERROR () {
		set(LEVEL_ERROR);
	}

	static public void WARN () {
		set(LEVEL_WARN);
	}

	static public void INFO () {
		set(LEVEL_INFO);
	}

	static public void DEBUG () {
		set(LEVEL_DEBUG);
	}

	static public void TRACE () {
		set(LEVEL_TRACE);
	}

	/** Sets the logger that will write the log messages. */
	static public void setLogger (Logger logger) {
		Log.logger = logger;
	}

	static private Logger logger = new Logger();

	static public void error (String message, Throwable ex) {
		if (ERROR) logger.log(LEVEL_ERROR, null, message, ex);
	}

	static public void error (String category, String message, Throwable ex) {
		if (ERROR) logger.log(LEVEL_ERROR, category, message, ex);
	}

	static public void error (String message) {
		if (ERROR) logger.log(LEVEL_ERROR, null, message, null);
	}

	static public void error (String category, String message) {
		if (ERROR) logger.log(LEVEL_ERROR, category, message, null);
	}

	static public void warn (String message, Throwable ex) {
		if (WARN) logger.log(LEVEL_WARN, null, message, ex);
	}

	static public void warn (String category, String message, Throwable ex) {
		if (WARN) logger.log(LEVEL_WARN, category, message, ex);
	}

	static public void warn (String message) {
		if (WARN) logger.log(LEVEL_WARN, null, message, null);
	}

	static public void warn (String category, String message) {
		if (WARN) logger.log(LEVEL_WARN, category, message, null);
	}

	static public void info (String message, Throwable ex) {
		if (INFO) logger.log(LEVEL_INFO, null, message, ex);
	}

	static public void info (String category, String message, Throwable ex) {
		if (INFO) logger.log(LEVEL_INFO, category, message, ex);
	}

	static public void info (String message) {
		if (INFO) logger.log(LEVEL_INFO, null, message, null);
	}

	static public void info (String category, String message) {
		if (INFO) logger.log(LEVEL_INFO, category, message, null);
	}

	static public void debug (String message, Throwable ex) {
		if (DEBUG) logger.log(LEVEL_DEBUG, null, message, ex);
	}

	static public void debug (String category, String message, Throwable ex) {
		if (DEBUG) logger.log(LEVEL_DEBUG, category, message, ex);
	}

	static public void debug (String message) {
		if (DEBUG) logger.log(LEVEL_DEBUG, null, message, null);
	}

	static public void debug (String category, String message) {
		if (DEBUG) logger.log(LEVEL_DEBUG, category, message, null);
	}

	static public void trace (String message, Throwable ex) {
		if (TRACE) logger.log(LEVEL_TRACE, null, message, ex);
	}

	static public void trace (String category, String message, Throwable ex) {
		if (TRACE) logger.log(LEVEL_TRACE, category, message, ex);
	}

	static public void trace (String message) {
		if (TRACE) logger.log(LEVEL_TRACE, null, message, null);
	}

	static public void trace (String category, String message) {
		if (TRACE) logger.log(LEVEL_TRACE, category, message, null);
	}

	private Log () {
	}

	/** Performs the actual logging. The default implementation logs to System.out. Extend it and set it with
	 * {@link Log#setLogger(Logger)} to handle logging differently, or use a {@link SystemLogger}. */
	static public class Logger {
		private final long firstLogTime = System.currentTimeMillis();

		public void log (int level, String category, String message, Throwable ex) {
			StringBuilder builder = new StringBuilder(256);

			long time = System.currentTimeMillis() - firstLogTime;
			long minutes = time / (1000 * 60);
			long seconds = time / (1000) % 60;
			if (minutes <= 9) builder.append('0');
			builder.append(minutes);
			builder.append(':');
			if (seconds <= 9) builder.append('0');
			builder.append(seconds);

			switch (level) {
			case LEVEL_ERROR:
				builder.append(" ERROR: ");
				break;
			case LEVEL_WARN:
				builder.append("  WARN: ");
				break;
			case LEVEL_INFO:
				builder.append("  INFO: ");
				break;
			case LEVEL_DEBUG:
				builder.append(" DEBUG: ");
				break;
			case LEVEL_TRACE:
				builder.append(" TRACE: ");
				break;
			}

			if (category != null) {
				builder.append('[');
				builder.append(category);
				builder.append("] ");
			}

			builder.append(message);

			if (ex != null) {
				StringWriter writer = new StringWriter(256);
				ex.printStackTrace(new PrintWriter(writer));
				builder.append('\n');
				builder.append(writer.toString().trim());
			}

			print(builder.toString());
		}

		/** Prints the message to System.out. Called by the default implementation of
		 * {@link #log(int, String, String, Throwable)}. */
		protected void print (String message) {
			System.out.println(message);
		}
	}
}
