package com.ozzyrys.videoapplet.streaming.client;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;

public class StreamClientInitializer extends ChannelInitializer<SocketChannel>
{
    protected final IStreamClientListener streamClientListener;

    public StreamClientInitializer(IStreamClientListener streamClientListener)
    {
        super();
        this.streamClientListener = streamClientListener;
    }

    @Override
    public void initChannel(SocketChannel ch) throws Exception
    {
        ChannelPipeline pipeline = ch.pipeline();

//        pipeline.addLast("inflater", ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP));
        
        pipeline.addLast("decoder", new ObjectDecoder(ClassResolvers.cacheDisabled(null)));
        
        pipeline.addLast("handler", new StreamClientHandler(streamClientListener));
    }
}