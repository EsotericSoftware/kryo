
package com.esotericsoftware.kryo;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer;
import com.esotericsoftware.minlog.Log;

public class TestKryoMainNate {

	public static void main (String[] args) throws IOException {
		Log.DEBUG();

		Kryo kryo = new Kryo();
		// Set serializer that can handle added & removed fields (but can't handle type change).
		kryo.setDefaultSerializer(CompatibleFieldSerializer.class);

		if (false) {
			Output output = new Output(new FileOutputStream("kryo.dat"));
			kryo.writeObject(output, new TestKryoData());
			output.close();
		} else {
			Input input = new Input(new FileInputStream("kryo.dat"));
			TestKryoData dataWrapper = kryo.readObject(input, TestKryoData.class);
			input.close();

			System.out.println("ddd value should be 'ddd', got: " + dataWrapper.ddd);
		}

		System.out.println("Done!");
	}

	static public class TestKryoData {
		public String aaa = "aaa";
		public String bbb = "bbb";
		public String ccc = "ccc";
		public String ddd = bbb;
		//public String ddd = "ddd";
	}
}
