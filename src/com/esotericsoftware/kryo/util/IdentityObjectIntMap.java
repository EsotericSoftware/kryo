
package com.esotericsoftware.kryo.util;

/** An unordered map where identity comparison is used for keys and the values are ints. This implementation is a cuckoo hash map
 * using 3 hashes, random walking, and a small stash for problematic keys. Null keys are not allowed. No allocation is done except
 * when growing the table size. <br>
 * <br>
 * This map performs very fast get, containsKey, and remove (typically O(1), worst case O(log(n))). Put may be a bit slower,
 * depending on hash collisions. Load factors greater than 0.91 greatly increase the chances the map will have to rehash to the
 * next higher POT size.
 * @author Nathan Sweet */
public class IdentityObjectIntMap<K> {
	private static final int PRIME1 = 0xbe1f14b1;
	private static final int PRIME2 = 0xb4b82e39;
	private static final int PRIME3 = 0xced1c241;

	public int size;

	K[] keyTable;
	int[] valueTable;
	int capacity, stashSize;

	private float loadFactor;
	private int hashShift, mask, threshold;
	private int stashCapacity;
	private int pushIterations;

	/** Creates a new map with an initial capacity of 32 and a load factor of 0.8. This map will hold 25 items before growing the
	 * backing table. */
	public IdentityObjectIntMap () {
		this(32, 0.8f);
	}

	/** Creates a new map with a load factor of 0.8. This map will hold initialCapacity * 0.8 items before growing the backing
	 * table. */
	public IdentityObjectIntMap (int initialCapacity) {
		this(initialCapacity, 0.8f);
	}

	/** Creates a new map with the specified initial capacity and load factor. This map will hold initialCapacity * loadFactor items
	 * before growing the backing table. */
	public IdentityObjectIntMap (int initialCapacity, float loadFactor) {
		if (initialCapacity < 0) throw new IllegalArgumentException("initialCapacity must be >= 0: " + initialCapacity);
		if (capacity > 1 << 30) throw new IllegalArgumentException("initialCapacity is too large: " + initialCapacity);
		capacity = ObjectMap.nextPowerOfTwo(initialCapacity);

		if (loadFactor <= 0) throw new IllegalArgumentException("loadFactor must be > 0: " + loadFactor);
		this.loadFactor = loadFactor;

		threshold = (int)(capacity * loadFactor);
		mask = capacity - 1;
		hashShift = 31 - Integer.numberOfTrailingZeros(capacity);
		stashCapacity = Math.max(3, (int)Math.ceil(Math.log(capacity)) * 2);
		pushIterations = Math.max(Math.min(capacity, 8), (int)Math.sqrt(capacity) / 8);

		keyTable = (K[])new Object[capacity + stashCapacity];
		valueTable = new int[keyTable.length];
	}

	public void put (K key, int value) {
		if (key == null) throw new IllegalArgumentException("key cannot be null.");
		K[] keyTable = this.keyTable;

		// Check for existing keys.
		int hashCode = System.identityHashCode(key);
		int index1 = hashCode & mask;
		K key1 = keyTable[index1];
		if (key == key1) {
			valueTable[index1] = value;
			return;
		}

		int index2 = hash2(hashCode);
		K key2 = keyTable[index2];
		if (key == key2) {
			valueTable[index2] = value;
			return;
		}

		int index3 = hash3(hashCode);
		K key3 = keyTable[index3];
		if (key == key3) {
			valueTable[index3] = value;
			return;
		}

		// Update key in the stash.
		for (int i = capacity, n = i + stashSize; i < n; i++) {
			if (keyTable[i] == key) {
				valueTable[i] = value;
				return;
			}
		}

		// Check for empty buckets.
		if (key1 == null) {
			keyTable[index1] = key;
			valueTable[index1] = value;
			if (size++ >= threshold) resize(capacity << 1);
			return;
		}

		if (key2 == null) {
			keyTable[index2] = key;
			valueTable[index2] = value;
			if (size++ >= threshold) resize(capacity << 1);
			return;
		}

		if (key3 == null) {
			keyTable[index3] = key;
			valueTable[index3] = value;
			if (size++ >= threshold) resize(capacity << 1);
			return;
		}

		push(key, value, index1, key1, index2, key2, index3, key3);
	}

	/** Skips checks for existing keys. */
	private void putResize (K key, int value) {
		// Check for empty buckets.
		int hashCode = System.identityHashCode(key);
		int index1 = hashCode & mask;
		K key1 = keyTable[index1];
		if (key1 == null) {
			keyTable[index1] = key;
			valueTable[index1] = value;
			if (size++ >= threshold) resize(capacity << 1);
			return;
		}

		int index2 = hash2(hashCode);
		K key2 = keyTable[index2];
		if (key2 == null) {
			keyTable[index2] = key;
			valueTable[index2] = value;
			if (size++ >= threshold) resize(capacity << 1);
			return;
		}

		int index3 = hash3(hashCode);
		K key3 = keyTable[index3];
		if (key3 == null) {
			keyTable[index3] = key;
			valueTable[index3] = value;
			if (size++ >= threshold) resize(capacity << 1);
			return;
		}

		push(key, value, index1, key1, index2, key2, index3, key3);
	}

