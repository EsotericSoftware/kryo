
package com.esotericsoftware.kryo.compress;

import java.nio.ByteBuffer;

import com.esotericsoftware.kryo.Compressor;
import com.esotericsoftware.kryo.Context;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoListener;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.util.IntHashMap;

/**
 * Caches bytes for the last object serialized (per {@link Context#getRemoteEntityID() remote entitiy}) and only emits deltas on
 * subsequent serializations. Also caches bytes for the last object received (per {@link Context#getRemoteEntityID() remote
 * entitiy}), in order to apply deltas received.
 * @author Nathan Sweet <misc@n4te.com>
 */
public class DeltaCompressor extends Compressor {
	private final Delta delta;
	private final IntHashMap<ByteBuffer> contextToRemoteData = new IntHashMap();
	private final IntHashMap<ByteBuffer> contextToLocalData = new IntHashMap();

	private KryoListener removeBuffersListener = new KryoListener() {
		public void remoteEntityRemoved (int id) {
			contextToRemoteData.remove(id);
			contextToLocalData.remove(id);
		}
	};

	/**
	 * Creates a DeltaCompressor with a buffer size of 2048 abd a chunk size of 8.
	 */
	public DeltaCompressor (Serializer serializer) {
		this(serializer, 2048, 8);
	}

	/**
	 * @see Delta#Delta(int, int)
	 */
	public DeltaCompressor (Serializer serializer, int bufferSize, int chunkSize) {
		super(serializer, bufferSize);
		delta = new Delta(bufferSize, chunkSize);
	}

	public void compress (ByteBuffer newData, Object object, ByteBuffer outputBuffer) {
		int start = newData.position();

		int remoteID = getContext().getRemoteEntityID();
		ByteBuffer remoteData = contextToRemoteData.get(remoteID);

		delta.compress(remoteData, newData, outputBuffer);

		if (remoteData == null) {
			remoteData = ByteBuffer.allocateDirect(bufferSize);
			contextToRemoteData.put(remoteID, remoteData);
			Kryo kryo = getKryo();
			if (kryo != null) kryo.addListener(removeBuffersListener);
		}
		remoteData.clear();
		newData.position(start);
		remoteData.put(newData);
		remoteData.flip();
	}

	public void decompress (ByteBuffer deltaData, Class type, ByteBuffer outputBuffer) {
		int remoteID = getContext().getRemoteEntityID();
		ByteBuffer localData = contextToLocalData.get(remoteID);

		delta.decompress(localData, deltaData, outputBuffer);

		if (localData == null) {
			localData = ByteBuffer.allocateDirect(bufferSize);
			contextToLocalData.put(remoteID, localData);
			Kryo kryo = getKryo();
			if (kryo != null) kryo.addListener(removeBuffersListener);
		}
		localData.clear();
		outputBuffer.flip();
		localData.put(outputBuffer);
		localData.flip();
	}
}
