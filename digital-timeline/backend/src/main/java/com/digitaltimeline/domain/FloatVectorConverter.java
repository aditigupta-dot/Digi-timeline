package com.digitaltimeline.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.nio.ByteBuffer;

/** Stores a float[] embedding as compact bytes (4 bytes per dimension) in a bytea / varbinary column. */
@Converter
public class FloatVectorConverter implements AttributeConverter<float[], byte[]> {

    @Override
    public byte[] convertToDatabaseColumn(float[] vector) {
        if (vector == null) return null;
        ByteBuffer buf = ByteBuffer.allocate(vector.length * Float.BYTES);
        for (float f : vector) buf.putFloat(f);
        return buf.array();
    }

    @Override
    public float[] convertToEntityAttribute(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        float[] out = new float[bytes.length / Float.BYTES];
        for (int i = 0; i < out.length; i++) out[i] = buf.getFloat();
        return out;
    }
}
