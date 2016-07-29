package com.xien.rpc.client;

import com.xien.rpc.common.domain.RpcRequest;

import java.util.List;
import java.util.Random;

/**
 * Create on 16/7/29.
 */
public interface SendPolicy {

    static SendPolicy roundRobin() {
        return RoundRobinPolicy.INSTANCE;
    }

    static SendPolicy random() {
        return RandomPolicy.INSTANCE;
    }

    ServerProxy select(List<ServerProxy> servers, RpcRequest request);
}

class RoundRobinPolicy implements SendPolicy {
    static final RoundRobinPolicy INSTANCE = new RoundRobinPolicy();

    private volatile int index = 0;
    @Override
    public ServerProxy select(List<ServerProxy> servers, RpcRequest request) {
        return servers.get((index++) % servers.size());
    }
}

class RandomPolicy implements SendPolicy {
    static final RandomPolicy INSTANCE = new RandomPolicy();

    private final Random random = new Random();

    @Override
    public ServerProxy select(List<ServerProxy> servers, RpcRequest request) {
        return servers.get(random.nextInt(servers.size()));
    }
}
