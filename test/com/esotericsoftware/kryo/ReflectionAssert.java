/* Copyright (c) 2016, Martin Grotzke
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

package com.esotericsoftware.kryo;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Calendar;
import java.util.Collection;
import java.util.Currency;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;

/** An assertion utility that provides reflection based assertion, with improvements compared to commons-lang's
 * EqualsBuilder#reflectionEquals and unitils' ReflectionAssert.assertReflectionEquals. */
class ReflectionAssert {

	/** Checks both objects for equality using reflection. There are special equal checks implemented for some types like
	 * {@link Set} and {@link Map} that are not available in
	 * {@link org.apache.commons.lang.builder.EqualsBuilder#reflectionEquals(Object, Object)} . Collections must be of the same
	 * implementation, basically {@link #assertReflectionEquals(Object, Object, boolean)} is invoked with <code>true</code> for
	 * param <code>requireMatchingCollectionClasses</code>.
	 *
	 * Also unitils' ReflectionAssert.assertReflectionEquals seems to not properly support StringBuffer/StringBuilder.
	 *
	 * @param one one of both objects to compare.
	 * @param another one of both objects to compare. */
	static void assertReflectionEquals (final Object one, final Object another) {
		assertReflectionEquals(one, another, true);
	}

	/** Checks both objects for equality using reflection. There are special equal checks implemented for some types like
	 * {@link Set} and {@link Map} that are not available in
	 * {@link org.apache.commons.lang.builder.EqualsBuilder#reflectionEquals(Object, Object)} .
	 *
	 * @param one one of both objects to compare.
	 * @param another one of both objects to compare.
	 * @param requireMatchingCollectionClasses if <code>true</code>, collections like set, map, list must be of the same
	 *           implementing class. If <code>false</code>, it's only checked if both objects are a {@link List}, {@link Set} or
	 *           {@link Map}. */
	static void assertReflectionEquals (final Object one, final Object another, final boolean requireMatchingCollectionClasses) {
		assertReflectionEquals(one, another, requireMatchingCollectionClasses, new IdentityHashMap<Object, Object>(), "");
	}

	// CHECKSTYLE:OFF
	private static void assertReflectionEquals (final Object one, final Object another,
		final boolean requireMatchingCollectionClasses, final Map<Object, Object> alreadyChecked, final String path) {
		if (one == another) {
			return;
		}
		// If one == null we know another != null (because of previous 'one == another')...
		if (one == null || another == null) {
			Assert.fail("One of both is null on path: '" + (StringUtils.isEmpty(path) ? "." : path) + "': " + one + ", " + another);
		}
		if (alreadyChecked.containsKey(one)) {
			return;
		}
		alreadyChecked.put(one, another);

		if (!requireMatchingCollectionClasses && oneIsAssignable(one, another, List.class, Set.class, Map.class)) {
			if (isOnlyOneAssignable(List.class, one, another)) {
				Assert.fail("One of both collections on path '" + (StringUtils.isEmpty(path) ? "." : path)
					+ "' is not a java.util.List: " + one.getClass() + ", " + another.getClass());
			}
			if (isOnlyOneAssignable(Set.class, one, another)) {
				Assert.fail("One of both collections on path '" + (StringUtils.isEmpty(path) ? "." : path)
					+ "' is not a java.util.Set: " + one.getClass() + ", " + another.getClass());
			}
			if (isOnlyOneAssignable(Map.class, one, another)) {
				Assert.fail("One of both collections on path '" + (StringUtils.isEmpty(path) ? "." : path)
					+ "' is not a java.util.Map: " + one.getClass() + ", " + another.getClass());
			}
		} else {
			Assert.assertEquals("Classes don't match on path '" + (StringUtils.isEmpty(path) ? "." : path) + "' - ", one.getClass(),
				another.getClass());
		}

		if (one instanceof AtomicInteger || one instanceof AtomicLong) {
			Assert.assertEquals("Values not equals for path '" + (StringUtils.isEmpty(path) ? "." : path) + "' - ",
				((Number)one).longValue(), ((Number)another).longValue());
			return;
		}

		if (one instanceof Calendar) {
			Assert.assertEquals("Values not equals for path '" + (StringUtils.isEmpty(path) ? "." : path) + "' - \n"
				+ ((Calendar)one).getTimeInMillis() + "\n" + ((Calendar)another).getTimeInMillis() + "\n", one, another);
			return;
		}

		if (one.getClass().isPrimitive() || one instanceof String || one instanceof Character || one instanceof Boolean
			|| one instanceof Number || one instanceof Date) {
			Assert.assertEquals("Values not equals for path '" + (StringUtils.isEmpty(path) ? "." : path) + "' - ", one, another);
			return;
		}

		if (Map.class.isAssignableFrom(one.getClass())) {
			assertMapEquals((Map<?, ?>)one, (Map<?, ?>)another, requireMatchingCollectionClasses, alreadyChecked, path);
			return;
		}

		if (Collection.class.isAssignableFrom(one.getClass())) {
			assertCollectionEquals((Collection<?>)one, (Collection<?>)another, requireMatchingCollectionClasses, alreadyChecked,
				path);
			return;
		}

		if (one instanceof Currency) {
			// Check that the transient field defaultFractionDigits is initialized
			// correctly (that was issue #34)
			final Currency currency1 = (Currency)one;
			final Currency currency2 = (Currency)another;
			Assert.assertEquals(
				"Currency code does not match for currency on path '" + (StringUtils.isEmpty(path) ? "." : path) + "' - ",
				currency1.getCurrencyCode(), currency2.getCurrencyCode());
			Assert.assertEquals("Currency default fraction digits do not match for currency on path '"
				+ (StringUtils.isEmpty(path) ? "." : path) + "' - ", currency1.getDefaultFractionDigits(),
				currency2.getDefaultFractionDigits());
			return;
		}

		Class<?> clazz = one.getClass();
		while (clazz != null) {
			assertEqualDeclaredFields(clazz, one, another, requireMatchingCollectionClasses, alreadyChecked, path);
			clazz = clazz.getSuperclass();
		}

	} // CHECKSTYLE:ON

