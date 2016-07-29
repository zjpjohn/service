package com.xien.rpc.common.serialize;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Create on 16/7/27.
 */
public class RpcEncoder extends MessageToByteEncoder<Object> {
    private final Class<?> cls;

    public RpcEncoder(Class<?> cls) {
        Preconditions.checkNotNull(cls);
        this.cls = cls;
    }

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Object t, ByteBuf byteBuf) throws Exception {
        if (cls.isAssignableFrom(t.getClass())) {
            byte[] data = SerializationUtil.getInstance().serialize(t);
            byteBuf.writeInt(data.length);
            byteBuf.writeBytes(data);
        }
    }
}
