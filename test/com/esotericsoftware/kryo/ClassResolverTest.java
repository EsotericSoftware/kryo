package com.esotericsoftware.kryo;

import java.util.HashSet;
import java.util.Set;

import com.esotericsoftware.kryo.FieldSerializerTest.DefaultTypes;
import com.esotericsoftware.kryo.FieldSerializerTest.HasStringField;
import com.esotericsoftware.kryo.util.DefaultClassResolver;

public class ClassResolverTest extends KryoTestCase {

	public void testSetClassResolverCopiesAllRegistrations() {
		ClassResolver cr1 = kryo.getClassResolver();

		kryo.register(DefaultTypes.class);
		assertNotNull(cr1.getRegistration(DefaultTypes.class));

		ClassResolver cr2 = new DefaultClassResolver();
		kryo.setClassResolver(cr2, true);

		assertNotNull(cr2.getRegistration(DefaultTypes.class));
		assertEquals(toSet(cr1.getRegistrations()), toSet(cr2.getRegistrations()));
	}

	public void testSetClassResolverCopiesOnlyPrimitiveRegistrations() {
		ClassResolver cr1 = kryo.getClassResolver();

		Registration registration = kryo.register(DefaultTypes.class);
		assertNotNull(cr1.getRegistration(DefaultTypes.class));

		ClassResolver cr2 = new DefaultClassResolver();
		kryo.setClassResolver(cr2, false);

		assertNull(cr2.getRegistration(DefaultTypes.class));
		Set<Registration> cr1Registrations = toSet(cr1.getRegistrations());
		assertTrue(cr1Registrations.remove(registration));
		assertEquals(cr1Registrations, toSet(cr2.getRegistrations()));
	}

	public void testSetClassResolverAfterRegistrations () {
		// Registrations and assertions from FieldSerializerTest.testRegistration
		int id = kryo.getNextRegistrationId();
		kryo.register(DefaultTypes.class, id);
		kryo.register(DefaultTypes.class, id);
		kryo.register(new Registration(byte[].class, kryo.getDefaultSerializer(byte[].class), id + 1));
		kryo.register(byte[].class, kryo.getDefaultSerializer(byte[].class), id + 1);
		kryo.register(HasStringField.class, kryo.getDefaultSerializer(HasStringField.class));

		// Set the class resolver
		kryo.setClassResolver(new DefaultClassResolver(), true);

		// Assertions from FieldSerializerTest.testRegistration
		DefaultTypes test = new DefaultTypes();
		test.intField = 12;
		test.StringField = "meow";
		test.CharacterField = 'z';
		test.byteArrayField = new byte[] {0, 1, 2, 3, 4};
		test.child = new DefaultTypes();
		roundTrip(75, 95, test);
	}

	public void testSetClassResolverBetweenRegistrations () {
		// Registrations (slightly adjusted) and assertions from FieldSerializerTest.testRegistration
		kryo.register(DefaultTypes.class, kryo.getNextRegistrationId());
		kryo.register(new Registration(byte[].class, kryo.getDefaultSerializer(byte[].class), kryo.getNextRegistrationId()));

		// Set the class resolver
		kryo.setClassResolver(new DefaultClassResolver(), true);

		// Continue registrations
		kryo.register(byte[].class, kryo.getDefaultSerializer(byte[].class), kryo.getNextRegistrationId());
		kryo.register(HasStringField.class, kryo.getDefaultSerializer(HasStringField.class));

		// Assertions from FieldSerializerTest.testRegistration
		DefaultTypes test = new DefaultTypes();
		test.intField = 12;
		test.StringField = "meow";
		test.CharacterField = 'z';
		test.byteArrayField = new byte[] {0, 1, 2, 3, 4};
		test.child = new DefaultTypes();
		roundTrip(75, 95, test);
	}
}
