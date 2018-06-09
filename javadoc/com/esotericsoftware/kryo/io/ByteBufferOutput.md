#Class ByteBufferOutput
Package [com.esotericsoftware.kryo.io](README.md)<br>

> *java.lang.Object* > *java.io.OutputStream* > [Output](Output.md) > [ByteBufferOutput](ByteBufferOutput.md)

All implemented interfaces :
> *java.io.Flushable*, *java.io.Closeable*

An OutputStream that buffers data in a byte array and optionally flushes to another OutputStream. Utility methods are provided
 for efficiently writing primitive types and strings.


##Summary
####Fields
| Type and modifiers | Field name |
| --- | --- |
| **protected static final** | [nativeOrder](#nativeorder) |
| **protected** | [niobuffer](#niobuffer) |
| **protected** | [varIntsEnabled](#varintsenabled) |

####Constructors
| Visibility | Signature |
| --- | --- |
| **public** | [ByteBufferOutput](#bytebufferoutput)() |
| **public** | [ByteBufferOutput](#bytebufferoutputint)(**int** bufferSize) |
| **public** | [ByteBufferOutput](#bytebufferoutputint-int)(**int** bufferSize, **int** maxBufferSize) |
| **public** | [ByteBufferOutput](#bytebufferoutputoutputstream)(*java.io.OutputStream* outputStream) |
| **public** | [ByteBufferOutput](#bytebufferoutputoutputstream-int)(*java.io.OutputStream* outputStream, **int** bufferSize) |
| **public** | [ByteBufferOutput](#bytebufferoutputbytebuffer)(*java.nio.ByteBuffer* buffer) |
| **public** | [ByteBufferOutput](#bytebufferoutputbytebuffer-int)(*java.nio.ByteBuffer* buffer, **int** maxBufferSize) |
| **public** | [ByteBufferOutput](#bytebufferoutputlong-int)(**long** address, **int** maxBufferSize) |

####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **public** *java.nio.ByteBuffer* | [getByteBuffer](#getbytebuffer)() |
| **public** **boolean** | [getVarIntsEnabled](#getvarintsenabled)() |
| **public** *java.nio.ByteOrder* | [order](#order)() |
| **public** **void** | [order](#orderbyteorder)(*java.nio.ByteOrder* byteOrder) |
| **public** **void** | [release](#release)() |
| **public** **void** | [setBuffer](#setbufferbytebuffer)(*java.nio.ByteBuffer* buffer) |
| **public** **void** | [setBuffer](#setbufferbytebuffer-int)(*java.nio.ByteBuffer* buffer, **int** maxBufferSize) |
| **public** **void** | [setVarIntsEnabled](#setvarintsenabledboolean)(**boolean** varIntsEnabled) |
| **public** **int** | [writeLongS](#writelongslong-boolean)(**long** value, **boolean** optimizePositive) |

---


##Constructors
####ByteBufferOutput()
> Creates an uninitialized Output. A buffer must be set before the Output is used.


---

####ByteBufferOutput(int)
> Creates a new Output for writing to a direct ByteBuffer.

> **Parameters**
* bufferSize : The initial and maximum size of the buffer. An exception is thrown if this size is exceeded.


---

####ByteBufferOutput(int, int)
> Creates a new Output for writing to a direct ByteBuffer.

> **Parameters**
* bufferSize : The initial size of the buffer.
* maxBufferSize : The buffer is doubled as needed until it exceeds maxBufferSize and an exception is thrown.


---

####ByteBufferOutput(OutputStream)
> Creates a new Output for writing to an OutputStream. A buffer size of 4096 is used.


---

####ByteBufferOutput(OutputStream, int)
> Creates a new Output for writing to an OutputStream.


---

####ByteBufferOutput(ByteBuffer)
> Creates a new Output for writing to a ByteBuffer.


---

####ByteBufferOutput(ByteBuffer, int)
> Creates a new Output for writing to a ByteBuffer.

> **Parameters**
* maxBufferSize : The buffer is doubled as needed until it exceeds maxCapacity and an exception is thrown.


---

####ByteBufferOutput(long, int)
> Creates a direct ByteBuffer of a given size at a given address.
 
 Typical usage could look like this snippet:
 
 <pre>
 // Explicitly allocate memory
 long bufAddress = UnsafeUtil.unsafe().allocateMemory(4096);
 // Create a ByteBufferOutput using the allocated memory region
 ByteBufferOutput buffer = new ByteBufferOutput(bufAddress, 4096);
 
 // Do some operations on this buffer here
 
 // Say that ByteBuffer won't be used anymore
 buffer.release();
 // Release the allocated region
 UnsafeUtil.unsafe().freeMemory(bufAddress);
 </pre>

> **Parameters**
* address : starting address of a memory region pre-allocated using Unsafe.allocateMemory()
* maxBufferSize : 


---


##Fields
####niobuffer
> **protected** *java.nio.ByteBuffer*

> 

---

####varIntsEnabled
> **protected** **boolean**

> 

---

####nativeOrder
> **protected static final** *java.nio.ByteOrder*

> 

---


##Methods
####getByteBuffer()
> Returns the buffer. The bytes between zero and [ByteBufferOutput](ByteBufferOutput.md) are the data that has been written.


---

####getVarIntsEnabled()
> Return current setting for variable length encoding of integers

> **Returns**
* current setting for variable length encoding of integers


---

####order()
> 


---

####order(ByteOrder)
> 


---

####release()
> Release a direct buffer. [ByteBufferOutput](ByteBufferOutput.md) should be called before next write operations can be called.
 
 NOTE: If Cleaner is not accessible due to SecurityManager restrictions, reflection could be used to obtain the "clean"
 method and then invoke it.


---

####setBuffer(ByteBuffer)
> Sets the buffer that will be written to. maxCapacity is set to the specified buffer's capacity.


---

####setBuffer(ByteBuffer, int)
> Sets the buffer that will be written to. The byte order, position and capacity are set to match the specified buffer. The
 total is set to 0. The [ByteBufferOutput](ByteBufferOutput.md) is set to null.

> **Parameters**
* maxBufferSize : The buffer is doubled as needed until it exceeds maxCapacity and an exception is thrown.


---

####setVarIntsEnabled(boolean)
> Controls if a variable length encoding for integer types should be used when serializers suggest it.

> **Parameters**
* varIntsEnabled : 


---

####writeLongS(long, boolean)
> Writes a 1-9 byte long.

> **Parameters**
* optimizePositive : If true, small positive numbers will be more efficient (1 byte) and small negative numbers will be
           inefficient (9 bytes).


---

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)