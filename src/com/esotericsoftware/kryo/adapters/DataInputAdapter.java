package com.esotericsoftware.kryo.adapters;

import com.esotericsoftware.kryo.io.Input;

import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;

/**
 * Best attempt adapter for {@link DataInput}. Currently only {@link #readLine()} is unsupported. Other methods
 * behave slightly differently. For example, {@link #readUTF()} will return a null string.
 *
 * @author Robert DiFalco <robert.difalco@gmail.com>
 */
public class DataInputAdapter implements DataInput {

    protected final Input input;

    public DataInputAdapter( Input input ) {
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
        int total = 0;
        int cur = 0;

        while ((total<n) && ((cur = (int) input.skip( (long)n-total) ) > 0)) {
            total += cur;
        }

        return total;
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

    public String readLine() throws IOException {
        throw new UnsupportedOperationException();
    }

    public String readUTF() throws IOException {
        return input.readString();
    }
}
