#Interface SerializerFactory
Package [com.esotericsoftware.kryo.factories](README.md)<br>

> [SerializerFactory](SerializerFactory.md)



A serializer factory that allows the creation of serializers. This factory will be called when a [Kryo](../Kryo.md) serializer
 discovers a new type for which no serializer is yet known. For example, when a factory is registered via
 [Kryo](../Kryo.md) a different serializer can be created dependent on the type of a class.


##Summary
####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **public** [Serializer](../Serializer.md) | [makeSerializer](#makeserializerkryo-class)([Kryo](../Kryo.md) kryo, *java.lang.Class*<?> type) |

---


##Methods
####makeSerializer(Kryo, Class<?>)
> Creates a new serializer

> **Parameters**
* kryo : The serializer instance requesting the new serializer.
* type : The type of the object that is to be serialized.

> **Returns**
* An implementation of a serializer that is able to serialize an object of type .


---

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)