package com.xien.rpc.common.registry.zk;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.xien.rpc.common.registry.Discover;
import com.xien.rpc.common.registry.Listener;
import com.xien.rpc.common.registry.ServiceServer;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.retry.RetryNTimes;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Create on 16/7/28.
 */
public class ZKDiscover implements Discover {
    private static final ScheduledExecutorService SERVICE = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "zk-discover-cron");
            t.setDaemon(true);
            return t;
        }
    });

    private final CuratorFramework framework;
    private final String basePath;
    private volatile ScheduledFuture<?> future;

    public ZKDiscover(String connectStr, String basePath) {
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

    private Set<ServiceServer> get() throws Exception {

        Set<ServiceServer> serviceServers = new HashSet<ServiceServer>();

        for (String child : framework.getChildren().forPath(basePath)) {
            byte[] data = framework.getData().forPath(basePath + child);
            if (data == null || data.length == 0) {
                continue;
            }

            try {
                JSONObject obj = new JSONObject(new String(data, Charsets.UTF_8));

                ServiceServer server = ServiceServer.fromJson(obj);
                serviceServers.add(server);
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return serviceServers;
    }

    @Override
    public Set<ServiceServer> start(Listener listener) {
        Preconditions.checkNotNull(listener);
        Preconditions.checkArgument(framework.getState() == CuratorFrameworkState.LATENT, "Discover is already started");
        Preconditions.checkArgument(future == null, "Discover is already started");
        this.framework.start();

        future = SERVICE.scheduleWithFixedDelay(() -> {
            try {
                if (framework.getState() != CuratorFrameworkState.STARTED) {
                    return;
                }

                Set<ServiceServer> serviceServers = get();
                try {
                    listener.onUpdate(serviceServers, null);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            catch (Exception e) {
                listener.onUpdate(ImmutableSet.of(), e);
            }
        }, 10, 10, TimeUnit.SECONDS);

        try {
            return get();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        future.cancel(true);
        future = null;
        framework.close();
    }
}
