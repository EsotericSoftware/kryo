package com.esotericsoftware.kryo.io;

import com.esotericsoftware.kryo.Kryo;

import java.io.IOException;
import java.io.ObjectOutput;

/**
 * A kryo adapter for the {@link java.io.ObjectOutput} class. Note that this is not a Kryo implementation
 * of {@link java.io.ObjectOutputStream} which has special handling for default serialization and serialization
 * extras like writeReplace. By default it will simply delegate to the appropriate kryo method. Also, using
 * it will currently add one extra byte for each time {@link #writeObject(Object)} is invoked since we need
 * to allow unknown null objects.
 *
 * @author Robert DiFalco <robert.difalco@gmail.com>
 */
public class KryoObjectOutput extends KryoDataOutput implements ObjectOutput {

	 private final Kryo kryo;

	 public KryoObjectOutput (Kryo kryo, Output output) {
		  super(output);
		  this.kryo = kryo;
	 }

	 public void writeObject (Object obj) throws IOException {
		  kryo.writeClassAndObject(output, obj);
	 }

	 public void flush () throws IOException {
		  output.flush();
	 }

	 public void close () throws IOException {
		  output.close();
	 }
}
