
package com.esotericsoftware.kryo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.esotericsoftware.kryo.serializers.FieldSerializer;

/** Indicates a field can never be null when it is being serialized and deserialized. This optimization allows
 * {@link FieldSerializer} to save 1 byte.
 * @author Nathan Sweet <misc@n4te.com> */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface NotNull {
}
