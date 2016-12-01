#Class IntArray
Package [com.esotericsoftware.kryo.util](README.md)<br>

> *java.lang.Object* > [IntArray](IntArray.md)



A resizable, ordered or unordered int array. Avoids the boxing that occurs with ArrayList<Integer>. If unordered, this class
 avoids a memory copy when removing elements (the last element is moved to the removed element's position).


##Summary
####Fields
| Type and modifiers | Field name |
| --- | --- |
| **public** | [items](#items) |
| **public** | [ordered](#ordered) |
| **public** | [size](#size) |

####Constructors
| Visibility | Signature |
| --- | --- |
| **public** | [IntArray](#intarray)() |
| **public** | [IntArray](#intarrayint)(**int** capacity) |
| **public** | [IntArray](#intarrayboolean-int)(**boolean** ordered, **int** capacity) |
| **public** | [IntArray](#intarrayintarray)([IntArray](IntArray.md) array) |
| **public** | [IntArray](#intarrayint)(**int** array) |
| **public** | [IntArray](#intarrayboolean-int)(**boolean** ordered, **int** array) |

####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **public** **void** | [add](#addint)(**int** value) |
| **public** **void** | [addAll](#addallintarray)([IntArray](IntArray.md) array) |
| **public** **void** | [addAll](#addallintarray-int-int)([IntArray](IntArray.md) array, **int** offset, **int** length) |
| **public** **void** | [addAll](#addallint)(**int** array) |
| **public** **void** | [addAll](#addallint-int-int)(**int** array, **int** offset, **int** length) |
| **public** **void** | [clear](#clear)() |
| **public** **boolean** | [contains](#containsint)(**int** value) |
| **public** **int** | [ensureCapacity](#ensurecapacityint)(**int** additionalCapacity) |
| **public** **int** | [get](#getint)(**int** index) |
| **public** **int** | [indexOf](#indexofint)(**int** value) |
| **public** **void** | [insert](#insertint-int)(**int** index, **int** value) |
| **public** **int** | [peek](#peek)() |
| **public** **int** | [pop](#pop)() |
| **public** **int** | [removeIndex](#removeindexint)(**int** index) |
| **public** **boolean** | [removeValue](#removevalueint)(**int** value) |
| **protected** **int** | [resize](#resizeint)(**int** newSize) |
| **public** **void** | [reverse](#reverse)() |
| **public** **void** | [set](#setint-int)(**int** index, **int** value) |
| **public** **void** | [shrink](#shrink)() |
| **public** **void** | [sort](#sort)() |
| **public** **void** | [swap](#swapint-int)(**int** first, **int** second) |
| **public** **int** | [toArray](#toarray)() |
| **public** *java.lang.String* | [toString](#tostringstring)(*java.lang.String* separator) |
| **public** **void** | [truncate](#truncateint)(**int** newSize) |

---


##Constructors
####IntArray()
> Creates an ordered array with a capacity of 16.


---

####IntArray(int)
> Creates an ordered array with the specified capacity.


---

####IntArray(boolean, int)
> 

> **Parameters**
* ordered : If false, methods that remove elements may change the order of other elements in the array, which avoids a
           memory copy.
* capacity : Any elements added beyond this will cause the backing array to be grown.


---

####IntArray(IntArray)
> Creates a new array containing the elements in the specific array. The new array will be ordered if the specific array is
 ordered. The capacity is set to the number of elements, so any subsequent elements added will cause the backing array to be
 grown.


---

####IntArray(int[])
> Creates a new ordered array containing the elements in the specified array. The capacity is set to the number of elements,
 so any subsequent elements added will cause the backing array to be grown.


---

####IntArray(boolean, int[])
> Creates a new array containing the elements in the specified array. The capacity is set to the number of elements, so any
 subsequent elements added will cause the backing array to be grown.

> **Parameters**
* ordered : If false, methods that remove elements may change the order of other elements in the array, which avoids a
           memory copy.


---


##Fields
####items
> **public** **int**

> 

---

####ordered
> **public** **boolean**

> 

---

####size
> **public** **int**

> 

---


##Methods
####add(int)
> 


---

####addAll(IntArray)
> 


---

####addAll(IntArray, int, int)
> 


---

####addAll(int[])
> 


---

####addAll(int[], int, int)
> 


---

####clear()
> 


---

####contains(int)
> 


---

####ensureCapacity(int)
> Increases the size of the backing array to acommodate the specified number of additional items. Useful before adding many
 items to avoid multiple backing array resizes.

> **Returns**
* [IntArray](IntArray.md)


---

####get(int)
> 


---

####indexOf(int)
> 


---

####insert(int, int)
> 


---

####peek()
> Returns the last item.


---

####pop()
> Removes and returns the last item.


---

####removeIndex(int)
> Removes and returns the item at the specified index.


---

####removeValue(int)
> 


---

####resize(int)
> 


---

####reverse()
> 


---

####set(int, int)
> 


---

####shrink()
> Reduces the size of the backing array to the size of the actual items. This is useful to release memory when many items
 have been removed, or if it is known that more items will not be added.


---

####sort()
> 


---

####swap(int, int)
> 


---

####toArray()
> 


---

####toString(String)
> 


---

####truncate(int)
> Reduces the size of the array to the specified size. If the array is already smaller than the specified size, no action is
 taken.


---

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)