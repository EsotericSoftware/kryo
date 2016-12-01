#Class BeanSerializer
Package [com.esotericsoftware.kryo.serializers](README.md)<br>

> *java.lang.Object* > [Serializer](../Serializer.md) > [BeanSerializer](BeanSerializer.md)



Serializes Java beans using bean accessor methods. Only bean properties with both a getter and setter are serialized. This
 class is not as fast as [FieldSerializer](FieldSerializer.md) but is much faster and more efficient than Java serialization. Bytecode
 generation is used to invoke the bean property methods, if possible.
 
 BeanSerializer does not write header data, only the object data is stored. If the type of a bean property is not final (note
 primitives are final) then an extra byte is written for that property.


##Summary
####Constructors
| Visibility | Signature |
| --- | --- |
| **public** | [BeanSerializer](#beanserializerkryo-class)([Kryo](../Kryo.md) kryo, *java.lang.Class* type) |

####Methods
| Type and modifiers | Method signature |
| --- | --- |

---


##Constructors
####BeanSerializer(Kryo, Class)
> 


---


##Methods
---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)