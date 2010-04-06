
package com.esotericsoftware.kryo.compress;

import java.nio.ByteBuffer;

import com.esotericsoftware.kryo.Compressor;
import com.esotericsoftware.kryo.Context;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.util.IntHashMap;

/**
 * Caches bytes for the last object serialized (per {@link Context#getRemoteEntityID() remote entitiy}) and only emits deltas on
 * subsequent serializations. Also caches bytes for the last object received (per {@link Context#getRemoteEntityID() remote
 * entitiy}), in order to apply deltas received.
 * @author Nathan Sweet <misc@n4te.com>
 */
public class DeltaCompressor extends Compressor {
	private final Kryo kryo;
	private final int chunkSize;
	final IntHashMap<ByteBuffer> contextToRemoteData = new IntHashMap();
	final IntHashMap<ByteBuffer> contextToLocalData = new IntHashMap();

	private Kryo.Listener removeBuffersListener = new Kryo.Listener() {
		public void remoteEntityRemoved (int id) {
			contextToRemoteData.remove(id);
			contextToLocalData.remove(id);
		}
	};

	/**
	 * Creates a DeltaCompressor with a buffer size of 2048 abd a chunk size of 8.
	 */
	public DeltaCompressor (Kryo kryo, Serializer serializer) {
		this(kryo, serializer, 2048, 8);
	}

	/**
	 * @see Delta#Delta(int, int)
	 */
	public DeltaCompressor (Kryo kryo, Serializer serializer, int bufferSize, int chunkSize) {
		super(serializer, bufferSize);
		this.kryo = kryo;
		this.chunkSize = chunkSize;
	}

	public void compress (ByteBuffer newData, Object object, ByteBuffer outputBuffer) {
		int start = newData.position();

		Context context = Kryo.getContext();
		int remoteID = context.getRemoteEntityID();
		ByteBuffer remoteData = contextToRemoteData.get(remoteID);

		Delta delta = (Delta)context.get(this, "delta");
		if (delta == null) {
			delta = new Delta(bufferSize, chunkSize);
			context.put(this, "delta", delta);
		}
		delta.compress(remoteData, newData, outputBuffer);

		if (remoteData == null) {
			remoteData = ByteBuffer.allocate(bufferSize);
			contextToRemoteData.put(remoteID, remoteData);
			kryo.addListener(removeBuffersListener);
		}
		remoteData.clear();
		newData.position(start);
		remoteData.put(newData);
		remoteData.flip();
	}

	public void decompress (ByteBuffer deltaData, Class type, ByteBuffer outputBuffer) {
		Context context = Kryo.getContext();
		int remoteID = context.getRemoteEntityID();
		ByteBuffer localData = contextToLocalData.get(remoteID);

		Delta delta = (Delta)context.get(this, "delta");
		if (delta == null) {
			delta = new Delta(bufferSize, chunkSize);
			context.put(this, "delta", delta);
		}
		delta.decompress(localData, deltaData, outputBuffer);

		if (localData == null) {
			localData = ByteBuffer.allocate(bufferSize);
			contextToLocalData.put(remoteID, localData);
			kryo.addListener(removeBuffersListener);
		}
		localData.clear();
		outputBuffer.flip();
		localData.put(outputBuffer);
		localData.flip();
	}
}
