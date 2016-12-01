#Interface KryoCopyable
Package [com.esotericsoftware.kryo](README.md)<br>

> [KryoCopyable](KryoCopyable.md)



Allows implementing classes to perform their own copying. Hand written copying can be more efficient in some cases.
 
 This method is used instead of the registered serializer [Serializer](Serializer.md) method.


##Summary
####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **public** *java.lang.Object* | [copy](#copykryo)([Kryo](Kryo.md) kryo) |

---


##Methods
####copy(Kryo)
> Returns a copy that has the same values as this object. Before Kryo can be used to copy child objects,
 [Kryo](Kryo.md) must be called with the copy to ensure it can be referenced by the child objects.


---

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)