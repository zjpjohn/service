package com.xien.rpc.common.domain;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.Getter;
import lombok.ToString;

/**
 * Create on 16/7/28.
 */
@ToString
@Getter
public final class RpcResponse {

    public static RpcResponse onSuccess(String requestId, Object result) {
        return new RpcResponse(requestId, null, result);
    }

    public static RpcResponse onError(String requestId, Throwable error) {
        Preconditions.checkNotNull(error);
        return new RpcResponse(requestId, error, null);
    }

    public static RpcResponse onSuccess(RpcRequest request, Object result) {
        Preconditions.checkNotNull(request);
        return new RpcResponse(request, null, result);
    }

    public static RpcResponse onError(RpcRequest request, Throwable error) {
        Preconditions.checkNotNull(request);
        Preconditions.checkNotNull(error);
        return new RpcResponse(request, error, null);
    }

    private final String requestId;

    private final Throwable error;

    private final Object result;

    private RpcResponse(String requestId, Throwable error, Object result) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(requestId));

        this.requestId = requestId;
        this.error = error;
        this.result = result;

    }
    private RpcResponse(RpcRequest request, Throwable error, Object result) {
        this(request.getRequestId(), error, result);
    }

    public Object get() throws Throwable {
        if (error != null) {
            throw error;
        }
        return result;
    }

    @Override
    public int hashCode() {
        return requestId.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        else if (!(obj instanceof RpcResponse)) {
            return false;
        }

        return requestId.equals(((RpcResponse)obj).getRequestId());
    }
}
