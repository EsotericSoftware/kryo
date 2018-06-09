#Class FastOutput
Package [com.esotericsoftware.kryo.io](README.md)<br>

> *java.lang.Object* > *java.io.OutputStream* > [Output](Output.md) > [FastOutput](FastOutput.md)

All implemented interfaces :
> *java.io.Flushable*, *java.io.Closeable*

Same as Output, but does not use variable length encoding for integer types.


##Summary
####Constructors
| Visibility | Signature |
| --- | --- |
| **public** | [FastOutput](#fastoutput)() |
| **public** | [FastOutput](#fastoutputint)(**int** bufferSize) |
| **public** | [FastOutput](#fastoutputint-int)(**int** bufferSize, **int** maxBufferSize) |
| **public** | [FastOutput](#fastoutputbyte)(**byte** buffer) |
| **public** | [FastOutput](#fastoutputbyte-int)(**byte** buffer, **int** maxBufferSize) |
| **public** | [FastOutput](#fastoutputoutputstream)(*java.io.OutputStream* outputStream) |
| **public** | [FastOutput](#fastoutputoutputstream-int)(*java.io.OutputStream* outputStream, **int** bufferSize) |

####Methods
| Type and modifiers | Method signature |
| --- | --- |

---


##Constructors
####FastOutput()
> Creates an uninitialized Output. [FastOutput](FastOutput.md) must be called before the Output is used.


---

####FastOutput(int)
> Creates a new Output for writing to a byte array.

> **Parameters**
* bufferSize : The initial and maximum size of the buffer. An exception is thrown if this size is exceeded.


---

####FastOutput(int, int)
> Creates a new Output for writing to a byte array.

> **Parameters**
* bufferSize : The initial size of the buffer.
* maxBufferSize : The buffer is doubled as needed until it exceeds maxBufferSize and an exception is thrown.


---

####FastOutput(byte[])
> Creates a new Output for writing to a byte array.


---

####FastOutput(byte[], int)
> Creates a new Output for writing to a byte array.


---

####FastOutput(OutputStream)
> Creates a new Output for writing to an OutputStream. A buffer size of 4096 is used.


---

####FastOutput(OutputStream, int)
> Creates a new Output for writing to an OutputStream.


---


##Methods
---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)