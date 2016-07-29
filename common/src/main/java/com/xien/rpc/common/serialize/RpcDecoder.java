package com.xien.rpc.common.serialize;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * Create on 16/7/27.
 */
public class RpcDecoder extends ByteToMessageDecoder {
    private final Class<?> cls;

    public RpcDecoder(Class<?> cls) {
        Preconditions.checkNotNull(cls);
        this.cls = cls;
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        while (true) {
            if (byteBuf.readableBytes() < 4) {
                break;
            }

            byteBuf.markReaderIndex();
            int dateLen = byteBuf.readInt();
            if (dateLen < 0) {
                channelHandlerContext.close();
                return;
            }

            if (byteBuf.readableBytes() < dateLen) {
                byteBuf.resetReaderIndex();
                break;
            }

            byte[] data = new byte[dateLen];
            byteBuf.readBytes(data);

            Object obj = SerializationUtil.getInstance().deserialize(data, cls);
            list.add(obj);
        }
    }
}
