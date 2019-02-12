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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.ReferenceResolver;

import java.util.ArrayList;
import java.util.IdentityHashMap;

/** Uses an {@link IdentityHashMap} to track objects that have already been written. This can handle a graph with any number of
 * objects, but is slightly slower than {@link ListReferenceResolver} for graphs with few objects. Compared to
 * {@link MapReferenceResolver}, this may provide better performance for object graphs with a very high number of objects since
 * the IdentityHashMap does less work for put, even though put allocates.
 * @author Nathan Sweet */
public class HashMapReferenceResolver implements ReferenceResolver {
	protected Kryo kryo;
	protected final IdentityHashMap<Object, Integer> writtenObjects = new IdentityHashMap();
	protected final ArrayList readObjects = new ArrayList();

	public void setKryo (Kryo kryo) {
		this.kryo = kryo;
	}

	public int addWrittenObject (Object object) {
		int id = writtenObjects.size();
		writtenObjects.put(object, id);
		return id;
	}

	public int getWrittenId (Object object) {
		Integer id = writtenObjects.get(object);
		if (id == null) return -1;
		return id;
	}

	public int nextReadId (Class type) {
		int id = readObjects.size();
		readObjects.add(null);
		return id;
	}

	public void setReadObject (int id, Object object) {
		readObjects.set(id, object);
	}

	public Object getReadObject (Class type, int id) {
		return readObjects.get(id);
	}

	public void reset () {
		readObjects.clear();
		writtenObjects.clear();
	}

	/** Returns false for all primitive wrappers and enums. */
	public boolean useReferences (Class type) {
		return !Util.isWrapperClass(type) && !Util.isEnum(type);
	}
}
