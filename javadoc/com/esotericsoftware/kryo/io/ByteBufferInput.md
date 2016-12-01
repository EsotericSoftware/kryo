#Class ByteBufferInput
Package [com.esotericsoftware.kryo.io](README.md)<br>

> *java.lang.Object* > *java.io.InputStream* > [Input](Input.md) > [ByteBufferInput](ByteBufferInput.md)

All implemented interfaces :
> *java.io.Closeable*

An InputStream that reads data from a byte array and optionally fills the byte array from another InputStream as needed.
 Utility methods are provided for efficiently reading primitive types and strings.


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
| **public** | [ByteBufferInput](#bytebufferinput)() |
| **public** | [ByteBufferInput](#bytebufferinputint)(**int** bufferSize) |
| **public** | [ByteBufferInput](#bytebufferinputbyte)(**byte** buffer) |
| **public** | [ByteBufferInput](#bytebufferinputbytebuffer)(*java.nio.ByteBuffer* buffer) |
| **public** | [ByteBufferInput](#bytebufferinputinputstream)(*java.io.InputStream* inputStream) |
| **public** | [ByteBufferInput](#bytebufferinputinputstream-int)(*java.io.InputStream* inputStream, **int** bufferSize) |
| **public** | [ByteBufferInput](#bytebufferinputlong-int)(**long** address, **int** size) |

####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **protected** **int** | [fill](#fillbytebuffer-int-int)(*java.nio.ByteBuffer* buffer, **int** offset, **int** count) |
| **public** *java.nio.ByteBuffer* | [getByteBuffer](#getbytebuffer)() |
| **public** **boolean** | [getVarIntsEnabled](#getvarintsenabled)() |
| **public** *java.nio.ByteOrder* | [order](#order)() |
| **public** **void** | [order](#orderbyteorder)(*java.nio.ByteOrder* byteOrder) |
| **public** **void** | [release](#release)() |
| **public** **void** | [setBuffer](#setbufferbytebuffer)(*java.nio.ByteBuffer* buffer) |
| **public** **void** | [setVarIntsEnabled](#setvarintsenabledboolean)(**boolean** varIntsEnabled) |

---


##Constructors
####ByteBufferInput()
> Creates an uninitialized Input. A buffer must be set before the Input is used.


---

####ByteBufferInput(int)
> Creates a new Input for reading from a byte array.

> **Parameters**
* bufferSize : The size of the buffer. An exception is thrown if more bytes than this are read.


---

####ByteBufferInput(byte[])
> 


---

####ByteBufferInput(ByteBuffer)
> Creates a new Input for reading from a ByteBuffer.


---

####ByteBufferInput(InputStream)
> Creates a new Input for reading from an InputStream with a buffer size of 4096.


---

####ByteBufferInput(InputStream, int)
> Creates a new Input for reading from an InputStream.


---

####ByteBufferInput(long, int)
> This constructor allows for creation of a direct ByteBuffer of a given size at a given address.
 
 
 Typical usage could look like this snippet:
 
 <pre>
 // Explicitly allocate memory
 long bufAddress = UnsafeUtil.unsafe().allocateMemory(4096);
 // Create a ByteBufferInput using the allocated memory region
 ByteBufferInput buffer = new ByteBufferInput(bufAddress, 4096);
 
 // Do some operations on this buffer here
 
 // Say that ByteBuffer won't be used anymore
 buffer.release();
 // Release the allocated region
 UnsafeUtil.unsafe().freeMemory(bufAddress);
 </pre>

> **Parameters**
* address : starting address of a memory region pre-allocated using Unsafe.allocateMemory()


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
####fill(ByteBuffer, int, int)
> Fills the buffer with more bytes. Can be overridden to fill the bytes from a source other than the InputStream.


---

####getByteBuffer()
> 


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
> Releases a direct buffer. [ByteBufferInput](ByteBufferInput.md) must be called before any write operations can be performed.


---

####setBuffer(ByteBuffer)
> Sets a new buffer, discarding any previous buffer. The byte order, position, limit and capacity are set to match the
 specified buffer. The total is reset. The [ByteBufferInput](ByteBufferInput.md) is set to null.


---

####setVarIntsEnabled(boolean)
> Controls if a variable length encoding for integer types should be used when serializers suggest it.

> **Parameters**
* varIntsEnabled : 


---

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)