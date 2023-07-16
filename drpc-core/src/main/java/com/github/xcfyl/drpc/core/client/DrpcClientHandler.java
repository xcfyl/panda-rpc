package com.github.xcfyl.drpc.core.client;

import com.github.xcfyl.drpc.core.protocol.DrpcResponse;
import com.github.xcfyl.drpc.core.protocol.DrpcTransferProtocol;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;


/**
 * rpc客户端处理器
 *
 * @author 西城风雨楼
 * @date create at 2023/6/22 10:30
 */
public class DrpcClientHandler extends ChannelInboundHandlerAdapter {
    private final DrpcClientContext rpcClientContext;

    public DrpcClientHandler(DrpcClientContext rpcClientContext) {
        this.rpcClientContext = rpcClientContext;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        DrpcTransferProtocol protocol = (DrpcTransferProtocol) msg;
        DrpcResponse response = rpcClientContext.getSerializer().deserialize(protocol.getBody(), DrpcResponse.class);
        rpcClientContext.getResponseCache().put(response.getId(), response);
        ReferenceCountUtil.release(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Channel channel = ctx.channel();
        if(channel.isActive()){
            ctx.close();
        }
    }
}