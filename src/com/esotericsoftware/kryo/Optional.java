
package com.esotericsoftware.kryo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.esotericsoftware.kryo.serialize.FieldSerializer;

/**
 * Indicates a field should be ignored when its declaring class is registered unless the {@link Kryo#getContext() context} has a
 * value set for specified key. This can be useful to useful when a field must be serialized for one purpose, but not for another.
 * Eg, a class for a networked application might have a field that should not be serialized and sent to clients, but should be
 * serialized when stored on the server.
 * @author Nathan Sweet <misc@n4te.com>
 */
@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD) public @interface Optional {
	public String value();
}
