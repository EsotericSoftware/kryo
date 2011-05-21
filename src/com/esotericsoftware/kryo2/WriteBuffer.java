
package com.esotericsoftware.kryo2;

import java.util.ArrayList;

import com.esotericsoftware.kryo.SerializationException;

public class WriteBuffer {
	private final ArrayList<byte[]> buffers = new ArrayList();
	private final int bufferSize, coreBuffers, maxBuffers;
	private byte[] buffer;
	private int bufferIndex, position;
	private int marks;
	private byte[] temp = new byte[8];

	public WriteBuffer () {
		this(4096, 4, -1);
	}

	public WriteBuffer (int size) {
		this(size, 1, 1);
	}

	public WriteBuffer (int bufferSize, int maxBuffers) {
		this(bufferSize, maxBuffers == -1 ? Integer.MAX_VALUE : 0, maxBuffers);
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
		// BOZO - Faster with temp?
		writeByte(value >> 24);
		writeByte(value >> 16);
		writeByte(value >> 8);
		writeByte(value);
	}

	public int writeInt (int value, boolean optimizePositive) {
		// BOZO - Faster with temp?
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
		// BOZO - Faster with temp?
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

	// float

	public void writeFloat (float value) {
		// BOZO - Faster with temp?
		int i = Float.floatToIntBits(value);
		writeByte(i >> 24);
		writeByte(i >> 16);
		writeByte(i >> 8);
		writeByte(i);
	}

	public int writeFloat (float value, float precision, boolean optimizePositive) {
		return writeInt((int)(value * precision), optimizePositive);
	}

	// short

	public void writeShort (int value) {
		writeByte((value >>> 8) & 0xFF);
		writeByte(value & 0xFF);
	}

	public int writeShort (int value, boolean optimizePositive) {
		if (optimizePositive) {
			if (value >= 0 && value <= 254) {
				writeByte(value);
				return 1;
			}
			writeByte(-1); // short positive
			writeShort(value);
		} else {
			if (value >= -127 && value <= 127) {
				writeByte(value);
				return 1;
			}
			writeByte(-128); // short
			writeShort(value);
		}
		return 3;
	}

	// long

	public void writeLong (long value) {
		temp[0] = (byte)(value >>> 56);
		temp[1] = (byte)(value >>> 48);
		temp[2] = (byte)(value >>> 40);
		temp[3] = (byte)(value >>> 32);
		temp[4] = (byte)(value >>> 24);
		temp[5] = (byte)(value >>> 16);
		temp[6] = (byte)(value >>> 8);
		temp[7] = (byte)(value >>> 0);
		writeBytes(temp, 0, 8);
	}

	public int writeLong (long value, boolean optimizePositive) {
		// BOZO - Faster with temp?
		if (!optimizePositive) value = (value << 1) ^ (value >> 63);
		if ((value & ~0x7FL) == 0) {
			writeByte((int)value);
			return 1;
		}
		writeByte(((int)value & 0x7F) | 0x80);
		value >>>= 7;
		if ((value & ~0x7FL) == 0) {
			writeByte((int)value);
			return 2;
		}
		writeByte(((int)value & 0x7F) | 0x80);
		value >>>= 7;
		if ((value & ~0x7FL) == 0) {
			writeByte((int)value);
			return 3;
		}
		writeByte(((int)value & 0x7F) | 0x80);
		value >>>= 7;
		if ((value & ~0x7FL) == 0) {
			writeByte((int)value);
			return 4;
		}
		writeByte(((int)value & 0x7F) | 0x80);
		value >>>= 7;
		if ((value & ~0x7FL) == 0) {
			writeByte((int)value);
			return 5;
		}
		writeByte(((int)value & 0x7F) | 0x80);
		value >>>= 7;
		if ((value & ~0x7FL) == 0) {
			writeByte((int)value);
			return 6;
		}
		writeByte(((int)value & 0x7F) | 0x80);
		value >>>= 7;
		if ((value & ~0x7FL) == 0) {
			writeByte((int)value);
			return 7;
		}
		writeByte(((int)value & 0x7F) | 0x80);
		value >>>= 7;
		if ((value & ~0x7FL) == 0) {
			writeByte((int)value);
			return 8;
		}
		writeByte(((int)value & 0x7F) | 0x80);
		value >>>= 7;
		if ((value & ~0x7FL) == 0) {
			writeByte((int)value);
			return 9;
		}
		writeByte(((int)value & 0x7F) | 0x80);
		value >>>= 7;
		writeByte((byte)value);
		return 10;
	}

	// boolean

	public void writeBoolean (boolean value) {
		writeByte(value ? 1 : 0);
	}

	// char

	public void writeChar (char value) {
		writeByte((value >>> 8) & 0xFF);
		writeByte(value & 0xFF);
	}

	// double

	public void writeDouble (double value) {
		long i = Double.doubleToLongBits(value);
		temp[0] = (byte)(i >>> 56);
		temp[1] = (byte)(i >>> 48);
		temp[2] = (byte)(i >>> 40);
		temp[3] = (byte)(i >>> 32);
		temp[4] = (byte)(i >>> 24);
		temp[5] = (byte)(i >>> 16);
		temp[6] = (byte)(i >>> 8);
		temp[7] = (byte)(i >>> 0);
		writeBytes(temp, 0, 8);
	}

	public int writeDouble (double value, double precision, boolean optimizePositive) {
		return writeLong((long)(value * precision), optimizePositive);
	}
}
