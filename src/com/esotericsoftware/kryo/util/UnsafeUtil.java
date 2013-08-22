package com.esotericsoftware.kryo.util;

import static com.esotericsoftware.kryo.util.UnsafeUtil.unsafe;
import static com.esotericsoftware.minlog.Log.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import sun.misc.Cleaner;
import sun.misc.Unsafe;
import sun.nio.ch.DirectBuffer;

/**
 * A few utility methods for using @link{java.misc.Unsafe}, mostly for private
 * use.
 * 
 * Use of Unsafe on Android is forbidden, as Android provides only a very limited
 * functionality for this class compared to the JDK version.
 * 
 * @author Roman Levenstein <romixlev@gmail.com>
 */

public class UnsafeUtil {
	final static private Unsafe _unsafe;
	final static public long byteArrayBaseOffset;
	final static public long floatArrayBaseOffset;
	final static public long doubleArrayBaseOffset;
	final static public long intArrayBaseOffset;
	final static public long longArrayBaseOffset;
	final static public long shortArrayBaseOffset;
	final static public long charArrayBaseOffset;

	// Constructor to be used for creation of ByteBuffers that use preallocated memory regions 
	static Constructor<? extends ByteBuffer> directByteBufferConstr;
	
	
	static {
		try {
			if (!Util.isAndroid) {
				java.lang.reflect.Field field = sun.misc.Unsafe.class
						.getDeclaredField("theUnsafe");
				field.setAccessible(true);
				_unsafe = (sun.misc.Unsafe) field.get(null);
				byteArrayBaseOffset = _unsafe.arrayBaseOffset(byte[].class);
				charArrayBaseOffset = _unsafe.arrayBaseOffset(char[].class);
				shortArrayBaseOffset = _unsafe.arrayBaseOffset(short[].class);
				intArrayBaseOffset = _unsafe.arrayBaseOffset(int[].class);
				floatArrayBaseOffset = _unsafe.arrayBaseOffset(float[].class);
				longArrayBaseOffset = _unsafe.arrayBaseOffset(long[].class);
				doubleArrayBaseOffset = _unsafe.arrayBaseOffset(double[].class);
			} else {
				byteArrayBaseOffset = 0;
				charArrayBaseOffset = 0;
				shortArrayBaseOffset = 0;
				intArrayBaseOffset = 0;
				floatArrayBaseOffset = 0;
				longArrayBaseOffset = 0;
				doubleArrayBaseOffset = 0;
				_unsafe = null;
			}
		} catch (java.lang.Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	static {
		ByteBuffer buf = ByteBuffer.allocateDirect(1);
		try {
			directByteBufferConstr = buf.getClass().getDeclaredConstructor(long.class, int.class, Object.class);
			directByteBufferConstr.setAccessible(true);
		} catch (Exception e) {
			directByteBufferConstr = null;
		}		
	}
	
	/***
	 * 
	 * @return instance of java.misc.Unsafe
	 */
	final static public Unsafe unsafe() {
		return _unsafe;
	}
	
	public static Field[] sortFieldsByOffset (List<Field> allFields) {
		Field[] allFieldsArray = allFields.toArray(new Field[] {});

		Comparator<Field> fieldOffsetComparator = new Comparator<Field>() {
			@Override
			public int compare (Field f1, Field f2) {
				long offset1 = unsafe().objectFieldOffset(f1);
				long offset2 = unsafe().objectFieldOffset(f2);
				if (offset1 < offset2) return -1;
				if (offset1 == offset2) return 0;
				return 1;
			}
		};

		Arrays.sort(allFieldsArray, fieldOffsetComparator);

		for (Field f : allFields) {
			if (TRACE) trace("kryo", "Field '" + f.getName() + "' at offset " + unsafe().objectFieldOffset(f));
		}
		
		return allFieldsArray;
	}
	
	/***
	 * Create a ByteBuffer that uses a provided (off-heap) memory region instead of allocating a new one.
	 * 
	 * @param address address of the memory region to be used for a ByteBuffer
	 * @param maxBufferSize size of the memory region
	 * @return a new ByteBuffer that uses a provided memory region instead of allocating a new one 
	 */
	final static public ByteBuffer getDirectBufferAt(long address, int maxBufferSize) {
		if(directByteBufferConstr == null)
			return null;
		try {
			return directByteBufferConstr.newInstance(address, maxBufferSize, null);
		} catch (Exception e) {
			throw new RuntimeException("Cannot allocate ByteBuffer at a given address: " + address, e);
		}		
	}

	/***
	 * Release a direct buffer.  
	 * 
	 * NOTE: If Cleaner is not accessible due to SecurityManager restrictions, reflection could be used
	 * to obtain the "clean" method and then invoke it.
	 */
	static public void releaseBuffer(ByteBuffer niobuffer) {
		if(niobuffer != null && niobuffer.isDirect()) {
			Object cleaner = ((DirectBuffer) niobuffer).cleaner();
			if(cleaner != null)
				((Cleaner)cleaner).clean();
			niobuffer = null;
		}		
	}
}
