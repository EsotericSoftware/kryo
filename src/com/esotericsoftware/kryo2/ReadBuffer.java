
package com.esotericsoftware.kryo2;

import java.util.ArrayList;

import com.esotericsoftware.kryo.SerializationException;

// BOZO - Reuse read/write buffer instances?

public class ReadBuffer {
	private final ArrayList<Buffer> filledBuffers = new ArrayList();
	private final ArrayList<Buffer> emptyBuffers = new ArrayList();
	private final int bufferSize, coreBuffers, maxBuffers;
	private byte[] bufferBytes;
	private int bufferCount;
	private int bufferIndex, position;
	private int lastBufferIndex;
	private int marks;
	private char[] chars = new char[64];
	private byte[] temp = new byte[8];

	public ReadBuffer () {
		this(4096, 4, 4);
	}

	public ReadBuffer (int size) {
		this(size, 1, 1);
	}

	public ReadBuffer (int bufferSize, int maxBuffers) {
		this(bufferSize, maxBuffers, maxBuffers);
	}

	public ReadBuffer (int bufferSize, int coreBuffers, int maxBuffers) {
		this.bufferSize = bufferSize;
		this.coreBuffers = coreBuffers;
		this.maxBuffers = maxBuffers;

		Buffer buffer = new Buffer();
		filledBuffers.add(buffer);
		bufferBytes = buffer.bytes = new byte[bufferSize];
	}

	public ReadBuffer (byte[] bytes) {
		this(bytes, 0, bytes.length);
	}

	public ReadBuffer (byte[] bytes, int offset, int count) {
		bufferSize = bytes.length;
		coreBuffers = 1;
		maxBuffers = 1;

		Buffer buffer = new Buffer();
		filledBuffers.add(buffer);
		bufferBytes = buffer.bytes = bytes;
		bufferCount = buffer.count = count;
		position = offset;
	}

	public int mark () {
		marks++;
		return bufferIndex * (bufferSize + 1) + position;
	}

	public void positionToMark (int mark) {
		if (marks == 0) throw new IllegalStateException("No marks have been set.");
		bufferIndex = mark / (bufferSize + 1);
		position = mark % (bufferSize + 1);
		Buffer buffer = filledBuffers.get(bufferIndex);
		bufferBytes = buffer.bytes;
		bufferCount = buffer.count;
		marks--;
	}

	public void clear () {
		bufferIndex = 0;
		position = 0;
	}

	//

	public void skip (int count) {
		while (true) {
			int skipCount = Math.min(bufferCount - position, count);
			position += skipCount;
			count -= skipCount;
			if (count == 0) break;
			bufferEmpty();
		}
	}

	private void bufferEmpty () {
		position = 0;
		bufferIndex++;
		// Move read buffers from filled to empty list.
		if (marks == 0) {
			while (bufferIndex > 0) {
				Buffer buffer = filledBuffers.remove(0);
				if (filledBuffers.size() + emptyBuffers.size() < coreBuffers) emptyBuffers.add(buffer);
				bufferIndex--;
			}
		}
		// Use already filled buffer.
		Buffer buffer;
		if (bufferIndex < filledBuffers.size())
			buffer = filledBuffers.get(bufferIndex);
		else {
			if (!emptyBuffers.isEmpty()) {
				// Use an empty buffer.
				buffer = emptyBuffers.remove(0);
				filledBuffers.add(buffer);
			} else {
				// Add new buffer.
				if (maxBuffers != -1 && filledBuffers.size() + emptyBuffers.size() >= maxBuffers)
					throw new SerializationException("Maximum number of buffers reached: " + maxBuffers);
				buffer = new Buffer();
				buffer.bytes = new byte[bufferSize];
				filledBuffers.add(buffer);
			}
			// Fill buffer.
			buffer.count = input(buffer.bytes);
			if (buffer.count <= 0) throw new SerializationException("Buffer underflow.");
		}
		bufferBytes = buffer.bytes;
		bufferCount = buffer.count;
	}

	protected int input (byte[] bytes) {
		return -1;
	}

	// byte

	public byte readByte () {
		if (position == bufferCount) bufferEmpty();
		return bufferBytes[position++];
	}

