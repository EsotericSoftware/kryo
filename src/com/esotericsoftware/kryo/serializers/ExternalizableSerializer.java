package com.esotericsoftware.kryo.serializers;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.KryoObjectInput;
import com.esotericsoftware.kryo.io.KryoObjectOutput;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.ObjectMap;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Writes using the objects externalizable interface if it can reliably do so. Typically, a object can
 * be efficiently written with Kryo and Java's externalizable interface. However, there may be behavior
 * problems if the class uses either the 'readResolve' or 'writeReplace' methods. By enabling the check
 * for these we will fall back onto the standard {@link JavaSerializer} if we detect either of these
 * methods.
 * @author Robert DiFalco <robert.difalco@gmail.com>
 */
public class ExternalizableSerializer extends Serializer<Object> {

    @Override
    public void write( Kryo kryo, Output output, Object object ) {
        try {
            ((Externalizable)object).writeExternal( getObjectOutput( kryo, output ) );
        }
        catch ( IOException e ) {
            throw new KryoException( e );
        }
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public Object read( Kryo kryo, Input input, Class type ) {
        try {
            Externalizable object = (Externalizable)type.newInstance();
            object.readExternal( getObjectInput( kryo, input ) );
            return object;
        }
        catch ( Exception e ) {
            throw new KryoException( e );
        }
    }

    @SuppressWarnings( "unchecked" )
    private ObjectOutput getObjectOutput( Kryo kryo, Output output ) {
        ObjectMap graphContext = kryo.getGraphContext();
        ObjectOutput objectOutput = (ObjectOutput)graphContext.get( this );
        if (objectOutput == null) {
            objectOutput = new KryoObjectOutput( kryo, output );
            graphContext.put(this, objectOutput);
        }

        return objectOutput;
    }

    @SuppressWarnings( "unchecked" )
    private ObjectInput getObjectInput( Kryo kryo, Input input ) {
        ObjectMap graphContext = kryo.getGraphContext();
        ObjectInput objectInput = (ObjectInput)graphContext.get( this );
        if (objectInput == null) {
            objectInput = new KryoObjectInput( kryo, input );
            graphContext.put(this, objectInput);
        }

        return objectInput;
    }
}
