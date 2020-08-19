/* Copyright (c) 2008-2020, Nathan Sweet
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

package com.esotericsoftware.kryo.serializers;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.SerializerFactory;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import org.junit.Assert;
import org.junit.Test;
import org.objenesis.strategy.SerializingInstantiatorStrategy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;

public class ExtendedFieldNamesTest {

	@Test(expected = IllegalArgumentException.class)
	public void setExtendedFieldNamesDefault() throws IOException {
		final Child child = new Child();
		child.setCustomNote(true);
		child.setCid(3);
		Kryo kryo = getKryo(false);
		final File outputFile = File.createTempFile("temp_output", "dat");
		try (final Output output = new Output(new FileOutputStream(outputFile))) {
			kryo.writeObject(output, child);
		}
	}

	@Test
	public void setExtendedFieldNamesIsTrue() throws IOException {
		final Child child = new Child();
		child.setCustomNote(true);
		child.setCid(3);
		Kryo kryo = getKryo(true);
		final File outputFile = File.createTempFile("temp_output", "dat");
		try (final Output output = new Output(new FileOutputStream(outputFile))) {
			kryo.writeObject(output, child);
			final Input input = new Input(new FileInputStream(outputFile));
			final Child restoreChild = kryo.readObject(input, Child.class);
			Assert.assertEquals(true, restoreChild.customNote);
			input.close();
		}
	}

	private Kryo getKryo(Boolean isSetExtendedFieldNames) {
		final Kryo kryo = new Kryo();
		kryo.setDefaultSerializer(new SerializerFactory.BaseSerializerFactory() {
			@Override
			public Serializer newSerializer(Kryo kryo, Class type) {
				final CompatibleFieldSerializer.CompatibleFieldSerializerConfig config =
						new CompatibleFieldSerializer.CompatibleFieldSerializerConfig();
				config.setChunkedEncoding(true);
				config.setReadUnknownFieldData(true);
				if (isSetExtendedFieldNames)
					config.setExtendedFieldNames(true);
				final CompatibleFieldSerializer serializer = new CompatibleFieldSerializer(kryo, type, config);
				return serializer;
			}
		});
		kryo.setRegistrationRequired(false);
		kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new SerializingInstantiatorStrategy()));
		return kryo;
	}

	private class Child extends Father {
		private Integer cid = 1;
		private Boolean customNote = true;

		public void setCid(Integer cid) {
			this.cid = cid;
		}

		public int getCid() {
			return this.cid;
		}

		public void setCustomNote(Boolean customNote) {
			this.customNote = customNote;
		}

		public Boolean getCustomNote() {
			return this.customNote;
		}
	}

	private class Father extends Grandpa {
		private Integer fid = 1;
		private String name = "Alan";

		public void setFid(Integer fid) {
			this.fid = fid;
		}

		public int getFid() {
			return this.fid;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}
	}

	private class Grandpa implements Serializable {
		private Integer gid = 1;
		private Boolean customNote = false;

		public void setGid(Integer gid) {
			this.gid = gid;
		}

		public int getGid() {
			return this.gid;
		}

		public void setCustomNote(Boolean customNote) {
			this.customNote = customNote;
		}

		public Boolean getCustomNote() {
			return this.customNote;
		}
	}
}