	public int readUnsignedByte () {
		if (position == bufferCount) bufferEmpty();
		return bufferBytes[position++] & 0xFF;
	}

	public void readBytes (byte[] bytes) {
		readBytes(bytes, 0, bytes.length);
	}

	public void readBytes (byte[] bytes, int offset, int count) {
		while (true) {
			int copyCount = Math.min(bufferCount - position, count);
			System.arraycopy(bufferBytes, position, bytes, offset, copyCount);
			position += copyCount;
			count -= copyCount;
			if (count == 0) break;
			offset += copyCount;
			bufferEmpty();
		}
	}

	// int

	public int readInt () {
		if (bufferCount - position < 4) return readInt_slow();
		byte[] bufferBytes = this.bufferBytes;
		return (bufferBytes[position++] & 0xFF) << 24 //
			| (bufferBytes[position++] & 0xFF) << 16 //
			| (bufferBytes[position++] & 0xFF) << 8 //
			| bufferBytes[position++] & 0xFF;
	}

	private int readInt_slow () {
		return (readByte() & 0xFF) << 24 | (readByte() & 0xFF) << 16 | (readByte() & 0xFF) << 8 | readByte() & 0xFF;
	}

	public int readInt (boolean optimizePositive) {
		if (bufferCount - position < 5) return readInt_slow_var(optimizePositive);
		byte[] bufferBytes = this.bufferBytes;
		int b = bufferBytes[position++];
		int result = b & 0x7F;
		if ((b & 0x80) != 0) {
			b = bufferBytes[position++];
			result |= (b & 0x7F) << 7;
			if ((b & 0x80) != 0) {
				b = bufferBytes[position++];
				result |= (b & 0x7F) << 14;
				if ((b & 0x80) != 0) {
					b = bufferBytes[position++];
					result |= (b & 0x7F) << 21;
					if ((b & 0x80) != 0) {
						b = bufferBytes[position++];
						result |= (b & 0x7F) << 28;
					}
				}
			}
		}
		if (!optimizePositive) result = (result >>> 1) ^ -(result & 1);
		return result;
	}

	private int readInt_slow_var (boolean optimizePositive) {
		int b = readByte();
		int result = b & 0x7F;
		if ((b & 0x80) != 0) {
			b = readByte();
			result |= (b & 0x7F) << 7;
			if ((b & 0x80) != 0) {
				b = readByte();
				result |= (b & 0x7F) << 14;
				if ((b & 0x80) != 0) {
					b = readByte();
					result |= (b & 0x7F) << 21;
					if ((b & 0x80) != 0) {
						b = readByte();
						result |= (b & 0x7F) << 28;
					}
				}
			}
		}
		if (!optimizePositive) result = (result >>> 1) ^ -(result & 1);
		return result;
	}

	public boolean canReadInt () {
		if (bufferCount - position >= 5) return true;
		int start = mark();
		try {
			if (position == bufferCount) bufferEmpty();
			if ((bufferBytes[position++] & 0x80) == 0) return true;
			if (position == bufferCount) bufferEmpty();
			if ((bufferBytes[position++] & 0x80) == 0) return true;
			if (position == bufferCount) bufferEmpty();
			if ((bufferBytes[position++] & 0x80) == 0) return true;
			if (position == bufferCount) bufferEmpty();
			if ((bufferBytes[position++] & 0x80) == 0) return true;
			if (position == bufferCount) bufferEmpty();
			return true;
		} catch (SerializationException ignored) {
			return false;
		} finally {
			positionToMark(start);
		}
	}

	// string

