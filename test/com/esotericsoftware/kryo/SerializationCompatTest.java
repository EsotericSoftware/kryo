/* Copyright (c) 2008-2025, Nathan Sweet
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 * - Neither the name of Esoteric Software nor the names of its contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. */

package com.esotericsoftware.kryo;

import static com.esotericsoftware.kryo.ReflectionAssert.*;
import static java.lang.Integer.*;
import static org.junit.jupiter.api.Assertions.*;

import com.esotericsoftware.kryo.SerializationCompatTestData.TestData;
import com.esotericsoftware.kryo.SerializationCompatTestData.TestDataJava8;
import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import com.esotericsoftware.minlog.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objenesis.strategy.StdInstantiatorStrategy;

/** Test for serialization compatibility: data serialized with an older version (same major version) must be deserializable with
 * this newer (same major) version. Serialization compatibility is checked for each type that has a default serializer
 * (<code>Kryo.defaultSerializers</code>, populated from Kryo's constructor)
 *
 * Because the various {@link Input}/{@link Output} variants are not compatible, this test is done for each of these variants.
 *
 * This test uses previously created "canonical" tests files (one for each IO variant): it deserializes their content and checks
 * if the deserialized object is equals the expected object. The test files were created once with this test (it writes test files
 * if they're not yet existing), serializing an instance of {@link TestData} that contains fields for each default serializer
 * (respectively the related type) and some arbitratry other fields (just to have a class that's not too trivial).
 *
 * If any of these checks fail it may have different reasons: 1) the serialization format of an IO variant has changed 2) the
 * serialization format of a serializer has changed 3) the {@link TestData} structure/fields have changed
 *
 * In cases 1) and 2) the question is if that was intentionally and if it can't/shouldn't be avoided. If it was intentionally
 * probably kryo's major version should be incremented and new test files must be created (more on that later).
 *
 * In case 3) - assuming that this was intentional - new test files have to be created.
 *
 * To create new test files, just delete the existing ones and run this test. It will write new files so that you only have to
 * commit the changes. Depending on the situation you may consider creating new files from the smallest version of the same major
 * version (e.g. for 3.1.4 this is 3.0.0) - to do this just save this test and the {@link SerializationCompatTest}, go back to the
 * related tag and run the test (there's nothing here to automate creation of test files for a different version). */
class SerializationCompatTest extends KryoTestCase {
	// Set to true to delete failed test files, then set back to false, set expected bytes, and run again to generate new files.
	private static final boolean DELETE_FAILED_TEST_FILES = false;

	private static final int JAVA_VERSION;
	static {
		// java.version is e.g. "1.8.0", "9.0.4", or "14"
		String[] strVersions = System.getProperty("java.version").split("\\.");
		if (strVersions.length == 1) {
			JAVA_VERSION = parseInt(strVersions[0]);
		} else {
			int[] versions = new int[] {parseInt(strVersions[0]), parseInt(strVersions[1])};
			JAVA_VERSION = versions[0] > 1 ? versions[0] : versions[1];
		}
	}
	private static final int EXPECTED_DEFAULT_SERIALIZER_COUNT = JAVA_VERSION < 11
			? 58 : JAVA_VERSION < 14 ? 68 : 69;  // Also change Kryo#defaultSerializers.
	private static final List<TestDataDescription> TEST_DATAS = new ArrayList<>();

	static {
		TEST_DATAS.add(new TestDataDescription<>(new TestData(), 1940, 1958));
		if (JAVA_VERSION >= 8) TEST_DATAS.add(new TestDataDescription<>(new TestDataJava8(), 2098, 2116));
		if (JAVA_VERSION >= 11) TEST_DATAS.add(new TestDataDescription<>(createTestData(11), 2182, 2210));
		if (JAVA_VERSION >= 17) TEST_DATAS.add(new TestDataDescription<>(createTestData(17), 1948, 1966));
	};

	private static TestData createTestData(int version) {
		try {
			return (TestData) Class.forName("com.esotericsoftware.kryo.TestDataJava" + version).getConstructor().newInstance();
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("TestDataJava" + version + " could not be instantiated", e);
		}
	}

	@BeforeEach
	public void setUp () throws Exception {
		super.setUp();
		kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
		kryo.setReferences(true);
		kryo.setRegistrationRequired(false);
		// We register EnumSet so that the EnumSet implementation class name is not written: EnumSet implementations
		// have different names e.g. in oracle and ibm jdks (RegularEnumSet vs. MiniEnumSet), therefore the implementation class
		// could not be loaded by a different name. However, with the registered EnumSet, the serializer is used directly according
		// to the registered id.
		kryo.register(EnumSet.class);
	}

