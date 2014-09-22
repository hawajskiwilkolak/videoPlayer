package com.ozzyrys.videoapplet.streaming.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.logging.Level;
import java.util.logging.Logger;

public class StreamClientHandler extends SimpleChannelInboundHandler<Object>
{
    private static final Logger logger = Logger.getLogger(StreamClientHandler.class.getName());

    protected final IStreamClientListener streamClientListener;

    public StreamClientHandler(IStreamClientListener streamClientListener)
    {
        super();
        this.streamClientListener = streamClientListener;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx)
    {
        streamClientListener.onChannelActive(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception
    {
        streamClientListener.onChannelInactive(ctx);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, final Object msg)
    {
        streamClientListener.onChannelRead(ctx.channel(), msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
    {
//        logger.log(Level.WARNING, "Unexpected exception from downstream.", cause);
        streamClientListener.onException(ctx.channel(), cause);
        ctx.close();
    }
}