	public String readUTF () {
		int charCount = readInt(true);
		if (chars.length < charCount) chars = new char[charCount];
		if (bufferCount - position < charCount) return readUTF_slow(charCount, 0);
		char[] chars = this.chars;
		byte[] bufferBytes = this.bufferBytes;
		int c = 0, charIndex = 0;
		while (charIndex < charCount) {
			c = bufferBytes[position++] & 0xFF;
			if (c > 127) break;
			chars[charIndex++] = (char)c;
		}
		if (charIndex < charCount) {
			if (bufferCount - position < charCount - charIndex) return readUTF_slow(charCount, charIndex);
			while (true) {
				switch (c >> 4) {
				case 0:
				case 1:
				case 2:
				case 3:
				case 4:
				case 5:
				case 6:
				case 7:
					chars[charIndex++] = (char)c;
					break;
				case 12:
				case 13:
					chars[charIndex++] = (char)((c & 0x1F) << 6 | bufferBytes[position++] & 0x3F);
					break;
				case 14:
					chars[charIndex++] = (char)((c & 0x0F) << 12 | (bufferBytes[position++] & 0x3F) << 6 | bufferBytes[position++] & 0x3F);
					break;
				}
				if (charIndex >= charCount) break;
				c = bufferBytes[position++] & 0xFF;
			}
		}
		return new String(chars, 0, charCount);
	}

	private String readUTF_slow (int charCount, int charIndex) {
		char[] chars = this.chars;
		int c = 0;
		while (true) {
			if (position == bufferCount) bufferEmpty();
			c = bufferBytes[position++] & 0xFF;
			switch (c >> 4) {
			case 0:
			case 1:
			case 2:
			case 3:
			case 4:
			case 5:
			case 6:
			case 7:
				chars[charIndex++] = (char)c;
				break;
			case 12:
			case 13:
				chars[charIndex++] = (char)((c & 0x1F) << 6 | readByte() & 0x3F);
				break;
			case 14:
				chars[charIndex++] = (char)((c & 0x0F) << 12 | (readByte() & 0x3F) << 6 | readByte() & 0x3F);
				break;
			}
			if (charIndex >= charCount) break;
		}
		return new String(chars, 0, charCount);
	}

	public String readChars () {
		int charCount = readInt(true);
		if (chars.length < charCount) chars = new char[charCount];
		if (bufferCount - position < charCount) return readChars_slow(charCount);
		char[] chars = this.chars;
		byte[] bufferBytes = this.bufferBytes;
		int c, charIndex = 0;
		while (charIndex < charCount) {
			c = bufferBytes[position++] & 0xFF;
			chars[charIndex++] = (char)c;
		}
		return new String(chars, 0, charCount);
	}

	private String readChars_slow (int charCount) {
		char[] chars = this.chars;
		int c, charIndex = 0;
		while (charIndex < charCount) {
			if (position == bufferCount) bufferEmpty();
			c = bufferBytes[position++] & 0xFF;
			chars[charIndex++] = (char)c;
		}
		return new String(chars, 0, charCount);
	}

	// float

	public float readFloat () {
		return Float.intBitsToFloat(readInt());
	}

	public float readFloat (float precision, boolean optimizePositive) {
		return readInt(optimizePositive) / (float)precision;
	}

	// short

	public short readShort () {
		if (position == bufferCount) bufferEmpty();
		int b1 = bufferBytes[position++] & 0xFF;
		if (position == bufferCount) bufferEmpty();
		int b2 = bufferBytes[position++] & 0xFF;
		return (short)((b1 << 8) | b2);
	}

	public int readUnsignedShort () {
		if (position == bufferCount) bufferEmpty();
		int b1 = bufferBytes[position++] & 0xFF;
		if (position == bufferCount) bufferEmpty();
		int b2 = bufferBytes[position++] & 0xFF;
		return (b1 << 8) | b2;
	}

	public short readShort (boolean optimizePositive) {
		if (position == bufferCount) bufferEmpty();
		byte value = bufferBytes[position++];
		if (optimizePositive) {
			if (value == -1) return readShort(); // short positive
			return (short)(value & 0xFF);
		}
		if (value == -128) return readShort(); // short
		return value;
	}

	// long

	public long readLong () {
		if (bufferCount - position < 8) return readLong_slow();
		byte[] bufferBytes = this.bufferBytes;
		return (long)bufferBytes[position++] << 56 //
			| (long)(bufferBytes[position++] & 0xFF) << 48 //
			| (long)(bufferBytes[position++] & 0xFF) << 40 //
			| (long)(bufferBytes[position++] & 0xFF) << 32 //
			| (bufferBytes[position++] & 0xFF) << 24 //
			| (bufferBytes[position++] & 0xFF) << 16 //
			| (bufferBytes[position++] & 0xFF) << 8 //
			| bufferBytes[position++] & 0xFF;
	}

