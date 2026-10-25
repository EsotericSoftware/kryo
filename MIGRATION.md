# Migrating from Kryo 5 to Kryo 6

Kryo 6 has not been released yet. This document collects the changes that affect users of Kryo 5. Pull requests that change the serialized format, the behavior or the public API add an entry here.

## Key changes

* Kryo 6 requires Java 17 and Android 8.0 (API level 26), see [Requirements](#requirements).
* Kryo has no required dependencies anymore: Objenesis and ASM are optional, ReflectASM and MinLog are gone. The imports of `Log` and of the instantiator strategies change, see [Import changes](#import-changes).
* Data written by Kryo 5 can be read with the Kryo 5 format, `kryo.setFormat(Format.KRYO5)`, as far as Kryo's serializers are concerned, see [Reading data written by Kryo 5](#reading-data-written-by-kryo-5). The Kryo 6 format is smaller and faster, so write data again with it where you can.
* The default format changed in a few places: strings are written without references, records are serialized by FieldSerializer, maps and chunked encoding write less, enums with constant bodies are treated as final, see [Serialization format](#serialization-format).
* On Java 24+, fields are accessed with VarHandles instead of `sun.misc.Unsafe`, so Java doesn't warn about Kryo. The code that reads and writes the fields of a class can be generated, which is 1.8 to 2.8 times faster, see [Field access](#field-access).
* Default serializers for about 20 more JDK types, eg `UUID`, `Pattern`, the atomic types, `ByteBuffer`, `EnumMap`, the blocking queues and the unmodifiable and synchronized collections, see [New default serializers](#new-default-serializers).
* Data that Kryo 5 wrote or read wrongly without an exception now throws one: data from a newer class version in VersionFieldSerializer, collections modified while they are written, closures without ClosureSerializer. CompatibleFieldSerializer reads a serialized null as null instead of keeping the constructor's value. See [Behavior changes](#behavior-changes).

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

Kryo 6 has no required dependencies. [Objenesis](http://objenesis.org/) is an optional dependency of the regular jar, needed only on Android and other JVMs without the JDK's serialization constructors, see [Changed APIs](#changed-apis). [ASM](https://asm.ow2.io/) is an optional dependency of the regular jar, needed for code generation on Java 17 to 23, see [Field access](#field-access). The versioned jar includes both.

## Reading data written by Kryo 5

Kryo 6 changes the default serialized format in the places described under [Serialization format](#serialization-format). To read data written by Kryo 5, set the Kryo 5 format before registering classes:

```java
Kryo kryo = new Kryo();
kryo.setFormat(Format.KRYO5);
```

Kryo's serializers and reference resolvers then write and read the Kryo 5 format, also when they are registered explicitly, added as default serializers, or subclassed without overriding `useReferences`; each section under [Serialization format](#serialization-format) says what changes. Serializers are configured for the format when they are created, so `setFormat` throws an exception once the format was read with `getFormat`: when a class is registered, a serializer that depends on it is created, or data is written or read. Serializers you registered with Kryo 5 for types that now have default serializers, eg `UUIDSerializer`, must still be registered, see [New default serializers](#new-default-serializers).

The Kryo 5 format is a best effort. It doesn't cover custom serializers or custom class or reference resolvers, and some Kryo 5 data can't be read at all, eg unmodifiable collections, which Kryo 5 couldn't read back either. Use it to keep reading data during a transition, or to read the data once and write it again with the Kryo 6 format, which is smaller, faster and loses no data in skipped fields. Like with the migration from Kryo 4 to Kryo 5, reading data across major versions isn't guaranteed.

Kryo 5 can read data written with the Kryo 5 format, unless it contains generic types that Kryo 6 resolves but Kryo 5 ignored, see [Collections and maps](#collections-and-maps), or types Kryo 5 couldn't read, eg `EnumMap` or `ArrayBlockingQueue`.

Like in Kryo 5, a String field of FieldSerializer and its subclasses decides when it is created whether it uses references: Kryo 5 wrote String fields without references if `setReferences(true)` was called after registering the classes, but strings in collections and arrays with references. To read such data, keep the order of `setReferences` and `register` the Kryo 5 code used. Unlike Kryo 5, Kryo 6 throws an exception if `setReferences` or `setReferenceResolver` changes whether strings use references after a String field was created.

## Serialization format

The following changes affect data serialized with Kryo 5, the most common first. Each section says how to read the Kryo 5 data.

### Strings with references

With references enabled, Kryo's reference resolvers no longer use references for strings: strings are rarely shared, so tracking them costs more than it saves (eg about 17% throughput for an object graph with many strings). The data is usually smaller, because no reference marker is written before each string. Strings that are shared by identity are now written each time, so data with many shared strings can grow. To read data written by Kryo 5 with references, use references for strings:

```java
kryo.setReferenceResolver(new MapReferenceResolver() {
	public boolean useReferences (Class type) {
		return !Util.isWrapperClass(type) && !Util.isEnum(type);
	}
});
```

With the Kryo 5 format, Kryo's reference resolvers do this, and so do their subclasses that inherit `useReferences`. Custom reference resolvers decide in `useReferences` as before.

### Records

Kryo 5 serialized records with RecordSerializer, Kryo 6 serializes them with FieldSerializer and its subclasses, see [Records](README.md#records). RecordSerializer is no longer a default serializer and is deprecated. With the default configuration, FieldSerializer writes the same data, except for components with generic types, such as `List<String>`. FieldSerializer uses the type arguments to avoid writing the class of each element, while RecordSerializer wrote it. FieldSerializer cannot correctly read such records written by Kryo 5, and may even read them without an exception but with wrong values. To read such data, register RecordSerializer for the affected records, or for all records:

```java
kryo.register(SomeRecord.class, new RecordSerializer<>(SomeRecord.class));
// or for all records
kryo.addDefaultSerializer(Record.class, RecordSerializer.class);
```

If a record's canonical constructor throws an exception during deserialization, that exception is the cause of the KryoException. RecordSerializer added an InvocationTargetException in between.

Copying a record creates a new record and copies the components, like the fields of other classes. RecordSerializer returned the record itself, because it is an ImmutableSerializer.

### Maps

If the class of the keys or values of a map is unknown, MapSerializer writes it only once if all keys or values are not null and have the same class. Kryo 5 wrote the class of each key and value. If the map contains no null key or value, which is written with the size of the map, keys and values are written without a null marker. Kryo 5 wrote a null marker for each key or value whose serializer doesn't accept null, eg for the values of a `Map<String, Integer>` field, and for each with references. To read data written by Kryo 5, use the Kryo 5 format, which MapSerializer and its subclasses write and read.

### New default serializers

Kryo 6 adds default serializers for these JDK types, see [Default serializers](README.md#default-serializers) for the complete list:

* `Timestamp`, `URI`, `UUID`, `Pattern`, `AtomicBoolean`, `AtomicInteger`, `AtomicLong`, `AtomicReference` and `ConcurrentHashMap.KeySetView`. Kryo 5 serialized `Timestamp` with DateSerializer, which drops the nanoseconds, `KeySetView` with CollectionSerializer, which couldn't read it back, and the other types with the default serializer, usually FieldSerializer. Subclasses of the atomic types that declare non-transient fields still use the default serializer, because the new serializers would lose these fields. With FieldSerializer, that needs `--add-opens java.base/java.util.concurrent.atomic=ALL-UNNAMED` on Java 16+ like in Kryo 5, because it reads the private value field of the atomic type. To read data written by Kryo 5, register the serializers Kryo 5 used for these types:

  ```java
  kryo.register(Timestamp.class, new DateSerializer());
  kryo.register(UUID.class, new FieldSerializer<>(kryo, UUID.class));
  ```

  FieldSerializer for `UUID` needs `--add-opens java.base/java.util=ALL-UNNAMED` on Java 16+ like in Kryo 5, because it reads the private fields of `UUID`. These serializers were already available in Kryo 5, eg `UUIDSerializer`. If you registered them with Kryo 5, keep registering them, because the Kryo 5 format only applies what Kryo 5 used by default. Registered serializers take precedence.
* `ConcurrentSkipListSet` and `PriorityBlockingQueue`, which write the comparator like the serializers for `TreeSet` and `PriorityQueue`. Kryo 5 serialized them with CollectionSerializer, which lost the comparator. The Kryo 5 format uses CollectionSerializer for them, or register it for both to read data written by Kryo 5:

  ```java
  kryo.register(ConcurrentSkipListSet.class, new CollectionSerializer());
  kryo.register(PriorityBlockingQueue.class, new CollectionSerializer());
  ```
* `ArrayBlockingQueue`, `LinkedBlockingQueue` and `LinkedBlockingDeque`, which write the capacity. Kryo 5 couldn't read an `ArrayBlockingQueue`, and read a `LinkedBlockingQueue` or `LinkedBlockingDeque` with CollectionSerializer, which lost the capacity. The Kryo 5 format uses CollectionSerializer for `LinkedBlockingQueue` and `LinkedBlockingDeque`.
* `EnumMap`, which writes the enum type of the keys. Kryo 5 couldn't serialize an `EnumMap` without registering an `EnumMapSerializer` for its enum type, which is unchanged.
* `ByteBuffer`, for heap and direct buffers, which keeps the position, limit, capacity, byte order and whether it is read-only. Kryo 5 serialized heap buffers with FieldSerializer, which needs StdInstantiatorStrategy and on Java 17+ `--add-opens java.base/java.nio=ALL-UNNAMED`, and couldn't serialize direct buffers. The Kryo 5 format uses the default serializer for `ByteBuffer`, like Kryo 5.
* The set returned by `Collections.newSetFromMap`, and by `newSequencedSetFromMap` on Java 21+, which writes the map that backs it. Like for the unmodifiable and synchronized collections, the map is read from a private JDK field, and on Android a copy of the elements is written instead. Kryo 5 serialized the set with CollectionSerializer, which couldn't read it back, so there is no readable Kryo 5 data for it.
* `File`, `InetAddress` and `InetSocketAddress`, which use only public API and don't look up host names. Kryo 5 serialized them with FieldSerializer, which can't access their fields on Java 17+ without `--add-opens`, and even with it lost the host name and IP address of `InetAddress`, read an `InetSocketAddress` that throws an exception when it is used, and couldn't create `File` and `InetSocketAddress` without StdInstantiatorStrategy. The Kryo 5 format uses the default serializer for `File`, like Kryo 5, so the data stays aligned. Without it, data written by Kryo 5 can be read with FileSerializer only for `File` without references, because FileSerializer writes the path like FieldSerializer did. There is no readable Kryo 5 data for `InetAddress` and `InetSocketAddress`, so the Kryo 5 format uses the new serializers for them.
* The map entries of `Map.entry`, `AbstractMap.SimpleEntry` and `AbstractMap.SimpleImmutableEntry`, but not subclasses of the latter two. Kryo 5 serialized them with FieldSerializer, which can't access their fields on Java 16+ without `--add-opens java.base/java.util=ALL-UNNAMED`. The data is the same as FieldSerializer wrote: the key and the value with their classes, or without them if the type arguments of the field are final, eg `Map.Entry<String, Integer>`.
* The unmodifiable and synchronized collections returned by `Collections`, eg `Collections.unmodifiableList`; on Android, which doesn't allow access to the wrapped collection, a copy of the elements is written. Kryo 5 serialized them with CollectionSerializer or MapSerializer, which couldn't read them back, so there is no readable Kryo 5 data for them. If you added these serializers with `addDefaultSerializers` in Kryo 5, the data is the same. See [Unmodifiable and synchronized collections](README.md#unmodifiable-and-synchronized-collections) for the JDK internals they access.
* The comparators returned by `Collections.reverseOrder()`, `Collections.reverseOrder(Comparator)` and `String.CASE_INSENSITIVE_ORDER`, eg as the comparator of a `TreeSet`, and `java.sql.Date` and `java.sql.Time`. They write the same data as Kryo 5 where it could serialize these types, without reflection.

### Chunked encoding

CompatibleFieldSerializer and TaggedFieldSerializer with chunked encoding write each field with its length instead of in chunks. The class names and field names first written in an object are written before it, and with references the number of objects in each field is written too. So skipping a field, eg because the class of a removed field no longer exists, no longer breaks reading the rest of the data ([#1247](https://github.com/EsotericSoftware/kryo/issues/1247)). Kryo 5 lost class names, field names, and references first written in a skipped chunk, also if the classes were registered. The new format is smaller and faster.

The outermost object with chunked encoding is buffered until it is written completely, so it must be smaller than 2 GiB, Kryo 5 wrote it to the output in chunks. The field names are identified by class instead of their position, so a nested object can only be read as a different class than it was written if its class is written, which `readUnknownFieldData` (the default) does. Kryo 5 could read it, eg with `readUnknownFieldData` false after the final type of a field changed, although changing the type of a field is not supported.

Only the outermost object writes the class names and field names, also for nested objects a serializer writes to its own output, eg DeflateSerializer. A serializer can write a nested object to its own output and read it from the same input, or the other way around.

To read data written by Kryo 5 with chunked encoding, use the Kryo 5 format, which writes chunks like Kryo 5. Chunked encoding with the Kryo 5 format doesn't use [generated code](#field-access). `setChunkSize` of the serializer configs is deprecated and only applies to the Kryo 5 format.

A custom ClassResolver needs to implement `beginDeferredNames`, `endDeferredNames`, and `readDeferredNames`, and a custom ReferenceResolver `getObjectCount`, otherwise class names first written in a skipped field are lost, and the reference IDs of the objects read after a skipped field are shifted, like in Kryo 5. The new format is used anyway. DefaultClassResolver and the reference resolvers of Kryo implement them.

The end of a field is found with `Input#total()`, so a custom Input that reads or skips bytes from its InputStream without the buffer must add them to the total, see the Javadoc of `total`. Kryo 5 read chunks without it.

### Generic fields with CompatibleFieldSerializer and TaggedFieldSerializer

CompatibleFieldSerializer with `readUnknownFieldData` (the default) and TaggedFieldSerializer with `readUnknownTagData` no longer use the generic type of a field to optimize its value. Kryo 5 omitted the class of collection elements and map keys and values if the field's type arguments were final, eg `List<String>`, so the value could not be read anymore once the field was removed ([#1098](https://github.com/EsotericSoftware/kryo/issues/1098)). To read such data written by Kryo 5, use the Kryo 5 format, which uses the generic types like Kryo 5, so data with such a field that has since been removed can only be read if it was written with chunked encoding.

### Enums with constant bodies

The class of an enum value is not written if the enum is known, eg from the type of a field, also if its constants have bodies like `PLUS { ... }`. Kryo 5 wrote the class for these enums, because they aren't final, so adding or removing a body changed the serialized bytes ([#454](https://github.com/EsotericSoftware/kryo/issues/454)). The name of an unregistered enum constant with a body is the name of its enum, which Kryo 5 can read too. This also applies to a `Class` value: the class of a constant with a body, eg `Op.PLUS.getClass()`, is read as the enum, like in Kryo 5 when the enum is registered. To read data written by Kryo 5, use the Kryo 5 format, which treats these enums as not final.

### Immutable lists

The serializer for immutable lists created by `List.of` and `Stream.toList` supports null elements, which `Stream.toList` allows ([#1239](https://github.com/EsotericSoftware/kryo/issues/1239)). It writes whether a list contains null elements. To read data written by Kryo 5, use the Kryo 5 format, or disable null elements:

```java
// With registration required, after ImmutableCollectionsSerializers.registerSerializers(kryo).
((CollectionSerializer)kryo.getSerializer(List.of().getClass())).setElementsCanBeNull(false);
```

The lists of `List.of` share one serializer, so this applies to all of them.

`ImmutableCollectionsSerializers.registerSerializers` registers the same IDs on all platforms: on Android, the classes it lacks are registered with placeholders, and the classes only Android has, eg `Set0` before API level 34, share the ID of the JDK class they replace, so registered immutable collections can be read across the platforms. Kryo 5 skipped the missing classes, so the IDs differed between Android and the JVM. Written with their class names, some immutable collections can't be read across the platforms, see [Android](README.md#android).

### Enum sets, charsets and time zones

An unregistered enum set, charset or time zone is written with the class name `java.util.EnumSet`, `java.nio.charset.Charset` or `java.util.TimeZone` instead of its implementation class, eg `java.util.RegularEnumSet`, `sun.nio.cs.UTF_8` or `sun.util.calendar.ZoneInfo`, which differ between Java vendors and Android. With registration required, register `EnumSet.class`, `Charset.class` and `TimeZone.class`. Kryo 5 reads these names too, and Kryo 6 still reads the implementation classes written by Kryo 5 where they exist. A `Class` value of an unregistered implementation class is read as `EnumSet`, `Charset` or `TimeZone`, whether the base class is registered explicitly or implicitly, like in Kryo 5 for a registered `EnumSet`. An implementation class that is registered itself keeps its registration. A default serializer added for an unregistered implementation class, eg `kryo.addDefaultSerializer(RegularEnumSet.class, ...)`, is not used, because the class is resolved to the base class first: add it for the base class, or register the implementation class. This applies to the implementation classes of the JDK only: an application subclass of `TimeZone` or `Charset` is written with its own class name and uses its own default serializer, like any other class.

### Locales with a script or extensions

LocaleSerializer writes locales with a script or extensions, eg `sr-Cyrl-RS` or `en-US-u-ca-buddhist`, as a language tag, so the script and extensions are kept ([#1053](https://github.com/EsotericSoftware/kryo/issues/1053)). Kryo 5 lost them. Other locales, also `ja_JP_JP` and `th_TH_TH`, whose extensions come from the variant, are written as before, so Kryo 6 reads all locales written by Kryo 5. With the Kryo 5 format, locales with a script or extensions are written like Kryo 5 wrote them, without the script and extensions.

### ZonedDateTime with the later offset in an overlap

ZonedDateTimeSerializer writes the offset of a `ZonedDateTime` that has the later of the two offsets in an overlap, eg `2026-11-01T01:30-05:00[America/New_York]` when clocks are set back, so the offset is kept. Kryo 5 wrote only the date, time and zone, so it was read with the earlier offset, `2026-11-01T01:30-04:00[America/New_York]`, an instant one hour earlier. Other `ZonedDateTime`s are written as before, so Kryo 6 reads all `ZonedDateTime`s written by Kryo 5. With the Kryo 5 format, they are written like Kryo 5 wrote them, without the offset.

## Behavior changes

These changes don't affect the serialized format of the default configuration, except where a change says so, eg the generic types that Kryo 6 resolves but Kryo 5 ignored. They are grouped by the part of Kryo they concern, the most common first.

### Field access

* On Java 24+, FieldSerializer and its subclasses access fields with VarHandles instead of `sun.misc.Unsafe`, which Java deprecated for removal and warns about. Before Java 24, or if Unsafe memory access is allowed with `--sun-misc-unsafe-memory-access=allow`, they use Unsafe like Kryo 5. With VarHandles, final fields are set with reflection, which Java 26+ warns about, and field access is slower than with Unsafe unless `codeGeneration` is enabled, see [FieldSerializer settings](README.md#fieldserializer-settings). To always use Unsafe, configure the field access, eg for the default serializer: `config.setFieldAccess(FieldAccessType.UNSAFE); kryo.setDefaultSerializer(new FieldSerializerFactory(config));`, or set the system property `kryo.fieldAccess=UNSAFE`, which overrides the default for all Kryo instances. With `-Dkryo.unsafe=false`, fields are accessed with VarHandles. Kryo 5 used Unsafe where it was available, and otherwise, eg with `-Dkryo.unsafe=false` or on Android, ReflectASM for the public fields of public classes and reflection for the others.
* If Java denies setting final fields with reflection, which is the default in a future Java version (JEP 500, `--illegal-final-field-mutation=deny`), FieldSerializer and its subclasses set the final fields of serializable classes with the method handles that Java provides for deserialization, on Java 24+. This is slower than reflection, so allow setting final fields with `--enable-final-field-mutation=ALL-UNNAMED` (or the name of Kryo's module) if you can. Final fields that are transient or declared in a class that isn't serializable can't be set this way and fail if it is denied, see [FieldSerializer settings](README.md#fieldserializer-settings). With Unsafe field access, final fields are set with Unsafe like in Kryo 5.
* FieldSerializer and its subclasses can generate the code that writes and reads the fields of a class, which is 1.8 to 2.8 times faster than the loop over the cached fields and writes the same bytes. It is off by default: `config.setCodeGeneration(true)`, or the system property `kryo.codeGeneration=true`, see [Code generation](README.md#code-generation). On Java 24+ this needs nothing else, on older Java versions it needs [ASM](https://asm.ow2.io/) on the classpath (`org.ow2.asm:asm`, an optional dependency of Kryo), which the versioned jar `kryo6` includes.

### FieldSerializer and its subclasses

* `@Bind` and `@NotNull` are applied to String fields with all field access types. Kryo 5 ignored them for a String field without references if it accessed the field with Unsafe or ReflectASM, and applied them with reflection, so the data depended on the field access. A null value of a String field with `@NotNull` throws an exception when it is written. The Kryo 5 format ignores both annotations for String fields without references, like Kryo 5 did with its default field access.
* `@NotNull` is respected on fields that also have `@Bind`. Kryo 5 ignored it because `@Bind` always set `canBeNull`, so these fields are serialized without the null marker, except with the Kryo 5 format.
* FieldSerializer with `setFieldsAsAccessible(false)` serializes the public, non-final fields of public classes. Kryo 5 serialized no fields at all with this setting, and the Kryo 5 format doesn't either. The components of a record are serialized either way, like RecordSerializer did in Kryo 5.
* With Unsafe field access, FieldSerializer and its subclasses no longer store a value read whose class is not assignable to the field type, eg after the type of a field changed ([#1068](https://github.com/EsotericSoftware/kryo/issues/1068)). They throw an exception, or skip the field if CompatibleFieldSerializer uses chunked encoding. Kryo 5 stored the value anyway, which could crash the JVM when the field was used. With reflection and VarHandles, an exception was already thrown.
* FieldSerializer and its subclasses share the `Field` objects and the generic types of the fields of a class between all Kryo instances, which saves memory for applications with many classes and Kryo instances. `CachedField#getField` returns the shared `Field`, and the arrays returned by `GenericType#getTypeParameters` and `Generics#nextGenericTypes` must not be modified. Not on Android.
* A subclass of a field serializer can remove fields in `initializeCachedFields`, as its javadoc says. With TaggedFieldSerializer and VersionFieldSerializer this threw "Field not found" in Kryo 5, because their `removeField` called `initializeCachedFields` again. It no longer does, like FieldSerializer and CompatibleFieldSerializer never did in Kryo 5, so a subclass of these two serializers that derives something from the fields there needs to override `removeField` too.

### CompatibleFieldSerializer, TaggedFieldSerializer and VersionFieldSerializer

* CompatibleFieldSerializer with `readUnknownFieldData` (the default) and TaggedFieldSerializer with `readUnknownTagData` read a serialized `null` value as `null`. Kryo 5 skipped the field, so it kept the value assigned by the constructor ([#851](https://github.com/EsotericSoftware/kryo/issues/851)).
* CompatibleFieldSerializer reads the fields of a class that declares a field with the same name as a super class correctly: the fields are identified by their order, the field of the subclass first. Kryo 5 wrote them the same way but read both values into one field ([#699](https://github.com/EsotericSoftware/kryo/issues/699)). Removing one of them mixes up the values, `extendedFieldNames` identifies them by their class instead.
* CompatibleFieldSerializer checks that the class of a value read is assignable to the field type for every object. Kryo 5 checked it only after the field had been read or written once, so the first object could fail with a ClassCastException or get a value of the wrong type, instead of throwing "Read type is incompatible" or, with chunked encoding, skipping the field. Proxies, which are written as `InvocationHandler`, are compatible with fields of an interface type, like closures. Kryo 5 rejected them, except for the first object read.
* VersionFieldSerializer throws an exception when reading an object written by a newer version of its class ([#846](https://github.com/EsotericSoftware/kryo/issues/846)). Kryo 5 read it without an exception, but the fields added in the newer version were read into other fields or left unread, so the values and the following data were wrong.

### Collections and maps

* CollectionSerializer and MapSerializer throw an exception if the number of elements written doesn't match the size written before them, eg because the collection was modified concurrently ([#1181](https://github.com/EsotericSoftware/kryo/issues/1181)). Kryo 5 wrote data that couldn't be read correctly, which often failed later with an unrelated error, such as an unregistered class ID or a buffer underflow.
* The type parameters of a class are resolved from the declared type, eg the type of a field, through the super classes and interfaces of the class. Kryo 5 assigned the type arguments of the declared type by position to the type parameters of the class of the value, which failed with a ClassCastException when they didn't match, eg for a `Base<String, Integer>` field holding a `Sub<A, B> extends Base<B, A>`, and ignored them when their number didn't match. Kryo 6 also resolves a type variable that a super class without type arguments binds, eg `T` of `Base<T>` for `Top<V> extends Bind` and `Bind extends Base<Integer>`. Kryo 5 didn't resolve it, or resolved it to the type argument of an enclosing object of the same class, which wrote wrong values or failed with a ClassCastException ([#1541](https://github.com/EsotericSoftware/kryo/issues/1541)). When Kryo 6 resolves a type parameter that Kryo 5 ignored, the serialized bytes can differ. Likewise, MapSerializer and CollectionSerializer resolve the key, value and element types from the declared type, eg `Integer` keys for a field of type `class IntMap<V> extends HashMap<Integer, V>`. Kryo 5 used the type arguments of the declared type by position, which failed when they didn't match those of `Map` or `Collection` ([#860](https://github.com/EsotericSoftware/kryo/issues/860)).
* A subclass of `TreeSet`, `TreeMap`, `PriorityQueue`, `ConcurrentSkipListSet`, `ConcurrentSkipListMap`, `PriorityBlockingQueue`, `ArrayBlockingQueue`, `LinkedBlockingQueue` or `LinkedBlockingDeque` without a public constructor for the comparator or capacity, eg one that passes a fixed comparator to the super constructor, is created like other collections and maps, with `Kryo#newInstance`. Kryo 5 threw an exception for such a subclass of `TreeSet`, `TreeMap`, `PriorityQueue` and `ConcurrentSkipListMap`.
* The serializers for unmodifiable and synchronized collections read the wrapped collection with method handles if `java.util` is open to Kryo, otherwise with Unsafe. If neither is allowed, they throw an exception on first use that explains how to allow it. Kryo 5 failed to register them without Unsafe.

### Other

* Serializing or copying a closure throws an exception if `ClosureSerializer.Closure` is not registered, also if `registrationRequired` is false ([#1137](https://github.com/EsotericSoftware/kryo/issues/1137)). Kryo 5 registered the closure's class implicitly and wrote it with the default serializer, which can't be read because the class of a closure can't be found by its name.
* ClosureSerializer accesses no JDK internals: it no longer needs `--add-opens java.base/java.lang.invoke=ALL-UNNAMED` and logs no warnings without it. It resolves closures like Java serialization, with the `$deserializeLambda$` method of the capturing class, and caches the reflective access per class, so writing a closure is as fast as in Kryo 5 with the JDK internals open and 2.5x faster without. On the module path, the package of the capturing class must be open to Kryo, like for the private fields of a class.
* If the class of a class name is not found, a later reference to the same class name throws the same exception. Kryo 5 read the following bytes as a class name.
* Writing a time zone with an ID that `TimeZone.getTimeZone` doesn't know, eg a `SimpleTimeZone` with a custom ID, throws an exception, also in a `Calendar`. Kryo 5 wrote the ID, which was read as GMT. Custom IDs like `GMT+01:00` are known. With the Kryo 5 format, such a time zone is written like Kryo 5 did.
* A `Calendar` is read with the class that was written, eg `GregorianCalendar`, `BuddhistCalendar` or `JapaneseImperialCalendar`. Kryo 5 read every calendar with `Calendar.getInstance`, so the class, and with it the year, depended on the default locale of the reader, eg a `GregorianCalendar` was read as a `BuddhistCalendar` with the default locale `th_TH`. The data is unchanged.
* A reference resolves to the object a serializer returns, also if the serializer called `Kryo.reference` with another object first, eg the mutable collection an immutable `List.of`, `Set.of` or `Map.of` is read or copied with. Kryo 5 resolved a second reference to such a collection to the mutable collection. An element that refers to its own immutable collection still gets the mutable collection, which doesn't exist yet when the element is read. A custom `ReferenceResolver` gets `setReadObject` a second time for the same id, with the returned object.
* DefaultInstantiatorStrategy and BeanSerializer use method handles instead of ReflectASM. If a constructor throws an exception, that exception is the cause of the KryoException, like with ReflectASM in Kryo 5.
* DefaultInstantiatorStrategy uses the fallback strategy for a public no-arg constructor of a public class in a package that is not exported to Kryo, eg a JDK implementation class. Kryo 5 kept the constructor and creating the object failed with an IllegalAccessException.
* The `maxArraySize` of an Input also applies to the object that DeflateSerializer and BlowfishSerializer read from their own input, and, with the Kryo 5 format, to the field values of CompatibleFieldSerializer and TaggedFieldSerializer with chunked encoding. Kryo 5 read them without a limit.

## API changes

### Changed APIs

* `Kryo#setFormat(Format)` sets the serialization format, `KRYO6` by default or `KRYO5` to read data written by Kryo 5, see [Reading data written by Kryo 5](#reading-data-written-by-kryo-5). `Kryo#getFormat` returns it.
* `Kryo#addDefaultSerializer(Class, Supplier)` adds a default serializer created by a supplier, eg `DateSerializer::new`, without reflection.
* `LinkedHashMapSerializer` keeps the access order of a `LinkedHashMap`, see [LinkedHashMap with access order](README.md#linkedhashmap-with-access-order). It is not used by default.
* `SetFromMapSerializer` is the default serializer for the set returned by `Collections.newSetFromMap`, see [New default serializers](#new-default-serializers).
* `JavaSerializer#setObjectInputFilter` sets an `ObjectInputFilter` on the streams JavaSerializer reads from, to read Java serialization data from an untrusted source.
* The instantiator API uses Kryo's own interfaces and strategies instead of the Objenesis ones, with the same names and methods, see [Import changes](#import-changes). On the JDK they create objects like Objenesis did, with the serialization constructors of `sun.reflect.ReflectionFactory`, so Objenesis is no longer needed there and is an optional dependency of the regular jar. It is still used on Android and other JVMs without these constructors: add `org.objenesis:objenesis` there. Other Objenesis strategies are used with `ObjenesisStrategy`. The versioned jar includes Objenesis as before, but its shaded Objenesis classes in `com.esotericsoftware.kryo.kryo6.objenesis` are not Kryo strategies: use `com.esotericsoftware.kryo.kryo6.util.StdInstantiatorStrategy` and wrap other Objenesis strategies in `ObjenesisStrategy`.
* ClassResolver and ReferenceResolver have new methods for the chunked encoding, which custom resolvers need to implement, see [Chunked encoding](#chunked-encoding).
* `Generics#pushTypeVariables(GenericsHierarchy, GenericType[])` was removed. Use `pushTypeVariables(GenericsHierarchy, GenericType)` with the declared type returned by the new `Generics#nextGenericType()`.
* `FieldSerializer.CachedField` has the new abstract method `read(Input)`, which reads a field value without setting it, so a custom subclass of CachedField needs to implement it. Custom implementations of the `Generics` interface need to implement `nextGenericType()` and `pushTypeVariables(GenericsHierarchy, GenericType)`, which FieldSerializer calls with a null type if the declared type is not known, so the type variables of the class don't resolve to those of an enclosing object of the same class.
* `CachedField#getField` returns a `Field` shared by all Kryo instances, see [FieldSerializer and its subclasses](#fieldserializer-and-its-subclasses).
* `Output#longLength` takes a `long` instead of an `int`, like `Output#writeLong(long, boolean)`.
* `CompatibleFieldSerializerConfig#getReadUnknownTagData` was renamed to `getReadUnknownFieldData`, like its setter.

### Deprecated APIs

* RecordSerializer. Records are serialized by FieldSerializer and its subclasses. RecordSerializer is only needed to read records written by Kryo 5, see [Records](#records).
* BlowfishSerializer. Blowfish is an outdated cipher, the key is shared by all instances, and an encrypted object can only be read as the last object of the input. Encrypt the serialized bytes instead, eg with AES-GCM. BlowfishSerializer will be removed in Kryo 7.
* `CachedField#setOptimizePositive` and `@Bind(optimizePositive)`, which never had an effect: int and long fields are always written optimized for both negative and positive values. They will be removed in Kryo 7.

### Removed APIs

* ReflectASM is no longer used and no longer a dependency, see [Field access](#field-access).
* The MinLog dependency. Kryo logs with its own `com.esotericsoftware.kryo.util.Log`, a copy of MinLog with the same API, see [Logging](README.md#logging). To set Kryo's logging level or logger, use that class instead of `com.esotericsoftware.minlog.Log`. They are no longer shared with other libraries that use MinLog, so set them separately for each. `SystemLogger.install()` routes Kryo's logging to `System.Logger`.
* `CuckooObjectMap`, which was deprecated in Kryo 5.3.0.
* `UnmodifiableCollectionSerializers.registerSerializers(Kryo)` and `SynchronizedCollectionSerializers.registerSerializers(Kryo)`, which were deprecated in Kryo 5.7.1 because the IDs they assign depend on the JVM. Use `register(Kryo)` instead, which registers the classes in a fixed order. It assigns different IDs, so to read data written with `registerSerializers`, register the wrapper classes with the IDs that it assigned.
* The deprecated no-arg constructor of RecordSerializer. Use `RecordSerializer(Class)` instead.
* `Util#getPrimitiveClass` and `GenericsUtil#resolveTypeParameters`, which Kryo doesn't use.
