/* Copyright (c) 2008-2026, Nathan Sweet
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 * - Neither the name of Esoteric Software nor the names of its contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. */

package com.esotericsoftware.kryo.serializers;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Kryo.Format;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.IgnoreAndroid;
import com.esotericsoftware.kryo.util.ObjectMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;

/** Serializes objects using Java's built in serialization mechanism. Note that this is very inefficient and should be avoided if
 * possible.
 * @see Serializer
 * @see FieldSerializer
 * @see KryoSerializable
 * @author Nathan Sweet */
public class JavaSerializer extends Serializer {
	/** The graph context key for the input stream. The output stream is stored with this serializer as key. */
	private final Object readKey = new Object();
	@IgnoreAndroid private ObjectInputFilter objectInputFilter;

	/** Sets an {@link ObjectInputFilter} on each {@link ObjectInputStream} this serializer creates for reading, before any object
	 * is read from it. The filter then decides about every class, array length, reference and depth in the Java serialization
	 * data, as described in {@link ObjectInputStream#setObjectInputFilter(ObjectInputFilter)}. As with any such filter, a class is
	 * resolved, without being initialized, before the filter sees it. This is opt-in protection for reading data from an untrusted
	 * source. When null (the default), this serializer sets no filter and behavior is unchanged.
	 * @param objectInputFilter May be null. */
	@IgnoreAndroid
	public void setObjectInputFilter (ObjectInputFilter objectInputFilter) {
		this.objectInputFilter = objectInputFilter;
	}

	/** @return May be null. */
	@IgnoreAndroid
	public ObjectInputFilter getObjectInputFilter () {
		return objectInputFilter;
	}

	public void write (Kryo kryo, Output output, Object object) {
		try {
			ObjectMap graphContext = kryo.getGraphContext();
			GraphOutputStream stream = (GraphOutputStream)graphContext.get(this);
			if (stream == null) {
				stream = new GraphOutputStream(output);
				graphContext.put(this, stream);
			}
			// A nested call comes from the writeObject method of a value, which is written to the object stream already. The Kryo 5
			// format stays on the first output, so the data is the same as Kryo 5 wrote.
			if (stream.depth++ == 0 && kryo.getFormat() != Format.KRYO5) stream.output = output;
			try {
				stream.objectStream.writeObject(object);
				stream.objectStream.flush();
			} finally {
				stream.depth--;
			}
		} catch (Exception ex) {
			throw new KryoException("Error during Java serialization.", ex);
		}
	}

	public Object read (Kryo kryo, Input input, Class type) {
		try {
			ObjectMap graphContext = kryo.getGraphContext();
			GraphInputStream stream = (GraphInputStream)graphContext.get(readKey);
			if (stream == null) {
				stream = new GraphInputStream(input, kryo);
				if (objectInputFilter != null) applyObjectInputFilter(stream.objectStream);
				graphContext.put(readKey, stream);
			}
			if (stream.depth++ == 0 && kryo.getFormat() != Format.KRYO5) stream.input = input;
			try {
				return stream.objectStream.readObject();
			} finally {
				stream.depth--;
			}
		} catch (Exception ex) {
			throw new KryoException("Error during Java deserialization.", ex);
		}
	}

	/** Android has no ObjectInputFilter, so the filter is always null there. */
	@IgnoreAndroid
	private void applyObjectInputFilter (ObjectInputStream objectStream) {
		objectStream.setObjectInputFilter(objectInputFilter);
	}

	/** The object stream of a graph, which writes to the current output: the objects of a graph can be written to different
	 * outputs, eg with {@link DeflateSerializer} or chunked encoding. */
	private static class GraphOutputStream extends OutputStream {
		final ObjectOutputStream objectStream;
		Output output;
		int depth;

		GraphOutputStream (Output output) throws IOException {
			this.output = output;
			objectStream = new ObjectOutputStream(this);
		}

		public void write (int value) {
			output.write(value);
		}

		public void write (byte[] bytes, int offset, int length) {
			output.write(bytes, offset, length);
		}

		public void flush () {
			output.flush();
		}

		public void close () {
			output.close();
		}
	}

	/** The object stream of a graph, which reads from the current input. See {@link GraphOutputStream}. */
	private static class GraphInputStream extends InputStream {
		final ObjectInputStream objectStream;
		Input input;
		int depth;

		GraphInputStream (Input input, Kryo kryo) throws IOException {
			this.input = input;
			objectStream = new ObjectInputStreamWithKryoClassLoader(this, kryo);
		}

		public int read () {
			return input.read();
		}

		public int read (byte[] bytes, int offset, int count) {
			return input.read(bytes, offset, count);
		}

		public int available () throws IOException {
			return input.available();
		}

		public void close () {
			input.close();
		}
	}

	/** {@link ObjectInputStream} uses the last user-defined {@link ClassLoader}, which may not be the correct one. This is a known
	 * Java issue and is often solved by using a specific class loader. See:
	 * https://github.com/apache/spark/blob/v1.6.3/streaming/src/main/scala/org/apache/spark/streaming/Checkpoint.scala#L154
	 * https://issues.apache.org/jira/browse/GROOVY-1627 */
	private static class ObjectInputStreamWithKryoClassLoader extends ObjectInputStream {
		private final Kryo kryo;

		ObjectInputStreamWithKryoClassLoader (InputStream in, Kryo kryo) throws IOException {
			super(in);
			this.kryo = kryo;
		}

		protected Class resolveClass (ObjectStreamClass type) {
			try {
				return Class.forName(type.getName(), false, kryo.getClassLoader());
			} catch (ClassNotFoundException ignored) {
			}
			try {
				return super.resolveClass(type);
			} catch (ClassNotFoundException ex) {
				throw new KryoException("Class not found: " + type.getName(), ex);
			} catch (IOException ex) {
				throw new KryoException("Could not load class: " + type.getName(), ex);
			}
		}
	}
}
