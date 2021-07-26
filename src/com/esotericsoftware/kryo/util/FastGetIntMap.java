package com.esotericsoftware.kryo.util;

public class FastGetIntMap<V> extends IntMap<V> {
    /** Creates a new map with an initial capacity of 51 and a load factor of 0.8. */
    public FastGetIntMap () {
        this(51, 0.8f);
    }
    
    /** Creates a new map with a load factor of 0.8.
     * @param initialCapacity If not a power of two, it is increased to the next nearest power of two. */
    public FastGetIntMap (int initialCapacity) {
        this(initialCapacity, 0.8f);
    }
    
    /** Creates a new map with a initial load factor of 0.8(which may be overloaded in resize funtion).
     * @param initialCapacity If not a power of two, it is increased to the next nearest power of two. */
    public FastGetIntMap (int initialCapacity, float initialLoadFactor) {
        super(initialCapacity, initialLoadFactor);
    }
    
    /** Returns the value for the specified key, or null if the key is not in the map. unroll because of better performance
     * (benchmark shows about 2% higher performance) */
    @Override
    @Null
    public V get (int key) {
        if (key == 0) return hasZeroValue ? zeroValue : null;
        int[] keyTable = this.keyTable;
        for (int i = place(key);; i = i + 1 & mask) {
            int other = keyTable[i];
            if (other == key) return valueTable[i]; // Same key was found.
            if (other == 0) return null; // Empty space is available.
        }
    }
    
    @Override
    /** Returns the value for the specified key, or the default value if the key is not in the map. unroll because of better
     * performance */
    public V get (int key, @Null V defaultValue) {
        if (key == 0) return hasZeroValue ? zeroValue : null;
        int[] keyTable = this.keyTable;
        for (int i = place(key);; i = i + 1 & mask) {
            int other = keyTable[i];
            if (other == key) return valueTable[i]; // Same key was found.
            if (other == 0) return defaultValue; // Empty space is available.
        }
    }
    
    /** 1. remove magic number so that minimize the computation cost 2. with low loadFactor, we don't need to consider the hash
     * collision **/
    @Override
    protected int place (int item) {
        return item & mask;
    }
}
