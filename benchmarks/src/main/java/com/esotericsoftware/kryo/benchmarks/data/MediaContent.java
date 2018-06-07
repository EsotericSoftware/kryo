
package com.esotericsoftware.kryo.benchmarks.data;

import java.util.List;

public class MediaContent implements java.io.Serializable {
	public Media media;
	public List<Image> images;

	public MediaContent () {
	}

	public MediaContent (Media media, List<Image> images) {
		this.media = media;
		this.images = images;
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
