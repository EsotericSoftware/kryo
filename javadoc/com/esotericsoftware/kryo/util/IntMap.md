#Class IntMap
Package [com.esotericsoftware.kryo.util](README.md)<br>

> *java.lang.Object* > [IntMap](IntMap.md)



An unordered map that uses int keys. This implementation is a cuckoo hash map using 3 hashes (if table size is less than 2^16)
 or 4 hashes (if table size is greater than or equal to 2^16), random walking, and a small stash for problematic keys. Null
 values are allowed. No allocation is done except when growing the table size. <br>
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
| **public** | [IntMap](#intmap)() |
| **public** | [IntMap](#intmapint)(**int** initialCapacity) |
| **public** | [IntMap](#intmapint-float)(**int** initialCapacity, **float** loadFactor) |
| **public** | [IntMap](#intmapintmap)([IntMap](IntMap.md)<?> map) |

####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **public** **void** | [clear](#clearint)(**int** maximumCapacity) |
| **public** **void** | [clear](#clear)() |
| **public** **boolean** | [containsKey](#containskeyint)(**int** key) |
| **public** **boolean** | [containsValue](#containsvalueobject-boolean)(*java.lang.Object* value, **boolean** identity) |
| **public** **void** | [ensureCapacity](#ensurecapacityint)(**int** additionalCapacity) |
| **public** [Entries](Entries.md)<> | [entries](#entries)() |
| **public** **int** | [findKey](#findkeyobject-boolean-int)(*java.lang.Object* value, **boolean** identity, **int** notFound) |
| **public** *java.lang.Object* | [get](#getint)(**int** key) |
| **public** *java.lang.Object* | [get](#getint-v)(**int** key, *java.lang.Object* defaultValue) |
| **public** [Keys](Keys.md) | [keys](#keys)() |
| **public** *java.lang.Object* | [put](#putint-v)(**int** key, *java.lang.Object* value) |
| **public** **void** | [putAll](#putallintmap)([IntMap](IntMap.md)<> map) |
| **public** *java.lang.Object* | [remove](#removeint)(**int** key) |
| **public** **void** | [shrink](#shrinkint)(**int** maximumCapacity) |
| **public** [Values](Values.md)<> | [values](#values)() |

---


##Constructors
####IntMap()
> Creates a new map with an initial capacity of 32 and a load factor of 0.8. This map will hold 25 items before growing the
 backing table.


---

####IntMap(int)
> Creates a new map with a load factor of 0.8. This map will hold initialCapacity * 0.8 items before growing the backing
 table.


---

####IntMap(int, float)
> Creates a new map with the specified initial capacity and load factor. This map will hold initialCapacity * loadFactor
 items before growing the backing table.


---

####IntMap(IntMap<? extends V>)
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

####containsKey(int)
> 


---

####containsValue(Object, boolean)
> Returns true if the specified value is in the map. Note this traverses the entire map and compares every value, which may
 be an expensive operation.

> **Parameters**
* identity : If true, uses == to compare the specified value with values in the map. If false, uses
           [IntMap](IntMap.md).


---

####ensureCapacity(int)
> Increases the size of the backing array to acommodate the specified number of additional items. Useful before adding many
 items to avoid multiple backing array resizes.


---

####entries()
> Returns an iterator for the entries in the map. Remove is supported. Note that the same iterator instance is returned each
 time this method is called. Use the [Entries](Entries.md) constructor for nested or multithreaded iteration.


---

####findKey(Object, boolean, int)
> Returns the key for the specified value, or <tt>notFound</tt> if it is not in the map. Note this traverses the entire map
 and compares every value, which may be an expensive operation.

> **Parameters**
* identity : If true, uses == to compare the specified value with values in the map. If false, uses
           [IntMap](IntMap.md).


---

####get(int)
> 


---

####get(int, V)
> 


---

####keys()
> Returns an iterator for the keys in the map. Remove is supported. Note that the same iterator instance is returned each
 time this method is called. Use the [Entries](Entries.md) constructor for nested or multithreaded iteration.


---

####put(int, V)
> 


---

####putAll(IntMap<V>)
> 


---

####remove(int)
> 


---

####shrink(int)
> Reduces the size of the backing arrays to be the specified capacity or less. If the capacity is already less, nothing is
 done. If the map contains more items than the specified capacity, the next highest power of two capacity is used instead.


---

####values()
> Returns an iterator for the values in the map. Remove is supported. Note that the same iterator instance is returned each
 time this method is called. Use the [Entries](Entries.md) constructor for nested or multithreaded iteration.


---

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)