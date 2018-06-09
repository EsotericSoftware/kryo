#Interface ClassResolver
Package [com.esotericsoftware.kryo](README.md)<br>

> [ClassResolver](ClassResolver.md)



Handles class registration, writing class identifiers to bytes, and reading class identifiers from bytes.


##Summary
####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **public** [Registration](Registration.md) | [getRegistration](#getregistrationclass)(*java.lang.Class* type) |
| **public** [Registration](Registration.md) | [getRegistration](#getregistrationint)(**int** classID) |
| **public** [Registration](Registration.md) | [readClass](#readclassinput)([Input](io/Input.md) input) |
| **public** [Registration](Registration.md) | [register](#registerregistration)([Registration](Registration.md) registration) |
| **public** [Registration](Registration.md) | [registerImplicit](#registerimplicitclass)(*java.lang.Class* type) |
| **public** **void** | [reset](#reset)() |
| **public** **void** | [setKryo](#setkryokryo)([Kryo](Kryo.md) kryo) |
| **public** [Registration](Registration.md) | [writeClass](#writeclassoutput-class)([Output](io/Output.md) output, *java.lang.Class* type) |

---


##Methods
####getRegistration(Class)
> Returns the registration for the specified class, or null if the class is not registered.


---

####getRegistration(int)
> Returns the registration for the specified ID, or null if no class is registered with that ID.


---

####readClass(Input)
> Reads a class and returns its registration.

> **Returns**
* May be null.


---

####register(Registration)
> Stores the specified registration.


---

####registerImplicit(Class)
> Called when an unregistered type is encountered and [Kryo](Kryo.md) is false.


---

####reset()
> Called by [Kryo](Kryo.md).


---

####setKryo(Kryo)
> Sets the Kryo instance that this ClassResolver will be used for. This is called automatically by Kryo.


---

####writeClass(Output, Class)
> Writes a class and returns its registration.

> **Parameters**
* type : May be null.

> **Returns**
* Will be null if type is null.


---

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)