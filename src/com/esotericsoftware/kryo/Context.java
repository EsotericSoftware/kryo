
package com.esotericsoftware.kryo;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import com.esotericsoftware.kryo.compress.DeltaCompressor;

/**
 * Serves as thread local storage for serializers. A serializer instance can be used by multiple threads simultaneously, so should
 * be thread safe. This class provides scratch buffers and object storage that serializers can use to remain thread safe.
 * @see Kryo#getContext()
 * @author Nathan Sweet <misc@n4te.com>
 */
public class Context {
	private HashMap<Object, Object> map;
	private ArrayList<ByteBuffer> buffers = new ArrayList(2);
	private int remoteEntityID;
	private SerializerKey tempKey = new SerializerKey(null, null);
	private char[] charArray = new char[256];

	/**
	 * Returns a non-direct buffer of at least the specified size.
	 */
	public ByteBuffer getBuffer (int minimumSize) {
		ByteBuffer buffer = buffers.isEmpty() ? null : buffers.get(0);
		if (buffer == null || buffer.capacity() < minimumSize) {
			buffer = ByteBuffer.allocate(minimumSize);
		} else
			buffer.clear();
		return buffer;
	}

	/**
	 * Returns a list containing at least the specified number of non-direct buffers, each at least the specified size.
	 */
	public ArrayList<ByteBuffer> getBuffers (int count, int minimumSize) {
		int i = 0;
		for (int n = Math.min(count, buffers.size()); i < n; i++) {
			ByteBuffer buffer = buffers.get(i);
			if (buffer.capacity() < minimumSize) buffers.set(i, ByteBuffer.allocate(minimumSize));
		}
		for (; i < count; i++)
			buffers.add(ByteBuffer.allocate(minimumSize));
		return buffers;
	}

	/**
	 * Returns a char array of at least the specified size.
	 */
	public char[] getCharArray (int minimumSize) {
		if (charArray.length < minimumSize) charArray = new char[minimumSize];
		return charArray;
	}

	/**
	 * Stores an object in thread local storage. This allows serializers to easily make repeated use of objects that are not thread
	 * safe.
	 */
	public void put (String key, Object value) {
		if (map == null) map = new HashMap();
		map.put(key, value);
	}

	/**
	 * Returns an object from thread local storage, or null.
	 * @see #put(Serializer, String, Object)
	 */
	public Object get (String key) {
		if (map == null) map = new HashMap();
		return map.get(key);
	}

	/**
	 * Stores an object for a serializer in thread local storage. This allows serializers to easily make repeated use of objects
	 * that are not thread safe.
	 */
	public void put (Serializer serializer, String key, Object value) {
		if (map == null) map = new HashMap();
		map.put(new SerializerKey(serializer, key), value);
	}

	/**
	 * Returns an object for a serializer from thread local storage, or null.
	 * @see #put(Serializer, String, Object)
	 */
	public Object get (Serializer serializer, String key) {
		if (map == null) map = new HashMap();
		tempKey.serializer = serializer;
		tempKey.key = key;
		return map.get(tempKey);
	}

	/**
	 * Returns an identifier for the entity that either sent the serialized data or will be receiving the serialized data.
	 * Serailizers can use this knowledge during serialization. For example, see {@link DeltaCompressor}.
	 * @see Kryo#addListener(KryoListener)
	 */
	public int getRemoteEntityID () {
		return remoteEntityID;
	}

	/**
	 * Sets the remote entity ID. Should be set before serialization or deserialization if a serializer is used that makes use of
	 * the remote entity ID.
	 * @see #getRemoteEntityID()
	 */
	public void setRemoteEntityID (int remoteEntityID) {
		this.remoteEntityID = remoteEntityID;
	}

	static private class SerializerKey {
		Serializer serializer;
		String key;

		public SerializerKey (Serializer serializer, String key) {
			this.serializer = serializer;
			this.key = key;
		}

		public int hashCode () {
			int result = 31 + key.hashCode();
			result = 31 * result + serializer.hashCode();
			return result;
		}

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			SerializerKey other = (SerializerKey)obj;
			if (!key.equals(other.key)) return false;
			if (!serializer.equals(other.serializer)) return false;
			return true;
		}
	}
}
