#Class ClosureSerializer
Package [com.esotericsoftware.kryo.serializers](README.md)<br>

> *java.lang.Object* > [Serializer](../Serializer.md) > [ClosureSerializer](ClosureSerializer.md)



Serializer for Java8 closures. To serialize closures, use:
 
 <code>
 kryo.register(java.lang.invoke.SerializedLambda.class);<br>
 kryo.register(ClosureSerializer.Closure.class, new ClosureSerializer());</code>


##Summary
####Constructors
| Visibility | Signature |
| --- | --- |
| **public** | [ClosureSerializer](#closureserializer)() |

####Methods
| Type and modifiers | Method signature |
| --- | --- |

---


##Constructors
####ClosureSerializer()
> 


---


##Methods
---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)