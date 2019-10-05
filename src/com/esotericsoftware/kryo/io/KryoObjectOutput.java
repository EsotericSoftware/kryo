/* Copyright (c) 2008-2018, Nathan Sweet
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

package com.esotericsoftware.kryo.io;

import com.esotericsoftware.kryo.Kryo;

import java.io.IOException;
import java.io.ObjectOutput;

/** An {@link java.io.ObjectOutput} which writes data to an {@link Output}.
 * <p>
 * Note this is not an implementation of {@link java.io.ObjectOutputStream} which has special handling for Java serialization and
 * serialization extras like writeReplace. By default it will simply delegate to the appropriate Kryo method. Also, using it will
 * currently add one extra byte for each time {@link #writeObject(Object)} is invoked since we need to allow unknown null objects.
 * @author Robert DiFalco <robert.difalco@gmail.com> */
public class KryoObjectOutput extends KryoDataOutput implements ObjectOutput {
	private final Kryo kryo;

	public KryoObjectOutput (Kryo kryo, Output output) {
		super(output);
		this.kryo = kryo;
	}

	public void writeObject (Object object) throws IOException {
		kryo.writeClassAndObject(output, object);
	}

	public void flush () throws IOException {
		output.flush();
	}

	public void close () throws IOException {
		output.close();
	}
}
