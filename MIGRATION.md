# Migrating from Kryo 5 to Kryo 6

Kryo 6 has not been released yet. This document collects the changes that affect users of Kryo 5. Pull requests that change the serialized format, the behavior or the public API add an entry here.

## Key changes

* Kryo 6 requires Java 17 and Android 8.0 (API level 26), see [Requirements](#requirements).
* Kryo has no required dependencies anymore: Objenesis is optional, ReflectASM and MinLog are gone. The imports of `Log` and of the instantiator strategies change, see [Import changes](#import-changes).
* Data written by Kryo 5 can be read with `Kryo5Compatibility.configure(kryo)`, as far as Kryo's default serializers are concerned, see [Reading data written by Kryo 5](#reading-data-written-by-kryo-5). The Kryo 6 format is smaller and faster, so write data again with it where you can.
* The default format changed in a few places: strings are written without references, records are serialized by FieldSerializer, maps and chunked encoding write less, enums with constant bodies are treated as final, see [Serialization format](#serialization-format).
* On Java 24+, fields are accessed with VarHandles instead of `sun.misc.Unsafe`, so Java doesn't warn about Kryo, and the code that reads and writes the fields of a class can be generated, which is 45% to 120% faster, see [Field access](#field-access).
* Default serializers for about 20 more JDK types, eg `UUID`, `Pattern`, the atomic types, `ByteBuffer`, `EnumMap`, the blocking queues and the unmodifiable and synchronized collections, see [New default serializers](#new-default-serializers).
* The synthetic fields of anonymous, local and inner classes are serialized, so these objects work after reading, see [FieldSerializer and its subclasses](#fieldserializer-and-its-subclasses).
* Data that Kryo 5 wrote or read wrongly without an exception now throws one: duplicate field names in CompatibleFieldSerializer, data from a newer class version in VersionFieldSerializer, collections modified while they are written, closures without ClosureSerializer. CompatibleFieldSerializer reads a serialized null as null instead of keeping the constructor's value. See [Behavior changes](#behavior-changes).

## Import changes

Most code that compiles with Kryo 5 compiles with Kryo 6 unchanged. Two imports change, because Kryo no longer depends on MinLog and Objenesis.

Kryo's logging uses its own `Log` class, a copy of MinLog with the same API:

```java
// Kryo 5
import com.esotericsoftware.minlog.Log;
// Kryo 6
import com.esotericsoftware.kryo.util.Log;

Log.set(Log.LEVEL_WARN);
```

The instantiator strategies are Kryo's own classes with the same names:

```java
// Kryo 5
import org.objenesis.strategy.StdInstantiatorStrategy;
// Kryo 6
import com.esotericsoftware.kryo.util.StdInstantiatorStrategy;

kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
```

The same applies to `SerializingInstantiatorStrategy`, and to `InstantiatorStrategy` and `ObjectInstantiator` for custom strategies, which are all in `com.esotericsoftware.kryo.util` now. Other Objenesis strategies are wrapped in `ObjenesisStrategy`, see [Changed APIs](#changed-apis). With the versioned jar, the packages change from `com.esotericsoftware.kryo.kryo5` to `com.esotericsoftware.kryo.kryo6`, eg `com.esotericsoftware.kryo.kryo6.util.Log` instead of `com.esotericsoftware.kryo.kryo5.minlog.Log`.

## Requirements

Kryo 6 requires Java 17 or later, and Android 8.0 (API level 26) or later, see [Android](README.md#android). See [Installation](README.md#installation) for the Maven coordinates of the regular and the versioned jar.

Kryo 6 has no required dependencies. [Objenesis](http://objenesis.org/) is an optional dependency of the regular jar, needed only on Android and other JVMs without the JDK's serialization constructors, see [Changed APIs](#changed-apis). The versioned jar includes it as before.

## Reading data written by Kryo 5

Kryo 6 changes the default serialized format in the places described under [Serialization format](#serialization-format). To read data written by Kryo 5, configure Kryo after setting the default serializer:

```java
Kryo kryo = new Kryo();
kryo.setDefaultSerializer(...); // If not FieldSerializer.
Kryo5Compatibility.configure(kryo);
```

This restores the Kryo 5 format for Kryo's default serializers: it enables references for strings, uses RecordSerializer for records, the serializers Kryo 5 used for the types that have new default serializers, the Kryo 5 formats of MapSerializer and of the chunked encoding, treats enums with constant bodies as not final and ignores synthetic fields. Serializers that are registered explicitly or added as default serializers later need these settings themselves, they are listed in each section below.

`Kryo5Compatibility` is a best effort to read existing data with the default serializers. There is no guarantee that it covers a setup with custom serializers, custom class or reference resolvers, or settings that were changed after it was applied, and some Kryo 5 data can't be read at all, eg records with generic components or unmodifiable collections, see the sections below. Use it to keep reading data during a transition, or to read the data once and write it again with the Kryo 6 format, which is smaller, faster and loses no data in skipped fields. Like with the migration from Kryo 4 to Kryo 5, reading data across major versions isn't guaranteed.

Kryo 5 can read data written with this configuration, unless it contains locales with a script or generic types that Kryo 6 resolves but Kryo 5 ignored, see [Collections and maps](#collections-and-maps).

Like in Kryo 5, String fields of FieldSerializer and its subclasses decide when the serializer is created whether they use references: Kryo 5 wrote String fields without references if `setReferences(true)` was called after registering the classes, but strings in collections and arrays with references. To read such data, keep the order of `setReferences` and `register` the Kryo 5 code used, and call `Kryo5Compatibility.configure` before registering classes. Unlike Kryo 5, Kryo 6 throws an exception if `setReferences`, `setReferenceResolver` or `Kryo5Compatibility.configure` changes whether strings use references after a String field was created, because the data of that field would differ from the data of a Kryo instance that was configured before registering the classes.

## Serialization format

The following changes affect data serialized with Kryo 5, the most common first. Each section says how to read the Kryo 5 data without `Kryo5Compatibility`.

### Strings with references

With references enabled, Kryo's reference resolvers no longer use references for strings: strings are rarely shared, so tracking them costs more than it saves (eg about 17% throughput for an object graph with many strings). The data is usually smaller, because no reference marker is written before each string. Strings that are shared by identity are now written each time, so data with many shared strings can grow. To read data written by Kryo 5 with references, use references for strings:

```java
kryo.setReferenceResolver(new MapReferenceResolver() {
	public boolean useReferences (Class type) {
		return !Util.isWrapperClass(type) && !Util.isEnum(type);
	}
});
```

`Kryo5Compatibility` does this for Kryo's reference resolvers, also if none has been set yet. Subclasses of them and custom reference resolvers decide in `useReferences` as before.

### Records

Kryo 5 serialized records with RecordSerializer, Kryo 6 serializes them with FieldSerializer and its subclasses, see [Records](README.md#records). RecordSerializer is no longer a default serializer and is deprecated. With the default configuration, FieldSerializer writes the same data, except for components with generic types, such as `List<String>`. FieldSerializer uses the type arguments to avoid writing the class of each element, while RecordSerializer wrote it. FieldSerializer cannot correctly read such records written by Kryo 5, and may even read them without an exception but with wrong values. To read such data, register RecordSerializer for the affected records, or for all records:

```java
kryo.register(SomeRecord.class, new RecordSerializer<>(SomeRecord.class));
// or for all records
kryo.addDefaultSerializer(Record.class, RecordSerializer.class);
```

If a record's canonical constructor throws an exception during deserialization, that exception is the cause of the KryoException. RecordSerializer added an InvocationTargetException in between.

### Maps

If the class of the keys or values of a map is unknown, MapSerializer writes it only once if all keys or values are not null and have the same class. Kryo 5 wrote the class of each key and value. If the map contains no null key or value, which is written with the size of the map, keys and values are written without a null marker. Kryo 5 wrote a null marker for each key or value whose serializer doesn't accept null, eg for the values of a `Map<String, Integer>` field, and for each with references. To read data written by Kryo 5, disable this for the MapSerializer instances, eg for all maps that use the default MapSerializer:

```java
MapSerializer mapSerializer = new MapSerializer();
mapSerializer.setWriteSameClassOnce(false);
kryo.addDefaultSerializer(Map.class, mapSerializer);
```

Subclasses like TreeMapSerializer need the same setting.

### New default serializers

Kryo 6 adds default serializers for these JDK types, see [Default serializers](README.md#default-serializers) for the complete list:

* `Timestamp`, `URI`, `UUID`, `Pattern`, `AtomicBoolean`, `AtomicInteger`, `AtomicLong`, `AtomicReference` and `ConcurrentHashMap.KeySetView`. Kryo 5 serialized `Timestamp` with DateSerializer, which drops the nanoseconds, `KeySetView` with CollectionSerializer, which couldn't read it back, and the other types with the default serializer, usually FieldSerializer. Subclasses of the atomic types that declare non-transient fields still use the default serializer, because the new serializers would lose these fields. With FieldSerializer, that needs `--add-opens java.base/java.util.concurrent.atomic=ALL-UNNAMED` on Java 16+ like in Kryo 5, because it reads the private value field of the atomic type. To read data written by Kryo 5, register the serializers Kryo 5 used for these types:

  ```java
  kryo.register(Timestamp.class, new DateSerializer());
  kryo.register(UUID.class, new FieldSerializer<>(kryo, UUID.class));
  ```

  These serializers were already available in Kryo 5, eg `UUIDSerializer`. If you registered them with Kryo 5, keep registering them, because `Kryo5Compatibility` only applies what Kryo 5 used by default. Registered serializers take precedence over its settings.
* `ConcurrentSkipListSet` and `PriorityBlockingQueue`, which write the comparator like the serializers for `TreeSet` and `PriorityQueue`. Kryo 5 serialized them with CollectionSerializer, which lost the comparator. `Kryo5Compatibility` uses CollectionSerializer for them, or register it for both to read data written by Kryo 5:

  ```java
  kryo.register(ConcurrentSkipListSet.class, new CollectionSerializer());
  kryo.register(PriorityBlockingQueue.class, new CollectionSerializer());
  ```
* `ArrayBlockingQueue`, `LinkedBlockingQueue` and `LinkedBlockingDeque`, which write the capacity. Kryo 5 couldn't read an `ArrayBlockingQueue`, and read a `LinkedBlockingQueue` or `LinkedBlockingDeque` with CollectionSerializer, which lost the capacity. `Kryo5Compatibility` uses CollectionSerializer for `LinkedBlockingQueue` and `LinkedBlockingDeque`.
* `EnumMap`, which writes the enum type of the keys. Kryo 5 couldn't serialize an `EnumMap` without registering an `EnumMapSerializer` for its enum type, which is unchanged.
* `ByteBuffer`, for heap and direct buffers, which keeps the position, limit, capacity, byte order and whether it is read-only. Kryo 5 serialized heap buffers with FieldSerializer, which needs StdInstantiatorStrategy and on Java 17+ `--add-opens java.base/java.nio=ALL-UNNAMED`, and couldn't serialize direct buffers. `Kryo5Compatibility` uses the default serializer for `ByteBuffer`, like Kryo 5.
* The set returned by `Collections.newSetFromMap`, which writes the map that backs it, except on Android. Like for the unmodifiable and synchronized collections, the map is read from a private JDK field. Kryo 5 serialized the set with CollectionSerializer, which couldn't read it back, so there is no readable Kryo 5 data for it.
* `File`, `InetAddress` and `InetSocketAddress`, which use only public API and don't look up host names. Kryo 5 serialized them with FieldSerializer, which can't access their fields on Java 17+ without `--add-opens`, and even with it lost the IP address of `InetAddress` and couldn't create `File` and `InetSocketAddress` without StdInstantiatorStrategy. `Kryo5Compatibility` uses the default serializer for them, like Kryo 5, so the data stays aligned. Without it, data written by Kryo 5 can be read with FileSerializer only for `File` without references, because FileSerializer writes the path like FieldSerializer did.
* The unmodifiable and synchronized collections returned by `Collections`, eg `Collections.unmodifiableList`, except on Android. Kryo 5 serialized them with CollectionSerializer or MapSerializer, which couldn't read them back, so there is no readable Kryo 5 data for them. If you added these serializers with `addDefaultSerializers` in Kryo 5, the data is the same. See [Unmodifiable and synchronized collections](README.md#unmodifiable-and-synchronized-collections) for the JDK internals they access.
* The comparators returned by `Collections.reverseOrder()`, `Collections.reverseOrder(Comparator)` and `String.CASE_INSENSITIVE_ORDER`, eg as the comparator of a `TreeSet`, and `java.sql.Date` and `java.sql.Time`. They write the same data as Kryo 5 where it could serialize these types, without reflection.

### Chunked encoding

CompatibleFieldSerializer and TaggedFieldSerializer with chunked encoding write each field with its length instead of in chunks. The class names and field names first written in an object are written before it, and with references the number of objects in each field is written too. So skipping a field, eg because the class of a removed field no longer exists, no longer breaks reading the rest of the data ([#1247](https://github.com/EsotericSoftware/kryo/issues/1247)). Kryo 5 lost class names, field names, and references first written in a skipped chunk, also if the classes were registered. The new format is smaller and faster.

The outermost object with chunked encoding is buffered until it is written completely, so it must be smaller than 2 GiB, Kryo 5 wrote it to the output in chunks. The field names are identified by class instead of their position, so a nested object can only be read as a different class than it was written if its class is written, which `readUnknownFieldData` (the default) does. Kryo 5 could read it, eg with `readUnknownFieldData` false after the final type of a field changed, although changing the type of a field is not supported.

To read data written by Kryo 5 with chunked encoding, use `Kryo5Compatibility` or enable `legacyChunks`, which is deprecated because it is only needed for that:

```java
CompatibleFieldSerializerConfig config = new CompatibleFieldSerializerConfig();
config.setChunkedEncoding(true);
config.setLegacyChunks(true);
kryo.setDefaultSerializer(new CompatibleFieldSerializerFactory(config));
```

A custom ClassResolver needs to implement `beginDeferredNames`, `endDeferredNames`, and `readDeferredNames`, and a custom ReferenceResolver `getObjectCount`, otherwise class names first written in a skipped field are lost, and the reference IDs of the objects read after a skipped field are shifted, like in Kryo 5. The new format is used anyway. DefaultClassResolver and the reference resolvers of Kryo implement them.

### Generic fields with CompatibleFieldSerializer and TaggedFieldSerializer

CompatibleFieldSerializer with `readUnknownFieldData` (the default) and TaggedFieldSerializer with `readUnknownTagData` no longer use the generic type of a field to optimize its value. Kryo 5 omitted the class of collection elements and map keys and values if the field's type arguments were final, eg `List<String>`, so the value could not be read anymore once the field was removed ([#1098](https://github.com/EsotericSoftware/kryo/issues/1098)). To read such data written by Kryo 5, enable `optimizeGenerics`, which `Kryo5Compatibility` does. This restores the Kryo 5 behavior, so data with such a field that has since been removed can only be read if it was written with chunked encoding. The setting is deprecated, because it is only needed for that: the collection and map serializers write the class of the elements once per collection anyway, so the optimization saves almost nothing.

```java
CompatibleFieldSerializerConfig config = new CompatibleFieldSerializerConfig();
config.setOptimizeGenerics(true);
kryo.setDefaultSerializer(new CompatibleFieldSerializerFactory(config));
```

### Enums with constant bodies

The class of an enum value is not written if the enum is known, eg from the type of a field, also if its constants have bodies like `PLUS { ... }`. Kryo 5 wrote the class for these enums, because they aren't final, so adding or removing a body changed the serialized bytes ([#454](https://github.com/EsotericSoftware/kryo/issues/454)). The name of an unregistered enum constant with a body is the name of its enum, which Kryo 5 can read too. To read data written by Kryo 5, set this before registering classes, which `Kryo5Compatibility` does. The setting is deprecated, because it is only needed for that:

```java
kryo.setEnumsFinal(false);
```

### Immutable lists

The serializer for immutable lists created by `List.of` and `Stream.toList` supports null elements, which `Stream.toList` allows ([#1239](https://github.com/EsotericSoftware/kryo/issues/1239)). It writes whether a list contains null elements. To read data written by Kryo 5, disable null elements:

```java
((CollectionSerializer)kryo.getSerializer(List.of().getClass())).setElementsCanBeNull(false);
```

### Locales with a script

LocaleSerializer writes locales with a script, eg `sr-Cyrl-RS`, as a language tag, so the script and extensions are kept ([#1053](https://github.com/EsotericSoftware/kryo/issues/1053)). Kryo 5 lost them. Other locales are written as before, so Kryo 6 reads all locales written by Kryo 5.

## Behavior changes

These changes don't affect the serialized format of the default configuration, except where a change says so, eg the generic types that Kryo 6 resolves but Kryo 5 ignored. They are grouped by the part of Kryo they concern, the most common first.

### Field access

* On Java 24+, FieldSerializer and its subclasses access fields with VarHandles instead of `sun.misc.Unsafe`, which Java deprecated for removal and warns about. Before Java 24, or if Unsafe memory access is allowed with `--sun-misc-unsafe-memory-access=allow`, they use Unsafe like Kryo 5. With VarHandles, Kryo defines a small hidden class per field, which makes them nearly as fast as Unsafe, and final fields are set with reflection, which Java 26+ warns about, see [FieldSerializer settings](README.md#fieldserializer-settings). To always use Unsafe, configure the field access, eg for the default serializer: `config.setFieldAccess(FieldAccessType.UNSAFE); kryo.setDefaultSerializer(new FieldSerializerFactory(config));`, or set the system property `kryo.fieldAccess=UNSAFE`, which overrides the default for all Kryo instances. With `-Dkryo.unsafe=false`, fields are accessed with VarHandles, and `-Dkryo.hiddenFields=false` disables the hidden classes. Kryo 5 used ReflectASM for public classes and reflection otherwise.
* If Java denies setting final fields with reflection, which is the default in a future Java version (JEP 500, `--illegal-final-field-mutation=deny`), FieldSerializer and its subclasses set the final fields of serializable classes with the method handles that Java provides for deserialization, on Java 24+. This is slower than reflection, so allow setting final fields with `--enable-final-field-mutation=ALL-UNNAMED` (or the name of Kryo's module) if you can. Final fields that are transient or declared in a class that isn't serializable can't be set this way and fail if it is denied, see [FieldSerializer settings](README.md#fieldserializer-settings). With Unsafe field access, final fields are set with Unsafe like in Kryo 5.
* On Java 24+, FieldSerializer and its subclasses can generate the code that writes and reads the fields of a class, which is 45% to 120% faster than the loop over the cached fields and writes the same bytes. It is off by default: `config.setCodeGeneration(true)`, or the system property `kryo.codeGeneration=true`, see [FieldSerializer settings](README.md#fieldserializer-settings).

### FieldSerializer and its subclasses

* FieldSerializer and its subclasses serialize the synthetic fields of anonymous classes, local classes and non-static member classes: the outer instance and captured variables, without which these objects are unusable after reading. Kryo 5 ignored all synthetic fields (`ignoreSyntheticFields`), so the outer instance was null after reading. An inner object that is serialized with its outer instance usually needs references, because the outer instance refers to the inner object. Then the exception for the stack overflow explains this. TaggedFieldSerializer serializes only fields with `@Tag`, so it never serializes synthetic fields. To ignore synthetic fields like Kryo 5, set `ignoreSyntheticFields` to true, which `Kryo5Compatibility` does for the default serializer. `FieldSerializerConfig#getIgnoreSyntheticFields` returns a `Boolean`, which is null unless the setting was set.
* `@Bind` and `@NotNull` are applied to String fields with all field access types. Kryo 5 ignored them for a String field without references if it accessed the field with Unsafe or ReflectASM, and applied them with reflection, so the data depended on the field access. A String field with `@Bind` and a serializer that Kryo 5 wrote this way was written as a plain string, to read it remove the serializer from the annotation. A null value of a String field with `@NotNull` throws an exception when it is written.
* `@NotNull` is respected on fields that also have `@Bind`. Kryo 5 ignored it because `@Bind` always set `canBeNull`, so these fields are serialized without the null marker.
* FieldSerializer with `setFieldsAsAccessible(false)` serializes the public, non-final fields of public classes. Kryo 5 serialized no fields at all with this setting.
* With Unsafe field access, FieldSerializer and its subclasses no longer store a value read whose class is not assignable to the field type, eg after the type of a field changed ([#1068](https://github.com/EsotericSoftware/kryo/issues/1068)). They throw an exception, or skip the field if CompatibleFieldSerializer uses chunked encoding. Kryo 5 stored the value anyway, which could crash the JVM when the field was used. With reflection and VarHandles, an exception was already thrown.
* FieldSerializer and its subclasses share the `Field` objects and the generic types of the fields of a class between all Kryo instances, which saves memory for applications with many classes and Kryo instances. `CachedField#getField` returns the shared `Field`, and the arrays returned by `GenericType#getTypeParameters` and `Generics#nextGenericTypes` must not be modified. Not on Android.
* A subclass of a field serializer can remove fields in `initializeCachedFields`, as its javadoc says. With TaggedFieldSerializer and VersionFieldSerializer this threw "Field not found" in Kryo 5, because their `removeField` called `initializeCachedFields` again. It no longer does, like FieldSerializer and CompatibleFieldSerializer never did in Kryo 5, so a subclass of these two serializers that derives something from the fields there needs to override `removeField` too.

### CompatibleFieldSerializer, TaggedFieldSerializer and VersionFieldSerializer

* CompatibleFieldSerializer with `readUnknownFieldData` (the default) and TaggedFieldSerializer with `readUnknownTagData` read a serialized `null` value as `null`. Kryo 5 skipped the field, so it kept the value assigned by the constructor ([#851](https://github.com/EsotericSoftware/kryo/issues/851)).
* CompatibleFieldSerializer throws an exception when serializing or deserializing a class that declares a field with the same name as a super class, unless `extendedFieldNames` is true. Kryo 5 mixed up the values of these fields when reading ([#699](https://github.com/EsotericSoftware/kryo/issues/699)).
* CompatibleFieldSerializer checks that the class of a value read is assignable to the field type for every object. Kryo 5 checked it only after the field had been read or written once, so the first object could fail with a ClassCastException or get a value of the wrong type, instead of throwing "Read type is incompatible" or, with chunked encoding, skipping the field. Proxies, which are written as `InvocationHandler`, are compatible with fields of an interface type, like closures. Kryo 5 rejected them, except for the first object read.
* VersionFieldSerializer throws an exception when reading an object written by a newer version of its class ([#846](https://github.com/EsotericSoftware/kryo/issues/846)). Kryo 5 read it without an exception, but the fields added in the newer version were read into other fields or left unread, so the values and the following data were wrong.

### Collections and maps

* CollectionSerializer and MapSerializer throw an exception if the number of elements written doesn't match the size written before them, eg because the collection was modified concurrently ([#1181](https://github.com/EsotericSoftware/kryo/issues/1181)). Kryo 5 wrote data that couldn't be read correctly, which often failed later with an unrelated error, such as an unregistered class ID or a buffer underflow.
* The type parameters of a class are resolved from the declared type, eg the type of a field, through the super classes and interfaces of the class. Kryo 5 assigned the type arguments of the declared type by position to the type parameters of the class of the value, which failed with a ClassCastException when they didn't match, eg for a `Base<String, Integer>` field holding a `Sub<A, B> extends Base<B, A>`, and ignored them when their number didn't match. When Kryo 6 resolves a type parameter that Kryo 5 ignored, the serialized bytes can differ. Likewise, MapSerializer and CollectionSerializer resolve the key, value and element types from the declared type, eg `Integer` keys for a field of type `class IntMap<V> extends HashMap<Integer, V>`. Kryo 5 used the type arguments of the declared type by position, which failed when they didn't match those of `Map` or `Collection` ([#860](https://github.com/EsotericSoftware/kryo/issues/860)).
* The serializers for unmodifiable and synchronized collections read the wrapped collection with method handles if `java.util` is open to Kryo, otherwise with Unsafe. If neither is allowed, they throw an exception on first use that explains how to allow it. Kryo 5 failed to register them without Unsafe.

### Other

* Serializing or copying a closure throws an exception if `ClosureSerializer.Closure` is not registered, also if `registrationRequired` is false ([#1137](https://github.com/EsotericSoftware/kryo/issues/1137)). Kryo 5 registered the closure's class implicitly and wrote it with the default serializer, which can't be read because the class of a closure can't be found by its name.
* ClosureSerializer accesses no JDK internals: it no longer needs `--add-opens java.base/java.lang.invoke=ALL-UNNAMED` and logs no warnings without it. It resolves closures like Java serialization, with the `$deserializeLambda$` method of the capturing class, and caches the reflective access per class, so writing a closure is as fast as in Kryo 5 with the JDK internals open and 2.5x faster without. On the module path, the package of the capturing class must be open to Kryo, like for the private fields of a class.
* If the class of a class name is not found, a later reference to the same class name throws the same exception. Kryo 5 read the following bytes as a class name.
* DefaultInstantiatorStrategy and BeanSerializer use method handles instead of ReflectASM. If a constructor throws an exception, that exception is the cause of the KryoException, like with ReflectASM in Kryo 5.

## API changes

### Changed APIs

* The instantiator API uses Kryo's own interfaces instead of the Objenesis ones: `InstantiatorStrategy` and `ObjectInstantiator` in `com.esotericsoftware.kryo.util`, with the same methods, and `StdInstantiatorStrategy` and `SerializingInstantiatorStrategy` in that package, so `new DefaultInstantiatorStrategy(new StdInstantiatorStrategy())` only needs the import changed from `org.objenesis.strategy`. On the JDK they create objects like Objenesis did, with the serialization constructors of `sun.reflect.ReflectionFactory`, so Objenesis is no longer needed there and is an optional dependency of the regular jar. It is still used on Android and other JVMs without these constructors: add `org.objenesis:objenesis` there. Other Objenesis strategies are used with `ObjenesisStrategy`. The versioned jar includes Objenesis as before.
* A custom ClassResolver needs to implement `beginDeferredNames`, `endDeferredNames`, and `readDeferredNames`, and a custom ReferenceResolver `getObjectCount`, for the [chunked encoding](#chunked-encoding) to keep the class names and references of skipped fields.
* `Generics#pushTypeVariables(GenericsHierarchy, GenericType[])` was removed. Use `pushTypeVariables(GenericsHierarchy, GenericType)` with the declared type returned by the new `Generics#nextGenericType()`.
* `FieldSerializer.CachedField` has the new abstract method `read(Input)`, which reads a field value without setting it, so a custom subclass of CachedField needs to implement it. Custom implementations of the `Generics` interface need to implement `nextGenericType()` and `pushTypeVariables(GenericsHierarchy, GenericType)`.
* `CachedField#getField` returns a `Field` shared by all Kryo instances, and `FieldSerializerConfig#getIgnoreSyntheticFields` returns a `Boolean`, see [FieldSerializer and its subclasses](#fieldserializer-and-its-subclasses).

### Deprecated APIs

* RecordSerializer. Records are serialized by FieldSerializer and its subclasses. RecordSerializer is only needed to read records written by Kryo 5, see [Records](#records).
* BlowfishSerializer. Blowfish is an outdated cipher, the key is shared by all instances, and an encrypted object can only be read as the last object of the input. Encrypt the serialized bytes instead, eg with AES-GCM. BlowfishSerializer will be removed in Kryo 7.
* `Kryo#setEnumsFinal`, which is only needed to read data written by Kryo 5, see [Enums with constant bodies](#enums-with-constant-bodies).
* `setOptimizeGenerics` of CompatibleFieldSerializerConfig and TaggedFieldSerializerConfig, which is only needed to read data written by Kryo 5, see [Generic fields](#generic-fields-with-compatiblefieldserializer-and-taggedfieldserializer).
* `setLegacyChunks` and `setChunkSize` of CompatibleFieldSerializerConfig and TaggedFieldSerializerConfig, which are only needed to read data written by Kryo 5 with chunked encoding, see [Chunked encoding](#chunked-encoding).
* `CachedField#setOptimizePositive` and `@Bind(optimizePositive)`, which never had an effect: int and long fields are always written optimized for both negative and positive values. They will be removed in Kryo 7.

### Removed APIs

* ReflectASM is no longer used and no longer a dependency. Kryo 5 used it for the public fields of public classes when Unsafe was not used. Kryo 6 uses VarHandles for all fields, which are as fast, see [FieldSerializer settings](README.md#fieldserializer-settings).
* The MinLog dependency. Kryo logs with its own `com.esotericsoftware.kryo.util.Log`, a copy of MinLog with the same API, see [Logging](README.md#logging). To set Kryo's logging level or logger, use that class instead of `com.esotericsoftware.minlog.Log`. They are no longer shared with other libraries that use MinLog, so set them separately for each. `SystemLogger.install()` routes Kryo's logging to `System.Logger`.
* `CuckooObjectMap`, which was deprecated in Kryo 5.3.0.
* `UnmodifiableCollectionSerializers.registerSerializers(Kryo)` and `SynchronizedCollectionSerializers.registerSerializers(Kryo)`, which were deprecated in Kryo 5.7.1 because the IDs they assign depend on the JVM. Use `register(Kryo)` instead, which registers the classes in a fixed order. It assigns different IDs, so to read data written with `registerSerializers`, register the wrapper classes with the IDs that it assigned.
* The deprecated no-arg constructor of RecordSerializer. Use `RecordSerializer(Class)` instead.
