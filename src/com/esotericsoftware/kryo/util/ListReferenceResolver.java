
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
