#Class InputChunked
Package [com.esotericsoftware.kryo.io](README.md)<br>

> *java.lang.Object* > *java.io.InputStream* > [Input](Input.md) > [InputChunked](InputChunked.md)

All implemented interfaces :
> *java.io.Closeable*

An InputStream that reads lengths and chunks of data from another OutputStream, allowing chunks to be skipped.


##Summary
####Constructors
| Visibility | Signature |
| --- | --- |
| **public** | [InputChunked](#inputchunked)() |
| **public** | [InputChunked](#inputchunkedint)(**int** bufferSize) |
| **public** | [InputChunked](#inputchunkedinputstream)(*java.io.InputStream* inputStream) |
| **public** | [InputChunked](#inputchunkedinputstream-int)(*java.io.InputStream* inputStream, **int** bufferSize) |

####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **public** **void** | [nextChunks](#nextchunks)() |

---


##Constructors
####InputChunked()
> Creates an uninitialized InputChunked with a buffer size of 2048. The InputStream must be set before it can be used.


---

####InputChunked(int)
> Creates an uninitialized InputChunked. The InputStream must be set before it can be used.


---

####InputChunked(InputStream)
> Creates an InputChunked with a buffer size of 2048.


---

####InputChunked(InputStream, int)
> 


---


##Methods
####nextChunks()
> Advances the stream to the next set of chunks. InputChunked will appear to hit the end of the data until this method is
 called.


---

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)