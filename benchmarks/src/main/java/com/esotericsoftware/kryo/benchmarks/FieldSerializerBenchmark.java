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

package com.esotericsoftware.kryo.benchmarks;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.SerializerFactory.CompatibleFieldSerializerFactory;
import com.esotericsoftware.kryo.SerializerFactory.TaggedFieldSerializerFactory;
import com.esotericsoftware.kryo.benchmarks.data.Image;
import com.esotericsoftware.kryo.benchmarks.data.Image.Size;
import com.esotericsoftware.kryo.benchmarks.data.Media;
import com.esotericsoftware.kryo.benchmarks.data.Media.Player;
import com.esotericsoftware.kryo.benchmarks.data.MediaContent;
import com.esotericsoftware.kryo.benchmarks.data.Sample;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.CollectionSerializer;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.esotericsoftware.kryo.serializers.VersionFieldSerializer;

import java.util.ArrayList;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

public class FieldSerializerBenchmark {
	@Benchmark
	public void field (FieldSerializerState state) {
		state.roundTrip();
	}

	@Benchmark
	public void compatible (CompatibleState state) {
		state.roundTrip();
	}

	@Benchmark
	public void tagged (TaggedState state) {
		state.roundTrip();
	}

	@Benchmark
	public void version (VersionState state) {
		state.roundTrip();
	}

	@Benchmark
	public void custom (CustomState state) {
		state.roundTrip();
	}

	//

	@State(Scope.Thread)
	static public abstract class BenchmarkState {
		@Param({"true", "false"}) public boolean references;
		@Param() public ObjectType objectType;

		final Kryo kryo = new Kryo();
		final Output output = new Output(1024 * 512);
		final Input input = new Input(output.getBuffer());
		Object object;

		@Setup(Level.Trial)
		public void setup () {
			switch (objectType) {
			case sample:
				object = new Sample().populate(references);
				kryo.register(double[].class);
				kryo.register(int[].class);
				kryo.register(long[].class);
				kryo.register(float[].class);
				kryo.register(double[].class);
				kryo.register(short[].class);
				kryo.register(char[].class);
				kryo.register(boolean[].class);
				kryo.register(object.getClass());
				break;
			case media:
				object = new MediaContent().populate(references);
				kryo.register(Image.class);
				kryo.register(Size.class);
				kryo.register(Media.class);
				kryo.register(Player.class);
				kryo.register(ArrayList.class);
				kryo.register(MediaContent.class);
				break;
			}

			kryo.setReferences(references);
		}

		public void roundTrip () {
			output.setPosition(0);
			kryo.writeObject(output, object);
			input.setPosition(0);
			input.setLimit(output.position());
			kryo.readObject(input, object.getClass());
		}

		static public enum ObjectType {
			sample, media
		}
	}

	static public class FieldSerializerState extends BenchmarkState {
		public void setup () {
			kryo.setDefaultSerializer(FieldSerializer.class);
			super.setup();
		}
	}

	static public class CompatibleState extends BenchmarkState {
		@Param({"true", "false"}) public boolean chunked;

		public void setup () {
			CompatibleFieldSerializerFactory factory = new CompatibleFieldSerializerFactory();
			factory.getConfig().setChunkedEncoding(chunked);
			factory.getConfig().setReadUnknownFieldData(true); // Typical to always use.
			kryo.setDefaultSerializer(factory);
			super.setup();
		}
	}

	static public class TaggedState extends BenchmarkState {
		@Param({"true", "false"}) public boolean chunked;

		public void setup () {
			TaggedFieldSerializerFactory factory = new TaggedFieldSerializerFactory();
			factory.getConfig().setChunkedEncoding(chunked);
			if (chunked) factory.getConfig().setReadUnknownTagData(true); // Typical to use with chunked.
			kryo.setDefaultSerializer(factory);
			super.setup();
		}
	}

	static public class VersionState extends BenchmarkState {
		public void setup () {
			kryo.setDefaultSerializer(VersionFieldSerializer.class);
			super.setup();
		}
	}