	private void push (K insertKey, int insertValue, int index1, K key1, int index2, K key2, int index3, K key3) {
		K[] keyTable = this.keyTable;
		int[] valueTable = this.valueTable;
		int mask = this.mask;

		// Push keys until an empty bucket is found.
		K evictedKey;
		int evictedValue;
		int i = 0, pushIterations = this.pushIterations;
		do {
			// Replace the key and value for one of the hashes.
			switch (ObjectMap.random.nextInt(3)) {
			case 0:
				evictedKey = key1;
				evictedValue = valueTable[index1];
				keyTable[index1] = insertKey;
				valueTable[index1] = insertValue;
				break;
			case 1:
				evictedKey = key2;
				evictedValue = valueTable[index2];
				keyTable[index2] = insertKey;
				valueTable[index2] = insertValue;
				break;
			default:
				evictedKey = key3;
				evictedValue = valueTable[index3];
				keyTable[index3] = insertKey;
				valueTable[index3] = insertValue;
				break;
			}

			// If the evicted key hashes to an empty bucket, put it there and stop.
			int hashCode = System.identityHashCode(evictedKey);
			index1 = hashCode & mask;
			key1 = keyTable[index1];
			if (key1 == null) {
				keyTable[index1] = evictedKey;
				valueTable[index1] = evictedValue;
				if (size++ >= threshold) resize(capacity << 1);
				return;
			}

			index2 = hash2(hashCode);
			key2 = keyTable[index2];
			if (key2 == null) {
				keyTable[index2] = evictedKey;
				valueTable[index2] = evictedValue;
				if (size++ >= threshold) resize(capacity << 1);
				return;
			}

			index3 = hash3(hashCode);
			key3 = keyTable[index3];
			if (key3 == null) {
				keyTable[index3] = evictedKey;
				valueTable[index3] = evictedValue;
				if (size++ >= threshold) resize(capacity << 1);
				return;
			}

			if (++i == pushIterations) break;

			insertKey = evictedKey;
			insertValue = evictedValue;
		} while (true);

		putStash(evictedKey, evictedValue);
	}

	private void putStash (K key, int value) {
		if (stashSize == stashCapacity) {
			// Too many pushes occurred and the stash is full, increase the table size.
			resize(capacity << 1);
			put(key, value);
			return;
		}
		// Store key in the stash.
		int index = capacity + stashSize;
		keyTable[index] = key;
		valueTable[index] = value;
		stashSize++;
		size++;
	}

	/** @param defaultValue Returned if the key was not associated with a value. */
	public int get (K key, int defaultValue) {
		int hashCode = System.identityHashCode(key);
		int index = hashCode & mask;
		if (key != keyTable[index]) {
			index = hash2(hashCode);
			if (key != keyTable[index]) {
				index = hash3(hashCode);
				if (key != keyTable[index]) return getStash(key, defaultValue);
			}
		}
		return valueTable[index];
	}

	private int getStash (K key, int defaultValue) {
		K[] keyTable = this.keyTable;
		for (int i = capacity, n = i + stashSize; i < n; i++)
			if (key == keyTable[i]) return valueTable[i];
		return defaultValue;
	}

	/** Returns the key's current value and increments the stored value. If the key is not in the map, defaultValue + increment is
	 * put into the map. */
	public int getAndIncrement (K key, int defaultValue, int increment) {
		int hashCode = System.identityHashCode(key);
		int index = hashCode & mask;
		if (key != keyTable[index]) {
			index = hash2(hashCode);
			if (key != keyTable[index]) {
				index = hash3(hashCode);
				if (key != keyTable[index]) return getAndIncrementStash(key, defaultValue, increment);
			}
		}
		int value = valueTable[index];
		valueTable[index] = value + increment;
		return value;
	}

	private int getAndIncrementStash (K key, int defaultValue, int increment) {
		K[] keyTable = this.keyTable;
		for (int i = capacity, n = i + stashSize; i < n; i++)
			if (key == keyTable[i]) {
				int value = valueTable[i];
				valueTable[i] = value + increment;
				return value;
			}
		put(key, defaultValue + increment);
		return defaultValue;
	}

	public int remove (K key, int defaultValue) {
		int hashCode = System.identityHashCode(key);
		int index = hashCode & mask;
		if (key == keyTable[index]) {
			keyTable[index] = null;
			int oldValue = valueTable[index];
			size--;
			return oldValue;
		}

		index = hash2(hashCode);
		if (key == keyTable[index]) {
			keyTable[index] = null;
			int oldValue = valueTable[index];
			size--;
			return oldValue;
		}

		index = hash3(hashCode);
		if (key == keyTable[index]) {
			keyTable[index] = null;
			int oldValue = valueTable[index];
			size--;
			return oldValue;
		}

		return removeStash(key, defaultValue);
	}

	int removeStash (K key, int defaultValue) {
		K[] keyTable = this.keyTable;
		for (int i = capacity, n = i + stashSize; i < n; i++) {
			if (key == keyTable[i]) {
				int oldValue = valueTable[i];
				removeStashIndex(i);
				size--;
				return oldValue;
			}
		}
		return defaultValue;
	}

