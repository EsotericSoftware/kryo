#Class Serializer
Package [com.esotericsoftware.kryo](README.md)<br>

> *java.lang.Object* > [Serializer](Serializer.md)



Reads and writes objects to and from bytes.


##Summary
####Constructors
| Visibility | Signature |
| --- | --- |
| **public** | [Serializer](#serializer)() |
| **public** | [Serializer](#serializerboolean)(**boolean** acceptsNull) |
| **public** | [Serializer](#serializerboolean-boolean)(**boolean** acceptsNull, **boolean** immutable) |

####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **public** *java.lang.Object* | [copy](#copykryo-t)([Kryo](Kryo.md) kryo, *java.lang.Object* original) |
| **public** **boolean** | [getAcceptsNull](#getacceptsnull)() |
| **public** **boolean** | [isImmutable](#isimmutable)() |
| **public abstract** *java.lang.Object* | [read](#readkryo-input-class)([Kryo](Kryo.md) kryo, [Input](io/Input.md) input, *java.lang.Class*<> type) |
| **public** **void** | [setAcceptsNull](#setacceptsnullboolean)(**boolean** acceptsNull) |
| **public** **void** | [setGenerics](#setgenericskryo-class)([Kryo](Kryo.md) kryo, *java.lang.Class* generics) |
| **public** **void** | [setImmutable](#setimmutableboolean)(**boolean** immutable) |
| **public abstract** **void** | [write](#writekryo-output-t)([Kryo](Kryo.md) kryo, [Output](io/Output.md) output, *java.lang.Object* object) |

---


##Constructors
####Serializer()
> 


---

####Serializer(boolean)
> 


---

####Serializer(boolean, boolean)
> 


---


##Methods
####copy(Kryo, T)
> Returns a copy of the specified object. The default implementation returns the original if [Serializer](Serializer.md) is true,
 else throws [KryoException](KryoException.md). Subclasses should override this method if needed to support [Kryo](Kryo.md).
 
 Before Kryo can be used to copy child objects, [Kryo](Kryo.md) must be called with the copy to ensure it can
 be referenced by the child objects. Any serializer that uses [Kryo](Kryo.md) to copy a child object may need to be reentrant.
 
 This method should not be called directly, instead this serializer can be passed to [Kryo](Kryo.md) copy methods that accept a
 serialier.


---

####getAcceptsNull()
> 


---

####isImmutable()
> 


---

####read(Kryo, Input, Class<T>)
> Reads bytes and returns a new object of the specified concrete type.
 
 Before Kryo can be used to read child objects, [Kryo](Kryo.md) must be called with the parent object to
 ensure it can be referenced by the child objects. Any serializer that uses [Kryo](Kryo.md) to read a child object may need to
 be reentrant.
 
 This method should not be called directly, instead this serializer can be passed to [Kryo](Kryo.md) read methods that accept a
 serialier.

> **Returns**
* May be null if [Serializer](Serializer.md) is true.


---

####setAcceptsNull(boolean)
> If true, this serializer will handle writing and reading null values. If false, the Kryo framework handles null values and
 the serializer will never receive null.
 
 This can be set to true on a serializer that does not accept nulls if it is known that the serializer will never encounter
 null. Doing this will prevent the framework from writing a byte to denote null.


---

####setGenerics(Kryo, Class[])
> Sets the generic types of the field or method this serializer will be used for on the next call to read or write.
 Subsequent calls to read and write must not use this generic type information. The default implementation does nothing.
 Subclasses may use the information provided to this method for more efficient serialization, eg to use the same type for all
 items in a list.

> **Parameters**
* generics : Some (but never all) elements may be null if there is no generic type information at that index.


---

####setImmutable(boolean)
> If true, the type this serializer will be used for is considered immutable. This causes [Serializer](Serializer.md) to
 return the original object.


---

####write(Kryo, Output, T)
> Writes the bytes for the object to the output.
 
 This method should not be called directly, instead this serializer can be passed to [Kryo](Kryo.md) write methods that accept a
 serialier.

> **Parameters**
* object : May be null if [Serializer](Serializer.md) is true.


---

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)