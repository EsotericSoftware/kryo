#Annotation NotNull
Package [com.esotericsoftware.kryo](README.md)<br>

> [NotNull](NotNull.md)

All implemented interfaces :
> *java.lang.annotation.Annotation*

Indicates a field can never be null when it is being serialized and deserialized. Some serializers use this to save space. Eg,
 [FieldSerializer](serializers/FieldSerializer.md) may save 1 byte per field.

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)