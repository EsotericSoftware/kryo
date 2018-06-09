#Class ByteBufferInputStream
Package [com.esotericsoftware.kryo.io](README.md)<br>

> *java.lang.Object* > *java.io.InputStream* > [ByteBufferInputStream](ByteBufferInputStream.md)

All implemented interfaces :
> *java.io.Closeable*

An InputStream whose source is a *java.nio.ByteBuffer*.


##Summary
####Constructors
| Visibility | Signature |
| --- | --- |
| **public** | [ByteBufferInputStream](#bytebufferinputstream)() |
| **public** | [ByteBufferInputStream](#bytebufferinputstreamint)(**int** bufferSize) |
| **public** | [ByteBufferInputStream](#bytebufferinputstreambytebuffer)(*java.nio.ByteBuffer* byteBuffer) |

####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **public** *java.nio.ByteBuffer* | [getByteBuffer](#getbytebuffer)() |
| **public** **void** | [setByteBuffer](#setbytebufferbytebuffer)(*java.nio.ByteBuffer* byteBuffer) |

---


##Constructors
####ByteBufferInputStream()
> 


---

####ByteBufferInputStream(int)
> Creates a stream with a new non-direct buffer of the specified size. The position and limit of the buffer is zero.


---

####ByteBufferInputStream(ByteBuffer)
> Creates an uninitialized stream that cannot be used until [ByteBufferInputStream](ByteBufferInputStream.md) is called.


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