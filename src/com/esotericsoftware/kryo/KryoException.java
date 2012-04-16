
package com.esotericsoftware.kryo;

/** General Kryo RuntimeException.
 * @author Nathan Sweet <misc@n4te.com> */
public class KryoException extends RuntimeException {
	private StringBuffer trace;

	public KryoException () {
		super();
	}

	public KryoException (String message, Throwable cause) {
		super(message, cause);
	}

	public KryoException (String message) {
		super(message);
	}

	public KryoException (Throwable cause) {
		super(cause);
	}

	public String getMessage () {
		if (trace == null) return super.getMessage();
		StringBuffer buffer = new StringBuffer(512);
		buffer.append(super.getMessage());
		if (buffer.length() > 0) buffer.append('\n');
		buffer.append("Serialization trace:");
		buffer.append(trace);
		return buffer.toString();
	}

	/** Adds information to the exception message about where in the the object graph serialization failure occurred.
	 * {@link Serializer Serializers} can catch {@link KryoException}, add trace information, and rethrow the exception. */
	public void addTrace (String info) {
		if (info == null) throw new IllegalArgumentException("info cannot be null.");
		if (trace == null) trace = new StringBuffer(512);
		trace.append('\n');
		trace.append(info);
	}
}