	private long readLong_slow () {
		return (long)readByte() << 56 //
			| (long)readUnsignedByte() << 48 //
			| (long)readUnsignedByte() << 40 //
			| (long)readUnsignedByte() << 32 //
			| readUnsignedByte() << 24 //
			| readUnsignedByte() << 16 //
			| readUnsignedByte() << 8 //
			| readUnsignedByte();
	}

	public long readLong (boolean optimizePositive) {
		if (bufferCount - position < 10) return readLong_slow_var(optimizePositive);
		int b = bufferBytes[position++];
		long result = b & 0x7F;
		if ((b & 0x80) != 0) {
			b = bufferBytes[position++];
			result |= (b & 0x7F) << 7;
			if ((b & 0x80) != 0) {
				b = bufferBytes[position++];
				result |= (b & 0x7F) << 14;
				if ((b & 0x80) != 0) {
					b = bufferBytes[position++];
					result |= (b & 0x7F) << 21;
					if ((b & 0x80) != 0) {
						b = bufferBytes[position++];
						result |= (long)(b & 0x7F) << 28;
						if ((b & 0x80) != 0) {
							b = bufferBytes[position++];
							result |= (long)(b & 0x7F) << 35;
							if ((b & 0x80) != 0) {
								b = bufferBytes[position++];
								result |= (long)(b & 0x7F) << 42;
								if ((b & 0x80) != 0) {
									b = bufferBytes[position++];
									result |= (long)(b & 0x7F) << 49;
									if ((b & 0x80) != 0) {
										b = bufferBytes[position++];
										result |= (long)(b & 0x7F) << 56;
										if ((b & 0x80) != 0) {
											b = bufferBytes[position++];
											result |= (long)(b & 0x7F) << 63;
										}
									}
								}
							}
						}
					}
				}
			}
		}
		if (!optimizePositive) result = (result >>> 1) ^ -(result & 1);
		return result;
	}

	private long readLong_slow_var (boolean optimizePositive) {
		int b = readByte();
		long result = b & 0x7F;
		if ((b & 0x80) != 0) {
			b = readByte();
			result |= (b & 0x7F) << 7;
			if ((b & 0x80) != 0) {
				b = readByte();
				result |= (b & 0x7F) << 14;
				if ((b & 0x80) != 0) {
					b = readByte();
					result |= (b & 0x7F) << 21;
					if ((b & 0x80) != 0) {
						b = readByte();
						result |= (long)(b & 0x7F) << 28;
						if ((b & 0x80) != 0) {
							b = readByte();
							result |= (long)(b & 0x7F) << 35;
							if ((b & 0x80) != 0) {
								b = readByte();
								result |= (long)(b & 0x7F) << 42;
								if ((b & 0x80) != 0) {
									b = readByte();
									result |= (long)(b & 0x7F) << 49;
									if ((b & 0x80) != 0) {
										b = readByte();
										result |= (long)(b & 0x7F) << 56;
										if ((b & 0x80) != 0) {
											b = readByte();
											result |= (long)(b & 0x7F) << 63;
										}
									}
								}
							}
						}
					}
				}
			}
		}
		if (!optimizePositive) result = (result >>> 1) ^ -(result & 1);
		return result;
	}

	// boolean

	public boolean readBoolean () {
		if (position == bufferCount) bufferEmpty();
		return bufferBytes[position++] == 1;
	}

	// char

	public char readChar () {
		if (position == bufferCount) bufferEmpty();
		int b1 = bufferBytes[position++] & 0xFF;
		if (position == bufferCount) bufferEmpty();
		int b2 = bufferBytes[position++] & 0xFF;
		return (char)((b1 << 8) | b2);
	}

	// double

	public double readDouble () {
		return Double.longBitsToDouble(readLong());
	}

	public double readDouble (double precision, boolean optimizePositive) {
		return readLong(optimizePositive) / (double)precision;
	}

	//

	static class Buffer {
		byte[] bytes;
		int count;
	}
}
