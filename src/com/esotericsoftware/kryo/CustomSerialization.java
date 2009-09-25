
package com.esotericsoftware.kryo;

import java.nio.ByteBuffer;

import com.esotericsoftware.kryo.serialize.CustomSerializer;

/**
 * Allows implementing classes to perform their own serialization. Custom serialization can be more efficient in some cases.<br>
 * <br>
 * Custom serialization can make use of any Serializer needed. Eg:<br>
 * <br>
 * public void writeObjectData (ByteBuffer buffer) {<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;StringSerializer.put(stringValue, buffer);<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;fieldSerializerInstance.writeObjectData(context, buffer, someOtherClassValue);<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;buffer.putInt(intValue);<br>
 * }<br>
 * <br>
 * public void readObjectData (ByteBuffer buffer) {<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;stringValue = StringSerializer.get(buffer);<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;someOtherClassValue = fieldSerializerInstance.readObjectData(context, buffer, SomeOtherClass.class);<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;intValue = buffer.getInt();<br>
 * }
 * @see CustomSerializer
 * @author Nathan Sweet <misc@n4te.com>
 */
public interface CustomSerialization {
	public void writeObjectData (ByteBuffer buffer);

	public void readObjectData (ByteBuffer buffer);
}
