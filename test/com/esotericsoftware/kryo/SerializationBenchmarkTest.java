package com.esotericsoftware.kryo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Arrays;

//import org.gridgain.grid.marshaller.GridMarshaller;
//import org.gridgain.grid.marshaller.optimized.GridOptimizedMarshaller;

import com.esotericsoftware.kryo.io.FastInput;
import com.esotericsoftware.kryo.io.FastOutput;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.io.UnsafeInput;
import com.esotericsoftware.kryo.io.UnsafeOutput;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.esotericsoftware.minlog.Log;

/***
 * This test was originally taken from a GridGain blog.
 * It is a compares the speed of serialization using Java serialization,
 * Kryo, Kryo with Unsafe patches and GridGain's serialization.
 *
 * @author Roman Levenstein <romixlev@gmail.com>
 */
public class SerializationBenchmarkTest extends KryoTestCase {
	/** Number of runs. */
	private static final int RUN_CNT = 3;

	/** Number of iterations. Set it to something rather big for obtaining meaningful results */
//	private static final int ITER_CNT = 200000;
	private static final int ITER_CNT = 200;

	SampleObject obj = createObject();

	protected void setUp () throws Exception {
		super.setUp();
		Log.WARN();
	}
	
//	public static void main(String[] args) throws Exception {
//		// Create sample object.
//		SampleObject obj = createObject();
//		
//		SerializationBenchmarkTest benchmark = new SerializationBenchmarkTest();
//
//		// Run Java serialization test.
//		// benchmark.testJavaSerialization(obj);
//		// benchmark.testJavaSerializationWithoutTryCatch(obj);
//
//		// Run Kryo serialization test.
//		// benchmark.kryoSerialization(obj);
//		benchmark.testKryoSerializationUmodified(obj);
//		benchmark.testKryoSerializationWithoutTryCatch(obj);
//		benchmark.testKryoUnsafeSerializationWithoutTryCatch(obj);
//
//		// Run GridGain serialization test.
////		benchmark.gridGainSerialization(obj);
//	}

	
	public void testJavaSerialization() throws Exception {
		long avgDur = 0;

		for (int i = 0; i < RUN_CNT; i++) {
			SampleObject newObj = null;

			long start = System.currentTimeMillis();

			for (int j = 0; j < ITER_CNT; j++) {
				ByteArrayOutputStream out = new ByteArrayOutputStream(4096);

				ObjectOutputStream objOut = null;

				try {
					objOut = new ObjectOutputStream(out);

					objOut.writeObject(obj);
				} finally {
					objOut.close();
					// U.close(objOut, null);
				}

				ObjectInputStream objIn = null;

				try {
					objIn = new ObjectInputStream(new ByteArrayInputStream(
							out.toByteArray()));

					newObj = (SampleObject) objIn.readObject();
				} finally {
					objIn.close();
					// U.close(objIn, null);
				}
			}

			long dur = System.currentTimeMillis() - start;
			// Check that unmarshalled object is equal to original one (should
			// never fail).
			if (!obj.equals(newObj))
				throw new RuntimeException(
						"Unmarshalled object is not equal to original object.");

			System.out
					.format(">>> Java serialization via Externalizable (run %d): %,d ms\n",
							i + 1, dur);
			avgDur += dur;
		}

		avgDur /= RUN_CNT;

		System.out
				.format("\n>>> Java serialization via Externalizable (average): %,d ms\n\n",
						avgDur);

	}

	public void testKryoSerializationUmodified()
			throws Exception {
		Kryo marsh = new Kryo();

		long avgDur = 0;

		for (int i = 0; i < RUN_CNT; i++) {
			SampleObject newObj = null;

			long start = System.currentTimeMillis();

			for (int j = 0; j < ITER_CNT; j++) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();

				Output kryoOut = null;

				try {
					kryoOut = new Output(out);

					marsh.writeObject(kryoOut, obj);
				} finally {
					// U.close(kryoOut, null);
					kryoOut.close();
				}

				Input kryoIn = null;

				try {
					kryoIn = new Input(new ByteArrayInputStream(
							out.toByteArray()));

					newObj = marsh.readObject(kryoIn, SampleObject.class);
				} finally {
					// U.close(kryoIn, null);
					kryoIn.close();
				}
			}

			long dur = System.currentTimeMillis() - start;

			// Check that unmarshalled object is equal to original one (should
			// never fail).
			if (!obj.equals(newObj))
				throw new RuntimeException(
						"Unmarshalled object is not equal to original object.");

			System.out
					.format(">>> Kryo unmodified serialization without try-catch (run %d): %,d ms\n",
							i + 1, dur);

			avgDur += dur;
		}

