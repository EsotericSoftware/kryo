package com.esotericsoftware.kryo.util;

import java.io.InputStream;
import java.io.OutputStream;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.StreamFactory;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * StreamFactory which provides usual Input/Output streams, which are
 * present in all versions of Kryo. 
 * 
 * @author Roman Levenstein <romixlev@gmail.com>
 */
public class DefaultStreamFactory implements StreamFactory {
	
	@Override
	public Input getInput() {
		return new Input();
	}

	@Override
	public Input getInput(int bufferSize) {
		return new Input(bufferSize);
	}

	@Override
	public Input getInput(byte[] buffer) {
		return new Input(buffer);
	}

	@Override
	public Input getInput(byte[] buffer, int offset, int count) {
		return new Input(buffer, offset, count);
	}

	@Override
	public Input getInput(InputStream inputStream) {
		return new Input(inputStream);
	}

	@Override
	public Input getInput(InputStream inputStream, int bufferSize) {
		return new Input(inputStream, bufferSize);
	}

	@Override
	public Output getOutput() {
		return new Output();
	}

	@Override
	public Output getOutput(int bufferSize) {
		return new Output(bufferSize);
	}

	@Override
	public Output getOutput(int bufferSize, int maxBufferSize) {
		return new Output(bufferSize, maxBufferSize);
	}

	@Override
	public Output getOutput(byte[] buffer) {
		return new Output(buffer);
	}

	@Override
	public Output getOutput(byte[] buffer, int maxBufferSize) {
		return new Output(buffer, maxBufferSize);
	}

	@Override
	public Output getOutput(OutputStream outputStream) {
		return new Output(outputStream);
	}

	@Override
	public Output getOutput(OutputStream outputStream, int bufferSize) {
		return new Output(outputStream, bufferSize);
	}

	@Override
	public void setKryo(Kryo kryo) {
	}

}
