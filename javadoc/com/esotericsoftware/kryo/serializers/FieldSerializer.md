#Class FieldSerializer
Package [com.esotericsoftware.kryo.serializers](README.md)<br>

> *java.lang.Object* > [Serializer](../Serializer.md) > [FieldSerializer](FieldSerializer.md)

All implemented interfaces :
> *java.util.Comparator*<[CachedField](CachedField.md)>

Serializes objects using direct field assignment. FieldSerializer is generic and can serialize most classes without any
 configuration. It is efficient and writes only the field data, without any extra information. It does not support adding,
 removing, or changing the type of fields without invalidating previously serialized bytes. This can be acceptable in many
 situations, such as when sending data over a network, but may not be a good choice for long term data storage because the Java
 classes cannot evolve. Because FieldSerializer attempts to read and write non-public fields by default, it is important to
 evaluate each class that will be serialized. If fields are public, bytecode generation will be used instead of reflection.


##Summary
####Fields
| Type and modifiers | Field name |
| --- | --- |
| **protected final** | [config](#config) |
| **protected** | [removedFields](#removedfields) |

####Constructors
| Visibility | Signature |
| --- | --- |
| **public** | [FieldSerializer](#fieldserializerkryo-class)([Kryo](../Kryo.md) kryo, *java.lang.Class* type) |
| **public** | [FieldSerializer](#fieldserializerkryo-class-class)([Kryo](../Kryo.md) kryo, *java.lang.Class* type, *java.lang.Class* generics) |
| **protected** | [FieldSerializer](#fieldserializerkryo-class-class-fieldserializerconfig)([Kryo](../Kryo.md) kryo, *java.lang.Class* type, *java.lang.Class* generics, [FieldSerializerConfig](FieldSerializerConfig.md) config) |

####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **public** **int** | [compare](#comparecachedfield-cachedfield)([CachedField](CachedField.md) o1, [CachedField](CachedField.md) o2) |
| **protected** *java.lang.Object* | [create](#createkryo-input-class)([Kryo](../Kryo.md) kryo, [Input](../io/Input.md) input, *java.lang.Class*<> type) |
| **protected** *java.lang.Object* | [createCopy](#createcopykryo-t)([Kryo](../Kryo.md) kryo, *java.lang.Object* original) |
| **protected** *java.lang.String* | [getCachedFieldName](#getcachedfieldnamecachedfield)([CachedField](CachedField.md) cachedField) |
| **public** **boolean** | [getCopyTransient](#getcopytransient)() |
| **public** [CachedField](CachedField.md) | [getField](#getfieldstring)(*java.lang.String* fieldName) |
| **public** [CachedField](CachedField.md) | [getFields](#getfields)() |
| **public** *java.lang.Class* | [getGenerics](#getgenerics)() |
| **public** [Kryo](../Kryo.md) | [getKryo](#getkryo)() |
| **public** **boolean** | [getSerializeTransient](#getserializetransient)() |
| **public** [CachedField](CachedField.md) | [getTransientFields](#gettransientfields)() |
| **public** *java.lang.Class* | [getType](#gettype)() |
| **public** **boolean** | [getUseAsmEnabled](#getuseasmenabled)() |
| **public** **boolean** | [getUseMemRegions](#getusememregions)() |
| **protected** **void** | [initializeCachedFields](#initializecachedfields)() |
| **protected** **void** | [rebuildCachedFields](#rebuildcachedfields)() |
| **protected** **void** | [rebuildCachedFields](#rebuildcachedfieldsboolean)(**boolean** minorRebuild) |
| **public** **void** | [removeField](#removefieldstring)(*java.lang.String* fieldName) |
| **public** **void** | [removeField](#removefieldcachedfield)([CachedField](CachedField.md) removeField) |
| **public** **void** | [setCopyTransient](#setcopytransientboolean)(**boolean** setCopyTransient) |
| **public** **void** | [setFieldsAsAccessible](#setfieldsasaccessibleboolean)(**boolean** setFieldsAsAccessible) |
| **public** **void** | [setFieldsCanBeNull](#setfieldscanbenullboolean)(**boolean** fieldsCanBeNull) |
| **public** **void** | [setFixedFieldTypes](#setfixedfieldtypesboolean)(**boolean** fixedFieldTypes) |
| **public** **void** | [setIgnoreSyntheticFields](#setignoresyntheticfieldsboolean)(**boolean** ignoreSyntheticFields) |
| **public** **void** | [setOptimizedGenerics](#setoptimizedgenericsboolean)(**boolean** setOptimizedGenerics) |
| **public** **void** | [setSerializeTransient](#setserializetransientboolean)(**boolean** setSerializeTransient) |
| **public** **void** | [setUseAsm](#setuseasmboolean)(**boolean** setUseAsm) |

---


##Constructors
####FieldSerializer(Kryo, Class)
> 


---

####FieldSerializer(Kryo, Class, Class[])
> 


---

####FieldSerializer(Kryo, Class, Class[], FieldSerializerConfig)
> 


---


##Fields
####config
> **protected final** [FieldSerializerConfig](FieldSerializerConfig.md)

> 

---

####removedFields
> **protected** *java.util.HashSet*<[CachedField](CachedField.md)>

> 

---


##Methods
####compare(FieldSerializer.CachedField, FieldSerializer.CachedField)
> 


---

####create(Kryo, Input, Class<T>)
> Used by [FieldSerializer](FieldSerializer.md) to create the new object. This can be overridden to customize object creation, eg
 to call a constructor with arguments. The default implementation uses [Kryo](../Kryo.md).


---

####createCopy(Kryo, T)
> Used by [FieldSerializer](FieldSerializer.md) to create the new object. This can be overridden to customize object creation, eg to
 call a constructor with arguments. The default implementation uses [Kryo](../Kryo.md).


---

####getCachedFieldName(FieldSerializer.CachedField)
> 


---

####getCopyTransient()
> 


---

####getField(String)
> Allows specific fields to be optimized.


---

####getFields()
> Get all fields controlled by this FieldSerializer

> **Returns**
* all fields controlled by this FieldSerializer


---

####getGenerics()
> Get generic type parameters of the class controlled by this serializer.

> **Returns**
* generic type parameters or null, if there are none.


---

####getKryo()
> 


---

####getSerializeTransient()
> 


---

####getTransientFields()
> Get all transient fields controlled by this FieldSerializer

> **Returns**
* all transient fields controlled by this FieldSerializer


---

####getType()
> 


---

####getUseAsmEnabled()
> 


---

####getUseMemRegions()
> 


---

####initializeCachedFields()
> 


---

####rebuildCachedFields()
> Called when the list of cached fields must be rebuilt. This is done any time settings are changed that affect which fields
 will be used. It is called from the constructor for FieldSerializer, but not for subclasses. Subclasses must call this from
 their constructor.


---

####rebuildCachedFields(boolean)
> Rebuilds the list of cached fields.

> **Parameters**
* minorRebuild : if set, processing due to changes in generic type parameters will be optimized


---

####removeField(String)
> Removes a field so that it won't be serialized.


---

####removeField(FieldSerializer.CachedField)
> Removes a field so that it won't be serialized.


---

####setCopyTransient(boolean)
> 


---

####setFieldsAsAccessible(boolean)
> Controls which fields are serialized. Calling this method resets the [FieldSerializer](FieldSerializer.md).

> **Parameters**
* setFieldsAsAccessible : If true, all non-transient fields (inlcuding private fields) will be serialized and
           *java.lang.reflect.Field* if necessary (default). If false, only fields in the public
           API will be serialized.


---

####setFieldsCanBeNull(boolean)
> Sets the default value for [CachedField](CachedField.md). Calling this method resets the [FieldSerializer](FieldSerializer.md).

> **Parameters**
* fieldsCanBeNull : False if none of the fields are null. Saves 0-1 byte per field. True if it is not known (default).


---

####setFixedFieldTypes(boolean)
> Sets the default value for [CachedField](CachedField.md) to the field's declared type. This allows FieldSerializer to
 be more efficient, since it knows field values will not be a subclass of their declared type. Default is false. Calling this
 method resets the [FieldSerializer](FieldSerializer.md).


---

####setIgnoreSyntheticFields(boolean)
> Controls if synthetic fields are serialized. Default is true. Calling this method resets the [FieldSerializer](FieldSerializer.md).

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
> 


---

####setUseAsm(boolean)
> Controls whether ASM should be used. Calling this method resets the [FieldSerializer](FieldSerializer.md).

> **Parameters**
* setUseAsm : If true, ASM will be used for fast serialization. If false, Unsafe will be used (default)


---

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)