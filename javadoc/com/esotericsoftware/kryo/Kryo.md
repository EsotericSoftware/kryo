#Class Kryo
Package [com.esotericsoftware.kryo](README.md)<br>

> *java.lang.Object* > [Kryo](Kryo.md)



Maps classes to serializers so object graphs can be serialized automatically.


##Summary
####Fields
| Type and modifiers | Field name |
| --- | --- |
| **public static final** | [NOT_NULL](#not_null) |
| **public static final** | [NULL](#null) |

####Constructors
| Visibility | Signature |
| --- | --- |
| **public** | [Kryo](#kryo)() |
| **public** | [Kryo](#kryoreferenceresolver)([ReferenceResolver](ReferenceResolver.md) referenceResolver) |
| **public** | [Kryo](#kryoclassresolver-referenceresolver)([ClassResolver](ClassResolver.md) classResolver, [ReferenceResolver](ReferenceResolver.md) referenceResolver) |
| **public** | [Kryo](#kryoclassresolver-referenceresolver-streamfactory)([ClassResolver](ClassResolver.md) classResolver, [ReferenceResolver](ReferenceResolver.md) referenceResolver, [StreamFactory](StreamFactory.md) streamFactory) |

####Methods
| Type and modifiers | Method signature |
| --- | --- |
| **public** **void** | [addDefaultSerializer](#adddefaultserializerclass-serializer)(*java.lang.Class* type, [Serializer](Serializer.md) serializer) |
| **public** **void** | [addDefaultSerializer](#adddefaultserializerclass-serializerfactory)(*java.lang.Class* type, [SerializerFactory](factories/SerializerFactory.md) serializerFactory) |
| **public** **void** | [addDefaultSerializer](#adddefaultserializerclass-class)(*java.lang.Class* type, *java.lang.Class*<?> serializerClass) |
| **public** *java.lang.Object* | [copy](#copyt)(*java.lang.Object* object) |
| **public** *java.lang.Object* | [copy](#copyt-serializer)(*java.lang.Object* object, [Serializer](Serializer.md) serializer) |
| **public** *java.lang.Object* | [copyShallow](#copyshallowt)(*java.lang.Object* object) |
| **public** *java.lang.Object* | [copyShallow](#copyshallowt-serializer)(*java.lang.Object* object, [Serializer](Serializer.md) serializer) |
| **public** **boolean** | [getAsmEnabled](#getasmenabled)() |
| **public** *java.lang.ClassLoader* | [getClassLoader](#getclassloader)() |
| **public** [ClassResolver](ClassResolver.md) | [getClassResolver](#getclassresolver)() |
| **public** [ObjectMap](util/ObjectMap.md) | [getContext](#getcontext)() |
| **public** [Serializer](Serializer.md) | [getDefaultSerializer](#getdefaultserializerclass)(*java.lang.Class* type) |
| **protected** [Serializer](Serializer.md) | [getDefaultSerializerForAnnotatedType](#getdefaultserializerforannotatedtypeclass)(*java.lang.Class* type) |
| **public** **int** | [getDepth](#getdepth)() |
| **public** [FieldSerializerConfig](serializers/FieldSerializerConfig.md) | [getFieldSerializerConfig](#getfieldserializerconfig)() |
| **public** [GenericsResolver](serializers/GenericsResolver.md) | [getGenericsResolver](#getgenericsresolver)() |
| **public** [ObjectMap](util/ObjectMap.md) | [getGraphContext](#getgraphcontext)() |
| **public** *org.objenesis.strategy.InstantiatorStrategy* | [getInstantiatorStrategy](#getinstantiatorstrategy)() |
| **public** **int** | [getNextRegistrationId](#getnextregistrationid)() |
| **public** [IdentityMap](util/IdentityMap.md) | [getOriginalToCopyMap](#getoriginaltocopymap)() |
| **public** [ReferenceResolver](ReferenceResolver.md) | [getReferenceResolver](#getreferenceresolver)() |
| **public** **boolean** | [getReferences](#getreferences)() |
| **public** [Registration](Registration.md) | [getRegistration](#getregistrationclass)(*java.lang.Class* type) |
| **public** [Registration](Registration.md) | [getRegistration](#getregistrationint)(**int** classID) |
| **public** [Serializer](Serializer.md) | [getSerializer](#getserializerclass)(*java.lang.Class* type) |
| **public** [StreamFactory](StreamFactory.md) | [getStreamFactory](#getstreamfactory)() |
| **public** [TaggedFieldSerializerConfig](serializers/TaggedFieldSerializerConfig.md) | [getTaggedFieldSerializerConfig](#gettaggedfieldserializerconfig)() |
| **protected** **boolean** | [isClosure](#isclosureclass)(*java.lang.Class* type) |
| **public** **boolean** | [isFinal](#isfinalclass)(*java.lang.Class* type) |
| **public** **boolean** | [isRegistrationRequired](#isregistrationrequired)() |
| **public** **boolean** | [isWarnUnregisteredClasses](#iswarnunregisteredclasses)() |
| **protected** [Serializer](Serializer.md) | [newDefaultSerializer](#newdefaultserializerclass)(*java.lang.Class* type) |
| **public** *java.lang.Object* | [newInstance](#newinstanceclass)(*java.lang.Class*<> type) |
| **protected** *org.objenesis.instantiator.ObjectInstantiator* | [newInstantiator](#newinstantiatorclass)(*java.lang.Class* type) |
| **public** [Registration](Registration.md) | [readClass](#readclassinput)([Input](io/Input.md) input) |
| **public** *java.lang.Object* | [readClassAndObject](#readclassandobjectinput)([Input](io/Input.md) input) |
| **public** *java.lang.Object* | [readObject](#readobjectinput-class)([Input](io/Input.md) input, *java.lang.Class*<> type) |
| **public** *java.lang.Object* | [readObject](#readobjectinput-class-serializer)([Input](io/Input.md) input, *java.lang.Class*<> type, [Serializer](Serializer.md) serializer) |
| **public** *java.lang.Object* | [readObjectOrNull](#readobjectornullinput-class)([Input](io/Input.md) input, *java.lang.Class*<> type) |
| **public** *java.lang.Object* | [readObjectOrNull](#readobjectornullinput-class-serializer)([Input](io/Input.md) input, *java.lang.Class*<> type, [Serializer](Serializer.md) serializer) |
| **public** **void** | [reference](#referenceobject)(*java.lang.Object* object) |
| **public** [Registration](Registration.md) | [register](#registerclass)(*java.lang.Class* type) |
| **public** [Registration](Registration.md) | [register](#registerclass-int)(*java.lang.Class* type, **int** id) |
| **public** [Registration](Registration.md) | [register](#registerclass-serializer)(*java.lang.Class* type, [Serializer](Serializer.md) serializer) |
| **public** [Registration](Registration.md) | [register](#registerclass-serializer-int)(*java.lang.Class* type, [Serializer](Serializer.md) serializer, **int** id) |
| **public** [Registration](Registration.md) | [register](#registerregistration)([Registration](Registration.md) registration) |
| **public** **void** | [reset](#reset)() |
| **public** **void** | [setAsmEnabled](#setasmenabledboolean)(**boolean** flag) |
| **public** **void** | [setAutoReset](#setautoresetboolean)(**boolean** autoReset) |
| **public** **void** | [setClassLoader](#setclassloaderclassloader)(*java.lang.ClassLoader* classLoader) |
| **public** **void** | [setCopyReferences](#setcopyreferencesboolean)(**boolean** copyReferences) |
| **public** **void** | [setDefaultSerializer](#setdefaultserializerserializerfactory)([SerializerFactory](factories/SerializerFactory.md) serializer) |
| **public** **void** | [setDefaultSerializer](#setdefaultserializerclass)(*java.lang.Class*<?> serializer) |
| **public** **void** | [setInstantiatorStrategy](#setinstantiatorstrategyinstantiatorstrategy)(*org.objenesis.strategy.InstantiatorStrategy* strategy) |
| **public** **void** | [setMaxDepth](#setmaxdepthint)(**int** maxDepth) |
| **public** **void** | [setReferenceResolver](#setreferenceresolverreferenceresolver)([ReferenceResolver](ReferenceResolver.md) referenceResolver) |
| **public** **boolean** | [setReferences](#setreferencesboolean)(**boolean** references) |
| **public** **void** | [setRegistrationRequired](#setregistrationrequiredboolean)(**boolean** registrationRequired) |
| **public** **void** | [setStreamFactory](#setstreamfactorystreamfactory)([StreamFactory](StreamFactory.md) streamFactory) |
| **public** **void** | [setWarnUnregisteredClasses](#setwarnunregisteredclassesboolean)(**boolean** warnUnregisteredClasses) |
| **protected** *java.lang.String* | [unregisteredClassMessage](#unregisteredclassmessageclass)(*java.lang.Class* type) |
| **public** [Registration](Registration.md) | [writeClass](#writeclassoutput-class)([Output](io/Output.md) output, *java.lang.Class* type) |
| **public** **void** | [writeClassAndObject](#writeclassandobjectoutput-object)([Output](io/Output.md) output, *java.lang.Object* object) |
| **public** **void** | [writeObject](#writeobjectoutput-object)([Output](io/Output.md) output, *java.lang.Object* object) |
| **public** **void** | [writeObject](#writeobjectoutput-object-serializer)([Output](io/Output.md) output, *java.lang.Object* object, [Serializer](Serializer.md) serializer) |
| **public** **void** | [writeObjectOrNull](#writeobjectornulloutput-object-class)([Output](io/Output.md) output, *java.lang.Object* object, *java.lang.Class* type) |
| **public** **void** | [writeObjectOrNull](#writeobjectornulloutput-object-serializer)([Output](io/Output.md) output, *java.lang.Object* object, [Serializer](Serializer.md) serializer) |

---


##Constructors
####Kryo()
> Creates a new Kryo with a [DefaultClassResolver](util/DefaultClassResolver.md) and a [MapReferenceResolver](util/MapReferenceResolver.md).


---

####Kryo(ReferenceResolver)
> Creates a new Kryo with a [DefaultClassResolver](util/DefaultClassResolver.md).

> **Parameters**
* referenceResolver : May be null to disable references.


---

####Kryo(ClassResolver, ReferenceResolver)
> 

> **Parameters**
* referenceResolver : May be null to disable references.


---

####Kryo(ClassResolver, ReferenceResolver, StreamFactory)
> 

> **Parameters**
* referenceResolver : May be null to disable references.


---


##Fields
####NOT_NULL
> **public static final** **byte**

> 

---

####NULL
> **public static final** **byte**

> 

---


##Methods
####addDefaultSerializer(Class, Serializer)
> Instances of the specified class will use the specified serializer when [Kryo](Kryo.md) or
 [Kryo](Kryo.md) are called.


---

####addDefaultSerializer(Class, SerializerFactory)
> Instances of the specified class will use the specified factory to create a serializer when [Kryo](Kryo.md) or
 [Kryo](Kryo.md) are called.


---

####addDefaultSerializer(Class, Class<? extends Serializer>)
> Instances of the specified class will use the specified serializer when [Kryo](Kryo.md) or
 [Kryo](Kryo.md) are called. Serializer instances are created as needed via
 [ReflectionSerializerFactory](factories/ReflectionSerializerFactory.md). By default, the following classes have a default
 serializer set:
 
 <table>
 <tr>
 <td>boolean</td>
 <td>Boolean</td>
 <td>byte</td>
 <td>Byte</td>
 <td>char</td>
 <tr>
 </tr>
 <td>Character</td>
 <td>short</td>
 <td>Short</td>
 <td>int</td>
 <td>Integer</td>
 <tr>
 </tr>
 <td>long</td>
 <td>Long</td>
 <td>float</td>
 <td>Float</td>
 <td>double</td>
 <tr>
 </tr>
 <td>Double</td>
 <td>String</td>
 <td>byte[]</td>
 <td>char[]</td>
 <td>short[]</td>
 <tr>
 </tr>
 <td>int[]</td>
 <td>long[]</td>
 <td>float[]</td>
 <td>double[]</td>
 <td>String[]</td>
 <tr>
 </tr>
 <td>Object[]</td>
 <td>Map</td>
 <td>BigInteger</td>
 <td>BigDecimal</td>
 <td>KryoSerializable</td>
 </tr>
 <tr>
 <td>Collection</td>
 <td>Date</td>
 <td>Collections.emptyList</td>
 <td>Collections.singleton</td>
 <td>Currency</td>
 </tr>
 <tr>
 <td>StringBuilder</td>
 <td>Enum</td>
 <td>Collections.emptyMap</td>
 <td>Collections.emptySet</td>
 <td>Calendar</td>
 </tr>
 <tr>
 <td>StringBuffer</td>
 <td>Class</td>
 <td>Collections.singletonList</td>
 <td>Collections.singletonMap</td>
 <td>TimeZone</td>
 </tr>
 <tr>
 <td>TreeMap</td>
 <td>EnumSet</td>
 </tr>
 </table>
 
 Note that the order default serializers are added is important for a class that may match multiple types. The above default
 serializers always have a lower priority than subsequent default serializers that are added.


---

####copy(T)
> Returns a deep copy of the object. Serializers for the classes involved must support [Serializer](Serializer.md).

> **Parameters**
* object : May be null.


---

####copy(T, Serializer)
> Returns a deep copy of the object using the specified serializer. Serializers for the classes involved must support
 [Serializer](Serializer.md).

> **Parameters**
* object : May be null.


---

####copyShallow(T)
> Returns a shallow copy of the object. Serializers for the classes involved must support
 [Serializer](Serializer.md).

> **Parameters**
* object : May be null.


---

####copyShallow(T, Serializer)
> Returns a shallow copy of the object using the specified serializer. Serializers for the classes involved must support
 [Serializer](Serializer.md).

> **Parameters**
* object : May be null.


---

####getAsmEnabled()
> 


---

####getClassLoader()
> 


---

####getClassResolver()
> 


---

####getContext()
> Name/value pairs that are available to all serializers.


---

####getDefaultSerializer(Class)
> Returns the best matching serializer for a class. This method can be overridden to implement custom logic to choose a
 serializer.


---

####getDefaultSerializerForAnnotatedType(Class)
> 


---

####getDepth()
> Returns the number of child objects away from the object graph root.


---

####getFieldSerializerConfig()
> The default configuration for [FieldSerializer](serializers/FieldSerializer.md) instances. Already existing serializer instances (e.g. implicitely
 created for already registered classes) are not affected by this configuration. You can override the configuration for a
 single [FieldSerializer](serializers/FieldSerializer.md).


---

####getGenericsResolver()
> 


---

####getGraphContext()
> Name/value pairs that are available to all serializers and are cleared after each object graph is serialized or
 deserialized.


---

####getInstantiatorStrategy()
> 


---

####getNextRegistrationId()
> Returns the lowest, next available integer ID.


---

####getOriginalToCopyMap()
> Returns the internal map of original to copy objects when a copy method is used. This can be used after a copy to map old
 objects to the copies, however it is cleared automatically by [Kryo](Kryo.md) so this is only useful when
 [Kryo](Kryo.md) is false.


---

####getReferenceResolver()
> 

> **Returns**
* May be null.


---

####getReferences()
> 


---

####getRegistration(Class)
> If the class is not registered and [Kryo](Kryo.md) is false, it is automatically registered
 using the [Kryo](Kryo.md).

> **Throws**
* *java.lang.IllegalArgumentException* if the class is not registered and [Kryo](Kryo.md) is true.


---

####getRegistration(int)
> 


---

####getSerializer(Class)
> Returns the serializer for the registration for the specified class.


---

####getStreamFactory()
> 


---

####getTaggedFieldSerializerConfig()
> 


---

####isClosure(Class)
> Returns true if the specified type is a closure.
 
 This can be overridden to support alternative implementations of clousres. Current version supports Oracle's Java8 only


---

####isFinal(Class)
> Returns true if the specified type is final. Final types can be serialized more efficiently because they are
 non-polymorphic.
 
 This can be overridden to force non-final classes to be treated as final. Eg, if an application uses ArrayList extensively
 but never uses an ArrayList subclass, treating ArrayList as final could allow FieldSerializer to save 1-2 bytes per
 ArrayList field.


---

####isRegistrationRequired()
> 


---

####isWarnUnregisteredClasses()
> 


---

####newDefaultSerializer(Class)
> Called by [Kryo](Kryo.md) when no default serializers matched the type. Subclasses can override this
 method to customize behavior. The default implementation calls [SerializerFactory](factories/SerializerFactory.md) using
 the [Kryo](Kryo.md).


---

####newInstance(Class<T>)
> Creates a new instance of a class using [Registration](Registration.md). If the registration's instantiator is null,
 a new one is set using [Kryo](Kryo.md).


---

####newInstantiator(Class)
> Returns a new instantiator for creating new instances of the specified type. By default, an instantiator is returned that
 uses reflection if the class has a zero argument constructor, an exception is thrown. If a
 [Kryo](Kryo.md) is set, it will be used instead of throwing an exception.


---

####readClass(Input)
> Reads a class and returns its registration.

> **Returns**
* May be null.


---

####readClassAndObject(Input)
> Reads the class and object or null using the registered serializer.

> **Returns**
* May be null.


---

####readObject(Input, Class<T>)
> Reads an object using the registered serializer.


---

####readObject(Input, Class<T>, Serializer)
> Reads an object using the specified serializer. The registered serializer is ignored.


---

####readObjectOrNull(Input, Class<T>)
> Reads an object or null using the registered serializer.

> **Returns**
* May be null.


---

####readObjectOrNull(Input, Class<T>, Serializer)
> Reads an object or null using the specified serializer. The registered serializer is ignored.

> **Returns**
* May be null.


---

####reference(Object)
> Called by [Serializer](Serializer.md) and [Serializer](Serializer.md) before Kryo can be used to
 deserialize or copy child objects. Calling this method is unnecessary if Kryo is not used to deserialize or copy child
 objects.

> **Parameters**
* object : May be null, unless calling this method from [Serializer](Serializer.md).


---

####register(Class)
> Registers the class using the lowest, next available integer ID and the [Kryo](Kryo.md). If the class is already registered, no change will be made and the existing registration will be returned.
 Registering a primitive also affects the corresponding primitive wrapper.
 
 Because the ID assigned is affected by the IDs registered before it, the order classes are registered is important when
 using this method. The order must be the same at deserialization as it was for serialization.


---

####register(Class, int)
> Registers the class using the specified ID and the [Kryo](Kryo.md). If the
 class is already registered this has no effect and the existing registration is returned. Registering a primitive also
 affects the corresponding primitive wrapper.
 
 IDs must be the same at deserialization as they were for serialization.

> **Parameters**
* id : Must be >= 0. Smaller IDs are serialized more efficiently. IDs 0-8 are used by default for primitive types and
           String, but these IDs can be repurposed.


---

####register(Class, Serializer)
> Registers the class using the lowest, next available integer ID and the specified serializer. If the class is already
 registered, the existing entry is updated with the new serializer. Registering a primitive also affects the corresponding
 primitive wrapper.
 
 Because the ID assigned is affected by the IDs registered before it, the order classes are registered is important when
 using this method. The order must be the same at deserialization as it was for serialization.


---

####register(Class, Serializer, int)
> Registers the class using the specified ID and serializer. Providing an ID that is already in use by the same type will
 cause the old entry to be overwritten. Registering a primitive also affects the corresponding primitive wrapper.
 
 IDs must be the same at deserialization as they were for serialization.

> **Parameters**
* id : Must be >= 0. Smaller IDs are serialized more efficiently. IDs 0-8 are used by default for primitive types and
           String, but these IDs can be repurposed.


---

####register(Registration)
> Stores the specified registration. If the ID is already in use by the same type, the old entry is overwritten. Registering
 a primitive also affects the corresponding primitive wrapper.
 
 IDs must be the same at deserialization as they were for serialization.
 
 Registration can be suclassed to efficiently store per type information, accessible in serializers via
 [Kryo](Kryo.md).


---

####reset()
> Resets unregistered class names, references to previously serialized or deserialized objects, and the
 [Kryo](Kryo.md). If [Kryo](Kryo.md) is true, this method is called
 automatically when an object graph has been completely serialized or deserialized. If overridden, the super method must be
 called.


---

####setAsmEnabled(boolean)
> Tells Kryo, if ASM-based backend should be used by new serializer instances created using this Kryo instance. Already
 existing serializer instances are not affected by this setting.
 
 
 By default, Kryo uses ASM-based backend.
 

> **Parameters**
* flag : if true, ASM-based backend will be used. Otherwise Unsafe-based backend could be used by some serializers, e.g.
           FieldSerializer


---

####setAutoReset(boolean)
> If true (the default), [Kryo](Kryo.md) is called automatically after an entire object graph has been read or written. If
 false, [Kryo](Kryo.md) must be called manually, which allows unregistered class names, references, and other information to
 span multiple object graphs.


---

####setClassLoader(ClassLoader)
> Sets the classloader to resolve unregistered class names to classes. The default is the loader that loaded the Kryo
 class.


---

####setCopyReferences(boolean)
> If true, when [Kryo](Kryo.md) and other copy methods encounter an object for the first time the object is copied and
 on subsequent encounters the copied object is used. If false, the overhead of tracking which objects have already been
 copied is avoided because each object is copied every time it is encountered, however a stack overflow will occur if an
 object graph is copied that contains a circular reference. Default is true.


---

####setDefaultSerializer(SerializerFactory)
> Sets the serializer factory to use when no [Kryo](Kryo.md) match an
 object's type. Default is [ReflectionSerializerFactory](factories/ReflectionSerializerFactory.md) with [FieldSerializer](serializers/FieldSerializer.md).


---

####setDefaultSerializer(Class<? extends Serializer>)
> Sets the serializer to use when no [Kryo](Kryo.md) match an object's type.
 Default is [FieldSerializer](serializers/FieldSerializer.md).


---

####setInstantiatorStrategy(InstantiatorStrategy)
> Sets the strategy used by [Kryo](Kryo.md) for creating objects. See *org.objenesis.strategy.StdInstantiatorStrategy* to
 create objects via without calling any constructor. See *org.objenesis.strategy.SerializingInstantiatorStrategy* to mimic Java's built-in
 serialization.

> **Parameters**
* strategy : May be null.


---

####setMaxDepth(int)
> Sets the maxiumum depth of an object graph. This can be used to prevent malicious data from causing a stack overflow.
 Default is *java.lang.Integer*.


---

####setReferenceResolver(ReferenceResolver)
> Sets the reference resolver and enables references.


---

####setReferences(boolean)
> If true, each appearance of an object in the graph after the first is stored as an integer ordinal. When set to true,
 [MapReferenceResolver](util/MapReferenceResolver.md) is used. This enables references to the same object and cyclic graphs to be serialized, but
 typically adds overhead of one byte per object. Default is true.

> **Returns**
* The previous value.


---

####setRegistrationRequired(boolean)
> If true, an exception is thrown when an unregistered class is encountered. Default is false.
 
 If false, when an unregistered class is encountered, its fully qualified class name will be serialized and the
 [Kryo](Kryo.md) for the class used to serialize the object. Subsequent
 appearances of the class within the same object graph are serialized as an int id.
 
 Registered classes are serialized as an int id, avoiding the overhead of serializing the class name, but have the drawback
 of needing to know the classes to be serialized up front.


---

####setStreamFactory(StreamFactory)
> 


---

####setWarnUnregisteredClasses(boolean)
> If true, kryo writes a warn log telling about the classes unregistered. Default is false.
 
 If false, no log are written when unregistered classes are encountered.
 


---

####unregisteredClassMessage(Class)
> 


---

####writeClass(Output, Class)
> Writes a class and returns its registration.

> **Parameters**
* type : May be null.

> **Returns**
* Will be null if type is null.


---

####writeClassAndObject(Output, Object)
> Writes the class and object or null using the registered serializer.

> **Parameters**
* object : May be null.


---

####writeObject(Output, Object)
> Writes an object using the registered serializer.


---

####writeObject(Output, Object, Serializer)
> Writes an object using the specified serializer. The registered serializer is ignored.


---

####writeObjectOrNull(Output, Object, Class)
> Writes an object or null using the registered serializer for the specified type.

> **Parameters**
* object : May be null.


---

####writeObjectOrNull(Output, Object, Serializer)
> Writes an object or null using the specified serializer. The registered serializer is ignored.

> **Parameters**
* object : May be null.


---

---

[![Marklet](https://img.shields.io/badge/Generated%20by-Marklet-green.svg)](https://github.com/Faylixe/marklet)