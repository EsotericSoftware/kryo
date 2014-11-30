/* Copyright (c) 2008, Nathan Sweet
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

package com.esotericsoftware.kryo.pool;

import java.lang.ref.SoftReference;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;

import com.esotericsoftware.kryo.Kryo;

/**
 * Internally uses {@link SoftReference}s for queued Kryo instances,
 * most importantly adjusts the {@link Queue#poll() poll}
 * behavior so that gc'ed Kryo instances are skipped.
 * Most other methods are unsupported.
 *
 * @author Martin Grotzke
 */
class SoftReferenceQueue implements Queue<Kryo> {
	
	private Queue<SoftReference<Kryo>> delegate;

	public SoftReferenceQueue (Queue<?> delegate) {
		this.delegate = (Queue<SoftReference<Kryo>>)delegate;
	}

	public Kryo poll () {
		Kryo res;
		SoftReference<Kryo> ref;
		while((ref = delegate.poll()) != null) {
			if((res = ref.get()) != null) {
				return res;
			}
		}
		return null;
	}

	public boolean offer (Kryo e) {
		return delegate.offer(new SoftReference(e));
	}

	public boolean add (Kryo e) {
		return delegate.add(new SoftReference(e));
	}

	public int size () {
		return delegate.size();
	}

	public boolean isEmpty () {
		return delegate.isEmpty();
	}

	public boolean contains (Object o) {
		return delegate.contains(o);
	}

	public void clear () {
		delegate.clear();
	}

	public boolean equals (Object o) {
		return delegate.equals(o);
	}

	public int hashCode () {
		return delegate.hashCode();
	}
	
	@Override
	public String toString () {
		return getClass().getSimpleName() + super.toString();
	}

	public Iterator<Kryo> iterator () {
		throw new UnsupportedOperationException();
	}

	public Kryo remove () {
		throw new UnsupportedOperationException();
	}

	public Object[] toArray () {
		throw new UnsupportedOperationException();
	}

	public Kryo element () {
		throw new UnsupportedOperationException();
	}

	public Kryo peek () {
		throw new UnsupportedOperationException();
	}

	public <T> T[] toArray (T[] a) {
		throw new UnsupportedOperationException();
	}

	public boolean remove (Object o) {
		throw new UnsupportedOperationException();
	}

	public boolean containsAll (Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	public boolean addAll (Collection<? extends Kryo> c) {
		throw new UnsupportedOperationException();
	}

	public boolean removeAll (Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	public boolean retainAll (Collection<?> c) {
		throw new UnsupportedOperationException();
	}
}
