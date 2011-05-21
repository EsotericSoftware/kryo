
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
		buffer = buffers.get(0);
		marks = 0;
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
		if (bufferSize - position < 4)
			writeInt_slow(value);
		else {
			byte[] buffer = this.buffer;
			buffer[position++] = (byte)(value >> 24);
			buffer[position++] = (byte)(value >> 16);
			buffer[position++] = (byte)(value >> 8);
			buffer[position++] = (byte)value;
		}
	}

	private void writeInt_slow (int value) {
		if (position == bufferSize) bufferFull();
		buffer[position++] = (byte)(value >> 24);
		if (position == bufferSize) bufferFull();
		buffer[position++] = (byte)(value >> 16);
		if (position == bufferSize) bufferFull();
		buffer[position++] = (byte)(value >> 8);
		if (position == bufferSize) bufferFull();
		buffer[position++] = (byte)value;
	}

	public int writeInt (int value, boolean optimizePositive) {
		if (!optimizePositive) value = (value << 1) ^ (value >> 31);
		if (bufferSize - position < 5) return writeInt_slow_var(value);
		byte[] buffer = this.buffer;
		if ((value & ~0x7F) == 0) {
			buffer[position++] = (byte)value;
			return 1;
		}
		buffer[position++] = (byte)((value & 0x7F) | 0x80);
		value >>>= 7;
		if ((value & ~0x7F) == 0) {
			buffer[position++] = (byte)value;
			return 2;
		}
		buffer[position++] = (byte)((value & 0x7F) | 0x80);
		value >>>= 7;
		if ((value & ~0x7F) == 0) {
			buffer[position++] = (byte)value;
			return 3;
		}
		buffer[position++] = (byte)((value & 0x7F) | 0x80);
		value >>>= 7;
		if ((value & ~0x7F) == 0) {
			buffer[position++] = (byte)value;
			return 4;
		}
		buffer[position++] = (byte)((value & 0x7F) | 0x80);
		value >>>= 7;
		buffer[position++] = (byte)value;
		return 5;
	}

	private int writeInt_slow_var (int value) {
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

	public void writeChars (String value) {
		int charCount = value.length();
		writeInt(charCount, true);
		if (bufferSize - position < charCount)
			writeChars_slow(value);
		else {
			byte[] buffer = this.buffer;
			for (int i = 0; i < charCount; i++)
				buffer[position++] = (byte)value.charAt(i);
		}
	}

	private void writeChars_slow (String value) {
		for (int i = 0, n = value.length(); i < n; i++) {
			if (position == bufferSize) bufferFull();
			buffer[position++] = (byte)value.charAt(i);
		}
	}

	public void writeUTF (String value) {
		int charCount = value.length();
		writeInt(charCount, true);
		if (bufferSize - position < charCount * 3)
			writeUTF_slow(value);
		else {
			byte[] buffer = this.buffer;
			int c;
			for (int i = 0; i < charCount; i++) {
				c = value.charAt(i);
				if (c <= 0x007F) {
					buffer[position++] = (byte)c;
				} else if (c > 0x07FF) {
					buffer[position++] = (byte)(0xE0 | c >> 12 & 0x0F);
					buffer[position++] = (byte)(0x80 | c >> 6 & 0x3F);
					buffer[position++] = (byte)(0x80 | c & 0x3F);
				} else {
					buffer[position++] = (byte)(0xC0 | c >> 6 & 0x1F);
					buffer[position++] = (byte)(0x80 | c & 0x3F);
				}
			}
		}
	}

	private void writeUTF_slow (String value) {
		int c;
		for (int i = 0, n = value.length(); i < n; i++) {
			c = value.charAt(i);
			if (position == bufferSize) bufferFull();
			if (c <= 0x007F)
				buffer[position++] = (byte)c;
			else if (c > 0x07FF) {
				buffer[position++] = (byte)(0xE0 | c >> 12 & 0x0F);
				if (position == bufferSize) bufferFull();
				buffer[position++] = (byte)(0x80 | c >> 6 & 0x3F);
				if (position == bufferSize) bufferFull();
				buffer[position++] = (byte)(0x80 | c & 0x3F);
			} else {
				buffer[position++] = (byte)(0xC0 | c >> 6 & 0x1F);
				if (position == bufferSize) bufferFull();
				buffer[position++] = (byte)(0x80 | c & 0x3F);
			}
		}
	}

	// float

	public void writeFloat (float value) {
		writeInt(Float.floatToIntBits(value));
	}

	public int writeFloat (float value, float precision, boolean optimizePositive) {
		return writeInt((int)(value * precision), optimizePositive);
	}

	// short

	public void writeShort (int value) {
		if (position == bufferSize) bufferFull();
		buffer[position++] = (byte)((value >>> 8) & 0xFF);
		if (position == bufferSize) bufferFull();
		buffer[position++] = (byte)(value & 0xFF);
	}

	public int writeShort (int value, boolean optimizePositive) {
		if (position == bufferSize) bufferFull();
		if (optimizePositive) {
			if (value >= 0 && value <= 254) {
				buffer[position++] = (byte)value;
				return 1;
			}
			buffer[position++] = -1; // short positive
		} else {
			if (value >= -127 && value <= 127) {
				buffer[position++] = (byte)value;
				return 1;
			}
			buffer[position++] = -128; // short
		}
		if (position == bufferSize) bufferFull();
		buffer[position++] = (byte)((value >>> 8) & 0xFF);
		if (position == bufferSize) bufferFull();
		buffer[position++] = (byte)(value & 0xFF);
		return 3;
	}

	// long

	public void writeLong (long value) {
		if (bufferSize - position < 8) {
			writeLong_slow(value);
			return;
		}
		byte[] buffer = this.buffer;
		buffer[position++] = (byte)(value >>> 56);
		buffer[position++] = (byte)(value >>> 48);
		buffer[position++] = (byte)(value >>> 40);
		buffer[position++] = (byte)(value >>> 32);
		buffer[position++] = (byte)(value >>> 24);
		buffer[position++] = (byte)(value >>> 16);
		buffer[position++] = (byte)(value >>> 8);
		buffer[position++] = (byte)(value);
	}

	private void writeLong_slow (long value) {
		if (position == bufferSize) bufferFull();
		buffer[position++] = (byte)(value >>> 56);
		if (position == bufferSize) bufferFull();
		buffer[position++] = (byte)(value >>> 48);
		if (position == bufferSize) bufferFull();
		buffer[position++] = (byte)(value >>> 40);
		if (position == bufferSize) bufferFull();
		buffer[position++] = (byte)(value >>> 32);
		if (position == bufferSize) bufferFull();
		buffer[position++] = (byte)(value >>> 24);
		if (position == bufferSize) bufferFull();
		buffer[position++] = (byte)(value >>> 16);
		if (position == bufferSize) bufferFull();
		buffer[position++] = (byte)(value >>> 8);
		if (position == bufferSize) bufferFull();
		buffer[position++] = (byte)value;
	}

	public int writeLong (long value, boolean optimizePositive) {
		if (!optimizePositive) value = (value << 1) ^ (value >> 63);
		if (bufferSize - position < 8) return writeLong_slow_var(value);
		byte[] buffer = this.buffer;
		if ((value & ~0x7FL) == 0) {
			buffer[position++] = (byte)value;
			return 1;
		}
		buffer[position++] = (byte)(((int)value & 0x7F) | 0x80);
		value >>>= 7;
		if ((value & ~0x7FL) == 0) {
			buffer[position++] = (byte)value;
			return 2;
		}
		buffer[position++] = (byte)(((int)value & 0x7F) | 0x80);
		value >>>= 7;
		if ((value & ~0x7FL) == 0) {
			buffer[position++] = (byte)value;
			return 3;
		}
		buffer[position++] = (byte)(((int)value & 0x7F) | 0x80);
		value >>>= 7;
		if ((value & ~0x7FL) == 0) {
			buffer[position++] = (byte)value;
			return 4;
		}
		buffer[position++] = (byte)(((int)value & 0x7F) | 0x80);
		value >>>= 7;
		if ((value & ~0x7FL) == 0) {
			buffer[position++] = (byte)value;
			return 5;
		}
		buffer[position++] = (byte)(((int)value & 0x7F) | 0x80);
		value >>>= 7;
		if ((value & ~0x7FL) == 0) {
			buffer[position++] = (byte)value;
			return 6;
		}
		buffer[position++] = (byte)(((int)value & 0x7F) | 0x80);
		value >>>= 7;
		if ((value & ~0x7FL) == 0) {
			buffer[position++] = (byte)value;
			return 7;
		}
		buffer[position++] = (byte)(((int)value & 0x7F) | 0x80);
		value >>>= 7;
		if ((value & ~0x7FL) == 0) {
			buffer[position++] = (byte)value;
			return 8;
		}
		buffer[position++] = (byte)(((int)value & 0x7F) | 0x80);
		value >>>= 7;
		if ((value & ~0x7FL) == 0) {
			buffer[position++] = (byte)value;
			return 9;
		}
		buffer[position++] = (byte)(((int)value & 0x7F) | 0x80);
		value >>>= 7;
		buffer[position++] = (byte)value;
		return 10;
	}

	private int writeLong_slow_var (long value) {
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
		if (position == bufferSize) bufferFull();
		buffer[position++] = (byte)(value ? 1 : 0);
	}

	// char

	public void writeChar (char value) {
		if (position == bufferSize) bufferFull();
		buffer[position++] = (byte)((value >>> 8) & 0xFF);
		if (position == bufferSize) bufferFull();
		buffer[position++] = (byte)(value & 0xFF);
	}

	// double

	public void writeDouble (double value) {
		writeLong(Double.doubleToLongBits(value));
	}

	public int writeDouble (double value, double precision, boolean optimizePositive) {
		return writeLong((long)(value * precision), optimizePositive);
	}
}
