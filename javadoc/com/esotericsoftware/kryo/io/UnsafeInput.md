#Class UnsafeInput
Package [com.esotericsoftware.kryo.io](README.md)<br>

> *java.lang.Object* > *java.io.InputStream* > [Input](Input.md) > [UnsafeInput](UnsafeInput.md)

All implemented interfaces :
> *java.io.Closeable*

An optimized InputStream that reads data from a byte array and optionally fills the byte array from another InputStream as
 needed. Utility methods are provided for efficiently writing primitive types, arrays of primitive types and strings. It uses


##Summary
####Constructors
| Visibility | Signature |
| --- | --- |
| **public** | [UnsafeInput](#unsafeinput)() |
| **public** | [UnsafeInput](#unsafeinputint)(**int** bufferSize) |
| **public** | [UnsafeInput](#unsafeinputbyte)(**byte** buffer) |
| **public** | [UnsafeInput](#unsafeinputbyte-int-int)(**byte** buffer, **int** offset, **int** count) |
| **public** | [UnsafeInput](#unsafeinputinputstream)(*java.io.InputStream* inputStream) |
| **public** | [UnsafeInput](#unsafeinputinputstream-int)(*java.io.InputStream* inputStream, **int** bufferSize) |

####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **public** **boolean** | [getVarIntsEnabled](#getvarintsenabled)() |
| **public final** **void** | [readBytes](#readbytesobject-long-long)(*java.lang.Object* dstObj, **long** offset, **long** count) |
| **public** **void** | [setVarIntsEnabled](#setvarintsenabledboolean)(**boolean** varIntsEnabled) |

---


##Constructors
####UnsafeInput()
> Creates an uninitialized Input. [UnsafeInput](UnsafeInput.md) must be called before the Input is used.


---

####UnsafeInput(int)
> Creates a new Input for reading from a byte array.

> **Parameters**
* bufferSize : The initial and maximum size of the buffer. An exception is thrown if this size is exceeded.


---

####UnsafeInput(byte[])
> Creates a new Input for reading from a byte array.


---

####UnsafeInput(byte[], int, int)
> Creates a new Input for reading from a byte array.


---

####UnsafeInput(InputStream)
> Creates a new Input for reading from an InputStream. A buffer size of 4096 is used.


---

####UnsafeInput(InputStream, int)
> Creates a new Input for reading from an InputStream.


---


##Methods
####getVarIntsEnabled()
> Return current setting for variable length encoding of integers

> **Returns**
* current setting for variable length encoding of integers


---

####readBytes(Object, long, long)
> 


---

####setVarIntsEnabled(boolean)
> Controls if a variable length encoding for integer types should be used when serializers suggest it.

> **Parameters**
* varIntsEnabled : 


---

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)