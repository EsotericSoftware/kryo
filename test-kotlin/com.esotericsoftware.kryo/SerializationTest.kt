package com.esotericsoftware.kryo

import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Test

class SerializationTest {

    // https://github.com/EsotericSoftware/kryo/issues/864
    @Test
    fun testLambda() {
        val kryo = Kryo().apply {
            isRegistrationRequired = false
        }

        val example = Example()

        val bytes = Output(1024).use { output ->
            kryo.writeClassAndObject(output, example)
            output.toBytes()
        }

        val deserialized = Input(bytes).use { input ->
            kryo.readClassAndObject(input)
        }

        assertEquals(example::class.java, deserialized::class.java)
        assertNotSame(example, deserialized)
    }

    class Example(private val p: (Long) -> String) {
        constructor() : this(@JvmSerializableLambda { it.toString() })
    }
}

