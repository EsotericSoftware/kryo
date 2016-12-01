#Annotation FieldSerializer.Optional
Package [com.esotericsoftware.kryo.serializers](README.md)<br>

> [Optional](Optional.md)

All implemented interfaces :
> *java.lang.annotation.Annotation*

Indicates a field should be ignored when its declaring class is registered unless the [Kryo](../Kryo.md) has
 a value set for the specified key. This can be useful when a field must be serialized for one purpose, but not for another.
 Eg, a class for a networked application could have a field that should not be serialized and sent to clients, but should be
 serialized when stored on the server.

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)