/* Copyright (c) 2025, Daniel Pavlov
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 * - Neither the name of Esoteric Software nor the names of its contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. */

package com.esotericsoftware.kryo;

import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import com.esotericsoftware.kryo.util.InstantiatorStrategy;
import com.esotericsoftware.kryo.util.ObjenesisStrategy;
import com.esotericsoftware.kryo.util.SerializingInstantiatorStrategy;
import com.esotericsoftware.kryo.util.StdInstantiatorStrategy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
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
        assertTrue(thrown.getMessage().contains("Kryo can't create an instance of an interface or abstract class."));
    }

    @Test
    public void testAbstractClassCannotBeInstantiated() {
        KryoException thrown = assertThrows(KryoException.class, () -> tryInstantiate(AbstracClass.class));
        assertTrue(thrown.getMessage().contains("The type you are trying to serialize into is abstract."));
    }

    @Test
    public void testInterfaceClassCannotBeInstantiated() {
        KryoException thrown = assertThrows(KryoException.class, () -> tryInstantiate(InterfaceClass.class));
        assertTrue(thrown.getMessage().contains("Kryo can't create an instance of an interface or abstract class."));
    }

    @Test
    public void testPrivateConstructor() {
        assertEquals(PrivateConstructor.class, instantiatorStrategy.newInstantiatorOf(PrivateConstructor.class).newInstance().getClass());
    }

    @Test
    public void testPublicConstructorOfPackagePrivateClass() {
        assertEquals(PackagePrivateClass.class, instantiatorStrategy.newInstantiatorOf(PackagePrivateClass.class).newInstance().getClass());
    }

    @Test
    public void testConstructorException() {
        KryoException thrown = assertThrows(KryoException.class, () -> tryInstantiate(ThrowingConstructor.class));
        assertSame(ThrowingConstructor.exception, thrown.getCause());
    }

    @Test
    public void testConstructorError() {
        assertSame(ErrorConstructor.error, assertThrows(Error.class, () -> tryInstantiate(ErrorConstructor.class)));
    }

        public void tryInstantiate(Class type) {
        instantiatorStrategy.newInstantiatorOf(type).newInstance();
    }

    private static abstract class AbstractStaticMemberClass {}

    private interface MemberInterface {}

    private static class PrivateConstructor {
        private PrivateConstructor() {}
    }

    static class ThrowingConstructor {
        static final RuntimeException exception = new IllegalStateException();

        public ThrowingConstructor() {
            throw exception;
        }
    }

    static class ErrorConstructor {
        static final Error error = new AssertionError();

        public ErrorConstructor() {
            throw error;
        }
    }

    @Test
    public void testObjenesisStrategies() {
        // The Objenesis strategies create classes without a no-arg constructor, through Kryo's wrappers.
        for (InstantiatorStrategy fallback : new InstantiatorStrategy[] {new StdInstantiatorStrategy(),
            new ObjenesisStrategy(new org.objenesis.strategy.StdInstantiatorStrategy())}) {
            InstantiatorStrategy strategy = new DefaultInstantiatorStrategy(fallback);
            NoArgless object = strategy.newInstantiatorOf(NoArgless.class).newInstance();
            assertEquals(0, object.value);
        }
        assertThrows(KryoException.class, () -> new DefaultInstantiatorStrategy().newInstantiatorOf(NoArgless.class));

        // Like Java serialization: the no-arg constructor of the first non-serializable super class runs, the others don't.
        SerializableNoArgless serializable = new SerializingInstantiatorStrategy().newInstantiatorOf(SerializableNoArgless.class)
            .newInstance();
        assertEquals(1, serializable.base);
        assertEquals(0, serializable.value);
        assertThrows(KryoException.class, () -> new SerializingInstantiatorStrategy().newInstantiatorOf(NoArgless.class));
    }

    static class Base {
        int base;

        Base() {
            base = 1;
        }
    }

    static class SerializableNoArgless extends Base implements java.io.Serializable {
        final int value;

        SerializableNoArgless(int value) {
            this.value = value;
        }
    }

    static class NoArgless {
        final int value;

        NoArgless(int value) {
            this.value = value;
        }
    }
}

class PackagePrivateClass {
    public PackagePrivateClass() {}
}

abstract class AbstracClass {}

interface InterfaceClass {}
