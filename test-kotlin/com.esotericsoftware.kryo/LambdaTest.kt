package com.esotericsoftware.kryo

import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class LambdaTest {
    class Example(private val p: (Long) -> String) {
        constructor() : this({ it.toString() })
    }

    @Test
    @Disabled("Expected to fail")
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
}

