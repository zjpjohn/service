package com.xien.rpc.server;

import com.google.common.base.Preconditions;
import com.xien.rpc.common.domain.RpcRequest;
import com.xien.rpc.common.domain.RpcResponse;
import com.xien.rpc.common.registry.Register;
import com.xien.rpc.common.registry.ServiceServer;
import com.xien.rpc.common.serialize.RpcDecoder;
import com.xien.rpc.common.serialize.RpcEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;

import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Create on 16/7/28.
 */
public class RpcServer implements Closeable {
    private final NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private final NioEventLoopGroup workerGroup = new NioEventLoopGroup(10);
    private final ExecutorService executorService;

    private final Register register;
    private final ServiceServer serviceServer;
    private final ServerBootstrap bootstrap;
    private final Map<String, ServiceFactory<?>> services = new ConcurrentHashMap<>();
    private volatile Channel channel;

    private class RpcHandler extends SimpleChannelInboundHandler<RpcRequest> {

        @Override
        protected void messageReceived(ChannelHandlerContext channelHandlerContext, RpcRequest request) throws Exception {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    RpcResponse response = null;
                    try {
                        ServiceFactory<?> factory = services.get(request.getClassName());
                        if (factory == null) {
                            response = RpcResponse.onError(request, new IllegalArgumentException("Unknown service - " + request.getClassName()));
                            return;
                        }

                        Object svc = factory.create();
                        if (svc == null) {
                            response = RpcResponse.onError(request, new IllegalArgumentException("Failed to create service instance - " + request.getClassName()));
                            return;
                        }

                        FastClass svcFastClass = FastClass.create(svc.getClass());
                        FastMethod svcFastMethod = svcFastClass.getMethod(request.getMethodName(), request.getParameterTypes());
                        if (svcFastMethod == null) {
                            response = RpcResponse.onError(request, new IllegalArgumentException("Unknown service method - " + request.getClassName() + "." + request.getMethodName()));
                            return;
                        }

                        response = RpcResponse.onSuccess(request, svcFastMethod.invoke(svc, request.getParameters()));
                    }
                    catch (Throwable e) {
                        response = RpcResponse.onError(request, e);
                    }
                    finally {
                        if (response != null && channelHandlerContext.channel().isActive()) {
                            channelHandlerContext.writeAndFlush(response);
                        }
                    }
                }
            });
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            ctx.close();
        }
    }

    public RpcServer(Register register, ServiceServer serviceServer, int nThreads) {
        Preconditions.checkNotNull(register);
        Preconditions.checkNotNull(serviceServer);

        this.register = register;
        this.serviceServer = serviceServer;
        this.executorService = Executors.newFixedThreadPool(nThreads);

        this.bootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 10)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel channel) throws Exception {
                        channel.pipeline()
                                .addLast(new RpcDecoder(RpcRequest.class))
                                .addLast(new RpcEncoder(RpcResponse.class))
                                .addLast(new RpcHandler());
                    }
                });
    }

    public void start() throws Exception {
        channel = bootstrap.bind(serviceServer.getAddress()).syncUninterruptibly().channel();

        register.start();
        register.register(serviceServer);
    }

    @Override
    public void close() {
        if (channel != null) {
            channel.close().syncUninterruptibly();
            channel = null;
        }

        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        register.close();
    }

    public <T> RpcServer register(Class<T> cls, T service) {
        Preconditions.checkArgument(cls != null && cls.isInterface());
        String name = cls.getCanonicalName();
        Preconditions.checkArgument(!services.containsKey(name), "Duplicated service - " + name);
        services.put(name, new SingletonServiceFactory<T>(service));
        return this;
    }

    public <T> RpcServer register(Class<T> cls, ServiceFactory<? extends T> factory) {
        Preconditions.checkArgument(cls != null && cls.isInterface());
        Preconditions.checkNotNull(factory);

        String name = cls.getCanonicalName();
        Preconditions.checkArgument(!services.containsKey(name), "Duplicated service - " + name);
        services.put(name, factory);

        return this;
    }

    public <T> boolean unregister(Class<T> cls) {
        if (cls == null) {
            return false;
        }

        return null != services.remove(cls.getCanonicalName());
    }
}
