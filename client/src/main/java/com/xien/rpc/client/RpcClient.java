package com.xien.rpc.client;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.xien.rpc.common.domain.RpcRequest;
import com.xien.rpc.common.domain.RpcResponse;
import com.xien.rpc.common.registry.Discover;
import com.xien.rpc.common.registry.ServiceServer;
import com.xien.rpc.common.serialize.RpcDecoder;
import com.xien.rpc.common.serialize.RpcEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.Closeable;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Create on 16/7/28.
 */
public class RpcClient implements Closeable {
    private final NioEventLoopGroup group = new NioEventLoopGroup(10);
    private final Predicate<ServiceServer> predicate;
    private final Discover discover;
    private final Map<Channel, ServerProxy> servers = new HashMap<>();
    private final SendPolicy sendPolicy;

    private class RpcHandler extends SimpleChannelInboundHandler<RpcResponse> {

        @Override
        protected void messageReceived(ChannelHandlerContext ctx, RpcResponse msg) throws Exception {
            ServerProxy proxy = servers.get(ctx.channel());
            if (proxy == null) {
                return;
            }

            proxy.onResponse(msg);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            ctx.close();
        }

        @Override
        public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
            servers.remove(ctx.channel());
            super.close(ctx, promise);
        }
    }

    public RpcClient(String service, Discover discover) {
        this((server) -> {
            return server.getService().equals(service);
        }, discover, SendPolicy.roundRobin());
    }

    public RpcClient(Predicate<ServiceServer> predicate, Discover discover, SendPolicy sendPolicy) {
        Preconditions.checkNotNull(predicate);
        Preconditions.checkNotNull(discover);
        Preconditions.checkNotNull(sendPolicy);
        this.sendPolicy = sendPolicy;
        this.predicate = predicate;
        this.discover = discover;
    }

    private synchronized void registryUpdate(Set<ServiceServer> servers) {
        // filter discovered servers
        {
            Iterator<ServiceServer> iterator = servers.iterator();
            while (iterator.hasNext()) {
                if (!predicate.apply(iterator.next())) {
                    iterator.remove();
                }
            }
        }

        Set<ServiceServer> aliveServers = new HashSet<ServiceServer>();
        // remove dead servers
        {
            Iterator<Map.Entry<Channel, ServerProxy>> iterator = this.servers.entrySet().iterator();
            while (iterator.hasNext()) {
                ServerProxy proxy = iterator.next().getValue();
                if (!servers.contains(proxy.getServer())) {
                    iterator.remove();
                    proxy.close();
                }
                else {
                    aliveServers.add(proxy.getServer());
                }
            }
        }


        // add new servers
        {
            Set<ServiceServer> newServers = new HashSet<ServiceServer>(servers);
            newServers.removeAll(aliveServers);
            for (ServiceServer server : newServers) {
                newServer(server);
            }
        }
    }

    public void start() {
        Set<ServiceServer> bootstrapServers = discover.start((servers, e) -> {
            if (e != null) {
                e.printStackTrace();
                return;
            }

            try {
                registryUpdate(servers);
            }
            catch (Exception e1) {
                e1.printStackTrace();
            }
        });

        registryUpdate(bootstrapServers);
    }

    RpcResponse send(RpcRequest request, long timeout, TimeUnit timeUnit) {
        if (servers.isEmpty()) {
            return RpcResponse.onError(request, new IllegalStateException("No available server"));
        }

        ServerProxy proxy = sendPolicy.select(ImmutableList.copyOf(servers.values()), request);
        if (proxy == null) {
            return RpcResponse.onError(request, new IllegalStateException("Server select failed"));
        }
        else {
            return proxy.send(request, timeout, timeUnit);
        }
    }

    public <T> T newService(Class<T> cls) {
        Preconditions.checkNotNull(cls);
        return (T) Proxy.newProxyInstance(RpcClient.class.getClassLoader(), new Class[]{cls}, new ServiceProxy(this, cls));
    }

    private void newServer(ServiceServer server) {
        Bootstrap bootstrap = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel channel) throws Exception {
                        channel.pipeline()
                                .addLast(new RpcEncoder(RpcRequest.class))
                                .addLast(new RpcDecoder(RpcResponse.class))
                                .addLast(new RpcHandler());
                    }
                });

        Channel channel = bootstrap.connect(server.getAddress()).syncUninterruptibly().channel();
        servers.put(channel, new ServerProxy(server, channel));
    }

    @Override
    public void close() {
        for (ServerProxy proxy : servers.values()) {
            proxy.close();
        }
        servers.clear();
        group.shutdownGracefully();
    }
}
