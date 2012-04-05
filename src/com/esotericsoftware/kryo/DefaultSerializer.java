
package com.esotericsoftware.kryo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Sets the default serializer to use for the annotated class. The specified Serializer class must have a constructor taking a
 * Kryo instance and a class, a Kryo instance, a class, or no arguments.
 * @see Kryo#register(Class)
 * @author Nathan Sweet <misc@n4te.com> */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DefaultSerializer {
	Class<? extends Serializer> value();
}
