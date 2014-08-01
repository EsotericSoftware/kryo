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