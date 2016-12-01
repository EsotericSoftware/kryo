#Class FastInput
Package [com.esotericsoftware.kryo.io](README.md)<br>

> *java.lang.Object* > *java.io.InputStream* > [Input](Input.md) > [FastInput](FastInput.md)

All implemented interfaces :
> *java.io.Closeable*

Same as Input, but does not use variable length encoding for integer types.


##Summary
####Constructors
| Visibility | Signature |
| --- | --- |
| **public** | [FastInput](#fastinput)() |
| **public** | [FastInput](#fastinputint)(**int** bufferSize) |
| **public** | [FastInput](#fastinputbyte)(**byte** buffer) |
| **public** | [FastInput](#fastinputbyte-int-int)(**byte** buffer, **int** offset, **int** count) |
| **public** | [FastInput](#fastinputinputstream)(*java.io.InputStream* outputStream) |
| **public** | [FastInput](#fastinputinputstream-int)(*java.io.InputStream* outputStream, **int** bufferSize) |

####Methods
| Type and modifiers | Method signature |
| --- | --- |

---


##Constructors
####FastInput()
> Creates an uninitialized Output. [FastInput](FastInput.md) must be called before the Output is used.


---

####FastInput(int)
> Creates a new Output for writing to a byte array.

> **Parameters**
* bufferSize : The initial and maximum size of the buffer. An exception is thrown if this size is exceeded.


---

####FastInput(byte[])
> Creates a new Output for writing to a byte array.


---

####FastInput(byte[], int, int)
> Creates a new Output for writing to a byte array.


---

####FastInput(InputStream)
> Creates a new Output for writing to an OutputStream. A buffer size of 4096 is used.


---

####FastInput(InputStream, int)
> Creates a new Output for writing to an OutputStream.


---


##Methods
---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)