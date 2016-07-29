package com.xien.rpc.server;

import com.google.common.base.Preconditions;

import java.io.Closeable;
import java.io.IOException;

/**
 * Create on 16/7/28.
 */
class SingletonServiceFactory<T> implements ServiceFactory<T> {

    private final T instance;

    SingletonServiceFactory(T instance) {
        Preconditions.checkNotNull(instance);
        this.instance = instance;
    }

    @Override
    public T create() {
        return instance;
    }

    @Override
    public void close() {
        if (instance instanceof Closeable) {
            try {
                ((Closeable)instance).close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
