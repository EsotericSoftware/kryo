
package com.esotericsoftware.kryo.benchmarks.data;

import com.esotericsoftware.kryo.benchmarks.data.Image.Size;
import com.esotericsoftware.kryo.benchmarks.data.Media.Player;
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;

import java.util.ArrayList;
import java.util.List;

public class MediaContent implements java.io.Serializable {
	@Tag(0) public Media media;
	@Tag(1) public List<Image> images;

	public MediaContent () {
	}

	public MediaContent (Media media, List<Image> images) {
		this.media = media;
		this.images = images;
	}

	public MediaContent populate (boolean circularReference) {
		media = new Media();
		media.uri = "http://javaone.com/keynote.ogg";
		media.width = 641;
		media.height = 481;
		media.format = "video/theora\u1234";
		media.duration = 18000001;
		media.size = 58982401;
		media.persons = new ArrayList();
		media.persons.add("Bill Gates, Jr.");
		media.persons.add("Steven Jobs");
		media.player = Player.FLASH;
		media.copyright = "Copyright (c) 2009, Scooby Dooby Doo";
		images = new ArrayList();
		Media media = circularReference ? this.media : null;
		images.add(new Image("http://javaone.com/keynote_huge.jpg", "Javaone Keynote\u1234", 32000, 24000, Size.LARGE, media));
		images.add(new Image("http://javaone.com/keynote_large.jpg", null, 1024, 768, Size.LARGE, media));
		images.add(new Image("http://javaone.com/keynote_small.jpg", null, 320, 240, Size.SMALL, media));
		return this;
	}

	public boolean equals (Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MediaContent other = (MediaContent)o;
		if (images != null ? !images.equals(other.images) : other.images != null) return false;
		if (media != null ? !media.equals(other.media) : other.media != null) return false;
		return true;
	}

	public int hashCode () {
		int result = media != null ? media.hashCode() : 0;
		result = 31 * result + (images != null ? images.hashCode() : 0);
		return result;
	}

	public String toString () {
		StringBuilder sb = new StringBuilder();
		sb.append("[MediaContent: ");
		sb.append("media=").append(media);
		sb.append(", images=").append(images);
		sb.append("]");
		return sb.toString();
	}
}
