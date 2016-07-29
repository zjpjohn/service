package com.xien.rpc.common.registry;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * Create on 16/7/28.
 */
public final class ServiceServer {

    public static ServiceServer fromJson(JSONObject obj) {
        String service = obj.getString("service");
        long version = obj.getLong("version");

        String hostname = obj.getString("address");
        int port = obj.getInt("port");
        InetSocketAddress address = new InetSocketAddress(hostname, port);

        Map<String, String> properties = new HashMap<>();

        JSONObject props = obj.optJSONObject("properties");
        if (props != null) {
            for (String key : props.keySet()) {
                properties.put(key, props.getString(key));
            }
        }

        return new ServiceServer(service, version, properties, address);
    }

    private final String service;
    private final long version;
    private final InetSocketAddress address;
    private final Map<String, String> properties;

    public ServiceServer(String service, long version, Map<String, String> properties, InetSocketAddress address) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(service));
        Preconditions.checkNotNull(properties);
        Preconditions.checkNotNull(address);

        this.service = service;
        this.version = version;
        this.address = address;
        this.properties = ImmutableMap.copyOf(properties);
    }

    public String getService() {
        return service;
    }

    public long getVersion() {
        return version;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("service", service);
        obj.put("version", version);
        obj.put("address", address.getHostName());
        obj.put("port", address.getPort());
        obj.put("properties", new JSONObject(properties));
        return obj;
    }

    @Override
    public String toString() {
        return address.toString() + "_" + service + "_" + version;
    }
}