		avgDur /= RUN_CNT;

		System.out.format(
				"\n>>> Kryo unmodified serialization (average): %,d ms\n\n",
				avgDur);

	}

	public void testKryoSerialization() throws Exception {
		Kryo marsh = new Kryo();
		marsh.register(SampleObject.class, 40);

		long avgDur = 0;
		byte[] out = new byte[4096 * 10];

		for (int i = 0; i < RUN_CNT; i++) {
			SampleObject newObj = null;

			long start = System.currentTimeMillis();

			for (int j = 0; j < ITER_CNT; j++) {

				Output kryoOut = null;

				try {
					kryoOut = new Output(out);
					marsh.writeObject(kryoOut, obj);
				} finally {
					kryoOut.close();
					// U.close(kryoOut, null);
				}

				Input kryoIn = null;

				try {
					kryoIn = new Input(kryoOut.toBytes());
					newObj = marsh.readObject(kryoIn, SampleObject.class);
				} finally {
					kryoIn.close();
					// U.close(kryoIn, null);
				}
			}

			long dur = System.currentTimeMillis() - start;
			// Check that unmarshalled object is equal to original one (should
			// never fail).
			if (!obj.equals(newObj))
				throw new RuntimeException(
						"Unmarshalled object is not equal to original object.");

			System.out
					.format(">>> Kryo serialization without try-catch (run %d): %,d ms\n",
							i + 1, dur);
			avgDur += dur;
		}

		avgDur /= RUN_CNT;

		System.out.format("\n>>> Kryo serialization (average): %,d ms\n\n",
				avgDur);

	}

	public void testJavaSerializationWithoutTryCatch()
			throws Exception {
		long avgDur = 0;

		for (int i = 0; i < RUN_CNT; i++) {
			SampleObject newObj = null;

			long start = System.currentTimeMillis();

			for (int j = 0; j < ITER_CNT; j++) {
				ByteArrayOutputStream out = new ByteArrayOutputStream(4096);

				ObjectOutputStream objOut = null;

				objOut = new ObjectOutputStream(out);

				objOut.writeObject(obj);
				objOut.close();
				// U.close(objOut, null);

				ObjectInputStream objIn = null;

				objIn = new ObjectInputStream(new ByteArrayInputStream(
						out.toByteArray()));

				newObj = (SampleObject) objIn.readObject();
				objIn.close();
				// U.close(objIn, null);
			}

			long dur = System.currentTimeMillis() - start;
			// Check that unmarshalled object is equal to original one (should
			// never fail).
			if (!obj.equals(newObj))
				throw new RuntimeException(
						"Unmarshalled object is not equal to original object.");
			System.out
					.format(">>> Java serialization without try-catch (run %d): %,d ms\n",
							i + 1, dur);

			avgDur += dur;
		}

		avgDur /= RUN_CNT;

		System.out
				.format("\n>>> Java serialization without try-catch via Externalizable (average): %,d ms\n\n",
						avgDur);
	}

	public void testKryoSerializationWithoutTryCatch()
			throws Exception {
		Kryo marsh = new Kryo();
		marsh.register(SampleObject.class, 40);

		long avgDur = 0;

		byte[] out = new byte[4096 * 10];

		for (int i = 0; i < RUN_CNT; i++) {
			SampleObject newObj = null;

			long start = System.currentTimeMillis();

			for (int j = 0; j < ITER_CNT; j++) {

				Output kryoOut = null;

				kryoOut = new Output(out);

				marsh.writeObject(kryoOut, obj);
				kryoOut.close();
				// U.close(kryoOut, null);

				Input kryoIn = null;

				kryoIn = new Input(kryoOut.getBuffer(), 0, kryoOut.position());

				newObj = marsh.readObject(kryoIn, SampleObject.class);
				kryoIn.close();
				// U.close(kryoIn, null);
			}

			long dur = System.currentTimeMillis() - start;
			// Check that unmarshalled object is equal to original one (should
			// never fail).
			if (!obj.equals(newObj))
				throw new RuntimeException(
						"Unmarshalled object is not equal to original object.");
			System.out
					.format(">>> Kryo serialization without try-catch (run %d): %,d ms\n",
							i + 1, dur);

			avgDur += dur;
		}

		avgDur /= RUN_CNT;

		System.out
				.format("\n>>> Kryo serialization without try-catch (average): %,d ms\n\n",
						avgDur);
	}

	public void testKryoSerializationWithoutTryCatchWithFastStreams()
			throws Exception {
		Kryo marsh = new Kryo();
		marsh.register(SampleObject.class, 40);

		long avgDur = 0;

		byte[] out = new byte[4096 * 10 * 4];

		for (int i = 0; i < RUN_CNT; i++) {
			SampleObject newObj = null;

			long start = System.currentTimeMillis();

			for (int j = 0; j < ITER_CNT; j++) {

				Output kryoOut = null;

				kryoOut = new FastOutput(out);

				marsh.writeObject(kryoOut, obj);
				kryoOut.close();
				// U.close(kryoOut, null);

				Input kryoIn = null;

				kryoIn = new FastInput(kryoOut.getBuffer(), 0, kryoOut.position());

				newObj = marsh.readObject(kryoIn, SampleObject.class);
				kryoIn.close();
				// U.close(kryoIn, null);
			}

			long dur = System.currentTimeMillis() - start;
			// Check that unmarshalled object is equal to original one (should
			// never fail).
			if (!obj.equals(newObj))
				throw new RuntimeException(
						"Unmarshalled object is not equal to original object.");
			System.out
					.format(">>> Kryo serialization without try-catch (run %d): %,d ms\n",
							i + 1, dur);

			avgDur += dur;
		}

		avgDur /= RUN_CNT;

		System.out
				.format("\n>>> Kryo serialization without try-catch (average): %,d ms\n\n",
						avgDur);
	}
	
	public void  testKryoUnsafeSerializationWithoutTryCatch()
			throws Exception {
		Kryo marsh = new Kryo();
		marsh.setRegistrationRequired(true);
		marsh.register(double[].class, 30);
		marsh.register(long[].class, 31);
		// Explicitly tell to use Unsafe-based serializer
		marsh.register(SampleObject.class, new FieldSerializer<SampleObject>(marsh, SampleObject.class), 40);
		
		// Use fastest possible serialization of object fields
		FieldSerializer<SampleObject> ser = (FieldSerializer<SampleObject>) marsh.getRegistration(SampleObject.class).getSerializer();
		ser.setUseAsm(false);

		long avgDur = 0;

		byte[] out = new byte[4096 * 10 * 4];
		for (int i = 0; i < RUN_CNT; i++) {
			SampleObject newObj = null;

			long start = System.currentTimeMillis();

			for (int j = 0; j < ITER_CNT; j++) {

				Output kryoOut = null;

				kryoOut = new UnsafeOutput(out);

				marsh.writeObject(kryoOut, obj);
				kryoOut.close();
				// U.close(kryoOut, null);

				Input kryoIn = null;

				kryoIn = new UnsafeInput(kryoOut.getBuffer(), 0,
						kryoOut.position());

				newObj = marsh.readObject(kryoIn, SampleObject.class);
				kryoIn.close();
				// U.close(kryoIn, null);
			}

			long dur = System.currentTimeMillis() - start;
			// Check that unmarshalled object is equal to original one (should
			// never fail).
			if (!obj.equals(newObj))
				throw new RuntimeException(
						"Unmarshalled object is not equal to original object.");

			System.out
					.format(">>> Kryo unsafe serialization without try-catch (run %d): %,d ms\n",
							i + 1, dur);

			avgDur += dur;
		}

		avgDur /= RUN_CNT;

		System.out
				.format("\n>>> Kryo unsafe serialization without try-catch (average): %,d ms\n\n",
						avgDur);

	}

	public void  testKryoUnsafeSerializationWithoutTryCatchWithoutAsm()
			throws Exception {
		Kryo marsh = new Kryo();
		marsh.setRegistrationRequired(true);
		marsh.register(double[].class, 30);
		marsh.register(long[].class, 31);
		// Explicitly tell to use Unsafe-based serializer
		marsh.register(SampleObject.class, new FieldSerializer<SampleObject>(marsh, SampleObject.class), 40);
		
//		// Use fastest possible serialization of object fields
//		FieldSerializer<SampleObject> ser = (FieldSerializer<SampleObject>) marsh.getRegistration(SampleObject.class).getSerializer();
//		ser.setUseAsm(false);

		long avgDur = 0;

		byte[] out = new byte[4096 * 10 * 4];
		for (int i = 0; i < RUN_CNT; i++) {
			SampleObject newObj = null;

			long start = System.currentTimeMillis();

			for (int j = 0; j < ITER_CNT; j++) {

				Output kryoOut = null;

				kryoOut = new UnsafeOutput(out);

				marsh.writeObject(kryoOut, obj);
				kryoOut.close();
				// U.close(kryoOut, null);

				Input kryoIn = null;

				kryoIn = new UnsafeInput(kryoOut.getBuffer(), 0,
						kryoOut.position());

				newObj = marsh.readObject(kryoIn, SampleObject.class);
				kryoIn.close();
				// U.close(kryoIn, null);
			}

			long dur = System.currentTimeMillis() - start;
			// Check that unmarshalled object is equal to original one (should
			// never fail).
			if (!obj.equals(newObj))
				throw new RuntimeException(
						"Unmarshalled object is not equal to original object.");

			System.out
					.format(">>> Kryo unsafe serialization without try-catch (run %d): %,d ms\n",
							i + 1, dur);

			avgDur += dur;
		}

		avgDur /= RUN_CNT;

		System.out
				.format("\n>>> Kryo unsafe serialization without try-catch (average): %,d ms\n\n",
						avgDur);

	}
	
	public void  testKryoUnsafeSerializationWithoutTryCatchWithoutReferences()
			throws Exception {
		Kryo marsh = new Kryo();
		kryo.setReferences(false);
		marsh.setRegistrationRequired(true);
		marsh.register(double[].class, 30);
		marsh.register(long[].class, 31);
		// Explicitly tell to use Unsafe-based serializer
		marsh.register(SampleObject.class, new FieldSerializer<SampleObject>(marsh, SampleObject.class), 40);
		
		// Use fastest possible serialization of object fields
		FieldSerializer<SampleObject> ser = (FieldSerializer<SampleObject>) marsh.getRegistration(SampleObject.class).getSerializer();
		ser.setUseAsm(false);

		long avgDur = 0;

		for (int i = 0; i < RUN_CNT; i++) {
			SampleObject newObj = null;
			byte[] out = new byte[4096 * 10 * 4];

			long start = System.currentTimeMillis();

			for (int j = 0; j < ITER_CNT; j++) {

				Output kryoOut = null;

				kryoOut = new UnsafeOutput(out);

				marsh.writeObject(kryoOut, obj);
				kryoOut.close();
				// U.close(kryoOut, null);

				Input kryoIn = null;

				kryoIn = new UnsafeInput(kryoOut.getBuffer(), 0,
						kryoOut.position());

				newObj = marsh.readObject(kryoIn, SampleObject.class);
				kryoIn.close();
				// U.close(kryoIn, null);
			}

			long dur = System.currentTimeMillis() - start;
			// Check that unmarshalled object is equal to original one (should
			// never fail).
			if (!obj.equals(newObj))
				throw new RuntimeException(
						"Unmarshalled object is not equal to original object.");

			System.out
					.format(">>> Kryo unsafe serialization without try-catch, without references (run %d): %,d ms\n",
							i + 1, dur);

			avgDur += dur;
		}

		avgDur /= RUN_CNT;

		System.out
				.format("\n>>> Kryo unsafe serialization without try-catch, without references (average): %,d ms\n\n",
						avgDur);

	}
