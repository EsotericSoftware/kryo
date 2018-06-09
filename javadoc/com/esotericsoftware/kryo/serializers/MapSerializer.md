#Class MapSerializer
Package [com.esotericsoftware.kryo.serializers](README.md)<br>

> *java.lang.Object* > [Serializer](../Serializer.md) > [MapSerializer](MapSerializer.md)



Serializes objects that implement the *java.util.Map* interface.
 
 With the default constructor, a map requires a 1-3 byte header and an extra 4 bytes is written for each key/value pair.


##Summary
####Constructors
| Visibility | Signature |
| --- | --- |
| **public** | [MapSerializer](#mapserializer)() |

####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **protected** *java.util.Map* | [create](#createkryo-input-class)([Kryo](../Kryo.md) kryo, [Input](../io/Input.md) input, *java.lang.Class*<*java.util.Map*> type) |
| **protected** *java.util.Map* | [createCopy](#createcopykryo-map)([Kryo](../Kryo.md) kryo, *java.util.Map* original) |
| **public** **void** | [setKeyClass](#setkeyclassclass-serializer)(*java.lang.Class* keyClass, [Serializer](../Serializer.md) keySerializer) |
| **public** **void** | [setKeysCanBeNull](#setkeyscanbenullboolean)(**boolean** keysCanBeNull) |
| **public** **void** | [setValueClass](#setvalueclassclass-serializer)(*java.lang.Class* valueClass, [Serializer](../Serializer.md) valueSerializer) |
| **public** **void** | [setValuesCanBeNull](#setvaluescanbenullboolean)(**boolean** valuesCanBeNull) |

---


##Constructors
####MapSerializer()
> 


---


##Methods
####create(Kryo, Input, Class<Map>)
> Used by [MapSerializer](MapSerializer.md) to create the new object. This can be overridden to customize object creation, eg
 to call a constructor with arguments. The default implementation uses [Kryo](../Kryo.md).


---

####createCopy(Kryo, Map)
> 


---

####setKeyClass(Class, Serializer)
> 

> **Parameters**
* keyClass : The concrete class of each key. This saves 1 byte per key. Set to null if the class is not known or varies
           per key (default).
* keySerializer : The serializer to use for each key.


---

####setKeysCanBeNull(boolean)
> 

> **Parameters**
* keysCanBeNull : False if all keys are not null. This saves 1 byte per key if keyClass is set. True if it is not known
           (default).


---

####setValueClass(Class, Serializer)
> 

> **Parameters**
* valueClass : The concrete class of each value. This saves 1 byte per value. Set to null if the class is not known or
           varies per value (default).
* valueSerializer : The serializer to use for each value.


---

####setValuesCanBeNull(boolean)
> 

> **Parameters**
* valuesCanBeNull : True if values are not null. This saves 1 byte per value if keyClass is set. False if it is not
           known (default).


---

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)