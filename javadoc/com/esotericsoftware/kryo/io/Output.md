#Class Output
Package [com.esotericsoftware.kryo.io](README.md)<br>

> *java.lang.Object* > *java.io.OutputStream* > [Output](Output.md)

All implemented interfaces :
> *java.io.Flushable*, *java.io.Closeable*

An OutputStream that buffers data in a byte array and optionally flushes to another OutputStream. Utility methods are provided
 for efficiently writing primitive types and strings.
 
 Encoding of integers: BIG_ENDIAN is used for storing fixed native size integer values LITTLE_ENDIAN is used for a variable
 length encoding of integer values


##Summary
####Fields
| Type and modifiers | Field name |
| --- | --- |
| **protected** | [buffer](#buffer) |
| **protected** | [capacity](#capacity) |
| **protected** | [maxCapacity](#maxcapacity) |
| **protected** | [outputStream](#outputstream) |
| **protected** | [position](#position) |
| **protected** | [total](#total) |

####Constructors
| Visibility | Signature |
| --- | --- |
| **public** | [Output](#output)() |
| **public** | [Output](#outputint)(**int** bufferSize) |
| **public** | [Output](#outputint-int)(**int** bufferSize, **int** maxBufferSize) |
| **public** | [Output](#outputbyte)(**byte** buffer) |
| **public** | [Output](#outputbyte-int)(**byte** buffer, **int** maxBufferSize) |
| **public** | [Output](#outputoutputstream)(*java.io.OutputStream* outputStream) |
| **public** | [Output](#outputoutputstream-int)(*java.io.OutputStream* outputStream, **int** bufferSize) |

####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **public** **void** | [clear](#clear)() |
| **public** **byte** | [getBuffer](#getbuffer)() |
| **public** *java.io.OutputStream* | [getOutputStream](#getoutputstream)() |
| **public static** **int** | [intLength](#intlengthint-boolean)(**int** value, **boolean** optimizePositive) |
| **public static** **int** | [longLength](#longlengthlong-boolean)(**long** value, **boolean** optimizePositive) |
| **public** **int** | [position](#position)() |
| **protected** **boolean** | [require](#requireint)(**int** required) |
| **public** **void** | [setBuffer](#setbufferbyte)(**byte** buffer) |
| **public** **void** | [setBuffer](#setbufferbyte-int)(**byte** buffer, **int** maxBufferSize) |
| **public** **void** | [setOutputStream](#setoutputstreamoutputstream)(*java.io.OutputStream* outputStream) |
| **public** **void** | [setPosition](#setpositionint)(**int** position) |
| **public** **byte** | [toBytes](#tobytes)() |
| **public** **long** | [total](#total)() |
| **public** **void** | [writeAscii](#writeasciistring)(*java.lang.String* value) |
| **public** **void** | [writeBoolean](#writebooleanboolean)(**boolean** value) |
| **public** **void** | [writeByte](#writebytebyte)(**byte** value) |
| **public** **void** | [writeByte](#writebyteint)(**int** value) |
| **public** **void** | [writeBytes](#writebytesbyte)(**byte** bytes) |
| **public** **void** | [writeBytes](#writebytesbyte-int-int)(**byte** bytes, **int** offset, **int** count) |
| **public** **void** | [writeChar](#writecharchar)(**char** value) |
| **public** **void** | [writeChars](#writecharschar)(**char** object) |
| **public** **void** | [writeDouble](#writedoubledouble)(**double** value) |
| **public** **int** | [writeDouble](#writedoubledouble-double-boolean)(**double** value, **double** precision, **boolean** optimizePositive) |
| **public** **void** | [writeDoubles](#writedoublesdouble)(**double** object) |
| **public** **void** | [writeFloat](#writefloatfloat)(**float** value) |
| **public** **int** | [writeFloat](#writefloatfloat-float-boolean)(**float** value, **float** precision, **boolean** optimizePositive) |
| **public** **void** | [writeFloats](#writefloatsfloat)(**float** object) |
| **public** **void** | [writeInt](#writeintint)(**int** value) |
| **public** **int** | [writeInt](#writeintint-boolean)(**int** value, **boolean** optimizePositive) |
| **public** **void** | [writeInts](#writeintsint-boolean)(**int** object, **boolean** optimizePositive) |
| **public** **void** | [writeInts](#writeintsint)(**int** object) |
| **public** **void** | [writeLong](#writelonglong)(**long** value) |
| **public** **int** | [writeLong](#writelonglong-boolean)(**long** value, **boolean** optimizePositive) |
| **public** **void** | [writeLongs](#writelongslong-boolean)(**long** object, **boolean** optimizePositive) |
| **public** **void** | [writeLongs](#writelongslong)(**long** object) |
| **public** **void** | [writeShort](#writeshortint)(**int** value) |
| **public** **void** | [writeShorts](#writeshortsshort)(**short** object) |
| **public** **void** | [writeString](#writestringstring)(*java.lang.String* value) |
| **public** **void** | [writeString](#writestringcharsequence)(*java.lang.CharSequence* value) |
| **public** **int** | [writeVarInt](#writevarintint-boolean)(**int** value, **boolean** optimizePositive) |
| **public** **int** | [writeVarLong](#writevarlonglong-boolean)(**long** value, **boolean** optimizePositive) |

---


##Constructors
####Output()
> Creates an uninitialized Output. [Output](Output.md) must be called before the Output is used.


---

####Output(int)
> Creates a new Output for writing to a byte array.

> **Parameters**
* bufferSize : The initial and maximum size of the buffer. An exception is thrown if this size is exceeded.


---

####Output(int, int)
> Creates a new Output for writing to a byte array.

> **Parameters**
* bufferSize : The initial size of the buffer.
* maxBufferSize : The buffer is doubled as needed until it exceeds maxBufferSize and an exception is thrown. Can be -1
           for no maximum.


---

####Output(byte[])
> Creates a new Output for writing to a byte array.


---

####Output(byte[], int)
> Creates a new Output for writing to a byte array.


---

####Output(OutputStream)
> Creates a new Output for writing to an OutputStream. A buffer size of 4096 is used.


---

####Output(OutputStream, int)
> Creates a new Output for writing to an OutputStream.


---


##Fields
####buffer
> **protected** **byte**

> 

---

####capacity
> **protected** **int**

> 

---

####maxCapacity
> **protected** **int**

> 

---

####outputStream
> **protected** *java.io.OutputStream*

> 

---

####position
> **protected** **int**

> 

---

####total
> **protected** **long**

> 

---


##Methods
####clear()
> Sets the position and total to zero.


---

####getBuffer()
> Returns the buffer. The bytes between zero and [Output](Output.md) are the data that has been written.


---

####getOutputStream()
> 


---

####intLength(int, boolean)
> Returns the number of bytes that would be written with [Output](Output.md).


---

####longLength(long, boolean)
> Returns the number of bytes that would be written with [Output](Output.md).


---

####position()
> Returns the current position in the buffer. This is the number of bytes that have not been flushed.


---

####require(int)
> 

> **Returns**
* true if the buffer has been resized.


---

####setBuffer(byte[])
> Sets the buffer that will be written to. [Output](Output.md) is called with the specified buffer's length as
 the maxBufferSize.


---

####setBuffer(byte[], int)
> Sets the buffer that will be written to. The position and total are reset, discarding any buffered bytes. The
 [Output](Output.md) is set to null.

> **Parameters**
* maxBufferSize : The buffer is doubled as needed until it exceeds maxBufferSize and an exception is thrown.


---

####setOutputStream(OutputStream)
> Sets a new OutputStream. The position and total are reset, discarding any buffered bytes.

> **Parameters**
* outputStream : May be null.


---

####setPosition(int)
> Sets the current position in the buffer.


---

####toBytes()
> Returns a new byte array containing the bytes currently in the buffer between zero and [Output](Output.md).


---

####total()
> Returns the total number of bytes written. This may include bytes that have not been flushed.


---

####writeAscii(String)
> Writes a string that is known to contain only ASCII characters. Non-ASCII strings passed to this method will be corrupted.
 Each byte is a 7 bit character with the remaining byte denoting if another character is available. This is slightly more
 efficient than [Output](Output.md). The string can be read using [Input](Input.md) or
 [Input](Input.md).

> **Parameters**
* value : May be null.


---

####writeBoolean(boolean)
> Writes a 1 byte boolean.


---

####writeByte(byte)
> 


---

####writeByte(int)
> 


---

####writeBytes(byte[])
> Writes the bytes. Note the byte[] length is not written.


---

####writeBytes(byte[], int, int)
> Writes the bytes. Note the byte[] length is not written.


---

####writeChar(char)
> Writes a 2 byte char. Uses BIG_ENDIAN byte order.


---

####writeChars(char[])
> Bulk output of a char array.


---

####writeDouble(double)
> Writes an 8 byte double.


---

####writeDouble(double, double, boolean)
> Writes a 1-9 byte double with reduced precision.

> **Parameters**
* optimizePositive : If true, small positive numbers will be more efficient (1 byte) and small negative numbers will be
           inefficient (9 bytes).


---

####writeDoubles(double[])
> Bulk output of a double array.


---

####writeFloat(float)
> Writes a 4 byte float.


---

####writeFloat(float, float, boolean)
> Writes a 1-5 byte float with reduced precision.

> **Parameters**
* optimizePositive : If true, small positive numbers will be more efficient (1 byte) and small negative numbers will be
           inefficient (5 bytes).


---

####writeFloats(float[])
> Bulk output of a float array.


---

####writeInt(int)
> Writes a 4 byte int. Uses BIG_ENDIAN byte order.


---

####writeInt(int, boolean)
> Writes a 1-5 byte int. This stream may consider such a variable length encoding request as a hint. It is not guaranteed
 that a variable length encoding will be really used. The stream may decide to use native-sized integer representation for
 efficiency reasons.

> **Parameters**
* optimizePositive : If true, small positive numbers will be more efficient (1 byte) and small negative numbers will be
           inefficient (5 bytes).


---

####writeInts(int[], boolean)
> Bulk output of an int array.


---

####writeInts(int[])
> Bulk output of an int array.


---

####writeLong(long)
> Writes an 8 byte long. Uses BIG_ENDIAN byte order.


---

####writeLong(long, boolean)
> Writes a 1-9 byte long. This stream may consider such a variable length encoding request as a hint. It is not guaranteed
 that a variable length encoding will be really used. The stream may decide to use native-sized integer representation for
 efficiency reasons.

> **Parameters**
* optimizePositive : If true, small positive numbers will be more efficient (1 byte) and small negative numbers will be
           inefficient (9 bytes).


---

####writeLongs(long[], boolean)
> Bulk output of an long array.


---

####writeLongs(long[])
> Bulk output of an long array.


---

####writeShort(int)
> Writes a 2 byte short. Uses BIG_ENDIAN byte order.


---

####writeShorts(short[])
> Bulk output of a short array.


---

####writeString(String)
> Writes the length and string, or null. Short strings are checked and if ASCII they are written more efficiently, else they
 are written as UTF8. If a string is known to be ASCII, [Output](Output.md) may be used. The string can be read using
 [Input](Input.md) or [Input](Input.md).

> **Parameters**
* value : May be null.


---

####writeString(CharSequence)
> Writes the length and CharSequence as UTF8, or null. The string can be read using [Input](Input.md) or
 [Input](Input.md).

> **Parameters**
* value : May be null.


---

####writeVarInt(int, boolean)
> Writes a 1-5 byte int. It is guaranteed that a varible length encoding will be used.

> **Parameters**
* optimizePositive : If true, small positive numbers will be more efficient (1 byte) and small negative numbers will be
           inefficient (5 bytes).


---

####writeVarLong(long, boolean)
> Writes a 1-9 byte long. It is guaranteed that a varible length encoding will be used.

> **Parameters**
* optimizePositive : If true, small positive numbers will be more efficient (1 byte) and small negative numbers will be
           inefficient (9 bytes).


---

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)