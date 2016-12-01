#Class KryoException
Package [com.esotericsoftware.kryo](README.md)<br>

> *java.lang.Object* > *java.lang.Throwable* > *java.lang.Exception* > *java.lang.RuntimeException* > [KryoException](KryoException.md)

All implemented interfaces :
> *java.io.Serializable*

General Kryo RuntimeException.


##Summary
####Constructors
| Visibility | Signature |
| --- | --- |
| **public** | [KryoException](#kryoexception)() |
| **public** | [KryoException](#kryoexceptionstring-throwable)(*java.lang.String* message, *java.lang.Throwable* cause) |
| **public** | [KryoException](#kryoexceptionstring)(*java.lang.String* message) |
| **public** | [KryoException](#kryoexceptionthrowable)(*java.lang.Throwable* cause) |

####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **public** **void** | [addTrace](#addtracestring)(*java.lang.String* info) |

---


##Constructors
####KryoException()
> 


---

####KryoException(String, Throwable)
> 


---

####KryoException(String)
> 


---

####KryoException(Throwable)
> 


---


##Methods
####addTrace(String)
> Adds information to the exception message about where in the the object graph serialization failure occurred.
 [Serializer](Serializer.md) can catch [KryoException](KryoException.md), add trace information, and rethrow the exception.


---

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)