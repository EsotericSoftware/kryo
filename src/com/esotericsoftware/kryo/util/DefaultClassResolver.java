/* Copyright (c) 2008-2026, Nathan Sweet
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

package com.esotericsoftware.kryo.util;

import static com.esotericsoftware.kryo.util.Util.*;
import static com.esotericsoftware.kryo.util.Log.*;

import com.esotericsoftware.kryo.ClassResolver;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.TimeZone;

/** Resolves classes by ID or by fully qualified class name.
 * @author Nathan Sweet */
public class DefaultClassResolver implements ClassResolver {
	public static final byte NAME = -1;

	protected Kryo kryo;

	protected final IntMap<Registration> idToRegistration = new IntMap<>();
	protected final IdentityMap<Class, Registration> classToRegistration = new IdentityMap<>();

	protected IdentityObjectIntMap<Class> classToNameId;
	protected IntMap<Class> nameIdToClass;
	protected ObjectMap<String, Class> nameToClass;
	protected int nextNameId;
	/** Names of classes that were not found, by name ID, so a later reference to the name ID can't be read as a class name. */
	protected IntMap<String> unknownNameIdToName;
	/** The nesting depth of {@link #beginDeferredNames()}. */
	protected int deferredNames;
	/** The classes whose names are deferred, in the order of their name IDs. */
	protected ArrayList<Class> deferredClasses;

	private int memoizedClassId = -1;
	private Registration memoizedClassIdValue;
	private Class memoizedClass;
	private Registration memoizedClassValue;

	public void setKryo (Kryo kryo) {
		this.kryo = kryo;
	}

	public Registration register (Registration registration) {
		memoizedClassId = -1;
		memoizedClass = null;
		if (registration == null) throw new IllegalArgumentException("registration cannot be null.");
		if (registration.getId() != NAME) {
			if (TRACE) {
				trace("kryo", "Register class ID " + registration.getId() + ": " + className(registration.getType()) + " ("
					+ registration.getSerializer().getClass().getName() + ")");
			}
			idToRegistration.put(registration.getId(), registration);
		} else if (TRACE) {
			trace("kryo", "Register class name: " + className(registration.getType()) + " ("
				+ registration.getSerializer().getClass().getName() + ")");
		}
		classToRegistration.put(registration.getType(), registration);
		Class wrapperClass = getWrapperClass(registration.getType());
		if (wrapperClass != registration.getType()) classToRegistration.put(wrapperClass, registration);
		return registration;
	}

	public Registration unregister (int classID) {
		Registration registration = idToRegistration.remove(classID);
		if (registration != null) {
			classToRegistration.remove(registration.getType());
			memoizedClassId = -1;
			memoizedClass = null;
			Class wrapperClass = getWrapperClass(registration.getType());
			if (wrapperClass != registration.getType()) classToRegistration.remove(wrapperClass);
		}
		return registration;
	}

	public Registration registerImplicit (Class type) {
		return register(new Registration(type, kryo.getDefaultSerializer(type), NAME));
	}

	public Registration getRegistration (Class type) {
		if (type == memoizedClass) return memoizedClassValue;
		Registration registration = classToRegistration.get(type);
		if (registration != null) {
			memoizedClass = type;
			memoizedClassValue = registration;
		}
		return registration;
	}

	public Registration getRegistration (int classID) {
		return idToRegistration.get(classID);
	}

	public Registration writeClass (Output output, Class type) {
		if (type == null) {
			if (TRACE || (DEBUG && kryo.getDepth() == 1)) log("Write", null, output.position());
			output.writeByte(Kryo.NULL);
			return null;
		}
		Registration registration = kryo.getRegistration(type);
		if (registration.getId() == NAME)
			writeName(output, type, registration);
		else {
			if (TRACE) trace("kryo", "Write class " + registration.getId() + ": " + className(type) + pos(output.position()));
			output.writeVarInt(registration.getId() + 2, true);
		}
		return registration;
	}

	protected void writeName (Output output, Class type, Registration registration) {
		output.writeByte(1); // NAME + 2
		// An enum constant with a body is written as its enum, so the name and its ID don't depend on the bodies. A charset or time
		// zone is written as Charset or TimeZone, because the implementation classes differ, see Kryo#getRegistration(Class).
		Class registeredType = registration.getType();
		if (registeredType.isEnum() || registeredType == Charset.class || registeredType == TimeZone.class) type = registeredType;
		if (classToNameId != null) {
			int nameId = classToNameId.get(type, -1);
			if (nameId != -1) {
				if (TRACE) trace("kryo", "Write class name reference " + nameId + ": " + className(type) + pos(output.position()));
				output.writeVarInt(nameId, true);
				return;
			}
		}
		// Only write the class name the first time encountered in object graph.
		if (TRACE) trace("kryo", "Write class name: " + className(type) + pos(output.position()));
		int nameId = nextNameId++;
		if (classToNameId == null) classToNameId = new IdentityObjectIntMap<>();
		classToNameId.put(type, nameId);
		output.writeVarInt(nameId, true);
		if (deferredNames > 0) { // The class name is written by endDeferredNames.
			if (deferredClasses == null) deferredClasses = new ArrayList<>();
			deferredClasses.add(type);
			return;
		}
		if (registration.isTypeNameAscii())
			output.writeAscii(type.getName());
		else
			output.writeString(type.getName());
	}

