#Class Util
Package [com.esotericsoftware.kryo.util](README.md)<br>

> *java.lang.Object* > [Util](Util.md)



A few utility methods, mostly for private use.


##Summary
####Fields
| Type and modifiers | Field name |
| --- | --- |
| **public static** | [isAndroid](#isandroid) |

####Constructors
| Visibility | Signature |
| --- | --- |
| **public** | [Util](#util)() |

####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **public static** *java.lang.String* | [className](#classnameclass)(*java.lang.Class* type) |
| **public static** **int** | [getDimensionCount](#getdimensioncountclass)(*java.lang.Class* arrayClass) |
| **public static** *java.lang.Class* | [getElementClass](#getelementclassclass)(*java.lang.Class* arrayClass) |
| **public static** *java.lang.Class* | [getPrimitiveClass](#getprimitiveclassclass)(*java.lang.Class* type) |
| **public static** *java.lang.Class* | [getWrapperClass](#getwrapperclassclass)(*java.lang.Class* type) |
| **public static** **boolean** | [isClassAvailable](#isclassavailablestring)(*java.lang.String* className) |
| **public static** **boolean** | [isWrapperClass](#iswrapperclassclass)(*java.lang.Class* type) |
| **public static** **void** | [log](#logstring-object)(*java.lang.String* message, *java.lang.Object* object) |
| **public static** *java.lang.String* | [string](#stringobject)(*java.lang.Object* object) |
| **public static** **int** | [swapInt](#swapintint)(**int** i) |
| **public static** **long** | [swapLong](#swaplonglong)(**long** value) |

---


##Constructors
####Util()
> 


---


##Fields
####isAndroid
> **public static** **boolean**

> 

---


##Methods
####className(Class)
> Returns the class formatted as a string. The format varies depending on the type.


---

####getDimensionCount(Class)
> Returns the number of dimensions of an array.


---

####getElementClass(Class)
> Returns the base element type of an n-dimensional array class.


---

####getPrimitiveClass(Class)
> Returns the primitive class for a primitive wrapper class. Otherwise returns the type parameter.

> **Parameters**
* type : Must be a wrapper class.


---

####getWrapperClass(Class)
> Returns the primitive wrapper class for a primitive class.

> **Parameters**
* type : Must be a primitive class.


---

####isClassAvailable(String)
> 


---

####isWrapperClass(Class)
> 


---

####log(String, Object)
> Logs a message about an object. The log level and the string format of the object depend on the object type.


---

####string(Object)
> Returns the object formatted as a string. The format depends on the object's type and whether *java.lang.Object* has
 been overridden.


---

####swapInt(int)
> Converts an "int" value between endian systems.


---

####swapLong(long)
> Converts a "long" value between endian systems.


---

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)