	static public class CustomState extends BenchmarkState {
		public void setup () {
			super.setup();
			switch (objectType) {
			case sample:
				kryo.register(Sample.class, new Serializer<Sample>() {
					public void write (Kryo kryo, Output output, Sample object) {
						output.writeInt(object.intValue);
						output.writeLong(object.longValue);
						output.writeFloat(object.floatValue);
						output.writeDouble(object.doubleValue);
						output.writeShort(object.shortValue);
						output.writeChar(object.charValue);
						output.writeBoolean(object.booleanValue);
						kryo.writeObject(output, object.IntValue);
						kryo.writeObject(output, object.LongValue);
						kryo.writeObject(output, object.FloatValue);
						kryo.writeObject(output, object.DoubleValue);
						kryo.writeObject(output, object.ShortValue);
						kryo.writeObject(output, object.CharValue);
						kryo.writeObject(output, object.BooleanValue);

						kryo.writeObject(output, object.intArray);
						kryo.writeObject(output, object.longArray);
						kryo.writeObject(output, object.floatArray);
						kryo.writeObject(output, object.doubleArray);
						kryo.writeObject(output, object.shortArray);
						kryo.writeObject(output, object.charArray);
						kryo.writeObject(output, object.booleanArray);

						kryo.writeObjectOrNull(output, object.string, String.class);
						kryo.writeObjectOrNull(output, object.sample, Sample.class);
					}

					public Sample read (Kryo kryo, Input input, Class<? extends Sample> type) {
						Sample object = new Sample();
						object.intValue = input.readInt();
						object.longValue = input.readLong();
						object.floatValue = input.readFloat();
						object.doubleValue = input.readDouble();
						object.shortValue = input.readShort();
						object.charValue = input.readChar();
						object.booleanValue = input.readBoolean();
						object.IntValue = kryo.readObject(input, Integer.class);
						object.LongValue = kryo.readObject(input, Long.class);
						object.FloatValue = kryo.readObject(input, Float.class);
						object.DoubleValue = kryo.readObject(input, Double.class);
						object.ShortValue = kryo.readObject(input, Short.class);
						object.CharValue = kryo.readObject(input, Character.class);
						object.BooleanValue = kryo.readObject(input, Boolean.class);

						object.intArray = kryo.readObject(input, int[].class);
						object.longArray = kryo.readObject(input, long[].class);
						object.floatArray = kryo.readObject(input, float[].class);
						object.doubleArray = kryo.readObject(input, double[].class);
						object.shortArray = kryo.readObject(input, short[].class);
						object.charArray = kryo.readObject(input, char[].class);
						object.booleanArray = kryo.readObject(input, boolean[].class);

						object.string = kryo.readObjectOrNull(input, String.class);
						object.sample = kryo.readObjectOrNull(input, Sample.class);

						return object;
					}
				});
				break;
			case media:
				MediaSerializer mediaSerializer = new MediaSerializer(kryo);
				ImageSerializer imageSerializer = new ImageSerializer();
				kryo.register(Image.class, imageSerializer);
				kryo.register(Media.class, mediaSerializer);
				kryo.register(MediaContent.class, new MediaContentSerializer(kryo, mediaSerializer, imageSerializer));
				break;
			}
		}
	}

	static class MediaContentSerializer extends Serializer<MediaContent> {
		private final MediaSerializer mediaSerializer;
		private final CollectionSerializer imagesSerializer;

		public MediaContentSerializer (Kryo kryo, MediaSerializer mediaSerializer, ImageSerializer imageSerializer) {
			this.mediaSerializer = mediaSerializer;
			imagesSerializer = new CollectionSerializer();
			imagesSerializer.setElementsCanBeNull(false);
			imagesSerializer.setElementClass(Image.class, imageSerializer);
		}

		public MediaContent read (Kryo kryo, Input input, Class<? extends MediaContent> type) {
			return new MediaContent(kryo.readObject(input, Media.class), kryo.readObject(input, ArrayList.class, imagesSerializer));
		}

		public void write (Kryo kryo, Output output, MediaContent mediaContent) {
			kryo.writeObject(output, mediaContent.media, mediaSerializer);
			kryo.writeObject(output, mediaContent.images, imagesSerializer);
		}
	}

	static class MediaSerializer extends Serializer<Media> {
		static private final Media.Player[] players = Media.Player.values();

		private final CollectionSerializer personsSerializer;

		public MediaSerializer (final Kryo kryo) {
			personsSerializer = new CollectionSerializer();
			personsSerializer.setElementsCanBeNull(false);
			personsSerializer.setElementClass(String.class, kryo.getSerializer(String.class));
		}

		public Media read (Kryo kryo, Input input, Class<? extends Media> type) {
			return new Media(input.readString(), input.readString(), input.readInt(true), input.readInt(true), input.readString(),
				input.readLong(true), input.readLong(true), input.readInt(true), input.readBoolean(),
				kryo.readObject(input, ArrayList.class, personsSerializer), players[input.readInt(true)], input.readString());
		}

		public void write (Kryo kryo, Output output, Media media) {
			output.writeString(media.uri);
			output.writeString(media.title);
			output.writeInt(media.width, true);
			output.writeInt(media.height, true);
			output.writeString(media.format);
			output.writeLong(media.duration, true);
			output.writeLong(media.size, true);
			output.writeInt(media.bitrate, true);
			output.writeBoolean(media.hasBitrate);
			kryo.writeObject(output, media.persons, personsSerializer);
			output.writeInt(media.player.ordinal(), true);
			output.writeString(media.copyright);
		}
	}

	static class ImageSerializer extends Serializer<Image> {
		static private final Size[] sizes = Size.values();

		public Image read (Kryo kryo, Input input, Class<? extends Image> type) {
			return new Image(input.readString(), input.readString(), input.readInt(true), input.readInt(true),
				sizes[input.readInt(true)], kryo.readObjectOrNull(input, Media.class));
		}

		public void write (Kryo kryo, Output output, Image image) {
			output.writeString(image.uri);
			output.writeString(image.title);
			output.writeInt(image.width, true);
			output.writeInt(image.height, true);
			output.writeInt(image.size.ordinal(), true);
			kryo.writeObjectOrNull(output, image.media, Media.class);
		}
	}
}
