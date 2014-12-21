/* Copyright (c) 2008, Nathan Sweet
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import static com.esotericsoftware.minlog.Log.DEBUG;
import static com.esotericsoftware.minlog.Log.debug;

/** Serializes objects using direct field assignment, with versioning backward compatibility. Fields can be
 * added without invalidating previously serialized bytes. Note that removing, renaming or changing the type of a field is not supported.
 * In addition, forward compatibility is not considered.
 * <p>
 * There is additional overhead compared to {@link FieldSerializer}. 
 * A varible length version code is appended before object. 
 * When deserializing, input version will be examined to decide which field to skip.
 * <p>
 * @author Tianyi HE <hty0807@gmail.com> */
public class VersioningFieldSerializer<T> extends FieldSerializer<T> {

    // use magic number to assume data format, this is not precise but will work
    // unicode char: 釒
    private final static char FORMAT_MAGIC = 37330;

    // minimal version of each field
    private int[] fieldVersion;

    private int typeVersion = 0;

    /**
     * modification on persist entities must use since to annotate new fields
     * in order to maintain backward compatibility
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Since {

        /**
         * Version of field, must less than 32768
         *
         * @return
         */
        short value() default 0;
    }

    public VersioningFieldSerializer(Kryo kryo, Class type) {
        super(kryo, type);
        initializeCachedFields();
    }

    @Override
    protected void initializeCachedFields() {
        CachedField[] fields = getFields();
        fieldVersion = new int[fields.length];
        for (int i = 0, n = fields.length; i < n; i++) {
            Field field = fields[i].getField();
            if (field.getAnnotation(Since.class) != null) {
                fieldVersion[i] = field.getAnnotation(Since.class).value();
                // use maximum version among fields as type version
                typeVersion = Math.max(fieldVersion[i], typeVersion);
            } else {
                fieldVersion[i] = 0;
            }
        }
        this.removedFields.clear();
        if (DEBUG)
            debug("Version for type " + getType().getName() + " is " + typeVersion);
    }

    @Override
    public void removeField(String fieldName) {
        super.removeField(fieldName);
        initializeCachedFields();
    }

    @Override
    public void removeField(CachedField field) {
        super.removeField(field);
        initializeCachedFields();
    }

    @Override
    public void write(Kryo kryo, Output output, T object) {
        CachedField[] fields = getFields();
        // write magic number
        output.writeChar(FORMAT_MAGIC);
        // write version
        output.writeVarInt(typeVersion, true);
        // write fields
        for (int i = 0, n = fields.length; i < n; i++) {
            fields[i].write(output, object);
        }
    }

    @Override
    public T read(Kryo kryo, Input input, Class<T> type) {
        T object = create(kryo, input, type);
        kryo.reference(object);
        // read magic
        char magic = input.readChar();
        int version = 0;
        // magic indicates whether input supports versioning
        if (magic != FORMAT_MAGIC) {
            // roll back previous operation, first 2 bytes may overlap the first field
            input.setPosition(input.position() - 2);
            if (DEBUG)
                debug("No version information, using 0");
        } else {
            // format is verified, read version
            version = input.readVarInt(true);
        }
        CachedField[] fields = getFields();
        for (int i = 0, n = fields.length; i < n; i++) {
            if (fieldVersion[i] > version) {
                if (DEBUG)
                    debug("Skip field " + fields[i].getField().getName());
                continue;
            }
            fields[i].read(input, object);
        }
        return object;
    }
}
