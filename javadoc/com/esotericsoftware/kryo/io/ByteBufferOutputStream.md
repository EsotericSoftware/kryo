#Class ByteBufferOutputStream
Package [com.esotericsoftware.kryo.io](README.md)<br>

> *java.lang.Object* > *java.io.OutputStream* > [ByteBufferOutputStream](ByteBufferOutputStream.md)

All implemented interfaces :
> *java.io.Flushable*, *java.io.Closeable*

An OutputStream whose target is a *java.nio.ByteBuffer*. If bytes would be written that would overflow the buffer,
 [ByteBufferOutputStream](ByteBufferOutputStream.md) is called. Subclasses can override flush to empty the buffer.


##Summary
####Constructors
| Visibility | Signature |
| --- | --- |
| **public** | [ByteBufferOutputStream](#bytebufferoutputstream)() |
| **public** | [ByteBufferOutputStream](#bytebufferoutputstreamint)(**int** bufferSize) |
| **public** | [ByteBufferOutputStream](#bytebufferoutputstreambytebuffer)(*java.nio.ByteBuffer* byteBuffer) |

####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **public** *java.nio.ByteBuffer* | [getByteBuffer](#getbytebuffer)() |
| **public** **void** | [setByteBuffer](#setbytebufferbytebuffer)(*java.nio.ByteBuffer* byteBuffer) |

---


##Constructors
####ByteBufferOutputStream()
> Creates an uninitialized stream that cannot be used until [ByteBufferOutputStream](ByteBufferOutputStream.md) is called.


---

####ByteBufferOutputStream(int)
> Creates a stream with a new non-direct buffer of the specified size.


---

####ByteBufferOutputStream(ByteBuffer)
> 


---


##Methods
####getByteBuffer()
> 


---

####setByteBuffer(ByteBuffer)
> 


---

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)