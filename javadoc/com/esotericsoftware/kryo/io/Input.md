#Class Input
Package [com.esotericsoftware.kryo.io](README.md)<br>

> *java.lang.Object* > *java.io.InputStream* > [Input](Input.md)

All implemented interfaces :
> *java.io.Closeable*

An InputStream that reads data from a byte array and optionally fills the byte array from another InputStream as needed.
 Utility methods are provided for efficiently reading primitive types and strings.
 
 The byte[] buffer may be modified and then returned to its original state during some read operations, so the same byte[]
 should not be used concurrently in separate threads.


##Summary
####Fields
| Type and modifiers | Field name |
| --- | --- |
| **protected** | [buffer](#buffer) |
| **protected** | [capacity](#capacity) |
| **protected** | [chars](#chars) |
| **protected** | [inputStream](#inputstream) |
| **protected** | [limit](#limit) |
| **protected** | [position](#position) |
| **protected** | [total](#total) |

####Constructors
| Visibility | Signature |
| --- | --- |
| **public** | [Input](#input)() |
| **public** | [Input](#inputint)(**int** bufferSize) |
| **public** | [Input](#inputbyte)(**byte** buffer) |
| **public** | [Input](#inputbyte-int-int)(**byte** buffer, **int** offset, **int** count) |
| **public** | [Input](#inputinputstream)(*java.io.InputStream* inputStream) |
| **public** | [Input](#inputinputstream-int)(*java.io.InputStream* inputStream, **int** bufferSize) |

####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **public** **boolean** | [canReadInt](#canreadint)() |
| **public** **boolean** | [canReadLong](#canreadlong)() |
| **public** **boolean** | [eof](#eof)() |
| **protected** **int** | [fill](#fillbyte-int-int)(**byte** buffer, **int** offset, **int** count) |
| **public** **byte** | [getBuffer](#getbuffer)() |
| **public** *java.io.InputStream* | [getInputStream](#getinputstream)() |
| **public** **int** | [limit](#limit)() |
| **public** **int** | [position](#position)() |
| **public** **boolean** | [readBoolean](#readboolean)() |
| **public** **byte** | [readByte](#readbyte)() |
| **public** **int** | [readByteUnsigned](#readbyteunsigned)() |
| **public** **byte** | [readBytes](#readbytesint)(**int** length) |
| **public** **void** | [readBytes](#readbytesbyte)(**byte** bytes) |
| **public** **void** | [readBytes](#readbytesbyte-int-int)(**byte** bytes, **int** offset, **int** count) |
| **public** **char** | [readChar](#readchar)() |
| **public** **char** | [readChars](#readcharsint)(**int** length) |
| **public** **double** | [readDouble](#readdouble)() |
| **public** **double** | [readDouble](#readdoubledouble-boolean)(**double** precision, **boolean** optimizePositive) |
| **public** **double** | [readDoubles](#readdoublesint)(**int** length) |
| **public** **float** | [readFloat](#readfloat)() |
| **public** **float** | [readFloat](#readfloatfloat-boolean)(**float** precision, **boolean** optimizePositive) |
| **public** **float** | [readFloats](#readfloatsint)(**int** length) |
| **public** **int** | [readInt](#readint)() |
| **public** **int** | [readInt](#readintboolean)(**boolean** optimizePositive) |
| **public** **int** | [readInts](#readintsint-boolean)(**int** length, **boolean** optimizePositive) |
| **public** **int** | [readInts](#readintsint)(**int** length) |
| **public** **long** | [readLong](#readlong)() |
| **public** **long** | [readLong](#readlongboolean)(**boolean** optimizePositive) |
| **public** **long** | [readLongs](#readlongsint-boolean)(**int** length, **boolean** optimizePositive) |
| **public** **long** | [readLongs](#readlongsint)(**int** length) |
| **public** **short** | [readShort](#readshort)() |
| **public** **int** | [readShortUnsigned](#readshortunsigned)() |
| **public** **short** | [readShorts](#readshortsint)(**int** length) |
| **public** *java.lang.String* | [readString](#readstring)() |
| **public** *java.lang.StringBuilder* | [readStringBuilder](#readstringbuilder)() |
| **public** **int** | [readVarInt](#readvarintboolean)(**boolean** optimizePositive) |
| **public** **long** | [readVarLong](#readvarlongboolean)(**boolean** optimizePositive) |
| **protected** **int** | [require](#requireint)(**int** required) |
| **public** **void** | [rewind](#rewind)() |
| **public** **void** | [setBuffer](#setbufferbyte)(**byte** bytes) |
| **public** **void** | [setBuffer](#setbufferbyte-int-int)(**byte** bytes, **int** offset, **int** count) |
| **public** **void** | [setInputStream](#setinputstreaminputstream)(*java.io.InputStream* inputStream) |
| **public** **void** | [setLimit](#setlimitint)(**int** limit) |
| **public** **void** | [setPosition](#setpositionint)(**int** position) |
| **public** **void** | [setTotal](#settotallong)(**long** total) |
| **public** **void** | [skip](#skipint)(**int** count) |
| **public** **long** | [total](#total)() |

---


##Constructors
####Input()
> Creates an uninitialized Input. [Input](Input.md) must be called before the Input is used.


---

####Input(int)
> Creates a new Input for reading from a byte array.

> **Parameters**
* bufferSize : The size of the buffer. An exception is thrown if more bytes than this are read.


---

####Input(byte[])
> Creates a new Input for reading from a byte array.

> **Parameters**
* buffer : An exception is thrown if more bytes than this are read.


---

####Input(byte[], int, int)
> Creates a new Input for reading from a byte array.

> **Parameters**
* buffer : An exception is thrown if more bytes than this are read.


---

####Input(InputStream)
> Creates a new Input for reading from an InputStream with a buffer size of 4096.


---

####Input(InputStream, int)
> Creates a new Input for reading from an InputStream.


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

####chars
> **protected** **char**

> 

---

####inputStream
> **protected** *java.io.InputStream*

> 

---

####limit
> **protected** **int**

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
####canReadInt()
> Returns true if enough bytes are available to read an int with [Input](Input.md).


---

####canReadLong()
> Returns true if enough bytes are available to read a long with [Input](Input.md).


---

####eof()
> 


---

####fill(byte[], int, int)
> Fills the buffer with more bytes. Can be overridden to fill the bytes from a source other than the InputStream.

> **Returns**
* -1 if there are no more bytes.


---

####getBuffer()
> 


---

####getInputStream()
> 


---

####limit()
> Returns the limit for the buffer.


---

####position()
> Returns the current position in the buffer.


---

####readBoolean()
> Reads a 1 byte boolean.


---

####readByte()
> Reads a single byte.


---

####readByteUnsigned()
> Reads a byte as an int from 0 to 255.


---

####readBytes(int)
> Reads the specified number of bytes into a new byte[].


---

####readBytes(byte[])
> Reads bytes.length bytes and writes them to the specified byte[], starting at index 0.


---

####readBytes(byte[], int, int)
> Reads count bytes and writes them to the specified byte[], starting at offset.


---

####readChar()
> Reads a 2 byte char.


---

####readChars(int)
> Bulk input of a char array.


---

####readDouble()
> Reads an 8 bytes double.


---

####readDouble(double, boolean)
> Reads a 1-9 byte double with reduced precision.


---

####readDoubles(int)
> Bulk input of a double array.


---

####readFloat()
> Reads a 4 byte float.


---

####readFloat(float, boolean)
> Reads a 1-5 byte float with reduced precision.


---

####readFloats(int)
> Bulk input of a float array.


---

####readInt()
> Reads a 4 byte int.


---

####readInt(boolean)
> Reads a 1-5 byte int. This stream may consider such a variable length encoding request as a hint. It is not guaranteed that
 a variable length encoding will be really used. The stream may decide to use native-sized integer representation for
 efficiency reasons.


---

####readInts(int, boolean)
> Bulk input of an int array.


---

####readInts(int)
> Bulk input of an int array.


---

####readLong()
> Reads an 8 byte long.


---

####readLong(boolean)
> Reads a 1-9 byte long. This stream may consider such a variable length encoding request as a hint. It is not guaranteed
 that a variable length encoding will be really used. The stream may decide to use native-sized integer representation for
 efficiency reasons.


---

####readLongs(int, boolean)
> Bulk input of a long array.


---

####readLongs(int)
> Bulk input of a long array.


---

####readShort()
> Reads a 2 byte short.


---

####readShortUnsigned()
> Reads a 2 byte short as an int from 0 to 65535.


---

####readShorts(int)
> Bulk input of a short array.


---

####readString()
> Reads the length and string of UTF8 characters, or null. This can read strings written by
 [Output](Output.md) , [Output](Output.md), and [Output](Output.md).

> **Returns**
* May be null.


---

####readStringBuilder()
> Reads the length and string of UTF8 characters, or null. This can read strings written by
 [Output](Output.md) , [Output](Output.md), and [Output](Output.md).

> **Returns**
* May be null.


---

####readVarInt(boolean)
> Reads a 1-5 byte int. It is guaranteed that a varible length encoding will be used.


---

####readVarLong(boolean)
> Reads a 1-9 byte long. It is guaranteed that a varible length encoding will be used.


---

####require(int)
> 

> **Parameters**
* required : Must be > 0. The buffer is filled until it has at least this many bytes.

> **Returns**
* the number of bytes remaining.

> **Throws**
* [KryoException](../KryoException.md) if EOS is reached before required bytes are read (buffer underflow).


---

####rewind()
> Sets the position and total to zero.


---

####setBuffer(byte[])
> Sets a new buffer. The position and total are reset, discarding any buffered bytes.


---

####setBuffer(byte[], int, int)
> Sets a new buffer. The position and total are reset, discarding any buffered bytes.


---

####setInputStream(InputStream)
> Sets a new InputStream. The position and total are reset, discarding any buffered bytes.

> **Parameters**
* inputStream : May be null.


---

####setLimit(int)
> Sets the limit in the buffer.


---

####setPosition(int)
> Sets the current position in the buffer.


---

####setTotal(long)
> Sets the number of bytes read.


---

####skip(int)
> Discards the specified number of bytes.


---

####total()
> Returns the number of bytes read.


---

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)