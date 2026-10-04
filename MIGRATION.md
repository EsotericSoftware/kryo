# Migrating from Kryo 5 to Kryo 6

Kryo 6 has not been released yet. This document collects the changes that affect users of Kryo 5. Pull requests that change the serialized format, the behavior or the public API add an entry here.

## Requirements

Kryo 6 requires Java 17 or later. See [Installation](README.md#installation) for the Maven coordinates of the regular and the versioned jar.

## Serialization format

The following changes affect data serialized with Kryo 5. To read data written by Kryo 5, configure Kryo after setting the default serializer:

```java
Kryo kryo = new Kryo();
kryo.setDefaultSerializer(...); // If not FieldSerializer.
Kryo5Compatibility.configure(kryo);
```

This applies the settings described below to Kryo's default serializers. Serializers that are registered explicitly or added as default serializers later need these settings themselves. Kryo 5 can read data written with this configuration, unless it contains locales with a script or generic types that Kryo 6 resolves but Kryo 5 ignored, see [Behavior changes](#behavior-changes).

### Records

Kryo 5 serialized records with RecordSerializer, Kryo 6 serializes them with FieldSerializer. With the default configuration, FieldSerializer writes the same data, except for components with generic types, such as `List<String>`. FieldSerializer uses the type arguments to avoid writing the class of each element, while RecordSerializer wrote it. FieldSerializer cannot correctly read such records written by Kryo 5, and may even read them without an exception but with wrong values. To read such data, register RecordSerializer for the affected records, or for all records:

```java
kryo.register(SomeRecord.class, new RecordSerializer<>(SomeRecord.class));
// or for all records
kryo.addDefaultSerializer(Record.class, RecordSerializer.class);
```

### New default serializers

Kryo 6 adds default serializers for `Timestamp`, `URI`, `UUID`, `Pattern`, `AtomicBoolean`, `AtomicInteger`, `AtomicLong`, `AtomicReference` and `ConcurrentHashMap.KeySetView`. Kryo 5 serialized `Timestamp` with DateSerializer, which drops the nanoseconds, `KeySetView` with CollectionSerializer, which couldn't read it back, and the other types with the default serializer, usually FieldSerializer. To read data written by Kryo 5, register the serializers Kryo 5 used for these types:

```java
kryo.register(Timestamp.class, new DateSerializer());
kryo.register(UUID.class, new FieldSerializer<>(kryo, UUID.class));
```

These serializers were already available in Kryo 5, eg `UUIDSerializer`. If you registered them with Kryo 5, keep registering them, because `Kryo5Compatibility` only applies what Kryo 5 used by default. Registered serializers take precedence over its settings.

Kryo 6 also adds default serializers for the unmodifiable and synchronized collections returned by `Collections`, eg `Collections.unmodifiableList`, except on Android. Kryo 5 serialized them with CollectionSerializer or MapSerializer, which couldn't read them back, so there is no readable Kryo 5 data for them. If you added these serializers with `addDefaultSerializers` in Kryo 5, the data is the same. See [Unmodifiable and synchronized collections](README.md#unmodifiable-and-synchronized-collections) for the JDK internals they access.

### Generic fields with CompatibleFieldSerializer and TaggedFieldSerializer

CompatibleFieldSerializer with `readUnknownFieldData` (the default) and TaggedFieldSerializer with `readUnknownTagData` no longer use the generic type of a field to optimize its value. Kryo 5 omitted the class of collection elements and map keys and values if the field's type arguments were final, eg `List<String>`, so the value could not be read anymore once the field was removed ([#1098](https://github.com/EsotericSoftware/kryo/issues/1098)). To read such data written by Kryo 5, enable `optimizeGenerics`. This restores the Kryo 5 behavior, so data with such a field that has since been removed can only be read if it was written with chunked encoding:

```java
CompatibleFieldSerializerConfig config = new CompatibleFieldSerializerConfig();
config.setOptimizeGenerics(true);
kryo.setDefaultSerializer(new CompatibleFieldSerializerFactory(config));
```

### Maps

If the class of the keys or values of a map is unknown, MapSerializer writes it only once if all keys or values are not null and have the same class. Kryo 5 wrote the class of each key and value. To read data written by Kryo 5, disable this for the MapSerializer instances, eg for all maps that use the default MapSerializer:

```java
MapSerializer mapSerializer = new MapSerializer();
mapSerializer.setWriteSameClassOnce(false);
kryo.addDefaultSerializer(Map.class, mapSerializer);
```

Subclasses like TreeMapSerializer need the same setting.

### Immutable lists

The serializer for immutable lists created by `List.of` and `Stream.toList` supports null elements, which `Stream.toList` allows ([#1239](https://github.com/EsotericSoftware/kryo/issues/1239)). It writes whether a list contains null elements. To read data written by Kryo 5, disable null elements:

```java
((CollectionSerializer)kryo.getSerializer(List.of().getClass())).setElementsCanBeNull(false);
```

### Locales with a script

LocaleSerializer writes locales with a script, eg `sr-Cyrl-RS`, as a language tag, so the script and extensions are kept ([#1053](https://github.com/EsotericSoftware/kryo/issues/1053)). Kryo 5 lost them. Other locales are written as before, so Kryo 6 reads all locales written by Kryo 5.

## Behavior changes

* RecordSerializer is no longer a default serializer. Records are serialized by FieldSerializer and its subclasses, see [Records](README.md#records).
* If a record's canonical constructor throws an exception during deserialization, that exception is the cause of the KryoException. RecordSerializer added an InvocationTargetException in between.
* CompatibleFieldSerializer with `readUnknownFieldData` (the default) and TaggedFieldSerializer with `readUnknownTagData` read a serialized `null` value as `null`. Kryo 5 skipped the field, so it kept the value assigned by the constructor ([#851](https://github.com/EsotericSoftware/kryo/issues/851)).
* CompatibleFieldSerializer throws an exception when serializing or deserializing a class that declares a field with the same name as a super class, unless `extendedFieldNames` is true. Kryo 5 mixed up the values of these fields when reading ([#699](https://github.com/EsotericSoftware/kryo/issues/699)).
* FieldSerializer with `setFieldsAsAccessible(false)` serializes the public, non-final fields of public classes. Kryo 5 serialized no fields at all with this setting.
* `@NotNull` is respected on fields that also have `@Bind`. Kryo 5 ignored it because `@Bind` always set `canBeNull`, so these fields are serialized without the null marker.
* On Java 24+, FieldSerializer and its subclasses access fields with VarHandles instead of `sun.misc.Unsafe`, which Java deprecated for removal and warns about. Before Java 24, or if Unsafe memory access is allowed with `--sun-misc-unsafe-memory-access=allow`, they use Unsafe like Kryo 5. With VarHandles, final fields are set with reflection, which Java 26+ warns about, see [FieldSerializer settings](README.md#fieldserializer-settings). To always use Unsafe, configure the field access, eg for the default serializer: `config.setFieldAccess(FieldAccessType.UNSAFE); kryo.setDefaultSerializer(new FieldSerializerFactory(config));`. With `-Dkryo.unsafe=false`, fields are accessed with VarHandles. Kryo 5 used ReflectASM for public classes and reflection otherwise.
* The serializers for unmodifiable and synchronized collections read the wrapped collection with method handles if `java.util` is open to Kryo, otherwise with Unsafe. If neither is allowed, they throw an exception on first use that explains how to allow it. Kryo 5 failed to register them without Unsafe.
* The type parameters of a class are resolved from the declared type, eg the type of a field, through the super classes and interfaces of the class. Kryo 5 assigned the type arguments of the declared type by position to the type parameters of the class of the value, which failed with a ClassCastException when they didn't match, eg for a `Base<String, Integer>` field holding a `Sub<A, B> extends Base<B, A>`, and ignored them when their number didn't match. When Kryo 6 resolves a type parameter that Kryo 5 ignored, the serialized bytes can differ.
* DefaultInstantiatorStrategy and BeanSerializer use method handles instead of ReflectASM. If a constructor throws an exception, that exception is the cause of the KryoException, like with ReflectASM in Kryo 5.
* Serializing or copying a closure throws an exception if `ClosureSerializer.Closure` is not registered, also if `registrationRequired` is false ([#1137](https://github.com/EsotericSoftware/kryo/issues/1137)). Kryo 5 registered the closure's class implicitly and wrote it with the default serializer, which can't be read because the class of a closure can't be found by its name.
* VersionFieldSerializer throws an exception when reading an object written by a newer version of its class ([#846](https://github.com/EsotericSoftware/kryo/issues/846)). Kryo 5 read it without an exception, but the fields added in the newer version were read into other fields or left unread, so the values and the following data were wrong.

## Deprecated APIs

* RecordSerializer. Records are serialized by FieldSerializer and its subclasses. RecordSerializer is only needed to read records written by Kryo 5, see [Records](#records).
* `FieldAccessType.ASM`, which uses ReflectASM like Kryo 5 did for public fields of public classes when Unsafe was not used. ReflectASM is only used if `ASM` is configured, Kryo 6 uses VarHandles instead, which are as fast. `ASM` and the ReflectASM dependency will be removed in Kryo 7. If VarHandles are slower for you than ReflectASM, please open an issue.

## Removed APIs

* `CuckooObjectMap`, which was deprecated in Kryo 5.3.0.
* `UnmodifiableCollectionSerializers.registerSerializers(Kryo)` and `SynchronizedCollectionSerializers.registerSerializers(Kryo)`, which were deprecated in Kryo 5.7.1 because the IDs they assign depend on the JVM. Use `register(Kryo)` instead, which registers the classes in a fixed order. It assigns different IDs, so to read data written with `registerSerializers`, register the wrapper classes with the IDs that it assigned.
* The deprecated no-arg constructor of RecordSerializer. Use `RecordSerializer(Class)` instead.
* `Generics#pushTypeVariables(GenericsHierarchy, GenericType[])`. Use `pushTypeVariables(GenericsHierarchy, GenericType)` with the declared type returned by the new `Generics#nextGenericType()`.
