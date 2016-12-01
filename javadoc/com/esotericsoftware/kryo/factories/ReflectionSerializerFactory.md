#Class ReflectionSerializerFactory
Package [com.esotericsoftware.kryo.factories](README.md)<br>

> *java.lang.Object* > [ReflectionSerializerFactory](ReflectionSerializerFactory.md)

All implemented interfaces :
> [SerializerFactory](SerializerFactory.md)

This factory instantiates new serializers of a given class via reflection. The constructors of the given
  must either take an instance of [Kryo](../Kryo.md) and an instance of *java.lang.Class* as its parameter, take
 only a [Kryo](../Kryo.md) or *java.lang.Class* as its only argument or take no arguments. If several of the described constructors are
 found, the first found constructor is used, in the order as they were just described.


##Summary
####Constructors
| Visibility | Signature |
| --- | --- |
| **public** | [ReflectionSerializerFactory](#reflectionserializerfactoryclass)(*java.lang.Class*<?> serializerClass) |

####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **public** [Serializer](../Serializer.md) | [makeSerializer](#makeserializerkryo-class)([Kryo](../Kryo.md) kryo, *java.lang.Class*<?> type) |
| **public static** [Serializer](../Serializer.md) | [makeSerializer](#makeserializerkryo-class-class)([Kryo](../Kryo.md) kryo, *java.lang.Class*<?> serializerClass, *java.lang.Class*<?> type) |

---


##Constructors
####ReflectionSerializerFactory(Class<? extends Serializer>)
> 


---


##Methods
####makeSerializer(Kryo, Class<?>)
> 


---

####makeSerializer(Kryo, Class<? extends Serializer>, Class<?>)
> Creates a new instance of the specified serializer for serializing the specified class. Serializers must have a zero
 argument constructor or one that takes (Kryo), (Class), or (Kryo, Class).


---

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)