	@Test
	void testDefaultSerializers () throws Exception {
		Field defaultSerializersField = Kryo.class.getDeclaredField("defaultSerializers");
		defaultSerializersField.setAccessible(true);
		List defaultSerializers = (List)defaultSerializersField.get(kryo);
		assertEquals(EXPECTED_DEFAULT_SERIALIZER_COUNT, defaultSerializers.size(),
				"The registered default serializers have changed.\n" //
						+ "Because serialization compatibility shall be checked for default serializers, you must extend " //
						+ "SerializationCompatTestData.TestData to have a field for the type of the new default serializer.\n" //
						+ "After that's done, you must create new versions of 'test/resources/data*' because the new TestData instance will " //
						+ "no longer be equals the formerly written/serialized one.");
	}

	@Test
	void testStandard () throws Exception {
		runTests("standard", new Function1<File, Input>() {
			public Input apply (File file) throws FileNotFoundException {
				return new Input(new FileInputStream(file));
			}
		}, new Function1<File, Output>() {
			public Output apply (File file) throws Exception {
				return new Output(new FileOutputStream(file));
			}
		});
	}

	@Test
	void testByteBuffer () throws Exception {
		runTests("bytebuffer", new Function1<File, Input>() {
			public Input apply (File file) throws FileNotFoundException {
				return new ByteBufferInput(new FileInputStream(file));
			}
		}, new Function1<File, Output>() {
			public Output apply (File file) throws Exception {
				return new ByteBufferOutput(new FileOutputStream(file));
			}
		});
	}

	private void runTests (String variant, Function1<File, Input> inputFactory, Function1<File, Output> outputFactory)
		throws Exception {
		setUp();
		for (TestDataDescription description : TEST_DATAS)
			runTest(description, variant, inputFactory, outputFactory);
	}

	private void runTest (TestDataDescription description, String variant, Function1<File, Input> inputFactory,
		Function1<File, Output> outputFactory) throws Exception {
		File testDir = new File("test");
		if (!testDir.exists()) testDir = new File("../test"); // Could be running in Eclipse subdirectory.
		File file = new File(testDir, "resources/" + description.classSimpleName() + "-" + variant + ".ser");
		file.getParentFile().mkdirs();

		if (file.exists()) {
			Log.info("Reading and testing " + description.classSimpleName() + " with mode '" + variant + "' from file "
				+ file.getAbsolutePath());
			Input in = inputFactory.apply(file);
			try {
				readAndRunTest(description, in);
			} catch (Throwable ex) {
				if (DELETE_FAILED_TEST_FILES) {
					System.out.println("Failed: " + file.getAbsolutePath());
					in.close();
					file.delete();
				} else
					throw ex;
			}
			in.close();
		} else {
			Log.info("Testing and writing " + description.classSimpleName() + " with mode '" + variant + "' to file "
				+ file.getAbsolutePath());
			Output out = outputFactory.apply(file);
			try {
				runTestAndWrite(description, out);
				out.close();
			} catch (Exception e) {
				// if anything failed (e.g. the initial test), we should delete the file as it may be empty or corruped
				out.close();
				file.delete();
				throw e;
			}
		}
	}

	private void readAndRunTest (TestDataDescription<?> description, Input in) throws FileNotFoundException {
		TestData actual = kryo.readObject(in, description.testDataClass());
		roundTrip(description.length, description.noGenericsLength, actual);
		try {
			assertReflectionEquals(actual, description.testData);
		} catch (AssertionError e) {
			Log.info("Serialization format is broken, please check " + getClass().getSimpleName() + "'s class doc to see"
				+ " what this means and how to proceed.");
			throw e;
		}
	}

	private void runTestAndWrite (TestDataDescription description, Output out) throws FileNotFoundException {
		roundTrip(description.length, description.noGenericsLength, description.testData);
		kryo.writeObject(out, description.testData);
	}

	protected void doAssertEquals (final Object one, final Object another) {
		try {
			assertReflectionEquals(one, another);
		} catch (Exception e) {
			fail("Test failed: " + e);
		}
	}

	private interface Function1<A, B> {
		B apply (A input) throws Exception;
	}

	private static class TestDataDescription<T extends TestData> {
		final T testData;
		final int length;
		final int noGenericsLength;

		TestDataDescription(T testData, int length, int noGenericsLength) {
			this.testData = testData;
			this.length = length;
			this.noGenericsLength = noGenericsLength;
		}

		Class<T> testDataClass () {
			return (Class<T>)testData.getClass();
		}

		String classSimpleName () {
			return testData.getClass().getSimpleName();
		}
	}
}
