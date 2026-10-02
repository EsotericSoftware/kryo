# Migrating from Kryo 5 to Kryo 6

Kryo 6 has not been released yet. This document collects the changes that affect users of Kryo 5. Pull requests that change the serialized format, the behavior or the public API add an entry here.

## Requirements

Kryo 6 requires Java 17 or later. See [Installation](README.md#installation) for the Maven coordinates of the regular and the versioned jar.

## Serialization format

The following changes affect data serialized with Kryo 5. In each case, registering the serializer that Kryo 5 used makes the data readable again.

### Records

Kryo 5 serialized records with RecordSerializer, Kryo 6 serializes them with FieldSerializer. With the default configuration, FieldSerializer writes the same data, except for components with generic types, such as `List<String>`. FieldSerializer uses the type arguments to avoid writing the class of each element, while RecordSerializer wrote it. FieldSerializer cannot correctly read such records written by Kryo 5, and may even read them without an exception but with wrong values. To read such data, register RecordSerializer for the affected records, or for all records:

```java
kryo.register(SomeRecord.class, new RecordSerializer<>(SomeRecord.class));
// or for all records
kryo.addDefaultSerializer(Record.class, RecordSerializer.class);
```

### New default serializers

Kryo 6 adds default serializers for `Timestamp`, `URI`, `UUID`, `Pattern`, `AtomicBoolean`, `AtomicInteger`, `AtomicLong`, `AtomicReference` and `ConcurrentHashMap.KeySetView`. Kryo 5 serialized `Timestamp` with DateSerializer, which drops the nanoseconds, and the other types with FieldSerializer. To read data written by Kryo 5, register the serializers Kryo 5 used for these types:

```java
kryo.register(Timestamp.class, new DateSerializer());
kryo.register(UUID.class, new FieldSerializer<>(kryo, UUID.class));
```

### Generic fields with CompatibleFieldSerializer and TaggedFieldSerializer

CompatibleFieldSerializer with `readUnknownFieldData` (the default) and TaggedFieldSerializer with `readUnknownTagData` no longer use the generic type of a field to optimize its value. Kryo 5 omitted the class of collection elements and map keys and values if the field's type arguments were final, eg `List<String>`, so the value could not be read anymore once the field was removed ([#1098](https://github.com/EsotericSoftware/kryo/issues/1098)). To read such data written by Kryo 5, override `optimizeGenerics`. This restores the Kryo 5 behavior, so data with such a field that has since been removed can only be read if it was written with chunked encoding:

```java
public class Kryo5CompatibleFieldSerializer<T> extends CompatibleFieldSerializer<T> {
	public Kryo5CompatibleFieldSerializer (Kryo kryo, Class type) {
		super(kryo, type);
	}

	protected boolean optimizeGenerics () {
		return true;
	}
}

kryo.setDefaultSerializer(Kryo5CompatibleFieldSerializer.class);
```

## Behavior changes

* RecordSerializer is no longer a default serializer. Records are serialized by FieldSerializer and its subclasses, see [Records](README.md#records).
* If a record's canonical constructor throws an exception during deserialization, that exception is the cause of the KryoException. RecordSerializer added an InvocationTargetException in between.
* CompatibleFieldSerializer with `readUnknownFieldData` (the default) and TaggedFieldSerializer with `readUnknownTagData` read a serialized `null` value as `null`. Kryo 5 skipped the field, so it kept the value assigned by the constructor ([#851](https://github.com/EsotericSoftware/kryo/issues/851)).
* CompatibleFieldSerializer throws an exception when serializing or deserializing a class that declares a field with the same name as a super class, unless `extendedFieldNames` is true. Kryo 5 mixed up the values of these fields when reading ([#699](https://github.com/EsotericSoftware/kryo/issues/699)).
* The type parameters of a class are resolved from the declared type, eg the type of a field, through the super classes and interfaces of the class. Kryo 5 assigned the type arguments of the declared type by position to the type parameters of the class of the value, which failed with a ClassCastException when they didn't match, eg for a `Base<String, Integer>` field holding a `Sub<A, B> extends Base<B, A>`, and ignored them when their number didn't match. When Kryo 6 resolves a type parameter that Kryo 5 ignored, the serialized bytes can differ.

## Removed APIs

* `CuckooObjectMap`, which was deprecated in Kryo 5.3.0.
* The deprecated no-arg constructor of RecordSerializer. Use `RecordSerializer(Class)` instead.
* `Generics#pushTypeVariables(GenericsHierarchy, GenericType[])`. Use `pushTypeVariables(GenericsHierarchy, GenericType)` with the declared type returned by the new `Generics#nextGenericType()`.
