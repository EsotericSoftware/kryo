#Class ObjectMap
Package [com.esotericsoftware.kryo.util](README.md)<br>

> *java.lang.Object* > [ObjectMap](ObjectMap.md)



An unordered map. This implementation is a cuckoo hash map using 3 hashes (if table size is less than 2^16) or 4 hashes (if
 table size is greater than or equal to 2^16), random walking, and a small stash for problematic keys Null keys are not allowed.
 Null values are allowed. No allocation is done except when growing the table size. <br>
 <br>
 This map performs very fast get, containsKey, and remove (typically O(1), worst case O(log(n))). Put may be a bit slower,
 depending on hash collisions. Load factors greater than 0.91 greatly increase the chances the map will have to rehash to the
 next higher POT size.


##Summary
####Fields
| Type and modifiers | Field name |
| --- | --- |
| **public** | [size](#size) |

####Constructors
| Visibility | Signature |
| --- | --- |
| **public** | [ObjectMap](#objectmap)() |
| **public** | [ObjectMap](#objectmapint)(**int** initialCapacity) |
| **public** | [ObjectMap](#objectmapint-float)(**int** initialCapacity, **float** loadFactor) |
| **public** | [ObjectMap](#objectmapobjectmap)([ObjectMap](ObjectMap.md)<?, ?> map) |

####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **public** **void** | [clear](#clearint)(**int** maximumCapacity) |
| **public** **void** | [clear](#clear)() |
| **public** **boolean** | [containsKey](#containskeyk)(*java.lang.Object* key) |
| **public** **boolean** | [containsValue](#containsvalueobject-boolean)(*java.lang.Object* value, **boolean** identity) |
| **public** **void** | [ensureCapacity](#ensurecapacityint)(**int** additionalCapacity) |
| **public** [Entries](Entries.md)<, > | [entries](#entries)() |
| **public** *java.lang.Object* | [findKey](#findkeyobject-boolean)(*java.lang.Object* value, **boolean** identity) |
| **public** *java.lang.Object* | [get](#getk)(*java.lang.Object* key) |
| **public** *java.lang.Object* | [get](#getk-v)(*java.lang.Object* key, *java.lang.Object* defaultValue) |
| **public** [Keys](Keys.md)<> | [keys](#keys)() |
| **public static** **int** | [nextPowerOfTwo](#nextpoweroftwoint)(**int** value) |
| **public** *java.lang.Object* | [put](#putk-v)(*java.lang.Object* key, *java.lang.Object* value) |
| **public** **void** | [putAll](#putallobjectmap)([ObjectMap](ObjectMap.md)<, > map) |
| **public** *java.lang.Object* | [remove](#removek)(*java.lang.Object* key) |
| **public** **void** | [shrink](#shrinkint)(**int** maximumCapacity) |
| **public** [Values](Values.md)<> | [values](#values)() |

---


##Constructors
####ObjectMap()
> Creates a new map with an initial capacity of 32 and a load factor of 0.8. This map will hold 25 items before growing the
 backing table.


---

####ObjectMap(int)
> Creates a new map with a load factor of 0.8. This map will hold initialCapacity * 0.8 items before growing the backing
 table.


---

####ObjectMap(int, float)
> Creates a new map with the specified initial capacity and load factor. This map will hold initialCapacity * loadFactor
 items before growing the backing table.


---

####ObjectMap(ObjectMap<? extends K, ? extends V>)
> Creates a new map identical to the specified map.


---


##Fields
####size
> **public** **int**

> 

---


##Methods
####clear(int)
> Clears the map and reduces the size of the backing arrays to be the specified capacity if they are larger.


---

####clear()
> 


---

####containsKey(K)
> 


---

####containsValue(Object, boolean)
> Returns true if the specified value is in the map. Note this traverses the entire map and compares every value, which may
 be an expensive operation.

> **Parameters**
* identity : If true, uses == to compare the specified value with values in the map. If false, uses
           [ObjectMap](ObjectMap.md).


---

####ensureCapacity(int)
> Increases the size of the backing array to acommodate the specified number of additional items. Useful before adding many
 items to avoid multiple backing array resizes.


---

####entries()
> Returns an iterator for the entries in the map. Remove is supported.


---

####findKey(Object, boolean)
> Returns the key for the specified value, or null if it is not in the map. Note this traverses the entire map and compares
 every value, which may be an expensive operation.

> **Parameters**
* identity : If true, uses == to compare the specified value with values in the map. If false, uses
           [ObjectMap](ObjectMap.md).


---

####get(K)
> 


---

####get(K, V)
> Returns the value for the specified key, or the default value if the key is not in the map.


---

####keys()
> Returns an iterator for the keys in the map. Remove is supported.


---

####nextPowerOfTwo(int)
> 


---

####put(K, V)
> Returns the old value associated with the specified key, or null.


---

####putAll(ObjectMap<K, V>)
> 


---

####remove(K)
> 


---

####shrink(int)
> Reduces the size of the backing arrays to be the specified capacity or less. If the capacity is already less, nothing is
 done. If the map contains more items than the specified capacity, the next highest power of two capacity is used instead.


---

####values()
> Returns an iterator for the values in the map. Remove is supported.


---

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)