	void removeStashIndex (int index) {
		// If the removed location was not last, move the last tuple to the removed location.
		stashSize--;
		int lastIndex = capacity + stashSize;
		if (index < lastIndex) {
			keyTable[index] = keyTable[lastIndex];
			valueTable[index] = valueTable[lastIndex];
		}
	}

	/** Reduces the size of the backing arrays to be the specified capacity or less. If the capacity is already less, nothing is
	 * done. If the map contains more items than the specified capacity, nothing is done. */
	public void shrink (int maximumCapacity) {
		if (maximumCapacity < 0) throw new IllegalArgumentException("maximumCapacity must be >= 0: " + maximumCapacity);
		if (size > maximumCapacity) maximumCapacity = size;
		if (capacity <= maximumCapacity) return;
		maximumCapacity = ObjectMap.nextPowerOfTwo(maximumCapacity);
		resize(maximumCapacity);
	}

	/** Clears the map and reduces the size of the backing arrays to be the specified capacity if they are larger. */
	public void clear (int maximumCapacity) {
		if (capacity <= maximumCapacity) {
			clear();
			return;
		}
		size = 0;
		resize(maximumCapacity);
	}

	public void clear () {
		K[] keyTable = this.keyTable;
		for (int i = capacity + stashSize; i-- > 0;)
			keyTable[i] = null;
		size = 0;
		stashSize = 0;
	}

	/** Returns true if the specified value is in the map. Note this traverses the entire map and compares every value, which may be
	 * an expensive operation. */
	public boolean containsValue (int value) {
		int[] valueTable = this.valueTable;
		for (int i = capacity + stashSize; i-- > 0;)
			if (valueTable[i] == value) return true;
		return false;
	}

	public boolean containsKey (K key) {
		int hashCode = System.identityHashCode(key);
		int index = hashCode & mask;
		if (key != keyTable[index]) {
			index = hash2(hashCode);
			if (key != keyTable[index]) {
				index = hash3(hashCode);
				if (key != keyTable[index]) return containsKeyStash(key);
			}
		}
		return true;
	}

	private boolean containsKeyStash (K key) {
		K[] keyTable = this.keyTable;
		for (int i = capacity, n = i + stashSize; i < n; i++)
			if (key == keyTable[i]) return true;
		return false;
	}

	/** Returns the key for the specified value, or null if it is not in the map. Note this traverses the entire map and compares
	 * every value, which may be an expensive operation. */
	public K findKey (int value) {
		int[] valueTable = this.valueTable;
		for (int i = capacity + stashSize; i-- > 0;)
			if (valueTable[i] == value) return keyTable[i];
		return null;
	}

	/** Increases the size of the backing array to acommodate the specified number of additional items. Useful before adding many
	 * items to avoid multiple backing array resizes. */
	public void ensureCapacity (int additionalCapacity) {
		int sizeNeeded = size + additionalCapacity;
		if (sizeNeeded >= threshold) resize(ObjectMap.nextPowerOfTwo((int)(sizeNeeded / loadFactor)));
	}

	private void resize (int newSize) {
		int oldEndIndex = capacity + stashSize;

		capacity = newSize;
		threshold = (int)(newSize * loadFactor);
		mask = newSize - 1;
		hashShift = 31 - Integer.numberOfTrailingZeros(newSize);
		stashCapacity = Math.max(3, (int)Math.ceil(Math.log(newSize)) * 2);
		pushIterations = Math.max(Math.min(newSize, 8), (int)Math.sqrt(newSize) / 8);

		K[] oldKeyTable = keyTable;
		int[] oldValueTable = valueTable;

		keyTable = (K[])new Object[newSize + stashCapacity];
		valueTable = new int[newSize + stashCapacity];

		int oldSize = size;
		size = 0;
		stashSize = 0;
		if (oldSize > 0) {
			for (int i = 0; i < oldEndIndex; i++) {
				K key = oldKeyTable[i];
				if (key != null) putResize(key, oldValueTable[i]);
			}
		}
	}

	private int hash2 (int h) {
		h *= PRIME2;
		return (h ^ h >>> hashShift) & mask;
	}

	private int hash3 (int h) {
		h *= PRIME3;
		return (h ^ h >>> hashShift) & mask;
	}

	public String toString () {
		if (size == 0) return "{}";
		StringBuilder buffer = new StringBuilder(32);
		buffer.append('{');
		K[] keyTable = this.keyTable;
		int[] valueTable = this.valueTable;
		int i = keyTable.length;
		while (i-- > 0) {
			K key = keyTable[i];
			if (key == null) continue;
			buffer.append(key);
			buffer.append('=');
			buffer.append(valueTable[i]);
			break;
		}
		while (i-- > 0) {
			K key = keyTable[i];
			if (key == null) continue;
			buffer.append(", ");
			buffer.append(key);
			buffer.append('=');
			buffer.append(valueTable[i]);
		}
		buffer.append('}');
		return buffer.toString();
	}
}
