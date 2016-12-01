#Class KryoObjectOutput
Package [com.esotericsoftware.kryo.io](README.md)<br>

> *java.lang.Object* > [KryoDataOutput](KryoDataOutput.md) > [KryoObjectOutput](KryoObjectOutput.md)

All implemented interfaces :
> *java.io.DataOutput*, *java.io.ObjectOutput*

A kryo adapter for the *java.io.ObjectOutput* class. Note that this is not a Kryo implementation of
 *java.io.ObjectOutputStream* which has special handling for default serialization and serialization extras like
 writeReplace. By default it will simply delegate to the appropriate kryo method. Also, using it will currently add one extra
 byte for each time [KryoObjectOutput](KryoObjectOutput.md) is invoked since we need to allow unknown null objects.


##Summary
####Constructors
| Visibility | Signature |
| --- | --- |
| **public** | [KryoObjectOutput](#kryoobjectoutputkryo-output)([Kryo](../Kryo.md) kryo, [Output](Output.md) output) |

####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **public** **void** | [close](#close)() |
| **public** **void** | [flush](#flush)() |
| **public** **void** | [writeObject](#writeobjectobject)(*java.lang.Object* obj) |

---


##Constructors
####KryoObjectOutput(Kryo, Output)
> 


---


##Methods
####close()
> 


---

####flush()
> 


---

####writeObject(Object)
> 


---

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)