	public Registration readClass (Input input) {
		int classID = input.readVarInt(true);
		switch (classID) {
		case Kryo.NULL:
			if (TRACE || (DEBUG && kryo.getDepth() == 1)) log("Read", null, input.position());
			return null;
		case NAME + 2: // Offset for NAME and NULL.
			return readName(input);
		}
		if (classID == memoizedClassId) {
			if (TRACE) trace("kryo",
				"Read class " + (classID - 2) + ": " + className(memoizedClassIdValue.getType()) + pos(input.position()));
			return memoizedClassIdValue;
		}
		Registration registration = idToRegistration.get(classID - 2);
		if (registration == null) throw new KryoException("Encountered unregistered class ID: " + (classID - 2));
		if (TRACE) trace("kryo", "Read class " + (classID - 2) + ": " + className(registration.getType()) + pos(input.position()));
		memoizedClassId = classID;
		memoizedClassIdValue = registration;
		return registration;
	}

	protected Registration readName (Input input) {
		int nameId = input.readVarInt(true);
		if (nameIdToClass == null) nameIdToClass = new IntMap<>();
		Class type = nameIdToClass.get(nameId);
		if (type == null) {
			if (unknownNameIdToName != null) {
				String className = unknownNameIdToName.get(nameId);
				if (className != null) throw new KryoException("Unable to find class: " + className);
			}
			// Only read the class name the first time encountered in object graph.
			type = readName(nameId, input.readString());
		} else {
			if (TRACE) trace("kryo", "Read class name reference " + nameId + ": " + className(type) + pos(input.position()));
		}
		return kryo.getRegistration(type);
	}

	/** Returns the class with the specified name and remembers it for the name ID.
	 * @throws KryoException if the class is not found. The name is remembered as unknown for the name ID. */
	private Class readName (int nameId, String className) {
		Class type;
		try {
			type = getTypeByName(className);
		} catch (RuntimeException ex) { // A subclass can reject a class.
			unknownName(nameId, className);
			throw ex;
		}
		if (type == null) {
			// Classes with a default serializer, eg JDK-internal classes, are found without reflection.
			type = kryo.getDefaultSerializerType(className);
			if (type == null) {
				try {
					type = Class.forName(className, false, kryo.getClassLoader());
				} catch (ClassNotFoundException ex) {
					// Fallback to Kryo's class loader.
					try {
						type = Class.forName(className, false, Kryo.class.getClassLoader());
					} catch (ClassNotFoundException ex2) {
						unknownName(nameId, className);
						throw new KryoException("Unable to find class: " + className, ex);
					}
				}
			}
			if (nameToClass == null) nameToClass = new ObjectMap<>();
			nameToClass.put(className, type);
		}
		nameIdToClass.put(nameId, type);
		if (TRACE) trace("kryo", "Read class name: " + className);
		return type;
	}

	/** Remembers the name of a class that is not found, so a later reference to the name ID is not read as a class name. */
	private void unknownName (int nameId, String className) {
		if (unknownNameIdToName == null) unknownNameIdToName = new IntMap<>();
		unknownNameIdToName.put(nameId, className);
	}

	public int beginDeferredNames () {
		deferredNames++;
		return deferredClasses == null ? 0 : deferredClasses.size();
	}

	public void endDeferredNames (Output output, int mark) {
		deferredNames--;
		ArrayList<Class> deferredClasses = this.deferredClasses;
		int size = deferredClasses == null ? 0 : deferredClasses.size();
		output.writeVarInt(size - mark, true);
		for (int i = mark; i < size; i++) {
			Class type = deferredClasses.get(i);
			output.writeVarInt(classToNameId.get(type, -1), true);
			Registration registration = getRegistration(type);
			if (registration != null && registration.isTypeNameAscii())
				output.writeAscii(type.getName());
			else
				output.writeString(type.getName());
		}
		// The outer scopes write the class names too, in case an inner scope is skipped.
		if (deferredNames == 0 && deferredClasses != null) deferredClasses.clear();
	}

	public void readDeferredNames (Input input) {
		if (nameIdToClass == null) nameIdToClass = new IntMap<>();
		for (int i = 0, n = input.readVarInt(true); i < n; i++) {
			int nameId = input.readVarInt(true);
			String className = input.readString();
			if (nameIdToClass.containsKey(nameId)) continue;
			if (unknownNameIdToName != null && unknownNameIdToName.containsKey(nameId)) continue;
			try {
				readName(nameId, className);
			} catch (RuntimeException ignored) { // The class is unknown, a reference to it is read as an unknown class.
			}
		}
	}

	protected Class getTypeByName (final String className) {
		return nameToClass != null ? nameToClass.get(className) : null;
	}

	public void reset () {
		// Class names are written for unregistered classes, also if they are allowed although registration is required.
		if (classToNameId != null) classToNameId.clear(2048);
		if (nameIdToClass != null) nameIdToClass.clear();
		if (unknownNameIdToName != null) unknownNameIdToName.clear();
		nextNameId = 0;
		deferredNames = 0;
		if (deferredClasses != null) deferredClasses.clear();
	}
}
