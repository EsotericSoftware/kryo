#Class FieldSerializer.CachedField
Package [com.esotericsoftware.kryo.serializers](README.md)<br>

> *java.lang.Object* > [CachedField](CachedField.md)



Controls how a field will be serialized.


##Summary
####Constructors
| Visibility | Signature |
| --- | --- |
| **public** | [CachedField](#cachedfield)() |

####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **public abstract** **void** | [copy](#copyobject-object)(*java.lang.Object* original, *java.lang.Object* copy) |
| **public** *java.lang.reflect.Field* | [getField](#getfield)() |
| **public** [Serializer](../Serializer.md) | [getSerializer](#getserializer)() |
| **public abstract** **void** | [read](#readinput-object)([Input](../io/Input.md) input, *java.lang.Object* object) |
| **public** **void** | [setCanBeNull](#setcanbenullboolean)(**boolean** canBeNull) |
| **public** **void** | [setClass](#setclassclass)(*java.lang.Class* valueClass) |
| **public** **void** | [setClass](#setclassclass-serializer)(*java.lang.Class* valueClass, [Serializer](../Serializer.md) serializer) |
| **public** **void** | [setSerializer](#setserializerserializer)([Serializer](../Serializer.md) serializer) |
| **public abstract** **void** | [write](#writeoutput-object)([Output](../io/Output.md) output, *java.lang.Object* object) |

---


##Constructors
####CachedField()
> 


---


##Methods
####copy(Object, Object)
> 


---

####getField()
> 


---

####getSerializer()
> 


---

####read(Input, Object)
> 


---

####setCanBeNull(boolean)
> 


---

####setClass(Class)
> 

> **Parameters**
* valueClass : The concrete class of the values for this field. This saves 1-2 bytes. The serializer registered for
           the specified class will be used. Only set to a non-null value if the field type in the class definition is
           final or the values for this field will not vary.


---

####setClass(Class, Serializer)
> 

> **Parameters**
* valueClass : The concrete class of the values for this field. This saves 1-2 bytes. Only set to a non-null value if
           the field type in the class definition is final or the values for this field will not vary.


---

####setSerializer(Serializer)
> 


---

####write(Output, Object)
> 


---

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)