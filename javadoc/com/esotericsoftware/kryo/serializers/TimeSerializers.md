#Class TimeSerializers
Package [com.esotericsoftware.kryo.serializers](README.md)<br>

> *java.lang.Object* > [TimeSerializers](TimeSerializers.md)



Serializers for java.time.*, are added as default serializers if java version is >= 8.

 Serializers are all private for now because they're not expected to be somehow used/extended/accessed by the user. If there
 should be a case where this is needed it can be changed - for now the public api should be kept as spall as possible.

 Implementation note: All serialization is inspired by oracles java.time.Ser.


##Summary
####Constructors
| Visibility | Signature |
| --- | --- |
| **public** | [TimeSerializers](#timeserializers)() |

####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **public static** **void** | [addDefaultSerializers](#adddefaultserializerskryo)([Kryo](../Kryo.md) kryo) |

---


##Constructors
####TimeSerializers()
> 


---


##Methods
####addDefaultSerializers(Kryo)
> 


---

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)