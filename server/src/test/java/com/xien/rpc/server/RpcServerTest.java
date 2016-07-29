package com.xien.rpc.server;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.xien.rpc.client.RpcClient;
import com.xien.rpc.common.registry.Discover;
import com.xien.rpc.common.registry.Listener;
import com.xien.rpc.common.registry.Register;
import com.xien.rpc.common.registry.ServiceServer;
import com.xien.rpc.common.registry.zk.ZKDiscover;
import com.xien.rpc.common.registry.zk.ZKRegister;
import org.junit.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Create on 16/7/29.
 */
public class RpcServerTest {

    private static final ServiceServer serviceServer = new ServiceServer("test", 0, ImmutableMap.of(), new InetSocketAddress(InetAddress.getLoopbackAddress(), 7110));
    private static final Set<ServiceServer> servers = ImmutableSet.of(
            serviceServer
    );

    private static class DummyRegister implements Register{

        @Override
        public void start() {
            System.out.println("Register started");
        }

        @Override
        public void register(ServiceServer serviceServer) throws Exception {
            System.out.println("Service registered - " + serviceServer.toJson().toString(1));
        }

        @Override
        public void close() {
            System.out.println("Register closed");
        }
    }

    private static class DummyDiscover implements Discover {

        @Override
        public Set<ServiceServer> start(Listener listener) {
            return ImmutableSet.copyOf(servers);
        }

        @Override
        public void close() {

        }
    }

    public interface HelloService {
        String sayHello(String name);
    }

    @org.junit.Test
    public void startServer() throws Exception {;

        RpcServer server = new RpcServer(new DummyRegister(), serviceServer, 10);
        server.register(HelloService.class, new HelloService() {

            @Override
            public String sayHello(String name) {
                return "Hello world, " + name;
            }
        });
        server.start();
        try {
            RpcClient client = new RpcClient("test", new DummyDiscover());
            client.start();
            try {
                HelloService helloService = client.newService(HelloService.class);
                System.out.println(helloService.sayHello("xien"));
            }
            finally {
                client.close();
            };
        }
        finally {
            server.close();
        }
    }

}