
package com.esotericsoftware.kryo2;

import java.util.ArrayList;

import com.esotericsoftware.kryo.SerializationException;

public class WriteBuffer {
	private final ArrayList<byte[]> buffers = new ArrayList();
	private final int bufferSize, coreBuffers, maxBuffers;
	private byte[] buffer;
	private int bufferIndex, position;
	private int marks;

	public WriteBuffer () {
		this(4096, 4, -1);
	}

	public WriteBuffer (int size) {
		this(size, 1, 1);
	}

	public WriteBuffer (int bufferSize, int maxBuffers) {
		this(bufferSize, maxBuffers, maxBuffers);
	}

	public WriteBuffer (int bufferSize, int coreBuffers, int maxBuffers) {
		this.bufferSize = bufferSize;
		this.coreBuffers = coreBuffers;
		this.maxBuffers = maxBuffers;

		buffer = new byte[bufferSize];
		buffers.add(buffer);
	}

	public int mark () {
		marks++;
		return bufferIndex * (bufferSize + 1) + position;
	}

	public void positionToMark (int mark) {
		if (marks == 0) throw new IllegalStateException("No marks have been set.");
		bufferIndex = mark / (bufferSize + 1);
		position = mark % (bufferSize + 1);
		buffer = buffers.get(bufferIndex);
		marks--;
	}

	public void clear () {
		bufferIndex = 0;
		position = 0;
	}

	private void bufferFull () {
		bufferIndex++;
		position = 0;
		// Attempt to process buffers.
		if (marks == 0) {
			while (bufferIndex > 0) {
				byte[] buffer = buffers.get(0);
				if (!output(buffer, bufferSize)) break;
				buffers.remove(0);
				if (buffers.size() < coreBuffers) buffers.add(buffer); // Keep processed buffer at the end.
				bufferIndex--;
			}
		}
		// Use existing buffer.
		if (bufferIndex < buffers.size()) {
			buffer = buffers.get(bufferIndex);
			return;
		}
		// Allocate new buffer.
		if (maxBuffers != -1 && buffers.size() >= maxBuffers)
			throw new SerializationException("Maximum number of buffers reached: " + maxBuffers);
		buffer = new byte[bufferSize];
		buffers.add(buffer);
	}

	public void flush () {
		if (marks > 0) throw new SerializationException(marks + " marks were set but never used.");
		for (int i = 0, n = bufferIndex; i <= n; i++)
			if (!output(buffers.get(i), i == n ? position : bufferSize)) break;
	}

	protected boolean output (byte[] bytes, int count) {
		return false;
	}

	public byte[] toBytes () {
		byte[] bytes = new byte[bufferIndex * bufferSize + position];
		int offset = 0;
		for (int i = 0, n = bufferIndex; i <= n; i++) {
			int count = i == n ? position : bufferSize;
			System.arraycopy(buffers.get(i), 0, bytes, offset, count);
			offset += count;
		}
		return bytes;
	}

	public byte[] popBytes () {
		byte[] buffer = buffers.remove(0);
		if (buffers.size() < coreBuffers) buffers.add(buffer); // Keep processed buffer at the end.
		bufferIndex--;
		return buffer;
	}

	// byte

	public void writeByte (int value) {
		if (position == bufferSize) bufferFull();
		buffer[position++] = (byte)value;
	}

	public void writeBytes (byte[] bytes) {
		writeBytes(bytes, 0, bytes.length);
	}

	public void writeBytes (byte[] bytes, int offset, int count) {
		while (true) {
			int copyCount = Math.min(bufferSize - position, count);
			System.arraycopy(bytes, offset, buffer, position, copyCount);
			position += copyCount;
			count -= copyCount;
			if (count == 0) break;
			offset += copyCount;
			bufferFull();
		}
	}

	// int

	public void writeInt (int value) {
		writeByte(value >> 24);
		writeByte(value >> 16);
		writeByte(value >> 8);
		writeByte(value);
	}

	public int writeInt (int value, boolean optimizePositive) {
		if (!optimizePositive) value = (value << 1) ^ (value >> 31);
		if ((value & ~0x7F) == 0) {
			writeByte(value);
			return 1;
		}
		writeByte((value & 0x7F) | 0x80);
		value >>>= 7;
		if ((value & ~0x7F) == 0) {
			writeByte(value);
			return 2;
		}
		writeByte((value & 0x7F) | 0x80);
		value >>>= 7;
		if ((value & ~0x7F) == 0) {
			writeByte(value);
			return 3;
		}
		writeByte((value & 0x7F) | 0x80);
		value >>>= 7;
		if ((value & ~0x7F) == 0) {
			writeByte(value);
			return 4;
		}
		writeByte((value & 0x7F) | 0x80);
		value >>>= 7;
		writeByte(value);
		return 5;
	}

	// string

	public void writeString (String value) {
		int charCount = value.length();
		writeInt(charCount, true);
		for (int i = 0; i < charCount; i++) {
			int c = value.charAt(i);
			if (c <= 0x007F)
				writeByte(c);
			else if (c > 0x07FF) {
				writeByte(0xE0 | c >> 12 & 0x0F);
				writeByte(0x80 | c >> 6 & 0x3F);
				writeByte(0x80 | c >> 0 & 0x3F);
			} else {
				writeByte(0xC0 | c >> 6 & 0x1F);
				writeByte(0x80 | c >> 0 & 0x3F);
			}
		}
	}

	//

	// BOZO - Implement rest of methods!
}
