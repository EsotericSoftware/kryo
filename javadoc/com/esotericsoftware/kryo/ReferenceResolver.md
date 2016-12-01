#Interface ReferenceResolver
Package [com.esotericsoftware.kryo](README.md)<br>

> [ReferenceResolver](ReferenceResolver.md)



When references are enabled, this tracks objects that have already been read or written, provides an ID for objects that are
 written, and looks up by ID objects that have been read.


##Summary
####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **public** **int** | [addWrittenObject](#addwrittenobjectobject)(*java.lang.Object* object) |
| **public** *java.lang.Object* | [getReadObject](#getreadobjectclass-int)(*java.lang.Class* type, **int** id) |
| **public** **int** | [getWrittenId](#getwrittenidobject)(*java.lang.Object* object) |
| **public** **int** | [nextReadId](#nextreadidclass)(*java.lang.Class* type) |
| **public** **void** | [reset](#reset)() |
| **public** **void** | [setKryo](#setkryokryo)([Kryo](Kryo.md) kryo) |
| **public** **void** | [setReadObject](#setreadobjectint-object)(**int** id, *java.lang.Object* object) |
| **public** **boolean** | [useReferences](#usereferencesclass)(*java.lang.Class* type) |

---


##Methods
####addWrittenObject(Object)
> Returns a new ID for an object that is being written for the first time.

> **Returns**
* The ID, which is stored more efficiently if it is positive and must not be -1 or -2.


---

####getReadObject(Class, int)
> Returns the object for the specified ID. The ID and object are guaranteed to have been previously passed in a call to
 [ReferenceResolver](ReferenceResolver.md).


---

####getWrittenId(Object)
> Returns an ID for the object if it has been written previously, otherwise returns -1.


---

####nextReadId(Class)
> Reserves the ID for the next object that will be read. This is called only the first time an object is encountered.

> **Parameters**
* type : The type of object that will be read.

> **Returns**
* The ID, which is stored more efficiently if it is positive and must not be -1 or -2.


---

####reset()
> Called by [Kryo](Kryo.md).


---

####setKryo(Kryo)
> Sets the Kryo instance that this ClassResolver will be used for. This is called automatically by Kryo.


---

####setReadObject(int, Object)
> Sets the ID for an object that has been read.

> **Parameters**
* id : The ID from [ReferenceResolver](ReferenceResolver.md).


---

####useReferences(Class)
> Returns true if references will be written for the specified type.

> **Parameters**
* type : Will never be a primitive type, but may be a primitive type wrapper.


---

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)