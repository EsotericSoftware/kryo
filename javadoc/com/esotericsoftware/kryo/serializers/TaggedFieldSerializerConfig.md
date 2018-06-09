#Class TaggedFieldSerializerConfig
Package [com.esotericsoftware.kryo.serializers](README.md)<br>

> *java.lang.Object* > [FieldSerializerConfig](FieldSerializerConfig.md) > [TaggedFieldSerializerConfig](TaggedFieldSerializerConfig.md)

All implemented interfaces :
> *java.lang.Cloneable*

Configuration for TaggedFieldSerializer instances.


##Summary
####Constructors
| Visibility | Signature |
| --- | --- |
| **public** | [TaggedFieldSerializerConfig](#taggedfieldserializerconfig)() |

####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **public** **boolean** | [isIgnoreUnknownTags](#isignoreunknowntags)() |
| **public** **void** | [setIgnoreUnknownTags](#setignoreunknowntagsboolean)(**boolean** ignoreUnknownTags) |

---


##Constructors
####TaggedFieldSerializerConfig()
> 


---


##Methods
####isIgnoreUnknownTags()
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