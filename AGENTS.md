# Kryo: notes for contributors and agents

Things that are not in the README, MIGRATION.md or obvious from the code. `kryo-6` is Kryo 6 (Java 17, this branch), `master` is Kryo 5 (Java 8) until Kryo 6 is released.

## Branches

- Bug fixes and additive features without a format change go to master first and reach kryo-6 by merging master into kryo-6, never the other way. Format changes and performance work go to kryo-6 only.
- When merging master into kryo-6, conflicts are resolved in favor of kryo-6: its defaults (eg `writeSameClassOnce`) and its versions in the poms.
- Dependabot targets only master. An update that needs Java 17 is applied to kryo-6 by hand and the Dependabot PR is closed with a comment.
- master still tests with JDK 8: no Java 9+ API in code or tests that is backported.

## Build and test

- `mvn -pl main test` runs the tests once. `main-versioned` runs the same tests again.
- CI runs the tests in several modes with `JAVA_TOOL_OPTIONS` (reflection, Unsafe, code generation with both backends). Run a change to field access or code generation in all of them, the same way. `JAVA_TOOL_OPTIONS` also reaches Maven's JVM.
- `--sun-misc-unsafe-memory-access=allow` makes Unsafe the default field access, so pass it only to runs that should use Unsafe.
- Benchmarks need the reactor classpath like `benchmarks/run.sh` does; `-pl benchmarks` alone runs against a stale Kryo snapshot from `~/.m2`. Keep A/B runs to a few minutes and alternate the variants; JMH differences below about 2% are noise, and any run while `mvn test` is going is garbage. A JMH `-jvmArgsAppend` value that starts with `--` needs a `-D` in front of it, or JMH takes it as its own option.
- The formatter covers `src` and `test-android`, not `test`.
- New source directories and dependencies also go into `eclipse/.classpath` and `eclipse/.project`.
- Javadoc stays free of raw `<` and `>` (use `{@code}` or `{@literal}`); the release build runs javadoc, the normal build doesn't.

## Compatibility

- `SerializationCompatTest` reads golden files in `test/resources` and checks the length of the data written now. A format change updates the expected lengths; the golden files stay readable.
- `Kryo5CompatibilityTest` reads data written by Kryo 5 (`test/resources/kryo5`). Kryo 6 must keep reading it with `Kryo5Compatibility`.
- Registration IDs must be the same on all Java versions and platforms: register in a fixed order, and register a placeholder class for a class that doesn't exist (see `ImmutableCollectionsSerializers`).
- japicmp compares the API with `kryo.api.baseline` in `main/pom.xml`. Bump it after every release, including RCs. Releases follow the wiki page HowTo-Release.
- Changes to the format, behavior or public API get an entry in MIGRATION.md.

## Android (API level 26+)

- Animal Sniffer checks `src` against API level 26. Code that only runs where an API exists is annotated with `@IgnoreAndroid`; an annotated class doesn't cover its nested classes.
- `android/test.sh` dexes Kryo with D8 (fails on D8 warnings) and runs `test-android` on a connected device or emulator; CI runs it on API levels 26 and 33.
- D8 rewrites JDK calls below the API level that has them (`List.of` below 30, VarHandles below 33). Take JDK classes from an instance with `Util.classForName(name, instance)`, and keep desugared VarHandle calls in a class Android never loads (see `Bytes.Handles`): ART verifies a whole class when it loads it.
- Android doesn't allow access to private JDK fields, and many JDK implementation classes have different names there (eg charsets, time zones).

## GraalVM native image

- `native-image/build.sh` runs the test. `Class.forName` needs reflection metadata; a class taken from an instance doesn't. Kryo itself must work without metadata.

## Design rules

- Kryo and its serializers are single threaded. FieldSerializer and its subclasses belong to one Kryo instance; only stateless serializers can be shared.

## Pull requests

- PRs are squash-merged. The tracking PR kryo-6 → master is merged with a merge commit, never squashed.
- Stacked PRs: never merge the base with `--delete-branch` while another PR targets its branch, GitHub closes that PR for good. Retarget it first.
