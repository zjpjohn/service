package com.xien.rpc.common.registry;

import java.io.Closeable;
import java.util.Set;

/**
 * Create on 16/7/28.
 */
public interface Discover extends Closeable {
    Set<ServiceServer> start(Listener listener);

    void close();
}
