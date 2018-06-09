#Class IdentityObjectIntMap
Package [com.esotericsoftware.kryo.util](README.md)<br>

> *java.lang.Object* > [IdentityObjectIntMap](IdentityObjectIntMap.md)



An unordered map where identity comparison is used for keys and the values are ints. This implementation is a cuckoo hash map
 using 3 hashes (if table size is less than 2^16) or 4 hashes (if table size is greater than or equal to 2^16), random walking,
 and a small stash for problematic keys. Null keys are not allowed. No allocation is done except when growing the table size.
 <br>
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
| **public** | [IdentityObjectIntMap](#identityobjectintmap)() |
| **public** | [IdentityObjectIntMap](#identityobjectintmapint)(**int** initialCapacity) |
| **public** | [IdentityObjectIntMap](#identityobjectintmapint-float)(**int** initialCapacity, **float** loadFactor) |

####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **public** **void** | [clear](#clearint)(**int** maximumCapacity) |
| **public** **void** | [clear](#clear)() |
| **public** **boolean** | [containsKey](#containskeyk)(*java.lang.Object* key) |
| **public** **boolean** | [containsValue](#containsvalueint)(**int** value) |
| **public** **void** | [ensureCapacity](#ensurecapacityint)(**int** additionalCapacity) |
| **public** *java.lang.Object* | [findKey](#findkeyint)(**int** value) |
| **public** **int** | [get](#getk-int)(*java.lang.Object* key, **int** defaultValue) |
| **public** **int** | [getAndIncrement](#getandincrementk-int-int)(*java.lang.Object* key, **int** defaultValue, **int** increment) |
| **public** **void** | [put](#putk-int)(*java.lang.Object* key, **int** value) |
| **public** **int** | [remove](#removek-int)(*java.lang.Object* key, **int** defaultValue) |
| **public** **void** | [shrink](#shrinkint)(**int** maximumCapacity) |

---


##Constructors
####IdentityObjectIntMap()
> Creates a new map with an initial capacity of 32 and a load factor of 0.8. This map will hold 25 items before growing the
 backing table.


---

####IdentityObjectIntMap(int)
> Creates a new map with a load factor of 0.8. This map will hold initialCapacity * 0.8 items before growing the backing
 table.


---

####IdentityObjectIntMap(int, float)
> Creates a new map with the specified initial capacity and load factor. This map will hold initialCapacity * loadFactor
 items before growing the backing table.


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

####containsValue(int)
> Returns true if the specified value is in the map. Note this traverses the entire map and compares every value, which may
 be an expensive operation.


---

####ensureCapacity(int)
> Increases the size of the backing array to acommodate the specified number of additional items. Useful before adding many
 items to avoid multiple backing array resizes.


---

####findKey(int)
> Returns the key for the specified value, or null if it is not in the map. Note this traverses the entire map and compares
 every value, which may be an expensive operation.


---

####get(K, int)
> 

> **Parameters**
* defaultValue : Returned if the key was not associated with a value.


---

####getAndIncrement(K, int, int)
> Returns the key's current value and increments the stored value. If the key is not in the map, defaultValue + increment is
 put into the map.


---

####put(K, int)
> 


---

####remove(K, int)
> 


---

####shrink(int)
> Reduces the size of the backing arrays to be the specified capacity or less. If the capacity is already less, nothing is
 done. If the map contains more items than the specified capacity, nothing is done.


---

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)