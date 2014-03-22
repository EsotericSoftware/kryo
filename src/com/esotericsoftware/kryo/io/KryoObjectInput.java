package com.esotericsoftware.kryo.io;

import com.esotericsoftware.kryo.Kryo;

import java.io.IOException;
import java.io.ObjectInput;

/**
 * A kryo implementation of {@link ObjectInput}. Note that this is not an implementation of {@link java.io.ObjectInputStream}
 * which has special handling for serialization in Java such as support for readResolve.
 *
 * @author Robert DiFalco <robert.difalco@gmail.com>
 */
public class KryoObjectInput extends KryoDataInput implements ObjectInput {

	 private final Kryo kryo;

	 public KryoObjectInput (Kryo kryo, Input in) {
		  super(in);
		  this.kryo = kryo;
	 }

	 public Object readObject () throws ClassNotFoundException, IOException {
		  return kryo.readClassAndObject(input);
	 }

	 public int read () throws IOException {
		  return input.read();
	 }

	 public int read (byte[] b) throws IOException {
		  return input.read(b);
	 }

	 public int read (byte[] b, int off, int len) throws IOException {
		  return input.read(b, off, len);
	 }

	 public long skip (long n) throws IOException {
		  return input.skip(n);
	 }

	 public int available () throws IOException {
		  return 0;
	 }

	 public void close () throws IOException {
		  input.close();
	 }
}
