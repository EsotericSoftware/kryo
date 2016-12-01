#Class UnsafeMemoryInput
Package [com.esotericsoftware.kryo.io](README.md)<br>

> *java.lang.Object* > *java.io.InputStream* > [Input](Input.md) > [ByteBufferInput](ByteBufferInput.md) > [UnsafeMemoryInput](UnsafeMemoryInput.md)

All implemented interfaces :
> *java.io.Closeable*

An optimized InputStream that reads data directly from the off-heap memory. Utility methods are provided for efficiently
 reading primitive types, arrays of primitive types and strings. It uses @link{sun.misc.Unsafe} to achieve a very good
 performance.
 
 
 Important notes:<br/>
 <li>Bulk operations, e.g. on arrays of primitive types, are always using native byte order.</li>
 <li>Fixed-size char, int, long, short, float and double elements are always read using native byte order.</li>
 <li>Best performance is achieved if no variable length encoding for integers is used.</li>
 <li>Serialized representation used as input for this class should always be produced using @link{UnsafeMemoryOutput}</li>
 


##Summary
####Constructors
| Visibility | Signature |
| --- | --- |
| **public** | [UnsafeMemoryInput](#unsafememoryinput)() |
| **public** | [UnsafeMemoryInput](#unsafememoryinputint)(**int** bufferSize) |
| **public** | [UnsafeMemoryInput](#unsafememoryinputbyte)(**byte** buffer) |
| **public** | [UnsafeMemoryInput](#unsafememoryinputbytebuffer)(*java.nio.ByteBuffer* buffer) |
| **public** | [UnsafeMemoryInput](#unsafememoryinputlong-int)(**long** address, **int** maxBufferSize) |
| **public** | [UnsafeMemoryInput](#unsafememoryinputinputstream)(*java.io.InputStream* inputStream) |
| **public** | [UnsafeMemoryInput](#unsafememoryinputinputstream-int)(*java.io.InputStream* inputStream, **int** bufferSize) |

####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **public final** **void** | [readBytes](#readbytesobject-long-long)(*java.lang.Object* dstObj, **long** offset, **long** count) |

---


##Constructors
####UnsafeMemoryInput()
> Creates an uninitialized Input. [UnsafeMemoryInput](UnsafeMemoryInput.md) must be called before the Input is used.


---

####UnsafeMemoryInput(int)
> Creates a new Input for reading from a byte array.

> **Parameters**
* bufferSize : The initial and maximum size of the buffer. An exception is thrown if this size is exceeded.


---

####UnsafeMemoryInput(byte[])
> Creates a new Input for reading from a byte array.


---

####UnsafeMemoryInput(ByteBuffer)
> 


---

####UnsafeMemoryInput(long, int)
> 


---

####UnsafeMemoryInput(InputStream)
> Creates a new Input for reading from an InputStream. A buffer size of 4096 is used.


---

####UnsafeMemoryInput(InputStream, int)
> Creates a new Input for reading from an InputStream.


---


##Methods
####readBytes(Object, long, long)
> 


---

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)