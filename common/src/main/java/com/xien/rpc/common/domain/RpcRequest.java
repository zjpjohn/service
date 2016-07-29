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
public final class RpcRequest {
    private final String requestId;

    private final String className;

    private final String methodName;

    private final Class<?>[] parameterTypes;

    private final Object[] parameters;

    public RpcRequest(String requestId, String className, String methodName, Class<?>[] parameterTypes, Object[] parameters) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(requestId));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(className));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(methodName));
        Preconditions.checkNotNull(parameterTypes);
        Preconditions.checkNotNull(parameters);
        Preconditions.checkArgument(parameterTypes.length == parameters.length);

        this.requestId = requestId;
        this.className = className;
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
        this.parameters = parameters;
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
        else if (!(obj instanceof RpcRequest)) {
            return false;
        }

        return requestId.equals(((RpcRequest)obj).getRequestId());
    }
}
