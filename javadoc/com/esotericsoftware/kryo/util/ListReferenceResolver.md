#Class ListReferenceResolver
Package [com.esotericsoftware.kryo.util](README.md)<br>

> *java.lang.Object* > [ListReferenceResolver](ListReferenceResolver.md)

All implemented interfaces :
> [ReferenceResolver](../ReferenceResolver.md)

Uses an *java.util.ArrayList* to track objects that have already been written. This is more efficient than
 [MapReferenceResolver](MapReferenceResolver.md) for graphs with few objects, providing an approximate 15% increase in deserialization speed. This
 should not be used for graphs with many objects because it uses a linear look up to find objects that have already been
 written.


##Summary
####Fields
| Type and modifiers | Field name |
| --- | --- |
| **protected** | [kryo](#kryo) |
| **protected final** | [seenObjects](#seenobjects) |

####Constructors
| Visibility | Signature |
| --- | --- |
| **public** | [ListReferenceResolver](#listreferenceresolver)() |

####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **public** **int** | [addWrittenObject](#addwrittenobjectobject)(*java.lang.Object* object) |
| **public** *java.lang.Object* | [getReadObject](#getreadobjectclass-int)(*java.lang.Class* type, **int** id) |
| **public** **int** | [getWrittenId](#getwrittenidobject)(*java.lang.Object* object) |
| **public** **int** | [nextReadId](#nextreadidclass)(*java.lang.Class* type) |
| **public** **void** | [reset](#reset)() |
| **public** **void** | [setKryo](#setkryokryo)([Kryo](../Kryo.md) kryo) |
| **public** **void** | [setReadObject](#setreadobjectint-object)(**int** id, *java.lang.Object* object) |
| **public** **boolean** | [useReferences](#usereferencesclass)(*java.lang.Class* type) |

---


##Constructors
####ListReferenceResolver()
> 


---


##Fields
####kryo
> **protected** [Kryo](../Kryo.md)

> 

---

####seenObjects
> **protected final** *java.util.ArrayList*

> 

---


##Methods
####addWrittenObject(Object)
> 


---

####getReadObject(Class, int)
> 


---

####getWrittenId(Object)
> 


---

####nextReadId(Class)
> 


---

####reset()
> 


---

####setKryo(Kryo)
> 


---

####setReadObject(int, Object)
> 


---

####useReferences(Class)
> Returns false for Boolean, Byte, Character, and Short.


---

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)