#Class CollectionSerializer
Package [com.esotericsoftware.kryo.serializers](README.md)<br>

> *java.lang.Object* > [Serializer](../Serializer.md) > [CollectionSerializer](CollectionSerializer.md)



Serializes objects that implement the *java.util.Collection* interface.
 
 With the default constructor, a collection requires a 1-3 byte header and an extra 2-3 bytes is written for each element in the
 collection. The alternate constructor can be used to improve efficiency to match that of using an array instead of a
 collection.


##Summary
####Constructors
| Visibility | Signature |
| --- | --- |
| **public** | [CollectionSerializer](#collectionserializer)() |
| **public** | [CollectionSerializer](#collectionserializerclass-serializer)(*java.lang.Class* elementClass, [Serializer](../Serializer.md) serializer) |
| **public** | [CollectionSerializer](#collectionserializerclass-serializer-boolean)(*java.lang.Class* elementClass, [Serializer](../Serializer.md) serializer, **boolean** elementsCanBeNull) |

####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **protected** *java.util.Collection* | [create](#createkryo-input-class)([Kryo](../Kryo.md) kryo, [Input](../io/Input.md) input, *java.lang.Class*<*java.util.Collection*> type) |
| **protected** *java.util.Collection* | [createCopy](#createcopykryo-collection)([Kryo](../Kryo.md) kryo, *java.util.Collection* original) |
| **public** **void** | [setElementClass](#setelementclassclass-serializer)(*java.lang.Class* elementClass, [Serializer](../Serializer.md) serializer) |
| **public** **void** | [setElementsCanBeNull](#setelementscanbenullboolean)(**boolean** elementsCanBeNull) |

---


##Constructors
####CollectionSerializer()
> 


---

####CollectionSerializer(Class, Serializer)
> 


---

####CollectionSerializer(Class, Serializer, boolean)
> 


---


##Methods
####create(Kryo, Input, Class<Collection>)
> Used by [CollectionSerializer](CollectionSerializer.md) to create the new object. This can be overridden to customize object creation, eg
 to call a constructor with arguments. The default implementation uses [Kryo](../Kryo.md).


---

####createCopy(Kryo, Collection)
> Used by [CollectionSerializer](CollectionSerializer.md) to create the new object. This can be overridden to customize object creation, eg
 to call a constructor with arguments. The default implementation uses [Kryo](../Kryo.md).


---

####setElementClass(Class, Serializer)
> 

> **Parameters**
* elementClass : The concrete class of each element. This saves 1-2 bytes per element. Set to null if the class is not
           known or varies per element (default).
* serializer : The serializer to use for each element.


---

####setElementsCanBeNull(boolean)
> 

> **Parameters**
* elementsCanBeNull : False if all elements are not null. This saves 1 byte per element if elementClass is set. True if
           it is not known (default).


---

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)