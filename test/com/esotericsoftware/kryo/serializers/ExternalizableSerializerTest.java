package com.esotericsoftware.kryo.serializers;

import com.esotericsoftware.kryo.KryoTestCase;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/** @author Robert DiFalco <robert.difalco@gmail.com> */
public class ExternalizableSerializerTest extends KryoTestCase {
    public void testRegister() {
        kryo.register(TestClass.class, new ExternalizableSerializer());
        kryo.register(String.class, new DefaultSerializers.StringSerializer() );
        TestClass test = new TestClass();
        test.stringField = "fubar";
        test.intField = 54321;
        roundTrip(11, 11, test);
        roundTrip(11, 11, test);
        roundTrip(11, 11, test);
    }

    public void testDefault() {
        kryo.setRegistrationRequired( false );
        kryo.addDefaultSerializer( Externalizable.class, new ExternalizableSerializer() );
        TestClass test = new TestClass();
        test.stringField = "fubar";
        test.intField = 54321;
        roundTrip(90, 90, test);
        roundTrip(90, 90, test);
        roundTrip(90, 90, test);
    }

    public void testReadResolve() {
        kryo.setRegistrationRequired( false );
        kryo.addDefaultSerializer( Externalizable.class, ExternalizableMaySerializeSerializer.class );

        ReadResolvable test = new ReadResolvable( "foobar" );
        Output output = new Output( 1024 );
        kryo.writeClassAndObject( output, test );
        output.flush();

        Input input = new Input( output.getBuffer() );
        Object result = kryo.readClassAndObject( input );
        input.close();

        // ensure read resolve happened!
        assertEquals( String.class, result.getClass() );
        assertEquals( test.value, result );
    }

    public void testSuppressReadResolve() {
        kryo.setRegistrationRequired( false );
        kryo.addDefaultSerializer( Externalizable.class, ExternalizableSerializer.class );

        ReadResolvable test = new ReadResolvable( "foobar" );
        Output output = new Output( 1024 );
        kryo.writeClassAndObject( output, test );
        output.flush();

        Input input = new Input( output.getBuffer() );
        Object result = kryo.readClassAndObject( input );
        input.close();

        // ensure read resolve DID NOT happen!
        assertEquals( ReadResolvable.class, result.getClass() );
        assertEquals( test.value, ((ReadResolvable)result).value );
    }

    public static class TestClass implements Externalizable {
        private String stringField;
        private int intField;

        public boolean equals (Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            TestClass other = (TestClass)obj;
            if (intField != other.intField) return false;
            if (stringField == null) {
                if (other.stringField != null) return false;
            } else if (!stringField.equals(other.stringField)) return false;
            return true;
        }

        public void writeExternal( ObjectOutput out ) throws IOException {
            out.writeObject( stringField );
            out.writeInt( intField );
        }

        public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException {
            stringField = (String)in.readObject();
            intField = in.readInt();
        }
    }

    public static class ReadResolvable implements Externalizable {
        private String value;
        private Object makeSureNullWorks;

        public ReadResolvable() {
        }

        public ReadResolvable( String value ) {
            this.value = value;
        }


        public void writeExternal( ObjectOutput out ) throws IOException {
            out.writeObject( value );
        }

        public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException {
            value = (String)in.readObject();
        }

        private Object readResolve() {
            return value;
        }
    }
}
