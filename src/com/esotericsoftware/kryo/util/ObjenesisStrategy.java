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

/** Creates objects with an <a href="http://objenesis.org/">Objenesis</a> strategy. Objenesis is an optional dependency of Kryo,
 * which the versioned jar includes: add {@code org.objenesis:objenesis} to use this class with the default jar.
 * {@link StdInstantiatorStrategy} and {@link SerializingInstantiatorStrategy} only use it where the JDK's serialization
 * constructors are not available, eg on Android. */
public class ObjenesisStrategy implements InstantiatorStrategy {
	private final org.objenesis.strategy.InstantiatorStrategy strategy;

	public ObjenesisStrategy (org.objenesis.strategy.InstantiatorStrategy strategy) {
		if (strategy == null) throw new IllegalArgumentException("strategy cannot be null.");
		this.strategy = strategy;
	}

	public <T> ObjectInstantiator<T> newInstantiatorOf (Class<T> type) {
		org.objenesis.instantiator.ObjectInstantiator<T> instantiator = strategy.newInstantiatorOf(type);
		return instantiator::newInstance;
	}

	public org.objenesis.strategy.InstantiatorStrategy getStrategy () {
		return strategy;
	}

	// Called by Instantiators, which doesn't reference Objenesis types, so it is loaded without Objenesis.

	static InstantiatorStrategy std () {
		return new ObjenesisStrategy(new org.objenesis.strategy.StdInstantiatorStrategy());
	}

	static InstantiatorStrategy serializing () {
		return new ObjenesisStrategy(new org.objenesis.strategy.SerializingInstantiatorStrategy());
	}
}