	private static boolean isOnlyOneAssignable (final Class<?> checkedClazz, final Object one, final Object another) {
		return checkedClazz.isAssignableFrom(one.getClass()) && !checkedClazz.isAssignableFrom(another.getClass())
			|| checkedClazz.isAssignableFrom(another.getClass()) && !checkedClazz.isAssignableFrom(one.getClass());
	}

	private static boolean oneIsAssignable (final Object one, final Object another, final Class<?>... checkedClazzes) {
		for (final Class<?> checkedClazz : checkedClazzes) {
			if (checkedClazz.isAssignableFrom(one.getClass()) || checkedClazz.isAssignableFrom(another.getClass())) {
				return true;
			}
		}
		return false;
	}

	/*
	 * TODO (MG): this assumes same iteration order, which must not be given for sets. There could be a specialized implementation
	 * for sets.
	 */
	private static void assertCollectionEquals (final Collection<?> m1, final Collection<?> m2,
		final boolean requireMatchingClasses, final Map<Object, Object> alreadyChecked, final String path) {
		Assert.assertEquals("Collection size does not match for path '" + (StringUtils.isEmpty(path) ? "." : path) + "' - ",
			m1.size(), m2.size());
		final Iterator<?> iter1 = m1.iterator();
		final Iterator<?> iter2 = m2.iterator();
		int i = 0;
		while (iter1.hasNext()) {
			assertReflectionEquals(iter1.next(), iter2.next(), requireMatchingClasses, alreadyChecked, path + "[" + i++ + "]");
		}
	}

	private static void assertMapEquals (final Map<?, ?> m1, final Map<?, ?> m2, final boolean requireMatchingClasses,
		final Map<Object, Object> alreadyChecked, final String path) {
		Assert.assertEquals("Map size does not match for path '" + (StringUtils.isEmpty(path) ? "." : path) + "', map contents:"
			+ "\nmap1: " + m1 + "\nmap2: " + m2 + "\n", m1.size(), m2.size());
		for (final Map.Entry<?, ?> entry : m1.entrySet()) {
			assertReflectionEquals(entry.getValue(), m2.get(entry.getKey()), requireMatchingClasses, alreadyChecked,
				path + "[" + entry.getKey() + "]");
		}
	}

	private static void assertEqualDeclaredFields (final Class<? extends Object> clazz, final Object one, final Object another,
		final boolean requireMatchingClasses, final Map<Object, Object> alreadyChecked, final String path) {
		for (final Field field : clazz.getDeclaredFields()) {
			field.setAccessible(true);
			if (!Modifier.isTransient(field.getModifiers())) {
				try {
					assertReflectionEquals(field.get(one), field.get(another), requireMatchingClasses, alreadyChecked,
						path + "." + field.getName());
				} catch (final IllegalArgumentException e) {
					throw new RuntimeException(e);
				} catch (final IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}
}
