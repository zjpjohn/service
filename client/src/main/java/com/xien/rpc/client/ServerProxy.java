package com.xien.rpc.client;

import com.xien.rpc.common.domain.RpcRequest;
import com.xien.rpc.common.domain.RpcResponse;
import com.xien.rpc.common.registry.ServiceServer;
import io.netty.channel.Channel;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * Create on 16/7/29.
 */

public class ServerProxy {
    private final ServiceServer server;
    private final Channel channel;
    private final Map<String, SynchronousQueue<RpcResponse>> tasks = new HashMap<>();

    public ServerProxy(ServiceServer server, Channel channel) {
        this.server = server;
        this.channel = channel;
    }

    public ServiceServer getServer() {
        return server;
    }

    Channel getChannel() {
        return channel;
    }

    RpcResponse send(RpcRequest request, long timeout, TimeUnit timeUnit) {
        if (!channel.isActive()) {
            return RpcResponse.onError(request, new IllegalStateException("Server not connected"));
        }

        SynchronousQueue<RpcResponse> queue = new SynchronousQueue<>();
        try {
            synchronized (this) {
                tasks.put(request.getRequestId(), queue);
            }

            channel.writeAndFlush(request);
            while (true) {
                if (!channel.isActive()) {
                    return RpcResponse.onError(request, new IllegalStateException("Server not connected"));
                }

                try {
                    RpcResponse response = queue.poll(timeout, timeUnit);
                    if (response != null) {
                        return response;
                    }
                    break;
                }
                catch (InterruptedException e) {
                    // ignore
                }
            }

            return RpcResponse.onError(request, new TimeoutException("Request timed out"));
        }
        catch (Throwable e) {
            return RpcResponse.onError(request, e);
        }
        finally {
            synchronized (this) {
                tasks.remove(request.getRequestId());
            }
        }
    }

    void onResponse(RpcResponse response) {
        SynchronousQueue<RpcResponse> queue;

        synchronized (this) {
            queue = tasks.remove(response.getRequestId());
        }
        if (queue != null) {
            while (true) {
                try {
                    queue.put(response);
                    break;
                }
                catch (InterruptedException e) {
                    // ignore
                }
            }
        }
    }

    void close() {
        synchronized (this) {
            for (Map.Entry<String, SynchronousQueue<RpcResponse>> entry : tasks.entrySet()) {
                entry.getValue().offer(RpcResponse.onError(entry.getKey(), new IllegalStateException("Server connection closed")));
            }
            tasks.clear();
        }
        channel.close();
    }
}
