
package com.esotericsoftware.kryo.serialize;

import static com.esotericsoftware.log.Log.*;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;

import com.esotericsoftware.kryo.Context;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;

// BOZO - Change *AreNoteNull to canBeNull.

/**
 * Serializes arrays.
 * <p>
 * With the default constructor, an array requires a header of 2-4 bytes plus 2 bytes for each dimension beyond the first. If the
 * array type is not final then an extra byte is written for each element.
 * @see Kryo#register(Class, Serializer)
 * @author Nathan Sweet <misc@n4te.com>
 */
public class ArraySerializer extends Serializer {
	private final Kryo kryo;
	private Integer fixedDimensionCount;
	private boolean elementsAreSameType;
	private boolean elementsAreNotNull;

	public ArraySerializer (Kryo kryo) {
		this.kryo = kryo;
	}

	/**
	 * @see ArraySerializer#ArraySerializer(Kryo, Integer, boolean, boolean)
	 */
	public ArraySerializer (Kryo kryo, Integer dimensions) {
		this.kryo = kryo;
		fixedDimensionCount = dimensions;
	}

	/**
	 * @param dimensions Sets the number of dimensions. Saves 1 byte. Set to null to determine the number of dimensions (default).
	 * @param elementsAreNotNull True if elements are not null. This saves 1 byte per element if the array type is final or
	 *           elementsAreSameClassAsType is true. False if it is not known (default).
	 * @param elementsAreSameType True if all elements are the same type as the array (ie they don't extend the array type). This
	 *           saves 1 byte per element if the array type is not final. Set to false if the array type is final or elements
	 *           extend the array type (default).
	 */
	public ArraySerializer (Kryo kryo, Integer dimensions, boolean elementsAreNotNull, boolean elementsAreSameType) {
		this.kryo = kryo;
		this.fixedDimensionCount = dimensions;
		this.elementsAreNotNull = elementsAreNotNull;
		this.elementsAreSameType = elementsAreSameType;
	}

	public void writeObjectData (ByteBuffer buffer, Object array) {
		// Write dimensions.
		int[] dimensions = getDimensions(array);
		if (fixedDimensionCount == null) ByteSerializer.putUnsigned(buffer, dimensions.length);
		for (int i = 0, n = dimensions.length; i < n; i++)
			IntSerializer.put(buffer, dimensions[i], true);
		// If element class is final (this includes primitives) then all elements are the same type.
		Serializer elementSerializer = null;
		Class elementClass = getElementClass(array.getClass());
		if (elementsAreSameType || Modifier.isFinal(elementClass.getModifiers()))
			elementSerializer = kryo.getRegisteredClass(elementClass).serializer;
		// Write array data.
		writeArray(buffer, array, elementSerializer, 0, dimensions.length);
		if (level <= TRACE) {
			StringBuilder stringBuffer = new StringBuilder(16);
			for (int i = 0, n = dimensions.length; i < n; i++) {
				stringBuffer.append('[');
				stringBuffer.append(dimensions[i]);
				stringBuffer.append(']');
			}
			trace("kryo", "Wrote array: " + elementClass.getName() + stringBuffer);
		}
	}

	private void writeArray (ByteBuffer buffer, Object array, Serializer elementSerializer, int dimension, int dimensionCount) {
		int length = Array.getLength(array);
		if (dimension > 0) {
			// Write array length. With Java's "jagged arrays" this could be less than the dimension size.
			IntSerializer.put(buffer, length, true);
		}
		// Write array data.
		boolean elementsAreArrays = dimension < dimensionCount - 1;
		for (int i = 0; i < length; i++) {
			Object element = Array.get(array, i);
			if (elementsAreArrays) {
				// Nested array.
				if (element != null) writeArray(buffer, element, elementSerializer, dimension + 1, dimensionCount);
			} else if (elementSerializer != null) {
				// Use same serializer for all elements.
				if (elementsAreNotNull)
					elementSerializer.writeObjectData(buffer, element);
				else
					elementSerializer.writeObject(buffer, element);
			} else {
				// Each element could be a different type. Store the class with the object.
				kryo.writeClassAndObject(buffer, element);
			}
		}
	}

