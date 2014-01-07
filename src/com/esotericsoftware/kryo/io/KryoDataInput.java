package com.esotericsoftware.kryo.io;

import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;

/**
 * Best attempt adapter for {@link DataInput}. Currently only {@link #readLine()} is unsupported. Other methods
 * behave slightly differently. For example, {@link #readUTF()} will return a null string.
 *
 * @author Robert DiFalco <robert.difalco@gmail.com>
 */
public class KryoDataInput implements DataInput {

    protected final Input input;

    public KryoDataInput( Input input ) {
        this.input = input;
    }

    public void readFully( byte[] b ) throws IOException {
        readFully( b, 0, b.length );
    }

    public void readFully( byte[] b, int off, int len ) throws IOException {
        if (len < 0)
            throw new IndexOutOfBoundsException();

        int n = 0;
        while (n < len) {
            int count = input.read(b, off + n, len - n);
            if (count < 0)
                throw new EOFException();

            n += count;
        }
    }

    public int skipBytes( int n ) throws IOException {
        return (int)input.skip( (long)n );
    }

    public boolean readBoolean() throws IOException {
        return input.readBoolean();
    }

    public byte readByte() throws IOException {
        return input.readByte();
    }

    public int readUnsignedByte() throws IOException {
        return input.readByteUnsigned();
    }

    public short readShort() throws IOException {
        return input.readShort();
    }

    public int readUnsignedShort() throws IOException {
        return input.readShortUnsigned();
    }

    public char readChar() throws IOException {
        return input.readChar();
    }

    public int readInt() throws IOException {
        return input.readInt();
    }

    public long readLong() throws IOException {
        return input.readLong();
    }

    public float readFloat() throws IOException {
        return input.readFloat();
    }

    public double readDouble() throws IOException {
        return input.readDouble();
    }

    /**
     * This is not currently implemented. The method will currently throw an {@link java.lang.UnsupportedOperationException}
     * whenever it is called.
     * @throws UnsupportedOperationException when called.
     */
    public String readLine() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Reads the length and string of UTF8 characters, or null. This can read strings written by
     * {@link KryoDataOutput#writeUTF(String)},
     * {@link com.esotericsoftware.kryo.io.Output#writeString(String)},
     * {@link com.esotericsoftware.kryo.io.Output#writeString(CharSequence)}, and
     * {@link com.esotericsoftware.kryo.io.Output#writeAscii(String)}.
     * @return May be null.
     */
    public String readUTF() throws IOException {
        return input.readString();
    }
}
