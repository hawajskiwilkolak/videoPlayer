package com.ozzyrys.videoapplet;

import com.ozzyrys.videoapplet.streaming.AVMessage;
import com.ozzyrys.videoapplet.streaming.IStreamFrameListener;
import com.ozzyrys.videoapplet.streaming.client.IStreamClientListener;
import com.ozzyrys.videoapplet.streaming.client.StreamClientInitializer;
import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IMetaData;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IRational;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.net.SocketAddress;
import java.util.logging.Level;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Player
{
    protected final static Logger logger = LoggerFactory.getLogger(Player.class);
    protected final Bootstrap clientBootstrap;
    protected final IStreamFrameListener streamFrameListener;
    protected Channel clientChannel;

    SourceDataLine line; //audio output
    IContainer container; //media container
    IStreamCoder videoCoder; //video coder
    IStreamCoder audioCoder; //audio coder

    //video properties
    IRational fps = IRational.make(15, 1); //15FPS
    Dimension resolution = new Dimension(640, 480); //640x480
    int videoBitRate = 250000; // 250kbps
    int videoBitRateTolerance = 50000;

    //audio properties
    int audioChannels = 2; //stereo
    int audioSampleRate = 22050; //input sample rate - 22050 Hz, could be 16000 perhaps?
    int audioSampleSize = 16; //input bit depth - 16bit
    int audioBitRate = 64000; //compressed audio bitrate (== quality) - 64kbps
    AudioFormat audioFormat;

    boolean wantAudio = false;
    boolean isRecording = false;
    boolean isStreaming = false;

    public Player(IStreamFrameListener streamFrameListener)
    {
        clientBootstrap = new Bootstrap();
        clientBootstrap.group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .handler(new StreamClientInitializer(new StreamClientListener()));

        this.streamFrameListener = streamFrameListener;

        initVideoCoder();
        initAudioCoder();

        // create the audio line out
        try
        {
            line = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, audioFormat));
            // open the line and connect the line
            line.open(audioFormat);
            line.start();
        }
        catch (LineUnavailableException ex)
        {
            java.util.logging.Logger.getLogger(Player.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void initVideoCoder()
    {
        //video coder
        videoCoder = IStreamCoder.make(IStreamCoder.Direction.DECODING, ICodec.findDecodingCodec(ICodec.ID.CODEC_ID_H264));

        videoCoder.setNumPicturesInGroupOfPictures(15);
        videoCoder.setBitRate(videoBitRate);
        videoCoder.setBitRateTolerance(videoBitRateTolerance);
        videoCoder.setPixelType(IPixelFormat.Type.YUV420P);
        videoCoder.setHeight(resolution.height);
        videoCoder.setWidth(resolution.width);
        videoCoder.setFlag(IStreamCoder.Flags.FLAG_QSCALE, true);
        videoCoder.setGlobalQuality(0);
        videoCoder.setFrameRate(fps);
        videoCoder.setTimeBase(IRational.make(1, 15));

        IMetaData codecOptions = IMetaData.make();
        codecOptions.setValue("tune", "zerolatency");// equals -tune zerolatency in ffmpeg
        if (videoCoder.open(codecOptions, null) < 0)
        {
            throw new RuntimeException("could not open video coder");
        }
    }

    private void initAudioCoder()
    {
        //audio coder
        audioCoder = IStreamCoder.make(IStreamCoder.Direction.DECODING, ICodec.findDecodingCodec(ICodec.ID.CODEC_ID_MP3));
        audioCoder.setBitRate(audioBitRate);
        audioCoder.setSampleRate(audioSampleRate);
        audioCoder.setSampleFormat(IAudioSamples.Format.FMT_S16);
        audioCoder.setChannels(audioChannels);
        audioCoder.setDefaultAudioFrameSize(audioSampleSize);

        if (audioCoder.open(null, null) < 0)
        {
            throw new RuntimeException("could not open audio coder");
        }

        audioFormat = new AudioFormat(audioSampleRate, audioSampleSize, audioChannels, true, false);
    }

    public void connect(SocketAddress streamServerAddress)
    {
        try
        {
            System.out.println("going to connect to stream server: " + streamServerAddress);
            logger.info("going to connect to stream server :{}", streamServerAddress);

            clientBootstrap.connect(streamServerAddress).sync();
            isStreaming = true;
        }
        catch (InterruptedException ex)
        {
            java.util.logging.Logger.getLogger(Player.class.getName()).log(Level.SEVERE, null, ex);
        }
        catch (Exception e)
        {
            System.out.println("Connection unsuccessful");
        }
    }

    public void disconnect()
    {
        if (clientChannel != null)
        {
            try
            {
                clientChannel.disconnect().sync();
                isStreaming = false;
            }
            catch (InterruptedException ex)
            {
                java.util.logging.Logger.getLogger(Player.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void createContainerFile(String filename)
    {
        //open container and add streams
        container = IContainer.make();
        if (container.open(filename, IContainer.Type.WRITE, null) < 0)
        {
            throw new RuntimeException("could not create container");
        }

        if (videoCoder == null || !videoCoder.isOpen())
        {
            initVideoCoder();
        }
        
        if(audioCoder == null || !audioCoder.isOpen())
        {
            initAudioCoder();
        }
        
        container.addNewStream(videoCoder);
        container.addNewStream(audioCoder);
    }

    public void startRecording(String filename)
    {
        if (container == null && filename == null)
        {
            return;
        }

        createContainerFile(filename);
        if (container.writeHeader() < 0)
        {
            System.out.println("broken header");
        }
        else
        {
            System.out.println("header written");
        }

        isRecording = true;

        System.out.println("Recording started to file: " + filename);
        logger.info("Recording started to file: {0}", filename);
    }

    public void stopRecording()
    {
        if (isRecording)
        {
            isRecording = false;
            if (container.writeTrailer() < 0)
            {
                System.out.println("broken trailer");
            }
            else
            {
                System.out.println("trailer written");
            }
            for (int i = 0; i < container.getNumStreams(); i++)
            {
                container.getStream(i).getStreamCoder().close();
            }
            container.close();

            initVideoCoder();
            initAudioCoder();
        }
    }

    public void enableAudio()
    {
        wantAudio = true;
    }

    public void disableAudio()
    {
        wantAudio = false;
    }

    public void toggleAudio()
    {
        wantAudio = !wantAudio;
    }

    public SourceDataLine getLine()
    {
        return line;
    }

    protected class StreamClientListener implements IStreamClientListener
    {
        @Override
        public void onChannelActive(Channel channel)
        {
            System.out.println("stream connected to server at: " + channel);
            logger.info("stream connected to server at :{}", channel);
            clientChannel = channel;
        }

        @Override
        public void onChannelInactive(ChannelHandlerContext ctx)
        {
            System.out.println("channel inactive");
            streamFrameListener.onVideoFrameReceived(null);
        }

        @Override
        public void onException(Channel channel, Throwable t)
        {
            System.out.println("exception at :" + channel + ",exception :" + t);
            logger.debug("exception at :{},exception :{}", channel, t);
        }

        @Override
        public void onChannelRead(Channel channel, Object msg)
        {
            AVMessage packet = (AVMessage) msg;
            if (packet.isAudio() && wantAudio && audioCoder.isOpen())
            {
                IPacket aPacket = packet.getPacket();

                IAudioSamples samples = IAudioSamples.make(1024, 2);

                int offset = 0;

                while (offset < aPacket.getSize())
                {
                    int bytesDecoded = audioCoder.decodeAudio(samples, aPacket, offset);
                    if (bytesDecoded < 0)
                    {
                        System.out.println("error decoding audio");
                        break;
                    }
                    offset += bytesDecoded;

                    if (samples.isComplete())
                    {
                        streamFrameListener.onAudioFrameReceived(samples.getData().getByteArray(0, samples.getSize()));
                    }
                }
            }
            else if (packet.isVideo() && videoCoder.isOpen())
            {
                IPacket vPacket = packet.getPacket();

                IVideoPicture picture = IVideoPicture.make(IPixelFormat.Type.YUV420P, resolution.width, resolution.height);

                int offset = 0;
                while (offset < vPacket.getSize())
                {
                    int bytesDecoded = videoCoder.decodeVideo(picture, vPacket, offset);
                    if (bytesDecoded < 0)
                    {
                        System.out.println("got error decoding video");
                        break;
                    }
                    offset += bytesDecoded;

                    if (picture.isComplete())
                    {
                        IConverter converter = ConverterFactory.createConverter(new BufferedImage(videoCoder.getWidth(), videoCoder.getHeight(), BufferedImage.TYPE_3BYTE_BGR), IPixelFormat.Type.YUV420P);//ConverterFactory.createConverter("pic2img", picture);

                        // process the video frame
                        streamFrameListener.onVideoFrameReceived(converter.toImage(picture));
                        converter.delete();
                        if (isRecording)
                        {
                            vPacket.setStreamIndex(0);
                            container.writePacket(vPacket);
                        }
                    }
                }
            }
        }
    }
}
