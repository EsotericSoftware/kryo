#Class PseudoSerializerFactory
Package [com.esotericsoftware.kryo.factories](README.md)<br>

> *java.lang.Object* > [PseudoSerializerFactory](PseudoSerializerFactory.md)

All implemented interfaces :
> [SerializerFactory](SerializerFactory.md)

A serializer factory that always returns a given serializer instance. This implementation of [SerializerFactory](SerializerFactory.md) is not
 a real factory since it only provides a given instance instead of dynamically creating new serializers. It can be used when all
 types should be serialized by the same serializer. This also allows serializers to be shared among different [Kryo](../Kryo.md)
 instances.


##Summary
####Constructors
| Visibility | Signature |
| --- | --- |
| **public** | [PseudoSerializerFactory](#pseudoserializerfactoryserializer)([Serializer](../Serializer.md)<?> serializer) |

####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **public** [Serializer](../Serializer.md) | [makeSerializer](#makeserializerkryo-class)([Kryo](../Kryo.md) kryo, *java.lang.Class*<?> type) |

---


##Constructors
####PseudoSerializerFactory(Serializer<?>)
> 


---


##Methods
####makeSerializer(Kryo, Class<?>)
> 


---

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)