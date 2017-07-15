/* Copyright (c) 2008-2017, Nathan Sweet
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

import static com.esotericsoftware.minlog.Log.*;

/** Configuration for TaggedFieldSerializer instances. */
public class TaggedFieldSerializerConfig extends FieldSerializerConfig {
	boolean skipUnknownTags;

	/** Set whether associated TaggedFieldSerializers should attempt to skip reading the data of unknown tags, rather than throwing
	 * a KryoException. Data can be skipped if it was tagged with {@link TaggedFieldSerializer.Tag#annexed()} set true. This
	 * enables forward compatibility.
	 * <p>
	 * This setting is false by default.
	 * </p>
	 *
	 * @param skipUnknownTags If true, unknown field tags will be skipped, with the assumption that they are future tagged values
	 *           with {@link TaggedFieldSerializer.Tag#annexed()} set true. If false KryoException will be thrown whenever unknown
	 *           tags are encountered. */
	public void setSkipUnknownTags (boolean skipUnknownTags) {
		this.skipUnknownTags = skipUnknownTags;
		if (TRACE) trace("kryo.TaggedFieldSerializerConfig", "setSkipUnknownTags: " + skipUnknownTags);
	}

	/** Whether the TaggedFieldSerializers should attempt to skip reading the data of unknown tags, rather than throwing a
	 * KryoException. The data may only be skipped if the later version of the application which created the data set those unknown
	 * tags with {@link TaggedFieldSerializer.Tag#annexed()} true. See {@link #setSkipUnknownTags(boolean)}. */
	public boolean getSkipUnknownTags () {
		return skipUnknownTags;
	}

	@Override
	public TaggedFieldSerializerConfig clone () {
		return (TaggedFieldSerializerConfig)super.clone();
	}
}
