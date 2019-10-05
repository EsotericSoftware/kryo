
package com.esotericsoftware.kryo.benchmarks.io;

import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.unsafe.UnsafeByteBufferInput;
import com.esotericsoftware.kryo.unsafe.UnsafeByteBufferOutput;
import com.esotericsoftware.kryo.unsafe.UnsafeInput;
import com.esotericsoftware.kryo.unsafe.UnsafeOutput;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Thread)
public class InputOutputState {
	@Param() public InputOutputState.BufferType bufferType;

	Output output;
	Input input;

	@Setup(Level.Trial)
	public void setup () {
		switch (bufferType) {
		case array:
			output = new Output(1024 * 512);
			input = new Input(output.getBuffer());
			break;
		case unsafeArray:
			output = new UnsafeOutput(1024 * 512);
			input = new UnsafeInput(output.getBuffer());
			break;
		case byteBuffer:
			output = new ByteBufferOutput(1024 * 512);
			input = new ByteBufferInput(((ByteBufferOutput)output).getByteBuffer());
			break;
		case unsafeByteBuffer:
			output = new UnsafeByteBufferOutput(1024 * 512);
			input = new UnsafeByteBufferInput(((UnsafeByteBufferOutput)output).getByteBuffer());
			break;
		}
	}

	public void reset () {
		input.setPosition(0);
		output.setPosition(0);
	}

	static public enum BufferType {
		array, unsafeArray, byteBuffer, unsafeByteBuffer
	}
}
