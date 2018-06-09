#Interface KryoPool
Package [com.esotericsoftware.kryo.pool](README.md)<br>

> [KryoPool](KryoPool.md)



A simple pool interface for [Kryo](../Kryo.md) instances. Use the [Builder](Builder.md) to construct a pool instance.
 
 Usage:
 
 <pre>
 import com.esotericsoftware.kryo.Kryo;
 import com.esotericsoftware.kryo.pool.*;
 
 KryoFactory factory = new KryoFactory() {
   public Kryo create () {
     Kryo kryo = new Kryo();
     // configure kryo instance, customize settings
     return kryo;
   }
 };
 // Simple pool, you might also activate SoftReferences to fight OOMEs.
 KryoPool pool = new KryoPool.Builder(factory).build();
 Kryo kryo = pool.borrow();
 // do s.th. with kryo here, and afterwards release it
 pool.release(kryo);
 
 // or use a callback to work with kryo (pool.run borrows+releases for you)
 String value = pool.run(new KryoCallback<String>() {
   public String execute(Kryo kryo) {
     return kryo.readObject(input, String.class);
   }
 });

 </pre>


##Summary
####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **public** [Kryo](../Kryo.md) | [borrow](#borrow)() |
| **public** **void** | [release](#releasekryo)([Kryo](../Kryo.md) kryo) |
| **public** *java.lang.Object* | [run](#runkryocallback)([KryoCallback](KryoCallback.md)<> callback) |

---


##Methods
####borrow()
> Takes a [Kryo](../Kryo.md) instance from the pool or creates a new one (using the factory) if the pool is empty.


---

####release(Kryo)
> Returns the given [Kryo](../Kryo.md) instance to the pool.


---

####run(KryoCallback<T>)
> Runs the provided [KryoCallback](KryoCallback.md) with a [Kryo](../Kryo.md) instance from the pool (borrow/release around
 [KryoCallback](KryoCallback.md)).


---

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)