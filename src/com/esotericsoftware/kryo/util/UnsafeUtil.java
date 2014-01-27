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
 * A few utility methods for using @link{sun.misc.Unsafe}, mostly for private
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
		Unsafe tmpUnsafe = null;
		long tmpByteArrayBaseOffset = 0;
		long tmpFloatArrayBaseOffset = 0;
		long tmpDoubleArrayBaseOffset = 0;
		long tmpIntArrayBaseOffset = 0;
		long tmpLongArrayBaseOffset = 0;
		long tmpShortArrayBaseOffset = 0;
		long tmpCharArrayBaseOffset = 0;

		try {
			if (!Util.isAndroid) {
				java.lang.reflect.Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
				field.setAccessible(true);
				tmpUnsafe = (sun.misc.Unsafe)field.get(null);
				tmpByteArrayBaseOffset = tmpUnsafe.arrayBaseOffset(byte[].class);
				tmpCharArrayBaseOffset = tmpUnsafe.arrayBaseOffset(char[].class);
				tmpShortArrayBaseOffset = tmpUnsafe.arrayBaseOffset(short[].class);
				tmpIntArrayBaseOffset = tmpUnsafe.arrayBaseOffset(int[].class);
				tmpFloatArrayBaseOffset = tmpUnsafe.arrayBaseOffset(float[].class);
				tmpLongArrayBaseOffset = tmpUnsafe.arrayBaseOffset(long[].class);
				tmpDoubleArrayBaseOffset = tmpUnsafe.arrayBaseOffset(double[].class);
			} else {
				if (TRACE) trace("kryo", "Running on Android platform. Use of sun.misc.Unsafe should be disabled");
			}
		} catch (java.lang.Exception e) {
			if (TRACE)
				trace("kryo", "sun.misc.Unsafe is not accessible or not available. Use of sun.misc.Unsafe should be disabled");
		}

		byteArrayBaseOffset = tmpByteArrayBaseOffset;
		charArrayBaseOffset = tmpCharArrayBaseOffset;
		shortArrayBaseOffset = tmpShortArrayBaseOffset;
		intArrayBaseOffset = tmpIntArrayBaseOffset;
		floatArrayBaseOffset = tmpFloatArrayBaseOffset;
		longArrayBaseOffset = tmpLongArrayBaseOffset;
		doubleArrayBaseOffset = tmpDoubleArrayBaseOffset;
		_unsafe = tmpUnsafe;
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
	 * Return the sun.misc.Unsafe object. If null is returned,
	 * no further Unsafe-related methods are allowed to be invoked from UnsafeUtil.
	 *  
	 * @return instance of sun.misc.Unsafe or null, if this class is not available or not accessible
	 */
	final static public Unsafe unsafe() {
		return _unsafe;
	}
	
	/***
	 * Sort the set of lists by their offsets from the object start address.
	 * 
	 * @param allFields set of fields to be sorted by their offsets
	 */
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
	 * @param size size of the memory region
	 * @return a new ByteBuffer that uses a provided memory region instead of allocating a new one 
	 */
	final static public ByteBuffer getDirectBufferAt(long address, int size) {
		if(directByteBufferConstr == null)
			return null;
		try {
			return directByteBufferConstr.newInstance(address, size, null);
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
