package com.esotericsoftware.kryo.serializers.java8;

import com.esotericsoftware.kryo.KryoTestCase;

import java.util.Objects;
import java.util.Optional;

public class OptionalSerializerTest extends KryoTestCase {

    {
        supportsCopy = true;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        kryo.register(Optional.class);
        kryo.register(TestClass.class);
    }

    public void testNull() {
        roundTrip(2, 2, new TestClass(null));
    }

    public void testEmpty() {
        roundTrip(3, 3, new TestClass(Optional.<String>empty()));
    }

    public void testPresent() {
        roundTrip(6, 6, new TestClass(Optional.of("foo")));
    }

    static class TestClass {
        Optional<String> maybe;
        public TestClass() {}
        public TestClass(Optional<String> maybe) {
            this.maybe = maybe;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestClass testClass = (TestClass) o;
            return Objects.equals(maybe, testClass.maybe);

        }

        @Override
        public int hashCode() {
            return Objects.hashCode(maybe);
        }
    }

}
