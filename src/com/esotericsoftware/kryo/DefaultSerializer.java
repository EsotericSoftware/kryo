
package com.esotericsoftware.kryo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Sets the default serializer to use for the annotated class. If the specified Serializer class has a constructor taking a Kryo
 * instance, it will be used. Otherwise the class must have a default constructor.
 * @see Kryo#getDefaultSerializer(Class)
 * @author Nathan Sweet <misc@n4te.com>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DefaultSerializer {
	Class<? extends Serializer> value();
}
