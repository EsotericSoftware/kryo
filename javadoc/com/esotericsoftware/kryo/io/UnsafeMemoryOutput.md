#Class UnsafeMemoryOutput
Package [com.esotericsoftware.kryo.io](README.md)<br>

> *java.lang.Object* > *java.io.OutputStream* > [Output](Output.md) > [ByteBufferOutput](ByteBufferOutput.md) > [UnsafeMemoryOutput](UnsafeMemoryOutput.md)

All implemented interfaces :
> *java.io.Flushable*, *java.io.Closeable*

An optimized OutputStream that writes data directly into the off-heap memory. Utility methods are provided for efficiently
 writing primitive types, arrays of primitive types and strings. It uses @link{sun.misc.Unsafe} to achieve a very good
 performance.
 
 
 Important notes:<br/>
 <li>This class increases performance, but may result in bigger size of serialized representation.</li>
 <li>Bulk operations, e.g. on arrays of primitive types, are always using native byte order.</li>
 <li>Fixed-size char, int, long, short, float and double elements are always written using native byte order.</li>
 <li>Best performance is achieved if no variable length encoding for integers is used.</li>
 <li>Output serialized using this class should always be deserilized using @link{UnsafeMemoryInput}</li>
 
 


##Summary
####Constructors
| Visibility | Signature |
| --- | --- |
| **public** | [UnsafeMemoryOutput](#unsafememoryoutput)() |
| **public** | [UnsafeMemoryOutput](#unsafememoryoutputint)(**int** bufferSize) |
| **public** | [UnsafeMemoryOutput](#unsafememoryoutputint-int)(**int** bufferSize, **int** maxBufferSize) |
| **public** | [UnsafeMemoryOutput](#unsafememoryoutputoutputstream)(*java.io.OutputStream* outputStream) |
| **public** | [UnsafeMemoryOutput](#unsafememoryoutputoutputstream-int)(*java.io.OutputStream* outputStream, **int** bufferSize) |
| **public** | [UnsafeMemoryOutput](#unsafememoryoutputlong-int)(**long** address, **int** maxBufferSize) |

####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **public final** **void** | [writeBytes](#writebytesobject-long-long)(*java.lang.Object* obj, **long** offset, **long** count) |

---


##Constructors
####UnsafeMemoryOutput()
> Creates an uninitialized Output. [UnsafeMemoryOutput](UnsafeMemoryOutput.md) must be called before the Output is used.


---

####UnsafeMemoryOutput(int)
> Creates a new Output for writing to a byte array.

> **Parameters**
* bufferSize : The initial and maximum size of the buffer. An exception is thrown if this size is exceeded.


---

####UnsafeMemoryOutput(int, int)
> Creates a new Output for writing to a byte array.

> **Parameters**
* bufferSize : The initial size of the buffer.
* maxBufferSize : The buffer is doubled as needed until it exceeds maxBufferSize and an exception is thrown.


---

####UnsafeMemoryOutput(OutputStream)
> Creates a new Output for writing to an OutputStream. A buffer size of 4096 is used.


---

####UnsafeMemoryOutput(OutputStream, int)
> Creates a new Output for writing to an OutputStream.


---

####UnsafeMemoryOutput(long, int)
> 


---


##Methods
####writeBytes(Object, long, long)
> Output count bytes from a memory region starting at the given #{offset} inside the in-memory representation of obj object.

> **Parameters**
* obj : 
* offset : 
* count : 


---

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)