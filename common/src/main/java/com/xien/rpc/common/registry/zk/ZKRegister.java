package com.xien.rpc.common.registry.zk;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.xien.rpc.common.registry.Register;
import com.xien.rpc.common.registry.ServiceServer;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.CreateMode;

/**
 * Create on 16/7/28.
 */
public class ZKRegister implements Register {
    private final CuratorFramework framework;
    private final String basePath;

    public ZKRegister(String connectStr) {
        this(connectStr, "/");
    }

    public ZKRegister(String connectStr, String basePath) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(basePath));
        Preconditions.checkNotNull(basePath);
        this.framework = CuratorFrameworkFactory.newClient(connectStr, 5000, 3000, new RetryNTimes(5, 3000));
        if (!basePath.startsWith("/")) {
            basePath = "/" + basePath;
        }

        if (basePath.endsWith("/")) {
            this.basePath = basePath;
        }
        else {
            this.basePath = basePath + "/";
        }
    }

    @Override
    public void start() {
        this.framework.start();
    }

    @Override
    public void register(ServiceServer serviceServer) throws Exception {
        Preconditions.checkArgument(framework.getState() == CuratorFrameworkState.STARTED);

        String path = basePath + serviceServer.toString().replaceAll("/", "_");

        if (null == framework.checkExists().forPath(path)) {
            framework.create().withMode(CreateMode.EPHEMERAL).forPath(path,
                    serviceServer.toJson().toString(1).getBytes(Charsets.UTF_8));
        }
    }

    @Override
    public void close() {
        this.framework.close();
    }
}
