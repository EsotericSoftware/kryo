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

package com.esotericsoftware.kryo.util;

import java.util.ArrayList;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.ReferenceResolver;

/** Uses an {@link ArrayList} to track objects that have already been written. This is more efficient than
 * {@link MapReferenceResolver} for graphs with few objects, providing an approximate 15% increase in deserialization speed. This
 * should not be used for graphs with many objects because it uses a linear look up to find objects that have already been
 * written.
 * @author Nathan Sweet <misc@n4te.com> */
public class ListReferenceResolver implements ReferenceResolver {
	protected Kryo kryo;
	protected final ArrayList seenObjects = new ArrayList();

	public void setKryo (Kryo kryo) {
		this.kryo = kryo;
	}

	public int addWrittenObject (Object object) {
		int id = seenObjects.size();
		seenObjects.add(object);
		return id;
	}

	public int getWrittenId (Object object) {
		for (int i = 0, n = seenObjects.size(); i < n; i++)
			if (seenObjects.get(i) == object) return i;
		return -1;
	}

	public int nextReadId (Class type) {
		int id = seenObjects.size();
		seenObjects.add(null);
		return id;
	}

	public void setReadObject (int id, Object object) {
		seenObjects.set(id, object);
	}

	public Object getReadObject (Class type, int id) {
		return seenObjects.get(id);
	}

	public void reset () {
		seenObjects.clear();
	}

	/** Returns false for Boolean, Byte, Character, and Short. */
	public boolean useReferences (Class type) {
		return !Util.isWrapperClass(type);
	}
}
