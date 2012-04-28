
package com.esotericsoftware.kryo.serializers;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/** Serializes arrays.
 * <p>
 * With the default constructor, an array requires a header of 2-4 bytes plus 2 bytes for each dimension beyond the first. If the
 * array type is not final then an extra byte is written for each element.
 * @see Kryo#register(Class, Serializer)
 * @author Nathan Sweet <misc@n4te.com> */
public class ArraySerializer extends Serializer {
	private Integer fixedDimensionCount;
	private boolean elementsAreSameType;
	private boolean elementsCanBeNull = true;
	private int[] dimensions, tempDimensions;

	/** @param dimensions The number of dimensions. Saves 1 byte. Set to null to determine the number of dimensions (default). */
	public void setDimensionCount (Integer dimensions) {
		this.fixedDimensionCount = dimensions;
	}

	/** Identical to calling {@link #setLengths(int[])} with: new int[] {length} */
	public void setLength (int length) {
		dimensions = new int[] {length};
	}

	/** Sets both the number of dimensions and the lengths of each. Saves 2-6 bytes plus 1-5 bytes for each dimension beyond the
	 * first. */
	public void setLengths (int[] lengths) {
		dimensions = lengths;
	}

	/** @param elementsCanBeNull False if all elements are not null. This saves 1 byte per element if the array type is final or
	 *           elementsAreSameClassAsType is true. True if it is not known (default). */
	public void setElementsCanBeNull (boolean elementsCanBeNull) {
		this.elementsCanBeNull = elementsCanBeNull;
	}

	/** @param elementsAreSameType True if all elements are the same type as the array (ie they don't extend the array type). This
	 *           saves 1 byte per element if the array type is not final. Set to false if the array type is final or elements
	 *           extend the array type (default). */
	public void setElementsAreSameType (boolean elementsAreSameType) {
		this.elementsAreSameType = elementsAreSameType;
	}

	public void write (Kryo kryo, Output output, Object array) {
		// Write dimensions.
		int[] dimensions = this.dimensions;
		if (dimensions == null) {
			dimensions = getDimensions(array);
			if (fixedDimensionCount == null) output.writeByte(dimensions.length);
			for (int i = 0, n = dimensions.length; i < n; i++)
				output.writeInt(dimensions[i], true);
		}
		// If element class is final (this includes primitives) then all elements are the same type.
		Serializer elementSerializer = null;
		Class elementClass = getElementClass(array.getClass());
		boolean elementsCanBeNull = this.elementsCanBeNull && !elementClass.isPrimitive();
		if (elementsAreSameType || Modifier.isFinal(elementClass.getModifiers()))
			elementSerializer = kryo.getRegistration(elementClass).getSerializer();
		// Write array data.
		writeArray(kryo, output, array, elementSerializer, 0, dimensions.length, elementsCanBeNull);
	}

	private void writeArray (Kryo kryo, Output output, Object array, Serializer elementSerializer, int dimension,
		int dimensionCount, boolean elementsCanBeNull) {
		int length = Array.getLength(array);
		if (dimension > 0) {
			// Write array length. With Java's "jagged arrays" this could be less than the dimension size.
			output.writeInt(length + 1, true);
		}
		// Write array data.
		boolean elementsAreArrays = dimension < dimensionCount - 1;
		for (int i = 0; i < length; i++) {
			Object element = Array.get(array, i);
			if (elementsAreArrays) {
				// Nested array.
				if (element != null)
					writeArray(kryo, output, element, elementSerializer, dimension + 1, dimensionCount, elementsCanBeNull);
				else
					output.writeInt(0, true);
			} else if (elementSerializer != null) {
				// Use same serializer for all elements.
				if (elementsCanBeNull)
					kryo.writeObjectOrNull(output, element, elementSerializer);
				else
					kryo.writeObject(output, element, elementSerializer);
			} else {
				// Each element could be a different type. Store the class with the object.
				kryo.writeClassAndObject(output, element);
			}
		}
	}

	public Object create (Kryo kryo, Input input, Class type) {
		// Get dimensions.
		tempDimensions = this.dimensions;
		if (tempDimensions == null) {
			int dimensionCount = fixedDimensionCount != null ? fixedDimensionCount : input.readByteUnsigned();
			tempDimensions = new int[dimensionCount];
			for (int i = 0; i < dimensionCount; i++)
				tempDimensions[i] = input.readInt(true);
		}
		return Array.newInstance(getElementClass(type), tempDimensions);
	}

	public void read (Kryo kryo, Input input, Object array) {
		// Get element serializer if all elements are the same type.
		Serializer elementSerializer = null;
		Class elementClass = getElementClass(array.getClass());
		boolean elementsCanBeNull = this.elementsCanBeNull && !elementClass.isPrimitive();
		if (elementsAreSameType || Modifier.isFinal(elementClass.getModifiers()))
			elementSerializer = kryo.getRegistration(elementClass).getSerializer();
		readArray(kryo, input, array, tempDimensions[0], elementSerializer, elementClass, 0, tempDimensions, elementsCanBeNull);
	}

	private void readArray (Kryo kryo, Input input, Object array, int length, Serializer elementSerializer, Class elementClass,
		int dimension, int[] dimensions, boolean elementsCanBeNull) {
		boolean elementsAreArrays = dimension < dimensions.length - 1;
		for (int i = 0; i < length; i++) {
			if (elementsAreArrays) {
				// Nested array.
				int nestedLength = input.readInt(true) - 1;
				if (nestedLength == -1)
					Array.set(array, i, null);
				else {
					Object element = Array.get(array, i);
					int nestedDimension = dimension + 1;
					if (nestedLength != dimensions[nestedDimension]) {
						// Nested array length is different from the dimensions.
						int[] nestedDimensions = new int[dimensions.length - nestedDimension];
						System.arraycopy(dimensions, nestedDimension, nestedDimensions, 0, nestedDimensions.length);
						nestedDimensions[0] = nestedLength;
						element = Array.newInstance(elementClass, nestedDimensions);
						Array.set(array, i, element);
					}
					readArray(kryo, input, element, nestedLength, elementSerializer, elementClass, dimension + 1, dimensions,
						elementsCanBeNull);
				}
			} else if (elementSerializer != null) {
				// Use same serializer (and class) for all elements.
				if (elementsCanBeNull)
					Array.set(array, i, kryo.readObjectOrNull(input, elementClass, elementSerializer));
				else
					Array.set(array, i, kryo.readObject(input, elementClass, elementSerializer));
			} else {
				// Each element could be a different type. Look up the class with the object.
				Array.set(array, i, kryo.readClassAndObject(input));
			}
		}
	}

	public Object createCopy (Kryo kryo, Object original) {
		return Array.newInstance(original.getClass().getComponentType(), Array.getLength(original));
	}

	public void copy (Kryo kryo, Object original, Object copy) {
		int length = Array.getLength(original);
		if (original.getClass().getComponentType().getComponentType() == null) { // single dimension
			for (int i = 0; i < length; i++)
				Array.set(copy, i, kryo.copy(Array.get(original, i)));
		} else {
			for (int i = 0; i < length; i++)
				Array.set(copy, i, kryo.copy(Array.get(original, i), this));
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
