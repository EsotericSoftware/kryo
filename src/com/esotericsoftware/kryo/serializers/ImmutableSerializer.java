
package com.esotericsoftware.kryo.serializers;

import com.esotericsoftware.kryo.Serializer;

/** A serializer which has {@link #setImmutable(boolean)} set to true. This convenience class exists only to reduce the typing
 * needed to define a serializer which is immutable.
 * @author Nathan Sweet */
abstract public class ImmutableSerializer<T> extends Serializer<T> {
	{
		setImmutable(true);
	}
}
