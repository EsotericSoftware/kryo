#Class TaggedFieldSerializer
Package [com.esotericsoftware.kryo.serializers](README.md)<br>

> *java.lang.Object* > [Serializer](../Serializer.md) > [FieldSerializer](FieldSerializer.md) > [TaggedFieldSerializer](TaggedFieldSerializer.md)

All implemented interfaces :
> *java.util.Comparator*<[CachedField](CachedField.md)>

Serializes objects using direct field assignment for fields that have a <code>@Tag(int)</code> annotation. This provides
 backward compatibility so new fields can be added. TaggedFieldSerializer has two advantages over [VersionFieldSerializer](VersionFieldSerializer.md)
 : 1) fields can be renamed and 2) fields marked with the <code>@Deprecated</code> annotation will be ignored when reading old
 bytes and won't be written to new bytes. Deprecation effectively removes the field from serialization, though the field and
 <code>@Tag</code> annotation must remain in the class. Deprecated fields can optionally be made private and/or renamed so they
 don't clutter the class (eg, <code>ignored</code>, <code>ignored2</code>). For these reasons, TaggedFieldSerializer generally
 provides more flexibility for classes to evolve. The downside is that it has a small amount of additional overhead compared to
 VersionFieldSerializer (an additional varint per field). Forward compatibility is not supported.


##Summary
####Constructors
| Visibility | Signature |
| --- | --- |
| **public** | [TaggedFieldSerializer](#taggedfieldserializerkryo-class)([Kryo](../Kryo.md) kryo, *java.lang.Class* type) |

####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **public** **boolean** | [isIgnoreUnkownTags](#isignoreunkowntags)() |
| **public** **void** | [setIgnoreUnknownTags](#setignoreunknowntagsboolean)(**boolean** ignoreUnknownTags) |

---


##Constructors
####TaggedFieldSerializer(Kryo, Class)
> 


---


##Methods
####isIgnoreUnkownTags()
> 


---

####setIgnoreUnknownTags(boolean)
> Tells Kryo, if should ignore unknown field tags when using TaggedFieldSerializer. Already existing serializer instances are
 not affected by this setting.

 
 By default, Kryo will throw KryoException if encounters unknown field tags.
 

> **Parameters**
* ignoreUnknownTags : if true, unknown field tags will be ignored. Otherwise KryoException will be thrown


---

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)