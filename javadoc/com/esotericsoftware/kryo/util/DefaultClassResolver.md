#Class DefaultClassResolver
Package [com.esotericsoftware.kryo.util](README.md)<br>

> *java.lang.Object* > [DefaultClassResolver](DefaultClassResolver.md)

All implemented interfaces :
> [ClassResolver](../ClassResolver.md)

Resolves classes by ID or by fully qualified class name.


##Summary
####Fields
| Type and modifiers | Field name |
| --- | --- |
| **public static final** | [NAME](#name) |
| **protected** | [classToNameId](#classtonameid) |
| **protected final** | [classToRegistration](#classtoregistration) |
| **protected final** | [idToRegistration](#idtoregistration) |
| **protected** | [kryo](#kryo) |
| **protected** | [nameIdToClass](#nameidtoclass) |
| **protected** | [nameToClass](#nametoclass) |
| **protected** | [nextNameId](#nextnameid) |

####Constructors
| Visibility | Signature |
| --- | --- |
| **public** | [DefaultClassResolver](#defaultclassresolver)() |

####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **public** [Registration](../Registration.md) | [getRegistration](#getregistrationclass)(*java.lang.Class* type) |
| **public** [Registration](../Registration.md) | [getRegistration](#getregistrationint)(**int** classID) |
| **protected** *java.lang.Class*<?> | [getTypeByName](#gettypebynamestring)(*java.lang.String* className) |
| **public** [Registration](../Registration.md) | [readClass](#readclassinput)([Input](../io/Input.md) input) |
| **protected** [Registration](../Registration.md) | [readName](#readnameinput)([Input](../io/Input.md) input) |
| **public** [Registration](../Registration.md) | [register](#registerregistration)([Registration](../Registration.md) registration) |
| **public** [Registration](../Registration.md) | [registerImplicit](#registerimplicitclass)(*java.lang.Class* type) |
| **public** **void** | [reset](#reset)() |
| **public** **void** | [setKryo](#setkryokryo)([Kryo](../Kryo.md) kryo) |
| **public** [Registration](../Registration.md) | [writeClass](#writeclassoutput-class)([Output](../io/Output.md) output, *java.lang.Class* type) |
| **protected** **void** | [writeName](#writenameoutput-class-registration)([Output](../io/Output.md) output, *java.lang.Class* type, [Registration](../Registration.md) registration) |

---


##Constructors
####DefaultClassResolver()
> 


---


##Fields
####classToNameId
> **protected** [IdentityObjectIntMap](IdentityObjectIntMap.md)<*java.lang.Class*>

> 

---

####classToRegistration
> **protected final** [ObjectMap](ObjectMap.md)<*java.lang.Class*, [Registration](../Registration.md)>

> 

---

####idToRegistration
> **protected final** [IntMap](IntMap.md)<[Registration](../Registration.md)>

> 

---

####kryo
> **protected** [Kryo](../Kryo.md)

> 

---

####nameIdToClass
> **protected** [IntMap](IntMap.md)<*java.lang.Class*>

> 

---

####nameToClass
> **protected** [ObjectMap](ObjectMap.md)<*java.lang.String*, *java.lang.Class*>

> 

---

####nextNameId
> **protected** **int**

> 

---

####NAME
> **public static final** **byte**

> 

---


##Methods
####getRegistration(Class)
> 


---

####getRegistration(int)
> 


---

####getTypeByName(String)
> 


---

####readClass(Input)
> 


---

####readName(Input)
> 


---

####register(Registration)
> 


---

####registerImplicit(Class)
> 


---

####reset()
> 


---

####setKryo(Kryo)
> 


---

####writeClass(Output, Class)
> 


---

####writeName(Output, Class, Registration)
> 


---

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)