#Class DefaultStreamFactory
Package [com.esotericsoftware.kryo.util](README.md)<br>

> *java.lang.Object* > [DefaultStreamFactory](DefaultStreamFactory.md)

All implemented interfaces :
> [StreamFactory](../StreamFactory.md)

StreamFactory which provides usual Input/Output streams, which are present in all versions of Kryo.


##Summary
####Constructors
| Visibility | Signature |
| --- | --- |
| **public** | [DefaultStreamFactory](#defaultstreamfactory)() |

####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **public** [Input](../io/Input.md) | [getInput](#getinput)() |
| **public** [Input](../io/Input.md) | [getInput](#getinputint)(**int** bufferSize) |
| **public** [Input](../io/Input.md) | [getInput](#getinputbyte)(**byte** buffer) |
| **public** [Input](../io/Input.md) | [getInput](#getinputbyte-int-int)(**byte** buffer, **int** offset, **int** count) |
| **public** [Input](../io/Input.md) | [getInput](#getinputinputstream)(*java.io.InputStream* inputStream) |
| **public** [Input](../io/Input.md) | [getInput](#getinputinputstream-int)(*java.io.InputStream* inputStream, **int** bufferSize) |
| **public** [Output](../io/Output.md) | [getOutput](#getoutput)() |
| **public** [Output](../io/Output.md) | [getOutput](#getoutputint)(**int** bufferSize) |
| **public** [Output](../io/Output.md) | [getOutput](#getoutputint-int)(**int** bufferSize, **int** maxBufferSize) |
| **public** [Output](../io/Output.md) | [getOutput](#getoutputbyte)(**byte** buffer) |
| **public** [Output](../io/Output.md) | [getOutput](#getoutputbyte-int)(**byte** buffer, **int** maxBufferSize) |
| **public** [Output](../io/Output.md) | [getOutput](#getoutputoutputstream)(*java.io.OutputStream* outputStream) |
| **public** [Output](../io/Output.md) | [getOutput](#getoutputoutputstream-int)(*java.io.OutputStream* outputStream, **int** bufferSize) |
| **public** **void** | [setKryo](#setkryokryo)([Kryo](../Kryo.md) kryo) |

---


##Constructors
####DefaultStreamFactory()
> 


---


##Methods
####getInput()
> 


---

####getInput(int)
> 


---

####getInput(byte[])
> 


---

####getInput(byte[], int, int)
> 


---

####getInput(InputStream)
> 


---

####getInput(InputStream, int)
> 


---

####getOutput()
> 


---

####getOutput(int)
> 


---

####getOutput(int, int)
> 


---

####getOutput(byte[])
> 


---

####getOutput(byte[], int)
> 


---

####getOutput(OutputStream)
> 


---

####getOutput(OutputStream, int)
> 


---

####setKryo(Kryo)
> 


---

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)