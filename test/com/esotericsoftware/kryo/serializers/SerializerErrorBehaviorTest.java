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
        assertThrows(KryoException.class, () -> simpleRoundtrip(AbstractClass.class, new ConcreteClass()));
    }

    @Test
    void testCustomObjectInsantiatorForAbstractClass() {
        Registration registration = kryo.register(AbstractClass.class);
        registration.setInstantiator(ConcreteClass::new);
    }

    @Test
    void testNonMemberAbstractClassCannotBeSerialized() {
        kryo.register(TestAbstractClass.class);
        kryo.register(TestConcreteClass.class);
        assertThrows(KryoException.class, () -> simpleRoundtrip(TestAbstractClass.class, new TestConcreteClass()));
    }

    public void simpleRoundtrip(Class type, Object object) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Output output = new Output(baos);
        kryo.writeObject(output, object);
        output.close();
        Input input = new Input(baos.toByteArray());
        kryo.readObject(input, type);
        input.close();
    }

    private static abstract class AbstractClass {}

    private static class ConcreteClass extends AbstractClass {}
}
