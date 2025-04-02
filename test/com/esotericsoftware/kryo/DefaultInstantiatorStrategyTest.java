package com.esotericsoftware.kryo;

import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class DefaultInstantiatorStrategyTest {

    DefaultInstantiatorStrategy instantiatorStrategy = new DefaultInstantiatorStrategy();

    @Test
    public void testAbstractStaticMemberClassCannotBeInstantiated() {
        KryoException thrown = assertThrows(KryoException.class, () -> tryInstantiate(AbstractStaticMemberClass.class));
        assertTrue(thrown.getMessage().contains("The type you are trying to serialize into is abstract."));
    }

    @Test
    public void testInterfaceMemberClassCannotBeInstantiated() {
        KryoException thrown = assertThrows(KryoException.class, () -> tryInstantiate(MemberInterface.class));
        assertTrue(thrown.getMessage().contains("The type you are trying to serialize into is abstract (interface)."));
    }

    @Test
    public void testAbstractClassCannotBeInstantiated() {
        KryoException thrown = assertThrows(KryoException.class, () -> tryInstantiate(AbstracClass.class));
        assertTrue(thrown.getMessage().contains("The type you are trying to serialize into is abstract."));
    }

    @Test
    public void testInterfaceClassCannotBeInstantiated() {
        KryoException thrown = assertThrows(KryoException.class, () -> tryInstantiate(InterfaceClass.class));
        assertTrue(thrown.getMessage().contains("The type you are trying to serialize into is abstract (interface)."));
    }

    public void tryInstantiate(Class type) {
        instantiatorStrategy.newInstantiatorOf(type).newInstance();
    }

    private static abstract class AbstractStaticMemberClass {}

    private interface MemberInterface {}
}

abstract class AbstracClass {}

interface InterfaceClass {}