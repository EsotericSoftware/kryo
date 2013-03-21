package com.esotericsoftware.kryo;

import java.io.InputStream;
import java.io.OutputStream;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/** Provides input and output streams based on system settings.
 * @author Roman Levenstein <romixlev@gmail.com> */
public interface StreamFactory {

	/** Creates an uninitialized Input. */
	public Input getInput ();

	/** Creates a new Input for reading from a byte array.
	 * @param bufferSize The size of the buffer. An exception is thrown if more bytes than this are read. */
	public Input getInput(int bufferSize);

	/** Creates a new Input for reading from a byte array.
	 * @param buffer An exception is thrown if more bytes than this are read. */
	public Input getInput(byte[] buffer) ;

	/** Creates a new Input for reading from a byte array.
	 * @param buffer An exception is thrown if more bytes than this are read. */
	public Input getInput(byte[] buffer, int offset, int count) ;

	/** Creates a new Input for reading from an InputStream with a buffer size of 4096. */
	public Input getInput(InputStream inputStream);
	
	/** Creates a new Input for reading from an InputStream. */
	public Input getInput(InputStream inputStream, int bufferSize);
	
	/** Creates an uninitialized Output. {@link Output#setBuffer(byte[], int)} must be called before the Output is used. */
	public Output getOutput();

	/** Creates a new Output for writing to a byte array.
	 * @param bufferSize The initial and maximum size of the buffer. An exception is thrown if this size is exceeded. */
	public Output getOutput(int bufferSize);

	/** Creates a new Output for writing to a byte array.
	 * @param bufferSize The initial size of the buffer.
	 * @param maxBufferSize The buffer is doubled as needed until it exceeds maxBufferSize and an exception is thrown. Can be -1
	 *           for no maximum. */
	public Output getOutput(int bufferSize, int maxBufferSize);
	
	/** Creates a new Output for writing to a byte array.
	 * @see Output#setBuffer(byte[]) */
	public Output getOutput(byte[] buffer);

	/** Creates a new Output for writing to a byte array.
	 * @see Output#setBuffer(byte[], int) */
	public Output getOutput(byte[] buffer, int maxBufferSize);

	/** Creates a new Output for writing to an OutputStream. A buffer size of 4096 is used. */
	public Output getOutput(OutputStream outputStream);
	
	/** Creates a new Output for writing to an OutputStream. */
	public Output getOutput(OutputStream outputStream, int bufferSize);

	public void setKryo(Kryo kryo);
	
}
