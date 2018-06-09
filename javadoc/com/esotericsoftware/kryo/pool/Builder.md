#Class KryoPool.Builder
Package [com.esotericsoftware.kryo.pool](README.md)<br>

> *java.lang.Object* > [Builder](Builder.md)



Builder for a [KryoPool](KryoPool.md) instance, constructs a *com.esotericsoftware.kryo.pool.KryoPoolQueueImpl* instance.


##Summary
####Constructors
| Visibility | Signature |
| --- | --- |
| **public** | [Builder](#builderkryofactory)([KryoFactory](KryoFactory.md) factory) |

####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **public** [KryoPool](KryoPool.md) | [build](#build)() |
| **public** [Builder](Builder.md) | [queue](#queuequeue)(*java.util.Queue*<[Kryo](../Kryo.md)> queue) |
| **public** [Builder](Builder.md) | [softReferences](#softreferences)() |

---


##Constructors
####Builder(KryoFactory)
> 


---


##Methods
####build()
> Build the pool.


---

####queue(Queue<Kryo>)
> Use the given queue for pooling kryo instances (by default a *java.util.concurrent.ConcurrentLinkedQueue* is used).


---

####softReferences()
> Use *java.lang.ref.SoftReference*s for pooled [Kryo](../Kryo.md) instances, so that instances may be garbage collected when there's
 memory demand (by default disabled).


---

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)