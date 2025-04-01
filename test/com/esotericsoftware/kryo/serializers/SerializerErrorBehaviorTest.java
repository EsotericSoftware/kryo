package com.esotericsoftware.kryo.serializers;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.KryoTestCase;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class SerializerErrorBehaviorTest extends KryoTestCase {

    @Test
    void testAbstractClassCannotBeSerialized() {
        kryo.register(AbstractClass.class);
        kryo.register(ConcreteClass.class);
        assertThrows(KryoException.class, this::simpleRoundtrip);
    }

    @Test
    void testCustomObjectInsantiatorForAbstractClass() {
        Registration registration = kryo.register(AbstractClass.class);
        registration.setInstantiator(ConcreteClass::new);
    }

    public void simpleRoundtrip() {
        AbstractClass abstractClass = new ConcreteClass();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Output output = new Output(baos);
        kryo.writeObject(output, abstractClass);
        output.close();
        Input input = new Input(baos.toByteArray());
        kryo.readObject(input, AbstractClass.class);
        input.close();
    }

    private static abstract class AbstractClass {}

    private static class ConcreteClass extends AbstractClass {}
}
