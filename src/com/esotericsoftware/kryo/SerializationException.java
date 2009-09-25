
package com.esotericsoftware.kryo;

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
}
