package com.esotericsoftware.kryo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.CollectionSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.StringSerializer;
import com.esotericsoftware.minlog.Log;

/** @author Nathan Sweet <misc@n4te.com> */
public class DeepNestingSerializationTest extends KryoTestCase {
	private static final int MAX_DS_TAG = 5;
	private static final int MAX_DS_SIZE = 5;
	private static final Object OBJECT = new Vector();
	private static final int MAX_ITER = 3;

	{
		supportsCopy = true;
	}

	Random rnd = new Random(0);

	public void testTwoNestedCollections() {
		kryo.register(ArrayList.class);
		kryo.register(LinkedList.class);
		kryo.register(ComplexObject.class);
		kryo.register(Vector.class);
		kryo.register(HashMap.class);
		kryo.register(Object.class);
		Vector v1 = new Vector();
		Vector v2 = new Vector();
		Vector v3 = new Vector();
		v2.add(2);
		v3.add(3);
		v1.add(v2);
		v1.add(v3);
		output = new Output(4096);
		kryo.writeClassAndObject(output, v2);
		kryo.writeClassAndObject(output, v3);
		input = new Input(output.toBytes());
		Vector v22 = (Vector) kryo.readClassAndObject(input);
		Vector v33 = (Vector) kryo.readClassAndObject(input);
		KryoTestCase.assertEquals(v22, v2);
		KryoTestCase.assertEquals(v33, v3);
	}
	
	public void testDeeplyNested() {
		Log.ERROR();
		kryo.register(ArrayList.class);
		kryo.register(LinkedList.class);
		kryo.register(ComplexObject.class);
		kryo.register(Vector.class);
		kryo.register(HashMap.class);
		kryo.register(Object.class);
		
		int depth = 8 * 100000;
		
		Object nestedComplexObject = createNestedComlplexObject(depth);
		
		for(int i=0; i < MAX_ITER; ++i) {
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		output = new Output(outStream);
		kryo.writeClassAndObject(output, nestedComplexObject);
		output.flush();
		byte[] bytes = outStream.toByteArray();
		int bytesLen = bytes.length;
		input = new Input(bytes);
		Object nco2 = kryo.readClassAndObject(input);
		validateComplexObject(nco2, depth);
//		roundTrip(5006, 1500006, nestedComplexObject);
//		roundTrip(6973, 7573, createNestedObject(1000));
		}
	}

	public void testComplexObject() {
		Log.ERROR();
		kryo.register(ArrayList.class);
		kryo.register(LinkedList.class);
		kryo.register(ComplexObject.class);
		kryo.register(Vector.class);
		kryo.register(HashMap.class);
		kryo.register(Object.class);
		
//		roundTrip(5006, 1500006, nestedComplexObject);
		roundTrip(7235, 7835, createNestedObject(1000));
	}
	
	public void testLazyEndlessStream() {
		Log.ERROR();
		kryo.register(ArrayList.class);
		kryo.register(LinkedList.class);
		kryo.register(ComplexObject.class);
		kryo.register(Vector.class);
		kryo.register(HashMap.class);
		kryo.register(Object.class);		
		kryo.register(EndlessStream.class, new EndlessStreamSerializer());
		OutputStream outStream = new OutputStream() {
			
			@Override
			public void write(int arg0) throws IOException {
				
			}
		};
		output = new Output(outStream);
		
		Object endlessNestedObjectStream = new EndlessStream();
		kryo.writeClassAndObject(output, endlessNestedObjectStream);		
	}
	
	private void validateComplexObject(Object co, int depth) {
	    ComplexObject cur = (ComplexObject) co;
	    int level = -1;
		while(cur!=null) {
			KryoTestCase.assertNull(cur.pin1);			
			KryoTestCase.assertNull(cur.pin2);			
			KryoTestCase.assertNull(cur.pin4);			
			KryoTestCase.assertNull(cur.pin5);			
			//KryoTestCase.assertNotNull(cur.pin3);
			cur = (ComplexObject) cur.pin3;
			level++;
		}
		
		KryoTestCase.assertEquals(depth, level);			
		
    }

	private Object createNestedComlplexObject(int level) {
		ComplexObject root = new ComplexObject();
		ComplexObject cur = root;
		for(;level>0;level--) {
			cur.pin3 = new ComplexObject();
			cur = (ComplexObject) cur.pin3;
		}
	    return root;
    }