	public <T> T readObjectData (ByteBuffer buffer, Class<T> type) {
		// Get dimensions.
		int dimensionCount = fixedDimensionCount != null ? fixedDimensionCount : ByteSerializer.getUnsigned(buffer);
		int[] dimensions = new int[dimensionCount];
		for (int i = 0; i < dimensionCount; i++)
			dimensions[i] = IntSerializer.get(buffer, true);
		// Get element serializer if all elements are the same type.
		Serializer elementSerializer = null;
		Class elementClass = getElementClass(type);
		if (elementsAreSameType || Modifier.isFinal(elementClass.getModifiers()))
			elementSerializer = kryo.getRegisteredClass(elementClass).serializer;
		// Create array and read in the data.
		T array = (T)Array.newInstance(elementClass, dimensions);
		readArray(buffer, array, elementSerializer, elementClass, 0, dimensions);
		if (level <= TRACE) {
			StringBuilder stringBuffer = new StringBuilder(16);
			for (int i = 0; i < dimensionCount; i++) {
				stringBuffer.append('[');
				stringBuffer.append(dimensions[i]);
				stringBuffer.append(']');
			}
			trace("kryo", "Read array: " + elementClass.getName() + stringBuffer);
		}
		return array;
	}

	private void readArray (ByteBuffer buffer, Object array, Serializer elementSerializer, Class elementClass, int dimension,
		int[] dimensions) {
		boolean elementsAreArrays = dimension < dimensions.length - 1;
		int length;
		if (dimension == 0)
			length = dimensions[0];
		else
			length = IntSerializer.get(buffer, true);
		for (int i = 0; i < length; i++) {
			if (elementsAreArrays) {
				// Nested array.
				Object element = Array.get(array, i);
				if (element != null) readArray(buffer, element, elementSerializer, elementClass, dimension + 1, dimensions);
			} else if (elementSerializer != null) {
				// Use same serializer (and class) for all elements.
				if (elementsAreNotNull)
					Array.set(array, i, elementSerializer.readObjectData(buffer, elementClass));
				else
					Array.set(array, i, elementSerializer.readObject(buffer, elementClass));
			} else {
				// Each element could be a different type. Look up the class with the object.
				Array.set(array, i, kryo.readClassAndObject(buffer));
			}
		}
	}

	static public int getDimensionCount (Class arrayClass) {
		int depth = 0;
		Class nextClass = arrayClass.getComponentType();
		while (nextClass != null) {
			depth++;
			nextClass = nextClass.getComponentType();
		}
		return depth;
	}

	static public int[] getDimensions (Object array) {
		int depth = 0;
		Class nextClass = array.getClass().getComponentType();
		while (nextClass != null) {
			depth++;
			nextClass = nextClass.getComponentType();
		}
		int[] dimensions = new int[depth];
		dimensions[0] = Array.getLength(array);
		if (depth > 1) collectDimensions(array, 1, dimensions);
		return dimensions;
	}

	static private void collectDimensions (Object array, int dimension, int[] dimensions) {
		boolean elementsAreArrays = dimension < dimensions.length - 1;
		for (int i = 0, s = Array.getLength(array); i < s; i++) {
			Object element = Array.get(array, i);
			if (element == null) continue;
			dimensions[dimension] = Math.max(dimensions[dimension], Array.getLength(element));
			if (elementsAreArrays) collectDimensions(element, dimension + 1, dimensions);
		}
	}

	static public Class getElementClass (Class arrayClass) {
		Class elementClass = arrayClass;
		while (elementClass.getComponentType() != null)
			elementClass = elementClass.getComponentType();
		return elementClass;
	}
}
