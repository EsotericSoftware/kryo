#Class OutputChunked
Package [com.esotericsoftware.kryo.io](README.md)<br>

> *java.lang.Object* > *java.io.OutputStream* > [Output](Output.md) > [OutputChunked](OutputChunked.md)

All implemented interfaces :
> *java.io.Flushable*, *java.io.Closeable*

An OutputStream that buffers data in a byte array and flushes to another OutputStream, writing the length before each flush.
 The length allows the chunks to be skipped when reading.


##Summary
####Constructors
| Visibility | Signature |
| --- | --- |
| **public** | [OutputChunked](#outputchunked)() |
| **public** | [OutputChunked](#outputchunkedint)(**int** bufferSize) |
| **public** | [OutputChunked](#outputchunkedoutputstream)(*java.io.OutputStream* outputStream) |
| **public** | [OutputChunked](#outputchunkedoutputstream-int)(*java.io.OutputStream* outputStream, **int** bufferSize) |

####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **public** **void** | [endChunks](#endchunks)() |

---


##Constructors
####OutputChunked()
> Creates an uninitialized OutputChunked with a maximum chunk size of 2048. The OutputStream must be set before it can be
 used.


---

####OutputChunked(int)
> Creates an uninitialized OutputChunked. The OutputStream must be set before it can be used.

> **Parameters**
* bufferSize : The maximum size of a chunk.


---

####OutputChunked(OutputStream)
> Creates an OutputChunked with a maximum chunk size of 2048.


---

####OutputChunked(OutputStream, int)
> 

> **Parameters**
* bufferSize : The maximum size of a chunk.


---


##Methods
####endChunks()
> Marks the end of some data that may have been written by any number of chunks. These chunks can then be skipped when
 reading.


---

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)