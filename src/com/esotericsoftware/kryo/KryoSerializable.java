
package com.esotericsoftware.kryo;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.KryoSerializableSerializer;

/** Allows implementing classes to perform their own serialization. Hand written serialization can be more efficient in some cases.
 * <p>
 * The default serializer for KryoSerializable is {@link KryoSerializableSerializer}, which uses {@link Kryo#newInstance(Class)}
 * to construct the class.
 * @author Nathan Sweet <misc@n4te.com> */
public interface KryoSerializable {
	public void write (Kryo kryo, Output output);

	public void read (Kryo kryo, Input input);
}
