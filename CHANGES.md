# Changelog

*Discontinued, check out [Releases](https://github.com/EsotericSoftware/kryo/releases) for newer releases*

## 3.0.2 - 3.0.3 (2015-07-26)

* Fixed [#331](https://github.com/EsotericSoftware/kryo/issues/331). Reading and writing arrays of bytes was broken by ([1408cfd](https://github.com/EsotericSoftware/kryo/commit/1408cfd76f26fca3d6a0a7dd9e38feaa2e36eb46)) in UnsafeMemoryInput/UnsafeMemoryOutput. ([9f822f7](https://github.com/EsotericSoftware/kryo/commit/9f822f7cc42ff30add4bb870d9e6dca9f2eb0518))
* Fix a problem with UnsafeMemoryOutput, which was reported on the mailing list. ([1408cfd](https://github.com/EsotericSoftware/kryo/commit/1408cfd76f26fca3d6a0a7dd9e38feaa2e36eb46))
* Fixed compile errors by reverting cast externalizable in ExternalizableSerializer ([6a4f956](https://github.com/EsotericSoftware/kryo/commit/6a4f956a6b1890415c5007503ffe77a9f56af385))
* Removed final modifier, so Input/Output can have all methods overridden ([22131ec](https://github.com/EsotericSoftware/kryo/commit/22131ecf59848dc222470874f38e655fe956a1be))
* Use Kryo to create instances. ([a0cb680](https://github.com/EsotericSoftware/kryo/commit/a0cb680aaea580167dfa2af4a6240eb12fd410b8))
* Added CONTRIBUTING.md, requested by eclipse.org. ([df07c66](https://github.com/EsotericSoftware/kryo/commit/df07c66082a8c4a9aa38a8dafde49413710cb692))
* Javadocs ([04eb603](https://github.com/EsotericSoftware/kryo/commit/04eb60361e2a75511a8d6af556a1762b93fe6689))
* Fixed containsValue. ([3b2a6e1](https://github.com/EsotericSoftware/kryo/commit/3b2a6e18cbb36486f78000707975fb0109da5aef))
* Improved enum name serializer. ([3398ca7](https://github.com/EsotericSoftware/kryo/commit/3398ca78a5fe38dbf2abd2c4aa4e94e389574157))
* Clean up. ([4c33c5e](https://github.com/EsotericSoftware/kryo/commit/4c33c5e47b5aaac8f6d869a667e2148ed91c3ff0))
* Fix for [#321](https://github.com/EsotericSoftware/kryo/issues/321) EnumNameSerializer ([538bd6c](https://github.com/EsotericSoftware/kryo/commit/538bd6c6d7da78ddd2b5c99ddc1d20033d85afdc))

### Compatibility

* Serialization compatible
 * Standard IO: Yes
 * Unsafe-based IO: Yes
* Binary compatible - Yes ([Details](https://rawgithub.com/EsotericSoftware/kryo/master/compat_reports/kryo/3.0.2_to_3.0.3/compat_report.html))
* Source compatible - Yes ([Details](https://rawgithub.com/EsotericSoftware/kryo/master/compat_reports/kryo/3.0.2_to_3.0.3/compat_report.html#Source))

## 3.0.1 - 3.0.2 (2015-06-17)

* Fixed issue [#314](https://github.com/EsotericSoftware/kryo/issues/314), improves serialisation of generics. ([4764dee](https://github.com/EsotericSoftware/kryo/commit/4764dee63cf65ceb59364f731ac444f7fab765b3))
* Build improvements, for java 8
* Docs improvements

### Compatibility

* Serialization compatible
 * Standard IO: Yes
 * Unsafe-based IO: Yes
* Binary compatible - Yes ([Details](https://rawgithub.com/EsotericSoftware/kryo/master/compat_reports/kryo/3.0.1_to_3.0.2/compat_report.html))
* Source compatible - Yes ([Details](https://rawgithub.com/EsotericSoftware/kryo/master/compat_reports/kryo/3.0.1_to_3.0.2/compat_report.html#Source))

## 3.0.0 - 3.0.1 (2015-03-24)

* Update reflectasm to 1.10.1 with java 8 support ([a2c0699](https://github.com/EsotericSoftware/kryo/commit/a2c0699e03de3638382f2a04062fdd700f60f14d))
* Warning about use when references are enabled. ([7e67a1f](https://github.com/EsotericSoftware/kryo/commit/7e67a1f285e98ba43bbe2b11262cda0615df54a2))
* Fix [#286](https://github.com/EsotericSoftware/kryo/issues/286) CompatibleFieldSerializer fails with IndexOutOfBoundsException on field removal: Add compatible option for VersionFieldSerializer ([907c58b](https://github.com/EsotericSoftware/kryo/commit/907c58b833d4fb9a6a0a72d7883eb2e2f1877283))
* Removed auto registration of Java8 closures. ([1c5562d](https://github.com/EsotericSoftware/kryo/commit/1c5562d4035a72904d1d0fd724998b722bf80f3a))
* Changed to no longer use StdInstantiatorStrategy by default. ([bfc02be](https://github.com/EsotericSoftware/kryo/commit/bfc02befd7f479165cf86fc7c8b22b75c2ff35ca))
* Add VersionFieldSerializer ([#274](https://github.com/EsotericSoftware/kryo/pull/274))
* Kryo would previously throw an error when you tried used a serializer with removed fields where the class contained a generic, and you removed a field on that generic. ([d54a59c](https://github.com/EsotericSoftware/kryo/commit/d54a59cfe357ffcaf98da7ca83f4c95dd358bced))
* Fix #265. Don't invoke getTypeParameters and getComponentType on each call of setGenerics(). Pre-compute and cache them instead whenever it is possible. ([143c097](https://github.com/EsotericSoftware/kryo/commit/143c097f9d081fdb3490b3ebb24c7f3713bce9df))

### Compatibility

* Serialization compatible
 * Standard IO: Yes
 * Unsafe-based IO: Yes
* Binary compatible - Yes ([Details](https://rawgithub.com/EsotericSoftware/kryo/master/compat_reports/kryo/3.0.0_to_3.0.1/compat_report.html))
* Source compatible - Yes ([Details](https://rawgithub.com/EsotericSoftware/kryo/master/compat_reports/kryo/3.0.0_to_3.0.1/compat_report.html#Source))

## 2.24.0 - 3.0.0 (2014-10-04)

* Fixed [#248](https://github.com/EsotericSoftware/kryo/issues/248). There was a bug in the buffer resizing code. ([23830f6](https://github.com/EsotericSoftware/kryo/commit/23830f64cffd7ee7844fc582ef2b68023aeab908))
* end() for deflater and inflater. ([a306471](https://github.com/EsotericSoftware/kryo/commit/a3064716bb47c64e55b0048a6f5dac15dd67aabe))
* Fixed DeflateSerializer. ([86aecf1](https://github.com/EsotericSoftware/kryo/commit/86aecf10b522bb99e126e2c89cfab33ad00d03d0))
* BigIntegerSerializer, BigDecimalSerializer, TreeMapSerializer and TreeSetSerializer optimizations and enhacements ([2d6204d](https://github.com/EsotericSoftware/kryo/commit/2d6204dc5a04c10689a413d5365a607bdd1edab9)):
 * Proper handle of subclasses (Fix [#166](https://github.com/EsotericSoftware/kryo/issues/166))
 * Small optimizations for common BigDecimal and BigInteger constants (Fix [#238](https://github.com/EsotericSoftware/kryo/issues/238))
 * Unit test to avoid regression of PermGen leaks (ensure fix of [#170](https://github.com/EsotericSoftware/kryo/issues/170) contributed by [#173](https://github.com/EsotericSoftware/kryo/issues/173))
* Remove unnecesary code ([8efb79c](https://github.com/EsotericSoftware/kryo/commit/8efb79c163b7ad539cb3099782e218b5bbe272f6))
* Override writeChar and readChar methods to use unsafe: writeChar now performs about 125% faster and readChar 10% faster than overriden safe versions. ([45f510d](https://github.com/EsotericSoftware/kryo/commit/45f510de2dc07a65cf3807f28f6a9f9aa1749aca))
* Small optimization ([19d88db](https://github.com/EsotericSoftware/kryo/commit/19d88db264a912fbc2ed33149a4398b91cc89202))
* Fix a NPE ([004cc5c](https://github.com/EsotericSoftware/kryo/commit/004cc5cd2a6c2ecc2c839f34ab5ce4951ca32700))
* DateSerializer and LocaleSerializer enhacements and tests ([ac4ebef](https://github.com/EsotericSoftware/kryo/commit/ac4ebef070f82a419263c97d18146c35d9e0cde7), [9e63c65](https://github.com/EsotericSoftware/kryo/commit/9e63c65c51937c1a6d95ec2f7a972112fa37ee5b), [2f403c7](https://github.com/EsotericSoftware/kryo/commit/2f403c7b26fa056cd1bd807d3c330d5731e61193), [213a767](https://github.com/EsotericSoftware/kryo/commit/213a767a87a0e067d38b25bdd3c2f33e0ca0d31e))
* Change KryoPool to interface + Builder, make SoftReferences optional. ([38c6815](https://github.com/EsotericSoftware/kryo/commit/38c681594cb48876f88b83cda731752d4b387a1f))
* Set StdInstantiatorStrategy as default fallback instantiator strategy. ([0a43a64](https://github.com/EsotericSoftware/kryo/commit/0a43a642f4fe77d7cf6d7ee22b44d4e2bac568e2))
* Fix [#223](https://github.com/EsotericSoftware/kryo/issues/223). setTotal should take long as a parameter. ([ffe6931](https://github.com/EsotericSoftware/kryo/commit/ffe6931b559c1579f44936f13e73a9f71640a96b))
* Bump version to 2.25.0-SNAPSHOT ([ca153ba](https://github.com/EsotericSoftware/kryo/commit/ca153ba7deca816f9b95405e6cf956da56f2e464))
* Change groupId to com.esotericsoftware. ([d8e519a](https://github.com/EsotericSoftware/kryo/commit/d8e519a65dc16d06ec37e25dfc2cc11a7332ee2f))
* Fix shaded pom to use the correct reflectasm groupId ([4259143](https://github.com/EsotericSoftware/kryo/commit/425914333db7271536dcb6f0f34c6bac8bf5f3e6))
* Add simple, queue based kryo pool. ([05fed9c](https://github.com/EsotericSoftware/kryo/commit/05fed9cfe0a775afa38c49c34822c10193d7b67a))
* Change default artifact *not* to shade reflectasm, use OSGi'ed reflectasm 1.3.0 ([e1e3b0b](https://github.com/EsotericSoftware/kryo/commit/e1e3b0b18684961bd0b97665a4e662ec64b8c1e5))
* Update minlog to 1.3.0 which is now OSGi'ed ([de5d2a3](https://github.com/EsotericSoftware/kryo/commit/de5d2a3209c3122031f130e82f0267e7229ae731))
* Add automated compatibility check (with previous version) ([f85d6c9](https://github.com/EsotericSoftware/kryo/commit/f85d6c98a371b4c25f1fcc5e753855e5371e279d))
* Fixed [#227](https://github.com/EsotericSoftware/kryo/issues/227) ([96f7225](https://github.com/EsotericSoftware/kryo/commit/96f7225694322e27268dd698fefdffff5f4cfb6c))
* Exception -> Throwable ([1f0ba1b](https://github.com/EsotericSoftware/kryo/commit/1f0ba1b94c83cf26fc6ce108641d32c2e3c171c3))
* Recover from exceptions in Util.string() ([2d15fc2](https://github.com/EsotericSoftware/kryo/commit/2d15fc2652ee777ba153409be0b258b30fc8a6ff))
* Properly handle removal of transient fields. Till now it was not possible to remove them. ([faca949](https://github.com/EsotericSoftware/kryo/commit/faca94981c41aa9bd92a8a7f81b073d6b85ba0c4))
* Fixed [#218](https://github.com/EsotericSoftware/kryo/issues/218) ([3bac35c](https://github.com/EsotericSoftware/kryo/commit/3bac35c8f28216295b391372e89a6cbf61b943a0))
* Add support for serialization of Java8 closures (see [#215](https://github.com/EsotericSoftware/kryo/issues/215)). ([0b733dd](https://github.com/EsotericSoftware/kryo/commit/0b733ddad02e51b08e85a28fd960790ff4e69e8e))

### Compatibility

* Serialization compatible
 * Standard IO: Yes
 * Unsafe-based IO: No
* Binary compatible - No ([Details](https://rawgithub.com/EsotericSoftware/kryo/master/compat_reports/kryo/2.24.0_to_3.0.0/compat_report.html))
* Source compatible - No ([Details](https://rawgithub.com/EsotericSoftware/kryo/master/compat_reports/kryo/2.24.0_to_3.0.0/compat_report.html#Source))

## 2.23.0 - 2.24.0 (2014-05-04)

* Fixed [[#213](https://github.com/EsotericSoftware/kryo/issues/213)](https://github.com/EsotericSoftware/kryo/issues/213). Now CompatibleFieldSerializer should work properly with classes having generic type parameters. ([1e9b23f](https://github.com/EsotericSoftware/kryo/commit/1e9b23fb05232e485cde476c130e1c02b245f830))
* Fixed [#211](https://github.com/EsotericSoftware/kryo/issues/211). Integer overflows should be fixed now. ([8d7d0b5](https://github.com/EsotericSoftware/kryo/commit/8d7d0b596d04970ac24cef1f7bc289913f645dee))
* Speed up the rebuildCachedFields method, which is very often used for generic types. ([219d4a7](https://github.com/EsotericSoftware/kryo/commit/219d4a77d7100176aaa18db489cd446cf5ec71ac))
* Fix [#198](https://github.com/EsotericSoftware/kryo/issues/198) by removing defaultStrategy for instantiators. ([acf4dfe](https://github.com/EsotericSoftware/kryo/commit/acf4dfe5e3b9f8cb7e2824ac85e76faf9b6c8ea5))
* Fixed a generics-related bug reported in [#207](https://github.com/EsotericSoftware/kryo/issues/207), hopefully without introducing a new one. ([6ebf7bb](https://github.com/EsotericSoftware/kryo/commit/6ebf7bb8ebf3193fdcb9bbd2e9727535b1427034))
* Do not invoke updateBufferAddress from a default constructor, because a buffer is not set yet. ([cef85c5](https://github.com/EsotericSoftware/kryo/commit/cef85c5cfe6c30a65243266772de0c25514314b3))
* Fixed IntMap. ([2d398bc](https://github.com/EsotericSoftware/kryo/commit/2d398bce497c4fb73aa46d5e4eaa8dcfaf4492ea))
* Fixed map iterator remove failing rarely. ([b02a589](https://github.com/EsotericSoftware/kryo/commit/b02a589c1b414f3987debaa856e03a8c2252cdde))
* Remove fields by reference, not name. ([5d9917d](https://github.com/EsotericSoftware/kryo/commit/5d9917dcab338d9a5f44313d330aab3da5bb0045))
* ObjectMap updated to latest. ([440a7a6](https://github.com/EsotericSoftware/kryo/commit/440a7a6f418f74574c63f0f2cfc20aacb7d5ae2c))
* Fixed [#192](https://github.com/EsotericSoftware/kryo/issues/192) ([90fd4c4](https://github.com/EsotericSoftware/kryo/commit/90fd4c4ae08c1be7adb02248ad05e96f436cf3c9))
* Formatting, changed exception type. ([2adc6ed](https://github.com/EsotericSoftware/kryo/commit/2adc6ed9d2568eb31e249af2954940f530a874a6))
* Fix [#189](https://github.com/EsotericSoftware/kryo/issues/189) Don't embed minlog Logger in jar ([cfd0ff9](https://github.com/EsotericSoftware/kryo/commit/cfd0ff9e617d8283166eddce97ba1bc80dff7b69))
* Fix a condition for proper filtering of transient/non-transient fields. ([81fda1d](https://github.com/EsotericSoftware/kryo/commit/81fda1d6ae940cd3ad1c3ed4c3d0e6ee3004e331))
* Add assembly execution so that the zip is uploaded during release:perform. ([001a420](https://github.com/EsotericSoftware/kryo/commit/001a420e2aed92850b35dfcc25aa2621f9e77aa1))
* Update assembly descriptor to include all jars (also the original-kryo*) ([3371d4f](https://github.com/EsotericSoftware/kryo/commit/3371d4f514cdc2452109e96f5df73345fa169051))
* Add plugin to build release zip (run `mvn assembly:assembly`) ([0594e5c](https://github.com/EsotericSoftware/kryo/commit/0594e5cb0709737766c5c92fa8a08b0f574d166e))
* ByteBufferInput/Output refactoring. ([1c44a0e](https://github.com/EsotericSoftware/kryo/commit/1c44a0ef8bc3b25b05f8ec75c66f5665bf6a8385))
* Fixed EOS being returned when 0 bytes should be read. ([bd01d4b](https://github.com/EsotericSoftware/kryo/commit/bd01d4bf091ff35ee9ec57d1445c06d5861a2a8b))
* Changed registration of a different class with the same ID to a debug message. ([7c8bc3b](https://github.com/EsotericSoftware/kryo/commit/7c8bc3b329da6d2e0b5f2e325ad59325e70547c8))
* Update to reflectasm-1.09-shaded. ([ac41721](https://github.com/EsotericSoftware/kryo/commit/ac41721f956f14982f41d7edec67b4ef5742c196))
* Fixed [#180](https://github.com/EsotericSoftware/kryo/issues/180). Added support for field annotations. ([c8b6367](https://github.com/EsotericSoftware/kryo/commit/c8b6367f0f736dfc4baade7b9afc8fa055401eef))
* Pull Request [#167](https://github.com/EsotericSoftware/kryo/issues/167): Add an externalizable serializer that uses ObjectInput and ObjectOutput adapters but has the ability to switch when fancy serialization stuff is tried. ([4a93030](https://github.com/EsotericSoftware/kryo/commit/4a93030adfe8b978f8dee67e4eec93c3704430ea))

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
