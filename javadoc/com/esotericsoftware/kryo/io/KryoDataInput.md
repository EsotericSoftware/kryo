#Class KryoDataInput
Package [com.esotericsoftware.kryo.io](README.md)<br>

> *java.lang.Object* > [KryoDataInput](KryoDataInput.md)

All implemented interfaces :
> *java.io.DataInput*

Best attempt adapter for *java.io.DataInput*. Currently only [KryoDataInput](KryoDataInput.md) is unsupported. Other methods behave slightly
 differently. For example, [KryoDataInput](KryoDataInput.md) may return a null string.


##Summary
####Fields
| Type and modifiers | Field name |
| --- | --- |
| **protected** | [input](#input) |

####Constructors
| Visibility | Signature |
| --- | --- |
| **public** | [KryoDataInput](#kryodatainputinput)([Input](Input.md) input) |

####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **public** **boolean** | [readBoolean](#readboolean)() |
| **public** **byte** | [readByte](#readbyte)() |
| **public** **char** | [readChar](#readchar)() |
| **public** **double** | [readDouble](#readdouble)() |
| **public** **float** | [readFloat](#readfloat)() |
| **public** **void** | [readFully](#readfullybyte)(**byte** b) |
| **public** **void** | [readFully](#readfullybyte-int-int)(**byte** b, **int** off, **int** len) |
| **public** **int** | [readInt](#readint)() |
| **public** *java.lang.String* | [readLine](#readline)() |
| **public** **long** | [readLong](#readlong)() |
| **public** **short** | [readShort](#readshort)() |
| **public** *java.lang.String* | [readUTF](#readutf)() |
| **public** **int** | [readUnsignedByte](#readunsignedbyte)() |
| **public** **int** | [readUnsignedShort](#readunsignedshort)() |
| **public** **void** | [setInput](#setinputinput)([Input](Input.md) input) |
| **public** **int** | [skipBytes](#skipbytesint)(**int** n) |

---


##Constructors
####KryoDataInput(Input)
> 


---


##Fields
####input
> **protected** [Input](Input.md)

> 

---


##Methods
####readBoolean()
> 


---

####readByte()
> 


---

####readChar()
> 


---

####readDouble()
> 


---

####readFloat()
> 


---

####readFully(byte[])
> 


---

####readFully(byte[], int, int)
> 


---

####readInt()
> 


---

####readLine()
> This is not currently implemented. The method will currently throw an *java.lang.UnsupportedOperationException*
 whenever it is called.

> **Throws**
* *java.lang.UnsupportedOperationException* when called.


---

####readLong()
> 


---

####readShort()
> 


---

####readUTF()
> Reads the length and string of UTF8 characters, or null. This can read strings written by
 [KryoDataOutput](KryoDataOutput.md), [Output](Output.md),
 [Output](Output.md), and
 [Output](Output.md).

> **Returns**
* May be null.


---

####readUnsignedByte()
> 


---

####readUnsignedShort()
> 


---

####setInput(Input)
> 


---

####skipBytes(int)
> 


---

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)