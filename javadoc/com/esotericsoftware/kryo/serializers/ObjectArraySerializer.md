#Class DefaultArraySerializers.ObjectArraySerializer
Package [com.esotericsoftware.kryo.serializers](README.md)<br>

> *java.lang.Object* > [Serializer](../Serializer.md) > [ObjectArraySerializer](ObjectArraySerializer.md)






##Summary
####Constructors
| Visibility | Signature |
| --- | --- |
| **public** | [ObjectArraySerializer](#objectarrayserializerkryo-class)([Kryo](../Kryo.md) kryo, *java.lang.Class* type) |

####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **public** **void** | [setElementsAreSameType](#setelementsaresametypeboolean)(**boolean** elementsAreSameType) |
| **public** **void** | [setElementsCanBeNull](#setelementscanbenullboolean)(**boolean** elementsCanBeNull) |

---


##Constructors
####ObjectArraySerializer(Kryo, Class)
> 


---


##Methods
####setElementsAreSameType(boolean)
> 

> **Parameters**
* elementsAreSameType : True if all elements are the same type as the array (ie they don't extend the array type).
           This saves 1 byte per element if the array type is not final. Set to false if the array type is final or
           elements extend the array type (default).


---

####setElementsCanBeNull(boolean)
> 

> **Parameters**
* elementsCanBeNull : False if all elements are not null. This saves 1 byte per element if the array type is final or
           elementsAreSameClassAsType is true. True if it is not known (default).


---

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)