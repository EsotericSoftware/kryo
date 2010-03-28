
package com.esotericsoftware.kryo;

import java.nio.BufferOverflowException;

/**
 * Indicates an error during serialization due to misconfiguration or during deserialization due to invalid input data.
 * @author Nathan Sweet <misc@n4te.com>
 */
public class SerializationException extends RuntimeException {
	public SerializationException () {
		super();
	}

	public SerializationException (String message, Throwable cause) {
		super(message, cause);
	}

	public SerializationException (String message) {
		super(message);
	}

	public SerializationException (Throwable cause) {
		super(cause);
	}

	/**
	 * Returns true if any of the exceptions that caused this exception are {@link BufferOverflowException}.
	 */
	public boolean causedByBufferOverflow () {
		return causedByBufferOverflow(getCause());
	}

	private boolean causedByBufferOverflow (Throwable ex) {
		Throwable cause = ex.getCause();
		if (cause == null || cause == ex) return false;
		if (cause instanceof BufferOverflowException) return true;
		return causedByBufferOverflow(cause);
	}
}
