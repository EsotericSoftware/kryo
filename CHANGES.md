# Changelog

## 2.23.0 - 2.24.0 (2014-05-04)

* Fixed #213. Now CompatibleFieldSerializer should work properly with classes having generic type parameters. ([1e9b23f](https://github.com/EsotericSoftware/kryo/commit/1e9b23fb05232e485cde476c130e1c02b245f830))
* Fixed #211. Integer overflows should be fixed now. ([8d7d0b5](https://github.com/EsotericSoftware/kryo/commit/8d7d0b596d04970ac24cef1f7bc289913f645dee))
* Speed up the rebuildCachedFields method, which is very often used for generic types. ([219d4a7](https://github.com/EsotericSoftware/kryo/commit/219d4a77d7100176aaa18db489cd446cf5ec71ac))
* Fix #198 by removing defaultStrategy for instantiators. ([acf4dfe](https://github.com/EsotericSoftware/kryo/commit/acf4dfe5e3b9f8cb7e2824ac85e76faf9b6c8ea5))
* Fixed a generics-related bug reported in #207, hopefully without introducing a new one. ([6ebf7bb](https://github.com/EsotericSoftware/kryo/commit/6ebf7bb8ebf3193fdcb9bbd2e9727535b1427034))
* Do not invoke updateBufferAddress from a default constructor, because a buffer is not set yet. ([cef85c5](https://github.com/EsotericSoftware/kryo/commit/cef85c5cfe6c30a65243266772de0c25514314b3))
* Fixed IntMap. ([2d398bc](https://github.com/EsotericSoftware/kryo/commit/2d398bce497c4fb73aa46d5e4eaa8dcfaf4492ea))
* Fixed map iterator remove failing rarely. ([b02a589](https://github.com/EsotericSoftware/kryo/commit/b02a589c1b414f3987debaa856e03a8c2252cdde))
* Remove fields by reference, not name. ([5d9917d](https://github.com/EsotericSoftware/kryo/commit/5d9917dcab338d9a5f44313d330aab3da5bb0045))
* ObjectMap updated to latest. ([440a7a6](https://github.com/EsotericSoftware/kryo/commit/440a7a6f418f74574c63f0f2cfc20aacb7d5ae2c))
* Fixed #192 ([90fd4c4](https://github.com/EsotericSoftware/kryo/commit/90fd4c4ae08c1be7adb02248ad05e96f436cf3c9))
* Formatting, changed exception type. ([2adc6ed](https://github.com/EsotericSoftware/kryo/commit/2adc6ed9d2568eb31e249af2954940f530a874a6))
* Fix #189 Don't embed minlog Logger in jar ([cfd0ff9](https://github.com/EsotericSoftware/kryo/commit/cfd0ff9e617d8283166eddce97ba1bc80dff7b69))
* Fix a condition for proper filtering of transient/non-transient fields. ([81fda1d](https://github.com/EsotericSoftware/kryo/commit/81fda1d6ae940cd3ad1c3ed4c3d0e6ee3004e331))
* Add assembly execution so that the zip is uploaded during release:perform. ([001a420](https://github.com/EsotericSoftware/kryo/commit/001a420e2aed92850b35dfcc25aa2621f9e77aa1))
* Update assembly descriptor to include all jars (also the original-kryo*) ([3371d4f](https://github.com/EsotericSoftware/kryo/commit/3371d4f514cdc2452109e96f5df73345fa169051))
* Add plugin to build release zip (run `mvn assembly:assembly`) ([0594e5c](https://github.com/EsotericSoftware/kryo/commit/0594e5cb0709737766c5c92fa8a08b0f574d166e))
* ByteBufferInput/Output refactoring. ([1c44a0e](https://github.com/EsotericSoftware/kryo/commit/1c44a0ef8bc3b25b05f8ec75c66f5665bf6a8385))
* Fixed EOS being returned when 0 bytes should be read. ([bd01d4b](https://github.com/EsotericSoftware/kryo/commit/bd01d4bf091ff35ee9ec57d1445c06d5861a2a8b))
* Changed registration of a different class with the same ID to a debug message. ([7c8bc3b](https://github.com/EsotericSoftware/kryo/commit/7c8bc3b329da6d2e0b5f2e325ad59325e70547c8))
* Update to reflectasm-1.09-shaded. ([ac41721](https://github.com/EsotericSoftware/kryo/commit/ac41721f956f14982f41d7edec67b4ef5742c196))
* Fixed #180. Added support for field annotations. ([c8b6367](https://github.com/EsotericSoftware/kryo/commit/c8b6367f0f736dfc4baade7b9afc8fa055401eef))
* Pull Request #167: Add an externalizable serializer that uses ObjectInput and ObjectOutput adapters but has the ability to switch when fancy serialization stuff is tried. ([4a93030](https://github.com/EsotericSoftware/kryo/commit/4a93030adfe8b978f8dee67e4eec93c3704430ea))

### Compatibility

* Serialization compatible - Yes
* Binary compatible - No ([Details](https://rawgithub.com/EsotericSoftware/kryo/master/compat_reports/kryo/2.23.0_to_2.24.0/compat_report.html))
* Source compatible - No ([Details](https://rawgithub.com/EsotericSoftware/kryo/master/compat_reports/kryo/2.23.0_to_2.24.0/compat_report.html#Source))


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