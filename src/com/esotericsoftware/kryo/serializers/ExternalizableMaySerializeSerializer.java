package com.esotericsoftware.kryo.serializers;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.lang.reflect.Method;

/**
 * Copyright (c) 2012,2013
 */
public class ExternalizableMaySerializeSerializer extends ExternalizableSerializer {

    private JavaSerializer javaSerializer;

    @Override
    public void write( Kryo kryo, Output output, Object object ) {
        JavaSerializer serializer = getJavaSerializer( object.getClass() );
        if ( serializer == null ) {
            super.write( kryo, output, object );
        }
        else {
            serializer.write( kryo, output, object );
        }
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public Object read( Kryo kryo, Input input, Class type ) {
        JavaSerializer serializer = getJavaSerializer( type );
        if ( serializer == null ) {
            return super.read( kryo, input, type );
        }
        else {
            return serializer.read( kryo, input, type );
        }
    }

    private JavaSerializer getJavaSerializer( Class<?> type ) {
        if ( javaSerializer == null && isJavaSerializerRequired( type ) ) {
            javaSerializer = new JavaSerializer();
        }

        return javaSerializer;
    }

    private boolean isJavaSerializerRequired( Class<?> type ) {
        return ( hasInheritableReplaceMethod( type, "writeReplace" )
                || hasInheritableReplaceMethod( type, "readResolve" ) );
    }

    /* find out if there are any pesky serialization extras on this class */
    private static boolean hasInheritableReplaceMethod( Class<?> type, String methodName ) {
        Method method = null;
        Class<?> current = type;
        while (current != null) {
            try {
                method = current.getDeclaredMethod( methodName );
                break;
            } catch (NoSuchMethodException ex) {
                current = current.getSuperclass();
            }
        }

        return ((method != null) && (method.getReturnType() == Object.class));
    }
}
