package com.xien.rpc.common.registry;

import java.util.List;
import java.util.Set;

/**
 * Create on 16/7/28.
 */
@FunctionalInterface
public interface Listener {

    void onUpdate(Set<ServiceServer> serviceServers, Exception e);
}
