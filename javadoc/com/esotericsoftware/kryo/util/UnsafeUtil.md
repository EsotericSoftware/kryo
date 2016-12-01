#Class UnsafeUtil
Package [com.esotericsoftware.kryo.util](README.md)<br>

> *java.lang.Object* > [UnsafeUtil](UnsafeUtil.md)



A few utility methods for using @link{sun.misc.Unsafe}, mostly for private use.
 
 Use of Unsafe on Android is forbidden, as Android provides only a very limited functionality for this class compared to the JDK
 version.


##Summary
####Fields
| Type and modifiers | Field name |
| --- | --- |
| **public static final** | [byteArrayBaseOffset](#bytearraybaseoffset) |
| **public static final** | [charArrayBaseOffset](#chararraybaseoffset) |
| **public static final** | [doubleArrayBaseOffset](#doublearraybaseoffset) |
| **public static final** | [floatArrayBaseOffset](#floatarraybaseoffset) |
| **public static final** | [intArrayBaseOffset](#intarraybaseoffset) |
| **public static final** | [longArrayBaseOffset](#longarraybaseoffset) |
| **public static final** | [shortArrayBaseOffset](#shortarraybaseoffset) |

####Constructors
| Visibility | Signature |
| --- | --- |
| **public** | [UnsafeUtil](#unsafeutil)() |

####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **public static final** *java.nio.ByteBuffer* | [getDirectBufferAt](#getdirectbufferatlong-int)(**long** address, **int** size) |
| **public static** **void** | [releaseBuffer](#releasebufferbytebuffer)(*java.nio.ByteBuffer* niobuffer) |
| **public static** *java.lang.reflect.Field* | [sortFieldsByOffset](#sortfieldsbyoffsetlist)(*java.util.List*<*java.lang.reflect.Field*> allFields) |
| **public static final** *sun.misc.Unsafe* | [unsafe](#unsafe)() |

---


##Constructors
####UnsafeUtil()
> 


---


##Fields
####byteArrayBaseOffset
> **public static final** **long**

> 

---

####charArrayBaseOffset
> **public static final** **long**

> 

---

####doubleArrayBaseOffset
> **public static final** **long**

> 

---

####floatArrayBaseOffset
> **public static final** **long**

> 

---

####intArrayBaseOffset
> **public static final** **long**

> 

---

####longArrayBaseOffset
> **public static final** **long**

> 

---

####shortArrayBaseOffset
> **public static final** **long**

> 

---


##Methods
####getDirectBufferAt(long, int)
> Create a ByteBuffer that uses a provided (off-heap) memory region instead of allocating a new one.

> **Parameters**
* address : address of the memory region to be used for a ByteBuffer
* size : size of the memory region

> **Returns**
* a new ByteBuffer that uses a provided memory region instead of allocating a new one


---

####releaseBuffer(ByteBuffer)
> Release a direct buffer.
 
 NOTE: If Cleaner is not accessible due to SecurityManager restrictions, reflection could be used to obtain the "clean"
 method and then invoke it.


---

####sortFieldsByOffset(List<Field>)
> Sort the set of lists by their offsets from the object start address.

> **Parameters**
* allFields : set of fields to be sorted by their offsets


---

####unsafe()
> Return the sun.misc.Unsafe object. If null is returned, no further Unsafe-related methods are allowed to be invoked from
 UnsafeUtil.

> **Returns**
* instance of sun.misc.Unsafe or null, if this class is not available or not accessible


---

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)