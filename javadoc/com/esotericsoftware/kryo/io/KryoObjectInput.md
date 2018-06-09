#Class KryoObjectInput
Package [com.esotericsoftware.kryo.io](README.md)<br>

> *java.lang.Object* > [KryoDataInput](KryoDataInput.md) > [KryoObjectInput](KryoObjectInput.md)

All implemented interfaces :
> *java.io.ObjectInput*, *java.io.DataInput*

A kryo implementation of *java.io.ObjectInput*. Note that this is not an implementation of *java.io.ObjectInputStream*
 which has special handling for serialization in Java such as support for readResolve.


##Summary
####Constructors
| Visibility | Signature |
| --- | --- |
| **public** | [KryoObjectInput](#kryoobjectinputkryo-input)([Kryo](../Kryo.md) kryo, [Input](Input.md) in) |

####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **public** **int** | [available](#available)() |
| **public** **void** | [close](#close)() |
| **public** **int** | [read](#read)() |
| **public** **int** | [read](#readbyte)(**byte** b) |
| **public** **int** | [read](#readbyte-int-int)(**byte** b, **int** off, **int** len) |
| **public** *java.lang.Object* | [readObject](#readobject)() |
| **public** **long** | [skip](#skiplong)(**long** n) |

---


##Constructors
####KryoObjectInput(Kryo, Input)
> 


---


##Methods
####available()
> 


---

####close()
> 


---

####read()
> 


---

####read(byte[])
> 


---

####read(byte[], int, int)
> 


---

####readObject()
> 


---

####skip(long)
> 


---

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)