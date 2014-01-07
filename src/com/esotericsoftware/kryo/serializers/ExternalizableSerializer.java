package com.esotericsoftware.kryo.serializers;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.adapters.KryoObjectInput;
import com.esotericsoftware.kryo.adapters.KryoObjectOutput;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Method;

/**
 * Writes using the objects externalizable interface if it can reliably do so. Typically, a object can
 * be efficiently written with Kryo and Java's externalizable interface. However, there may be behavior
 * problems if the class uses either the 'readResolve' or 'writeReplace' methods. By enabling the check
 * for these we will fall back onto the standard {@link JavaSerializer} if we detect either of these
 * methods.
 * @author Robert DiFalco <robert.difalco@gmail.com>
 */
public class ExternalizableSerializer extends Serializer<Object> {

    private boolean checkForReplaceMethods;
    private JavaSerializer javaSerializer;

    public ExternalizableSerializer() {
    }

    /**
     * If this is enabled the serializer will check to see if 'readResolve' or 'writeReplace' has been
     * defined for the type. If they have it will fall back on the basic and slow {@link JavaSerializer} class.
     *
     * @param checkForReplaceMethods true to check if the type has readResolve or writeReplace methods. The default is false.
     */
    public void setCheckForReplaceMethods( boolean checkForReplaceMethods ) {
        this.checkForReplaceMethods = checkForReplaceMethods;
    }

    @Override
    public void write( Kryo kryo, Output output, Object object ) {
        JavaSerializer serializer = getJavaSerializer( object.getClass() );
        if ( serializer == null ) {
            writeExternal( kryo, output, (Externalizable)object );
        }
        else {
            serializer.write( kryo, output, object );
        }
    }

    protected void writeExternal( Kryo kryo, Output output, Externalizable object ) {
        ObjectOutput objectOutput = new KryoObjectOutput( kryo, output );
        try {
            object.writeExternal( objectOutput );
        }
        catch ( IOException e ) {
            throw new KryoException( e );
        }
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public Object read( Kryo kryo, Input input, Class type ) {
        JavaSerializer serializer = getJavaSerializer( type );
        if ( serializer == null ) {
            return readExternal( kryo, input, (Class<Externalizable>)type );
        }
        else {
            return serializer.read( kryo, input, type );
        }
    }

    protected Externalizable readExternal( Kryo kryo, Input input, Class<Externalizable> type ) {
        ObjectInput objectInput = new KryoObjectInput( kryo, input );
        try {
            Externalizable object = type.newInstance();
            object.readExternal( objectInput );
            return object;
        }
        catch ( Exception e ) {
            throw new KryoException( e );
        }
    }

    private JavaSerializer getJavaSerializer( Class<?> type ) {
        if ( javaSerializer == null && isJavaSerializerRequired( type ) ) {
            javaSerializer = new JavaSerializer();
        }

        return javaSerializer;
    }

    private boolean isJavaSerializerRequired( Class<?> type ) {
        return this.checkForReplaceMethods &&
            ( hasInheritableReplaceMethod( type, "writeReplace" )
                || hasInheritableReplaceMethod( type, "readResolve" ) );
    }

    /* find out if there are any pesky serialization extras on this class */
    private static boolean hasInheritableReplaceMethod( Class<?> type, String methodName ) {
        Method method = null;
        Class<?> current = type;
        while (current != null) {
            try {
                method = current.getDeclaredMethod(methodName, null);
                break;
            } catch (NoSuchMethodException ex) {
                current = current.getSuperclass();
            }
        }

        return ((method != null) && (method.getReturnType() == Object.class));
    }
}
