#Class VersionFieldSerializer
Package [com.esotericsoftware.kryo.serializers](README.md)<br>

> *java.lang.Object* > [Serializer](../Serializer.md) > [FieldSerializer](FieldSerializer.md) > [VersionFieldSerializer](VersionFieldSerializer.md)

All implemented interfaces :
> *java.util.Comparator*<[CachedField](CachedField.md)>

Serializes objects using direct field assignment, with versioning backward compatibility. Allows fields to have a
 <code>@Since(int)</code> annotation to indicate the version they were added. For a particular field, the value in
 <code>@Since</code> should never change once created. This is less flexible than FieldSerializer, which can handle most classes
 without needing annotations, but it provides backward compatibility. This means that new fields can be added, but removing,
 renaming or changing the type of any field will invalidate previous serialized bytes. VersionFieldSerializer has very little
 overhead (a single additional varint) compared to FieldSerializer. Forward compatibility is not supported.


##Summary
####Constructors
| Visibility | Signature |
| --- | --- |
| **public** | [VersionFieldSerializer](#versionfieldserializerkryo-class)([Kryo](../Kryo.md) kryo, *java.lang.Class* type) |
| **public** | [VersionFieldSerializer](#versionfieldserializerkryo-class-boolean)([Kryo](../Kryo.md) kryo, *java.lang.Class* type, **boolean** compatible) |

####Methods
| Type and modifiers | Method signature |
| --- | --- |

---


##Constructors
####VersionFieldSerializer(Kryo, Class)
> 


---

####VersionFieldSerializer(Kryo, Class, boolean)
> 


---


##Methods
---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)