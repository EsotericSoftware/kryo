package com.esotericsoftware.kryo.io;

import java.io.DataOutput;
import java.io.IOException;

/**
 * A kryo implementation of {@link java.io.DataOutput}.
 *
 * @author Robert DiFalco <robert.difalco@gmail.com>
 */
public class KryoDataOutput implements DataOutput {

	 protected Output output;

	 public KryoDataOutput (Output output) {
		  setOutput(output);
	 }

	 public void setOutput (Output output) {
		  this.output = output;
	 }

	 public void write (int b) throws IOException {
		  output.write(b);
	 }

	 public void write (byte[] b) throws IOException {
		  output.write(b);
	 }

	 public void write (byte[] b, int off, int len) throws IOException {
		  output.write(b, off, len);
	 }

	 public void writeBoolean (boolean v) throws IOException {
		  output.writeBoolean(v);
	 }

	 public void writeByte (int v) throws IOException {
		  output.writeByte(v);
	 }

	 public void writeShort (int v) throws IOException {
		  output.writeShort(v);
	 }

	 public void writeChar (int v) throws IOException {
		  output.writeChar((char)v);
	 }

	 public void writeInt (int v) throws IOException {
		  output.writeInt(v);
	 }

	 public void writeLong (long v) throws IOException {
		  output.writeLong(v);
	 }

	 public void writeFloat (float v) throws IOException {
		  output.writeFloat(v);
	 }

	 public void writeDouble (double v) throws IOException {
		  output.writeDouble(v);
	 }

	 public void writeBytes (String s) throws IOException {
		  int len = s.length();
		  for (int i = 0; i < len; i++) {
				output.write((byte)s.charAt(i));
		  }
	 }

	 public void writeChars (String s) throws IOException {
		  int len = s.length();
		  for (int i = 0; i < len; i++) {
				int v = s.charAt(i);
				output.write((v >>> 8) & 0xFF);
				output.write((v >>> 0) & 0xFF);
		  }
	 }

	 public void writeUTF (String s) throws IOException {
		  output.writeString(s);
	 }
}
