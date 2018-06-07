
package com.esotericsoftware.kryo.benchmarks.data;

public class Image {
	public String uri;
	public String title; // Can be null
	public int width;
	public int height;
	public Size size;

	public Image () {
	}

	public Image (String uri, String title, int width, int height, Size size) {
		this.height = height;
		this.title = title;
		this.uri = uri;
		this.width = width;
		this.size = size;
	}

	public boolean equals (Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Image other = (Image)o;
		if (height != other.height) return false;
		if (width != other.width) return false;
		if (size != other.size) return false;
		if (title != null ? !title.equals(other.title) : other.title != null) return false;
		if (uri != null ? !uri.equals(other.uri) : other.uri != null) return false;
		return true;
	}

	public int hashCode () {
		int result = uri != null ? uri.hashCode() : 0;
		result = 31 * result + (title != null ? title.hashCode() : 0);
		result = 31 * result + width;
		result = 31 * result + height;
		result = 31 * result + (size != null ? size.hashCode() : 0);
		return result;
	}

	public String toString () {
		StringBuilder sb = new StringBuilder();
		sb.append("[Image ");
		sb.append("uri=").append(uri);
		sb.append(", title=").append(title);
		sb.append(", width=").append(width);
		sb.append(", height=").append(height);
		sb.append(", size=").append(size);
		sb.append("]");
		return sb.toString();
	}

	static public enum Size {
		SMALL, LARGE
	}
}
