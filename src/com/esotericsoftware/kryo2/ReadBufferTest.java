
package com.esotericsoftware.kryo2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.esotericsoftware.kryo.KryoTestCase;
import com.esotericsoftware.kryo.SerializationException;

import junit.framework.TestCase;

public class ReadBufferTest extends KryoTestCase {
	public void testMarks () throws IOException {
		runMarksTest(1024, 1, 1);
		runMarksTest(2, 1, -1);
		runMarksTest(3, 1, 40);
	}

	public void runMarksTest (int bufferSize, int coreBuffers, int maxBuffers) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write(new byte[] {11, 22, 33, 44, 55, 66, 77, 88, 99});
		final ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		ReadBuffer buffer = new ReadBuffer(bufferSize, coreBuffers, maxBuffers) {
			protected int input (byte[] buffer) {
				try {
					return in.read(buffer);
				} catch (IOException ex) {
					throw new SerializationException(ex);
				}
			}
		};

		ByteArrayOutputStream read = new ByteArrayOutputStream();
		read.write(buffer.readByte());
		read.write(buffer.readByte());
		read.write(buffer.readByte());
		int start = buffer.mark();
		read.write(buffer.readByte());
		read.write(buffer.readByte());
		read.write(buffer.readByte());
		int end = buffer.mark();
		buffer.positionToMark(start);
		read.write(buffer.readByte());
		read.write(buffer.readByte());
		buffer.positionToMark(end);
		read.write(buffer.readByte());
		read.write(buffer.readByte());
		read.write(buffer.readByte());

		assertEquals(read.toByteArray(), new byte[] { //
			11, 22, 33, 44, 55, 66, 44, 55, 77, 88, 99});
	}
}
