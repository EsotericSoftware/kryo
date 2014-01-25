# Changelog

## 2.22 - 2.23.0 (2014-01-25)

* Fix [#183](https://github.com/EsotericSoftware/kryo/issues/183) Problem with inner classes of a generic class ([f9cb9ea](https://github.com/EsotericSoftware/kryo/commit/f9cb9ea8e97fdfcacab685f054d523af1a110353))
* Fix [#176](https://github.com/EsotericSoftware/kryo/issues/176) Remove unused "kryo" fields from a number of classes. ([77e319f](https://github.com/EsotericSoftware/kryo/commit/77e319f9706b37d9edf7be85868ae520b0f52db5))
* Fix [#168](https://github.com/EsotericSoftware/kryo/issues/168) Infinite loop while extending buffer ([82d134d](https://github.com/EsotericSoftware/kryo/commit/82d134d5ab91918c70290289a9bafe1efeabf60b))
* Fix [#100](https://github.com/EsotericSoftware/kryo/issues/100) Serialization for java.util.Locale under java 1.7 is broken
* Fix [#88](https://github.com/EsotericSoftware/kryo/issues/88) Serialization of java.sql.Timestamp
* Fix [#161](https://github.com/EsotericSoftware/kryo/issues/161) Option for ByteBufferOutput#require to allocate a heap buffer ([faf05e0](https://github.com/EsotericSoftware/kryo/commit/faf05e0db69ef65bee741943bda1b83c3c46f197))
* Add a possibility to set a custom InstantiationStrategy (see issue [#138](https://github.com/EsotericSoftware/kryo/issues/138)) ([9f0bfa7](https://github.com/EsotericSoftware/kryo/commit/9f0bfa7e7a81e34ef536e5c6ae263538eaf944b7))
* Fix [#153](https://github.com/EsotericSoftware/kryo/issues/153) Update objenesis to latest version (2.1) ([1fc2dc8](https://github.com/EsotericSoftware/kryo/commit/1fc2dc8ad484ab0dc0af6ce86a5bef44c699631e))
* Fix [#140](https://github.com/EsotericSoftware/kryo/issues/140) Add optional OSGI imports for sun.misc and sun.nio.ch ([a59cef6](https://github.com/EsotericSoftware/kryo/commit/a59cef66c3f302e42e44f49f18ff28da01dc3dbc))
* Fix [#156](https://github.com/EsotericSoftware/kryo/issues/156) Depend on minlog and objenesis as standard dependencies ([f212086](https://github.com/EsotericSoftware/kryo/commit/f21208643e883fde952ad883fd81e5d7709e87eb))
* Fix [#158](https://github.com/EsotericSoftware/kryo/issues/158) FieldSerializer serializes removed fields in some situations ([fa2f729](https://github.com/EsotericSoftware/kryo/commit/fa2f729da3c87bfa94f6816ff80e390e0688c5c2))
* Some progress on [#149](https://github.com/EsotericSoftware/kryo/issues/149): Make ObjectField versions for primitive types work in the same way as AsmCacheField and UnsafeCacheField ([a137238](https://github.com/EsotericSoftware/kryo/commit/a1372389ef88218bea2ffda7f8282095b85738d8))
* Fix [#155](https://github.com/EsotericSoftware/kryo/issues/155) Test with double array fails ([adf0576](https://github.com/EsotericSoftware/kryo/commit/adf057611a2845c5f6410a9b1b050ef966a5bff5))
* Fix java.misc.Unsafe probing. Do not re-throw any exceptions. ([bb40b1f](https://github.com/EsotericSoftware/kryo/commit/bb40b1f956ec41ab0ea6502d044d2d9e170c8af7))
* Made references optional for copying. ([0a1c7e3](https://github.com/EsotericSoftware/kryo/commit/0a1c7e326c8b5ffae06ac4f6e03a7fec4aea6753))
* Fix [#154](https://github.com/EsotericSoftware/kryo/issues/154) Kryo ignores the KryoSerializable interface on objects of class which implement Map interface ([0234f8c](https://github.com/EsotericSoftware/kryo/commit/0234f8c01cf7c409808f9c93aebf7f1235f971d9))
* Fix issues [#148](https://github.com/EsotericSoftware/kryo/issues/148) and [#83](https://github.com/EsotericSoftware/kryo/issues/83) FieldSerializer copies transient fields / Copy should not ignore transient fields ([cd79d91](https://github.com/EsotericSoftware/kryo/commit/cd79d9142e46b7f498c1c46615d1a83348be2db0))
* Fix [#145](https://github.com/EsotericSoftware/kryo/issues/145) IntMap toString should not ignore 0 as a key ([0dbbc2f](https://github.com/EsotericSoftware/kryo/commit/0dbbc2f5b07a9ed737f9e2a562c3697dcefe33a6))
* Fix [#142](https://github.com/EsotericSoftware/kryo/issues/142) Change type (int->long) of field total in class Output return int ([859de2e](https://github.com/EsotericSoftware/kryo/commit/859de2ea94aa1e1e8a54c0b763f3e9f5315f0438))
* Fix [#144](https://github.com/EsotericSoftware/kryo/issues/144) IntMap.clear() does not work as expected ([a0da819](https://github.com/EsotericSoftware/kryo/commit/a0da8197565fe42557484897c5a7e2e799b5d7b3))
* Fix [#139](https://github.com/EsotericSoftware/kryo/issues/139) Kryo gets ClassCastException when deserializing TreeSet with Comparator (Kryo gets ClassCastException when deserializing TreeSet with Comparator) ([0b9d117](https://github.com/EsotericSoftware/kryo/commit/0b9d11775317f20c72aeb3d5cb333be38ff6d1c6))
* Fix [#91](https://github.com/EsotericSoftware/kryo/issues/91) Properly serialize empty EnumSets ([08db0d8](https://github.com/EsotericSoftware/kryo/commit/08db0d81f79588773fc1cdaaa64b1a4ec79920cf))

### Compatibility

* Serialization compatible - Yes
* Binary compatible - No ([Details](https://rawgithub.com/EsotericSoftware/kryo/master/compat_reports/kryo/2.22_to_2.23.0/compat_report.html))
* Source compatible - No ([Details](https://rawgithub.com/EsotericSoftware/kryo/master/compat_reports/kryo/2.22_to_2.23.0/compat_report.html#Source))