//	private static long gridGainSerialization()
//			throws Exception {
//		GridMarshaller marsh = new GridOptimizedMarshaller(false,
//				Arrays.asList(SampleObject.class.getName()), null);
//
//		long avgDur = 0;
//
//		for (int i = 0; i < RUN_CNT; i++) {
//			SampleObject newObj = null;
//
//			long start = System.currentTimeMillis();
//
//			for (int j = 0; j < ITER_CNT; j++)
//				newObj = marsh.unmarshal(marsh.marshal(obj), null);
//
//			long dur = System.currentTimeMillis() - start;
//
//			// Check that unmarshalled object is equal to original one (should
//			// never fail).
//			if (!obj.equals(newObj))
//				throw new RuntimeException(
//						"Unmarshalled object is not equal to original object.");
//
//			System.out.format(">>> GridGain serialization (run %d): %,d ms\n",
//					i + 1, dur);
//
//			avgDur += dur;
//		}
//
//		avgDur /= RUN_CNT;
//
//		System.out.format("\n>>> GridGain serialization (average): %,d ms\n\n",
//				avgDur);
//
//		return avgDur;
//	}

	private static SampleObject createObject() {
		long[] longArr = new long[3000];

		for (int i = 0; i < longArr.length; i++)
			longArr[i] = i;

		double[] dblArr = new double[3000];

		for (int i = 0; i < dblArr.length; i++)
			dblArr[i] = 0.1 * i;

		return new SampleObject(123, 123.456f, (short) 321, longArr, dblArr);
	}

	private static class SampleObject implements Externalizable
	,KryoSerializable
	{
		private int intVal;
		private float floatVal;
		private Short shortVal;
		private long[] longArr;
		private double[] dblArr;
		private SampleObject selfRef;

		public SampleObject() {
		}

		SampleObject(int intVal, float floatVal, Short shortVal,
				long[] longArr, double[] dblArr) {
			this.intVal = intVal;
			this.floatVal = floatVal;
			this.shortVal = shortVal;
			this.longArr = longArr;
			this.dblArr = dblArr;

			selfRef = this;
		}

		// Required by Java Externalizable.
		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
			out.writeInt(intVal);
			out.writeFloat(floatVal);
			out.writeShort(shortVal);
			out.writeObject(longArr);
			out.writeObject(dblArr);
			out.writeObject(selfRef);
		}

		// Required by Java Externalizable.
		@Override
		public void readExternal(ObjectInput in) throws IOException,
				ClassNotFoundException {
			intVal = in.readInt();
			floatVal = in.readFloat();
			shortVal = in.readShort();
			longArr = (long[]) in.readObject();
			dblArr = (double[]) in.readObject();
			selfRef = (SampleObject) in.readObject();
		}

		/** {@inheritDoc} */
		@SuppressWarnings("FloatingPointEquality")
		@Override
		public boolean equals(Object other) {
			if (this == other)
				return true;

			if (other == null || getClass() != other.getClass())
				return false;

			SampleObject obj = (SampleObject) other;

			assert this == selfRef;
			assert obj == obj.selfRef;

			return intVal == obj.intVal && floatVal == obj.floatVal
					&& shortVal.equals(obj.shortVal)
					&& Arrays.equals(dblArr, obj.dblArr)
					&& Arrays.equals(longArr, obj.longArr);
		}

		 // Required by Kryo serialization.
		@Override
		public void write(Kryo kryo, Output out) {
			kryo.writeObject(out, intVal);
			kryo.writeObject(out, floatVal);
			kryo.writeObject(out, shortVal);
			kryo.writeObject(out, longArr);
			kryo.writeObject(out, dblArr);
			kryo.writeObject(out, selfRef);
		}
		
		// Required by Kryo serialization.
		@Override
		public void read(Kryo kryo, Input in) {
			intVal = kryo.readObject(in, Integer.class);
			floatVal = kryo.readObject(in, Float.class);
			shortVal = kryo.readObject(in, Short.class);
			longArr = kryo.readObject(in, long[].class);
			dblArr = kryo.readObject(in, double[].class);
			selfRef = kryo.readObject(in, SampleObject.class);
		}
	}
}