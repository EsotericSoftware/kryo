#Class Registration
Package [com.esotericsoftware.kryo](README.md)<br>

> *java.lang.Object* > [Registration](Registration.md)



Describes the [Serializer](Serializer.md) and class ID to use for a class.


##Summary
####Constructors
| Visibility | Signature |
| --- | --- |
| **public** | [Registration](#registrationclass-serializer-int)(*java.lang.Class* type, [Serializer](Serializer.md) serializer, **int** id) |

####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **public** **int** | [getId](#getid)() |
| **public** *org.objenesis.instantiator.ObjectInstantiator* | [getInstantiator](#getinstantiator)() |
| **public** [Serializer](Serializer.md) | [getSerializer](#getserializer)() |
| **public** *java.lang.Class* | [getType](#gettype)() |
| **public** **void** | [setInstantiator](#setinstantiatorobjectinstantiator)(*org.objenesis.instantiator.ObjectInstantiator* instantiator) |
| **public** **void** | [setSerializer](#setserializerserializer)([Serializer](Serializer.md) serializer) |

---


##Constructors
####Registration(Class, Serializer, int)
> 


---


##Methods
####getId()
> Returns the registered class ID.


---

####getInstantiator()
> 

> **Returns**
* May be null if not yet set.


---

####getSerializer()
> 


---

####getType()
> 


---

####setInstantiator(ObjectInstantiator)
> Sets the instantiator that will create a new instance of the type in [Kryo](Kryo.md).


---

####setSerializer(Serializer)
> 


---

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)