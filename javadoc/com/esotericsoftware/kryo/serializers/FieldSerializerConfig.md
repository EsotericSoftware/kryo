#Class FieldSerializerConfig
Package [com.esotericsoftware.kryo.serializers](README.md)<br>

> *java.lang.Object* > [FieldSerializerConfig](FieldSerializerConfig.md)

All implemented interfaces :
> *java.lang.Cloneable*

Configuration for FieldSerializer instances. To configure defaults for new FieldSerializer instances use
 [Kryo](../Kryo.md), to configure a specific FieldSerializer instance use setters for configuration
 settings on this specific FieldSerializer.


##Summary
####Constructors
| Visibility | Signature |
| --- | --- |
| **public** | [FieldSerializerConfig](#fieldserializerconfig)() |

####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **public** [CachedFieldNameStrategy](CachedFieldNameStrategy.md) | [getCachedFieldNameStrategy](#getcachedfieldnamestrategy)() |
| **public** **boolean** | [isCopyTransient](#iscopytransient)() |
| **public** **boolean** | [isFieldsCanBeNull](#isfieldscanbenull)() |
| **public** **boolean** | [isFixedFieldTypes](#isfixedfieldtypes)() |
| **public** **boolean** | [isIgnoreSyntheticFields](#isignoresyntheticfields)() |
| **public** **boolean** | [isOptimizedGenerics](#isoptimizedgenerics)() |
| **public** **boolean** | [isSerializeTransient](#isserializetransient)() |
| **public** **boolean** | [isSetFieldsAsAccessible](#issetfieldsasaccessible)() |
| **public** **boolean** | [isUseAsm](#isuseasm)() |
| **public** **void** | [setCachedFieldNameStrategy](#setcachedfieldnamestrategycachedfieldnamestrategy)([CachedFieldNameStrategy](CachedFieldNameStrategy.md) cachedFieldNameStrategy) |
| **public** **void** | [setCopyTransient](#setcopytransientboolean)(**boolean** setCopyTransient) |
| **public** **void** | [setFieldsAsAccessible](#setfieldsasaccessibleboolean)(**boolean** setFieldsAsAccessible) |
| **public** **void** | [setFieldsCanBeNull](#setfieldscanbenullboolean)(**boolean** fieldsCanBeNull) |
| **public** **void** | [setFixedFieldTypes](#setfixedfieldtypesboolean)(**boolean** fixedFieldTypes) |
| **public** **void** | [setIgnoreSyntheticFields](#setignoresyntheticfieldsboolean)(**boolean** ignoreSyntheticFields) |
| **public** **void** | [setOptimizedGenerics](#setoptimizedgenericsboolean)(**boolean** setOptimizedGenerics) |
| **public** **void** | [setSerializeTransient](#setserializetransientboolean)(**boolean** serializeTransient) |
| **public** **void** | [setUseAsm](#setuseasmboolean)(**boolean** setUseAsm) |

---


##Constructors
####FieldSerializerConfig()
> 


---


##Methods
####getCachedFieldNameStrategy()
> 


---

####isCopyTransient()
> 


---

####isFieldsCanBeNull()
> 


---

####isFixedFieldTypes()
> 


---

####isIgnoreSyntheticFields()
> 


---

####isOptimizedGenerics()
> 


---

####isSerializeTransient()
> 


---

####isSetFieldsAsAccessible()
> 


---

####isUseAsm()
> 


---

####setCachedFieldNameStrategy(FieldSerializer.CachedFieldNameStrategy)
> 


---

####setCopyTransient(boolean)
> If false, when [Kryo](../Kryo.md) is called all transient fields that are accessible will be ignored from being
 copied. This has to be set before registering classes with kryo for it to be used by all field serializers. If transient
 fields has to be copied for specific classes then use [FieldSerializer](FieldSerializer.md). Default is true.


---

####setFieldsAsAccessible(boolean)
> Controls which fields are serialized.

> **Parameters**
* setFieldsAsAccessible : If true, all non-transient fields (inlcuding private fields) will be serialized and
           *java.lang.reflect.Field* if necessary (default). If false, only
           fields in the public API will be serialized.


---

####setFieldsCanBeNull(boolean)
> Sets the default value for [CachedField](CachedField.md).

> **Parameters**
* fieldsCanBeNull : False if none of the fields are null. Saves 0-1 byte per field. True if it is not known (default).


---

####setFixedFieldTypes(boolean)
> Sets the default value for [CachedField](CachedField.md) to the field's declared type. This allows
 FieldSerializer to be more efficient, since it knows field values will not be a subclass of their declared type. Default is
 false.


---

####setIgnoreSyntheticFields(boolean)
> Controls if synthetic fields are serialized. Default is true.

> **Parameters**
* ignoreSyntheticFields : If true, only non-synthetic fields will be serialized.


---

####setOptimizedGenerics(boolean)
> Controls if the serialization of generics should be optimized for smaller size.
 
 <strong>Important:</strong> This setting changes the serialized representation, so that data can be deserialized only with
 if this setting is the same as it was for serialization.
 

> **Parameters**
* setOptimizedGenerics : If true, the serialization of generics will be optimize for smaller size (default: false)


---

####setSerializeTransient(boolean)
> If set, transient fields will be serialized Default is false

> **Parameters**
* serializeTransient : 


---

####setUseAsm(boolean)
> Controls whether ASM should be used.

> **Parameters**
* setUseAsm : If true, ASM will be used for fast serialization. If false, Unsafe will be used (default)


---

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)