package com.ozzyrys.videoapplet.streaming.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

public interface IStreamClientListener
{
    public void onChannelActive(Channel channel);

    public void onChannelInactive(ChannelHandlerContext ctx);
    
    public void onChannelRead(Channel channel, Object msg);

    public void onException(Channel channel, Throwable t);
}
