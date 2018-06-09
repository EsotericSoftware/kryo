#Class MapReferenceResolver
Package [com.esotericsoftware.kryo.util](README.md)<br>

> *java.lang.Object* > [MapReferenceResolver](MapReferenceResolver.md)

All implemented interfaces :
> [ReferenceResolver](../ReferenceResolver.md)

Uses an [IdentityObjectIntMap](IdentityObjectIntMap.md) to track objects that have already been written. This can handle graph with any number of
 objects, but is slightly slower than [ListReferenceResolver](ListReferenceResolver.md) for graphs with few objects.


##Summary
####Fields
| Type and modifiers | Field name |
| --- | --- |
| **protected** | [kryo](#kryo) |
| **protected final** | [readObjects](#readobjects) |
| **protected final** | [writtenObjects](#writtenobjects) |

####Constructors
| Visibility | Signature |
| --- | --- |
| **public** | [MapReferenceResolver](#mapreferenceresolver)() |

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
####MapReferenceResolver()
> 


---


##Fields
####kryo
> **protected** [Kryo](../Kryo.md)

> 

---

####readObjects
> **protected final** *java.util.ArrayList*

> 

---

####writtenObjects
> **protected final** [IdentityObjectIntMap](IdentityObjectIntMap.md)

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
> Returns false for all primitive wrappers.


---

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)