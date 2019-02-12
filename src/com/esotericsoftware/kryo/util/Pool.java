/* Copyright (c) 2008-2018, Nathan Sweet
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

import java.lang.ref.SoftReference;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/** A pool of objects that can be reused to avoid allocations. The pool is optionally thread safe and can be configured to use
 * soft references.
 * @author Nathan Sweet
 * @author Martin Grotzke */
abstract public class Pool<T> {
	private final Queue<T> freeObjects;
	private int peak;

	/** Creates a pool with no maximum. */
	public Pool (boolean threadSafe, boolean softReferences) {
		this(threadSafe, softReferences, Integer.MAX_VALUE);
	}

	/** @param maximumCapacity The maximum number of free objects to store in this pool. Objects are not created until
	 *           {@link #obtain()} is called and no free objects are available. */
	public Pool (boolean threadSafe, boolean softReferences, final int maximumCapacity) {
		Queue<T> queue;
		if (threadSafe)
			queue = new LinkedBlockingQueue(maximumCapacity);
		else if (softReferences) {
			queue = new LinkedList() { // More efficient clean() than ArrayDeque.
				public boolean add (Object object) {
					if (size() >= maximumCapacity) return false;
					super.add(object);
					return true;
				}
			};
		} else {
			queue = new ArrayDeque() {
				public boolean add (Object object) {
					if (size() >= maximumCapacity) return false;
					super.add(object);
					return true;
				}
			};
		}
		freeObjects = softReferences ? new SoftReferenceQueue(queue) : queue;
	}

	abstract protected T create ();

	/** Returns an object from this pool. The object may be new (from {@link #create()}) or reused (previously {@link #free(Object)
	 * freed}). */
	public T obtain () {
		T object = freeObjects.poll();
		return object != null ? object : create();
	}

	/** Puts the specified object in the pool, making it eligible to be returned by {@link #obtain()}. If the pool already contains
	 * the maximum number of free objects, the specified object is reset but not added to the pool.
	 * <p>
	 * If using soft references and the pool contains the maximum number of free objects, the first soft reference whose object has
	 * been garbage collected is discarded to make room. */
	public void free (T object) {
		if (object == null) throw new IllegalArgumentException("object cannot be null.");
		if (!freeObjects.offer(object) && freeObjects instanceof SoftReferenceQueue) {
			((SoftReferenceQueue)freeObjects).cleanOne();
			freeObjects.offer(object);
		}
		peak = Math.max(peak, freeObjects.size());
		reset(object);
	}

	/** Called when an object is freed to clear the state of the object for possible later reuse. The default implementation calls
	 * {@link Poolable#reset()} if the object is {@link Poolable}. */
	protected void reset (T object) {
		if (object instanceof Poolable) ((Poolable)object).reset();
	}

	/** Removes all free objects from this pool. */
	public void clear () {
		freeObjects.clear();
	}

	/** If using soft references, all soft references whose objects have been garbage collected are removed from the pool. This can
	 * be useful to reduce the number of objects in the pool before calling {@link #getFree()} or when the pool has no maximum
	 * capacity. It is not necessary to call {@link #clean()} before calling {@link #free(Object)}, which will try to remove an
	 * empty reference if the maximum capacity has been reached. */
	public void clean () {
		if (freeObjects instanceof SoftReferenceQueue) ((SoftReferenceQueue)freeObjects).clean();
	}

	/** The number of objects available to be obtained.
	 * <p>
	 * If using soft references, this number may include objects that have been garbage collected. {@link #clean()} may be used
	 * first to remove empty soft references. */
	public int getFree () {
		return freeObjects.size();
	}

	/** The all-time highest number of free objects. This can help determine if a pool's maximum capacity is set appropriately. It
	 * can be reset any time with {@link #resetPeak()}.
	 * <p>
	 * If using soft references, this number may include objects that have been garbage collected. */
	public int getPeak () {
		return peak;
	}

	public void resetPeak () {
		peak = 0;
	}

	/** Objects implementing this interface will have {@link #reset()} called when passed to {@link Pool#free(Object)}. */
	static public interface Poolable {
		/** Resets the object for reuse. Object references should be nulled and fields may be set to default values. */
		public void reset ();
	}

	/** Wraps queue values with {@link SoftReference} for {@link Pool}.
	 * @author Martin Grotzke */
	static class SoftReferenceQueue<T> implements Queue<T> {
		private Queue delegate;

		public SoftReferenceQueue (Queue delegate) {
			this.delegate = delegate;
		}

		public T poll () {
			while (true) {
				SoftReference<T> reference = (SoftReference<T>)delegate.poll();
				if (reference == null) return null;
				T object = reference.get();
				if (object != null) return object;
			}
		}

		public boolean offer (T e) {
			return delegate.add(new SoftReference(e));
		}

		public int size () {
			return delegate.size();
		}

		public void clear () {
			delegate.clear();
		}

		void cleanOne () {
			for (Iterator iter = delegate.iterator(); iter.hasNext();) {
				if (((SoftReference)iter.next()).get() == null) {
					iter.remove();
					break;
				}
			}
		}

		void clean () {
			for (Iterator iter = delegate.iterator(); iter.hasNext();)
				if (((SoftReference)iter.next()).get() == null) iter.remove();
		}

		public boolean add (T e) {
			return false;
		}

		public boolean isEmpty () {
			return false;
		}

		public boolean contains (Object o) {
			return false;
		}

		public Iterator<T> iterator () {
			return null;
		}

		public T remove () {
			return null;
		}

		public Object[] toArray () {
			return null;
		}

		public T element () {
			return null;
		}

		public T peek () {
			return null;
		}

		public <E> E[] toArray (E[] a) {
			return null;
		}

		public boolean remove (Object o) {
			return false;
		}

		public boolean containsAll (Collection c) {
			return false;
		}

		public boolean addAll (Collection<? extends T> c) {
			return false;
		}

		public boolean removeAll (Collection c) {
			return false;
		}

		public boolean retainAll (Collection c) {
			return false;
		}
	}
}
