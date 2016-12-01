#Class ExternalizableSerializer
Package [com.esotericsoftware.kryo.serializers](README.md)<br>

> *java.lang.Object* > [Serializer](../Serializer.md) > [ExternalizableSerializer](ExternalizableSerializer.md)



Writes using the objects externalizable interface if it can reliably do so. Typically, a object can be efficiently written
 with Kryo and Java's externalizable interface. However, there may be behavior problems if the class uses either the
 'readResolve' or 'writeReplace' methods. We will fall back onto the standard [JavaSerializer](JavaSerializer.md) if we detect either of
 these methods.
 <p/>
 Note that this class does not specialize the type on . That is because if we fall back on the
  it may have an  method that returns an object of a different type.


##Summary
####Constructors
| Visibility | Signature |
| --- | --- |
| **public** | [ExternalizableSerializer](#externalizableserializer)() |

####Methods
| Type and modifiers | Method signature |
| --- | --- |

---


##Constructors
####ExternalizableSerializer()
> 


---


##Methods
---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)