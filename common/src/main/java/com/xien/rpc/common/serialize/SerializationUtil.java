package com.xien.rpc.common.serialize;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import com.google.common.base.Preconditions;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Create on 16/7/27.
 */
public class SerializationUtil {
    private static final SerializationUtil INSTANCE = new SerializationUtil();

    public static SerializationUtil getInstance() {
        return INSTANCE;
    }

    private final Map<Class<?>, Schema<?>> cachedSchema = new ConcurrentHashMap<>();

    private final Objenesis objenesis = new ObjenesisStd(true);


    private  <T> Schema<T> getSchema(Class<T> cls) {
        Preconditions.checkNotNull(cls);

        Schema<T> schema = (Schema<T>) cachedSchema.get(cls);
        if (schema == null) {
            schema = RuntimeSchema.createFrom(cls);
            cachedSchema.put(cls, schema);
        }
        return schema;
    }

    public <T> byte[] serialize(T obj) {
        Preconditions.checkNotNull(obj);

        Class<T> cls = (Class<T>) obj.getClass();

        LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
        try {
            Schema<T> schema = getSchema(cls);
            return ProtostuffIOUtil.toByteArray(obj, schema, buffer);
        }
        catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        finally {
            buffer.clear();
        }
    }

    public <T> T deserialize(byte[] data, Class<T> cls) {
        Preconditions.checkNotNull(data);
        Preconditions.checkNotNull(cls);

        try {
            T message = objenesis.newInstance(cls);
            Schema<T> schema = getSchema(cls);
            ProtostuffIOUtil.mergeFrom(data, message, schema);
            return message;
        }
        catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}
