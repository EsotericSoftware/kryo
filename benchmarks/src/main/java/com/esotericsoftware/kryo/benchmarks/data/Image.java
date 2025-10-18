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

package com.esotericsoftware.kryo.benchmarks.data;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;

public class Image {
	@Tag(0) public String uri;
	@Tag(1) public String title; // Can be null.
	@Tag(2) public int width;
	@Tag(3) public int height;
	@Tag(4) public Size size;
	@Tag(5) public Media media; // Can be null.

	public Image () {
	}

	public Image (String uri, String title, int width, int height, Size size, Media media) {
		this.height = height;
		this.title = title;
		this.uri = uri;
		this.width = width;
		this.size = size;
		this.media = media;
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
