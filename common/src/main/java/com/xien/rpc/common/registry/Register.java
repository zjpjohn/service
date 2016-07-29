package com.xien.rpc.common.registry;

import java.io.Closeable;

/**
 * Create on 16/7/28.
 */
public interface Register extends Closeable {

    void start();

    void register(ServiceServer serviceServer) throws Exception;

    void close();

}
