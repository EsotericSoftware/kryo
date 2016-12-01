#Interface StreamFactory
Package [com.esotericsoftware.kryo](README.md)<br>

> [StreamFactory](StreamFactory.md)



Provides input and output streams based on system settings.


##Summary
####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **public** [Input](io/Input.md) | [getInput](#getinput)() |
| **public** [Input](io/Input.md) | [getInput](#getinputint)(**int** bufferSize) |
| **public** [Input](io/Input.md) | [getInput](#getinputbyte)(**byte** buffer) |
| **public** [Input](io/Input.md) | [getInput](#getinputbyte-int-int)(**byte** buffer, **int** offset, **int** count) |
| **public** [Input](io/Input.md) | [getInput](#getinputinputstream)(*java.io.InputStream* inputStream) |
| **public** [Input](io/Input.md) | [getInput](#getinputinputstream-int)(*java.io.InputStream* inputStream, **int** bufferSize) |
| **public** [Output](io/Output.md) | [getOutput](#getoutput)() |
| **public** [Output](io/Output.md) | [getOutput](#getoutputint)(**int** bufferSize) |
| **public** [Output](io/Output.md) | [getOutput](#getoutputint-int)(**int** bufferSize, **int** maxBufferSize) |
| **public** [Output](io/Output.md) | [getOutput](#getoutputbyte)(**byte** buffer) |
| **public** [Output](io/Output.md) | [getOutput](#getoutputbyte-int)(**byte** buffer, **int** maxBufferSize) |
| **public** [Output](io/Output.md) | [getOutput](#getoutputoutputstream)(*java.io.OutputStream* outputStream) |
| **public** [Output](io/Output.md) | [getOutput](#getoutputoutputstream-int)(*java.io.OutputStream* outputStream, **int** bufferSize) |
| **public** **void** | [setKryo](#setkryokryo)([Kryo](Kryo.md) kryo) |

---


##Methods
####getInput()
> Creates an uninitialized Input.


---

####getInput(int)
> Creates a new Input for reading from a byte array.

> **Parameters**
* bufferSize : The size of the buffer. An exception is thrown if more bytes than this are read.


---

####getInput(byte[])
> Creates a new Input for reading from a byte array.

> **Parameters**
* buffer : An exception is thrown if more bytes than this are read.


---

####getInput(byte[], int, int)
> Creates a new Input for reading from a byte array.

> **Parameters**
* buffer : An exception is thrown if more bytes than this are read.


---

####getInput(InputStream)
> Creates a new Input for reading from an InputStream with a buffer size of 4096.


---

####getInput(InputStream, int)
> Creates a new Input for reading from an InputStream.


---

####getOutput()
> Creates an uninitialized Output. [Output](io/Output.md) must be called before the Output is used.


---

####getOutput(int)
> Creates a new Output for writing to a byte array.

> **Parameters**
* bufferSize : The initial and maximum size of the buffer. An exception is thrown if this size is exceeded.


---

####getOutput(int, int)
> Creates a new Output for writing to a byte array.

> **Parameters**
* bufferSize : The initial size of the buffer.
* maxBufferSize : The buffer is doubled as needed until it exceeds maxBufferSize and an exception is thrown. Can be -1
           for no maximum.


---

####getOutput(byte[])
> Creates a new Output for writing to a byte array.


---

####getOutput(byte[], int)
> Creates a new Output for writing to a byte array.


---

####getOutput(OutputStream)
> Creates a new Output for writing to an OutputStream. A buffer size of 4096 is used.


---

####getOutput(OutputStream, int)
> Creates a new Output for writing to an OutputStream.


---

####setKryo(Kryo)
> 


---

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)