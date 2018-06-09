#Interface KryoSerializable
Package [com.esotericsoftware.kryo](README.md)<br>

> [KryoSerializable](KryoSerializable.md)



Allows implementing classes to perform their own serialization. Hand written serialization can be more efficient in some
 cases.
 
 The default serializer for KryoSerializable is [KryoSerializableSerializer](serializers/KryoSerializableSerializer.md), which uses [Kryo](Kryo.md)
 to construct the class.


##Summary
####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **public** **void** | [read](#readkryo-input)([Kryo](Kryo.md) kryo, [Input](io/Input.md) input) |
| **public** **void** | [write](#writekryo-output)([Kryo](Kryo.md) kryo, [Output](io/Output.md) output) |

---


##Methods
####read(Kryo, Input)
> 


---

####write(Kryo, Output)
> 


---

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)