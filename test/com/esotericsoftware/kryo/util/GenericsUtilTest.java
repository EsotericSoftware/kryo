/* Copyright (c) 2008-2025, Nathan Sweet
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

package com.esotericsoftware.kryo.util;

import static com.esotericsoftware.kryo.util.GenericsUtil.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.HashMap;

import org.junit.jupiter.api.Test;

/** @author Nathan Sweet */
class GenericsUtilTest {
	@Test
	void testGenerics () throws Exception {
		String[] names = { //
			"ArrayList<String> fromField1", //
			"ArrayList<FLOAT> fromSubClass", //
			"ArrayList<STRING> fromSubSubClass", //
			"HashMap<FLOAT, STRING> multipleFromSubClasses", //
			"HashMap<FLOAT, NULL> multipleWithUnknown", //
			"FLOAT[] arrayFromSubClass", //
			"STRING[] arrayFromSubSubClass", //

			"STRING known", //
			"STRING[] array1", //
			"STRING[][] array2", //
			"ArrayList<NULL> unknown1", //
			"NULL arrayUnknown1", //
			"ArrayList<String> fromField2", //
			"ArrayList<STRING>[] arrayWithTypeVar", //
			"ArrayList<ArrayList> fromFieldNested", //
			"ARRAYLIST parameterizedTypeFromSubClass", //
			"ArrayList<ARRAYLIST>[] parameterizedArrayFromSubClass", //

			"ArrayList<String> fromField3", //
			"ArrayList raw", //
			"NULL unknown2", //
			"NULL arrayUnknown2", //
			"ArrayList<Object> unboundWildcard", //
			"ArrayList<NUMBER> upperBound", //
			"ArrayList<INTEGER> lowerBound", //

			"ArrayList<DOUBLE> multipleUpperBounds", //
		};
		// names = new String[] {"ArrayList<ArrayList<String>> fromFieldNested"};
		for (String value1 : names) {
			int index = value1.lastIndexOf(' ');
			String name = value1.substring(index + 1);

			Field field = Test4.class.getField(name);
			Class declaringClass = field.getDeclaringClass();
			Class serializingClass = Test4.class;
			Type genericType = field.getGenericType();

			Type fieldType = resolveType(declaringClass, serializingClass, genericType);
			String fieldClassName;
			if (fieldType instanceof Class)
				fieldClassName = ((Class)fieldType).getSimpleName();
			else //
				fieldClassName = ((TypeVariable)fieldType).getName();

			Type[] generics = resolveTypeParameters(declaringClass, serializingClass, genericType);

			String value2 = fieldClassName.replaceAll("[\\[\\]]", "");
			if (generics != null) {
				value2 += "<";
				for (int i = 0, n = generics.length; i < n; i++) {
					if (i > 0) value2 += ", ";
					Type arg = generics[i];
					if (arg == null)
						value2 += "null";
					else if (arg instanceof Class)
						value2 += ((Class)arg).getSimpleName();
					else //
						value2 += ((TypeVariable)arg).getName();
				}
				value2 += ">";
			}
			value2 += fieldClassName.replaceAll("[^\\[\\]]", "");
			value2 += " " + name;

			System.out.println(value1);
			System.out.println(value2);
			System.out.println();
			assertTrue(value1.equalsIgnoreCase(value2), value1 + " != " + value2);
		}
	}

	public static class Test1<FLOAT, STRING, NULL> {
		public ArrayList<String> fromField1;
		public ArrayList<FLOAT> fromSubClass;
		public ArrayList<STRING> fromSubSubClass;
		public HashMap<FLOAT, STRING> multipleFromSubClasses;
		public HashMap<FLOAT, NULL> multipleWithUnknown;
		public FLOAT[] arrayFromSubClass;
		public STRING[] arrayFromSubSubClass;
	}

	public static class Test2<STRING, NULL, ARRAYLIST> extends Test1<Float, STRING, NULL> {
		public STRING known;
		public STRING[] array1;
		public STRING[][] array2;
		public ArrayList<NULL> unknown1;
		public NULL[] arrayUnknown1;
		public ArrayList<String> fromField2;
		public ArrayList<STRING>[] arrayWithTypeVar;
		public ArrayList<ArrayList<String>> fromFieldNested;
		public ARRAYLIST parameterizedTypeFromSubClass;
		public ArrayList<ARRAYLIST>[] parameterizedArrayFromSubClass;
	}

	public static class Test3<DOUBLE extends Number & Comparable, NULL, LONG> extends Test2<String, NULL, ArrayList<LONG>> {
		public ArrayList<String> fromField3;
		public ArrayList raw;
		public NULL unknown2;
		public NULL[] arrayUnknown2;
		public ArrayList<?> unboundWildcard;
		public ArrayList<? extends Number> upperBound;
		public ArrayList<? super Integer> lowerBound;
		public ArrayList<DOUBLE> multipleUpperBounds;
	}

	public static class Test4<NULL> extends Test3<Double, NULL, Long> {
	}

	public static void main (String[] args) throws Exception {
		new GenericsUtilTest().testGenerics();
	}
}
