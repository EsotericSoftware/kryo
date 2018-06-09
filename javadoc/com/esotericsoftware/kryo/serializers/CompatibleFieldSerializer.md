#Class CompatibleFieldSerializer
Package [com.esotericsoftware.kryo.serializers](README.md)<br>

> *java.lang.Object* > [Serializer](../Serializer.md) > [FieldSerializer](FieldSerializer.md) > [CompatibleFieldSerializer](CompatibleFieldSerializer.md)

All implemented interfaces :
> *java.util.Comparator*<[CachedField](CachedField.md)>

Serializes objects using direct field assignment, providing both forward and backward compatibility. This means fields can be
 added or removed without invalidating previously serialized bytes. Changing the type of a field is not supported. Like
 [FieldSerializer](FieldSerializer.md), it can serialize most classes without needing annotations. The forward and backward compatibility
 comes at a cost: the first time the class is encountered in the serialized bytes, a simple schema is written containing the
 field name strings. Also, during serialization and deserialization buffers are allocated to perform chunked encoding. This is
 what enables CompatibleFieldSerializer to skip bytes for fields it does not know about.
 
 Removing fields when [Kryo](../Kryo.md) are enabled can cause compatibility issues. See
 <a href="https://github.com/EsotericSoftware/kryo/issues/286#issuecomment-74870545">here</a>.
 
 Note that the field data is identified by name. The situation where a super class has a field with the same name as a subclass
 must be avoided.


##Summary
####Constructors
| Visibility | Signature |
| --- | --- |
| **public** | [CompatibleFieldSerializer](#compatiblefieldserializerkryo-class)([Kryo](../Kryo.md) kryo, *java.lang.Class* type) |

####Methods
| Type and modifiers | Method signature |
| --- | --- |

---


##Constructors
####CompatibleFieldSerializer(Kryo, Class)
> 


---


##Methods
---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)