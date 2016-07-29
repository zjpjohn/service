package com.xien.rpc.server;

import java.io.Closeable;

/**
 * Create on 16/7/28.
 */
public interface ServiceFactory<T> extends Closeable {

    T create();

    void close();
}