	private Object createNestedObject(int level) {
		if (level > 0) {
			// Randomly pick a data structure to use at this level
			int dsTag = rnd.nextInt(MAX_DS_TAG);
			int dsSize = rnd.nextInt(MAX_DS_SIZE);
			Object lo = null;
			switch (dsTag) {
			case 0:
				lo = new ArrayList();
				((List) lo).add(createNestedObject(level - 1));
				for (int i = 1; i < dsSize; ++i) {
					((List) lo).add(OBJECT);
				}
				break;
			case 1:
				lo = new Vector(dsSize);
				((Vector) lo).add(createNestedObject(level - 1));
				for (int i = 1; i < dsSize; ++i) {
					((Vector) lo).add(OBJECT);
				}
				break;
			case 2:
				lo = new HashMap();
				((HashMap) lo).put("elem" + 0,
				        createNestedObject(level - 1));
				for (int i = 1; i < dsSize; ++i) {
					((HashMap) lo).put("elem" + i,
					        OBJECT);
				}
				break;
			case 3:
				lo = new LinkedList();
				((List) lo).add(createNestedObject(level - 1));
				for (int i = 0; i < dsSize; ++i) {
					((List) lo).add(OBJECT);
				}
				break;
			case 4:
				lo = new ComplexObject();
				for (int i = 0; i < 1; ++i) {
//				for (int i = 0; i < dsSize; ++i) {
					((ComplexObject) lo).setPin(i,
					        createNestedObject(level - 1));
				}
				break;
			}
			return lo;
		} else {
			return OBJECT;
		}
	}

	static class ComplexObject {
		Object pin1;
		Object pin2;
		Object pin3;
		Object pin4;
		Object pin5;
		
		

		@Override
        public int hashCode() {
	        final int prime = 31;
	        int result = 1;
	        result = prime * result + ((pin1 == null) ? 0 : pin1.hashCode());
	        result = prime * result + ((pin2 == null) ? 0 : pin2.hashCode());
	        result = prime * result + ((pin3 == null) ? 0 : pin3.hashCode());
	        result = prime * result + ((pin4 == null) ? 0 : pin4.hashCode());
	        result = prime * result + ((pin5 == null) ? 0 : pin5.hashCode());
	        return result;
        }



		@Override
        public boolean equals(Object obj) {
	        if (this == obj)
		        return true;
	        if (obj == null)
		        return false;
	        if (getClass() != obj.getClass())
		        return false;
	        ComplexObject other = (ComplexObject) obj;
	        if (pin1 == null) {
		        if (other.pin1 != null)
			        return false;
	        } else if (!pin1.equals(other.pin1))
		        return false;
	        if (pin2 == null) {
		        if (other.pin2 != null)
			        return false;
	        } else if (!pin2.equals(other.pin2))
		        return false;
	        if (pin3 == null) {
		        if (other.pin3 != null)
			        return false;
	        } else if (!pin3.equals(other.pin3))
		        return false;
	        if (pin4 == null) {
		        if (other.pin4 != null)
			        return false;
	        } else if (!pin4.equals(other.pin4))
		        return false;
	        if (pin5 == null) {
		        if (other.pin5 != null)
			        return false;
	        } else if (!pin5.equals(other.pin5))
		        return false;
	        return true;
        }



		public void setPin(int i, Object o) {
			switch (i) {
			case 0:
				pin1 = o;
				break;
			case 1:
				pin2 = o;
				break;
			case 2:
				pin3 = o;
				break;
			case 3:
				pin4 = o;
				break;
			case 4:
				pin5 = o;
				break;
			}
		}

	}
	
	static class EndlessStream {
		private int idx;

		public int getIdx() {
			return idx;
		}
		
	}
	
	static class EndlessStreamSerializer extends Serializer<EndlessStream> {

		{
			setSupportsContinuations(true);
		}
		
		@Override
        public void write(Kryo kryo, Output output, final EndlessStream o) {
			if(o.getIdx() == 200000000)
				return;
//			if(o.getIdx() % 10000000 == 0)
//				System.out.println("\nReached idx="+o.getIdx() + " continuations depth is " + kryo.continuations.size());
			output.writeInt(o.idx++);
			kryo.pushContinuation(new SerializationContinuation(output) {

				@Override
                public SerializationContinuation processWrite(Kryo kryo, Output out, boolean popCont) {
					if(popCont)
						kryo.popContinuation();
					write(kryo, out, o);
					return null;
                }
				
			});
        }

		@Override
        public EndlessStream read(Kryo kryo, Input input,
                Class<EndlessStream> type) {
	        // TODO Auto-generated method stub
	        return null;
        }
		
	}
}
