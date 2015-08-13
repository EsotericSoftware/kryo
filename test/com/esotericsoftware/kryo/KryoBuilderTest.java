package com.esotericsoftware.kryo;

import java.io.ByteArrayOutputStream;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class KryoBuilderTest extends KryoTestCase {

	public void testBuilder() {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		
		KryoBuilder builder = new KryoBuilder();
		builder.setRegistrationRequired(true);
		builder.register(int[].class);
		
		Output output = new Output(bytes);
		builder.build().writeClassAndObject(output, new int[] {1,2,3});
		output.close();
		
		Input input = new Input(bytes.toByteArray());
		int[] ia = (int[]) builder.build().readClassAndObject(input);
		
		assertEquals((Object) new int[] {1, 2,3}, (Object) ia);
	}
	
	
	public void testSerializeBuilder() throws Exception {
		setUp();
		kryo.register(KryoBuilder.class, new KryoBuilder.KryoBuilderSerializer());
		kryo.setRegistrationRequired(false);
		kryo.setReferences(true);
		
		KryoBuilder builder = new KryoBuilder();
		builder.setRegistrationRequired(true);
		builder.register(int[].class);
		
		roundTrip(42, 60, builder);
	}
}
