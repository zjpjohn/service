package com.xien.rpc.client;

import com.xien.rpc.common.domain.RpcRequest;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Create on 16/7/29.
 */
public class ServiceProxy implements InvocationHandler {
    private static final String UUID = java.util.UUID.randomUUID().toString();
    private static final AtomicLong ID = new AtomicLong(0);

    private static String generateId() {
        return UUID + "_" + ID.incrementAndGet();
    }

    private final RpcClient client;
    private final Class<?> cls;
    private final String clsName;
    private final Map<String, Class<?>[]> parameterTypes = new HashMap<>();

    ServiceProxy(RpcClient client, Class<?> cls) {
        this.client = client;
        this.cls = cls;
        this.clsName = cls.getCanonicalName();

        for (Method m : cls.getMethods()) {
            parameterTypes.put(m.getName(), m.getParameterTypes());
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        RpcRequest request = new RpcRequest(
                generateId(),
                clsName,
                method.getName(),
                parameterTypes.get(method.getName()),
                args == null ? new Object[0] : args
        );
        return client.send(request, 10, TimeUnit.SECONDS).get();
    }
}
