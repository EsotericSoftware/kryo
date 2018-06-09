#Class UnsafeOutput
Package [com.esotericsoftware.kryo.io](README.md)<br>

> *java.lang.Object* > *java.io.OutputStream* > [Output](Output.md) > [UnsafeOutput](UnsafeOutput.md)

All implemented interfaces :
> *java.io.Flushable*, *java.io.Closeable*

An optimized OutputStream that buffers data in a byte array and optionally flushes to another OutputStream. Utility methods
 are provided for efficiently writing primitive types, arrays of primitive types and strings. It uses @link{sun.misc.Unsafe} to
 achieve a very good performance.
 
 
 Important notes:<br/>
 <li>This class increases performance, but may result in bigger size of serialized representation.</li>
 <li>Bulk operations, e.g. on arrays of primitive types, are always using native byte order.</li>
 <li>Fixed-size char, int, long, short, float and double elements are always written using native byte order.</li>
 <li>Best performance is achieved if no variable length encoding for integers is used.</li>
 <li>Output serialized using this class should always be deserilized using @link{UnsafeInput}</li>
 
 


##Summary
####Constructors
| Visibility | Signature |
| --- | --- |
| **public** | [UnsafeOutput](#unsafeoutput)() |
| **public** | [UnsafeOutput](#unsafeoutputint)(**int** bufferSize) |
| **public** | [UnsafeOutput](#unsafeoutputint-int)(**int** bufferSize, **int** maxBufferSize) |
| **public** | [UnsafeOutput](#unsafeoutputbyte)(**byte** buffer) |
| **public** | [UnsafeOutput](#unsafeoutputbyte-int)(**byte** buffer, **int** maxBufferSize) |
| **public** | [UnsafeOutput](#unsafeoutputoutputstream)(*java.io.OutputStream* outputStream) |
| **public** | [UnsafeOutput](#unsafeoutputoutputstream-int)(*java.io.OutputStream* outputStream, **int** bufferSize) |

####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **public** **boolean** | [supportVarInts](#supportvarints)() |
| **public** **void** | [supportVarInts](#supportvarintsboolean)(**boolean** supportVarInts) |
| **public final** **void** | [writeBytes](#writebytesobject-long-long)(*java.lang.Object* obj, **long** offset, **long** count) |

---


##Constructors
####UnsafeOutput()
> Creates an uninitialized Output. [UnsafeOutput](UnsafeOutput.md) must be called before the Output is used.


---

####UnsafeOutput(int)
> Creates a new Output for writing to a byte array.

> **Parameters**
* bufferSize : The initial and maximum size of the buffer. An exception is thrown if this size is exceeded.


---

####UnsafeOutput(int, int)
> Creates a new Output for writing to a byte array.

> **Parameters**
* bufferSize : The initial size of the buffer.
* maxBufferSize : The buffer is doubled as needed until it exceeds maxBufferSize and an exception is thrown.


---

####UnsafeOutput(byte[])
> Creates a new Output for writing to a byte array.


---

####UnsafeOutput(byte[], int)
> Creates a new Output for writing to a byte array.


---

####UnsafeOutput(OutputStream)
> Creates a new Output for writing to an OutputStream. A buffer size of 4096 is used.


---

####UnsafeOutput(OutputStream, int)
> Creates a new Output for writing to an OutputStream.


---


##Methods
####supportVarInts()
> Return current setting for variable length encoding of integers

> **Returns**
* current setting for variable length encoding of integers


---

####supportVarInts(boolean)
> Controls if a variable length encoding for integer types should be used when serializers suggest it.

> **Parameters**
* supportVarInts : 


---

####writeBytes(Object, long, long)
> Output count bytes from a memory region starting at the given #{offset} inside the in-memory representation of obj object.

> **Parameters**
* obj : 
* offset : 
* count : 


---

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)