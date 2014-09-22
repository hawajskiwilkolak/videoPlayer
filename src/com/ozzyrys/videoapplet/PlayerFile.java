package com.ozzyrys.videoapplet;

import com.ozzyrys.videoapplet.streaming.IStreamFrameListener;
import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.MediaListenerAdapter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.mediatool.event.IAudioSamplesEvent;
import com.xuggle.mediatool.event.IVideoPictureEvent;
import java.awt.image.BufferedImage;

/**
 *
 * @author ozzyrys
 */
public class PlayerFile extends MediaListenerAdapter
{
    protected final IStreamFrameListener streamFrameListener;
    long firstTimestampInStream, systemClockStartTime;

    public PlayerFile(String filename, IStreamFrameListener streamFrameListener)
    {
        IMediaReader reader = ToolFactory.makeReader(filename);

        reader.setBufferedImageTypeToGenerate(BufferedImage.TYPE_3BYTE_BGR);

        reader.addListener(this);
        this.streamFrameListener = streamFrameListener;

        while (reader.readPacket() == null)
        {
        }
    }

    @Override
    public void onVideoPicture(IVideoPictureEvent event)
    {
        System.out.println("video time: "+event.getTimeStamp());
        BufferedImage frame = event.getImage();
        if (frame != null)
        {
            streamFrameListener.onVideoFrameReceived(frame);

            if (firstTimestampInStream == 0)
            {
                firstTimestampInStream = event.getTimeStamp();
                systemClockStartTime = System.currentTimeMillis();
            }
            else
            {
                long systemClockCurrentTime = System.currentTimeMillis();
                long millisecondsClockTimeSinceStartofVideo = systemClockCurrentTime - systemClockStartTime;

                long millisecondsStreamTimeSinceStartOfVideo = (event.getTimeStamp() - firstTimestampInStream) / 1000;
                final long millisecondsTolerance = 50; // and we give ourselfs 50 ms of tolerance
                final long millisecondsToSleep = (millisecondsStreamTimeSinceStartOfVideo - (millisecondsClockTimeSinceStartofVideo + millisecondsTolerance));
                if (millisecondsToSleep > 0)
                {
                    try
                    {
                        Thread.sleep(millisecondsToSleep);
                    }
                    catch (InterruptedException e)
                    {
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void onAudioSamples(IAudioSamplesEvent event)
    {
        System.out.println("audio time: "+event.getTimeStamp());
        streamFrameListener.onAudioFrameReceived(event.getAudioSamples().getData().getByteArray(0, event.getAudioSamples().getSize()));
        //line.write(event.getAudioSamples().getData().getByteArray(0, event.getAudioSamples().getSize()), 0, event.getAudioSamples().getSize());
    }
}
