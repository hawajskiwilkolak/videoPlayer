package com.ozzyrys.videoapplet.streaming;

import com.xuggle.ferry.IBuffer;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IRational;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 *
 * @author ozzyrys
 */
public class AVMessage implements Serializable
{
    public static enum Type
    {
        AUDIO,
        VIDEO
    }

    private final IPacketSerializable packet;
    private final Type type;

    public AVMessage(IPacket packet, Type type)
    {
        this.packet = new IPacketSerializable(packet);
        this.type = type;
    }

    public boolean isVideo()
    {
        return (this.type == Type.VIDEO);
    }

    public boolean isAudio()
    {
        return (this.type == Type.AUDIO);
    }

    public IPacket getPacket()
    {
        return packet.getIPacket();
    }

    public class IPacketSerializable implements Serializable
    {
        private final byte[] data;
        private final int size, flags, timeBaseDenominator, timeBaseNumerator, streamIndex;
        private final long dts, pts, duration, timestamp, position;

        public IPacketSerializable(IPacket packet)
        {
            this.dts = packet.getDts();
            this.pts = packet.getPts();
            this.size = packet.getSize();
            this.duration = packet.getDuration();
            this.flags = packet.getFlags();
            this.timeBaseDenominator = packet.getTimeBase().getDenominator();
            this.timeBaseNumerator = packet.getTimeBase().getNumerator();
            this.timestamp = packet.getTimeStamp();
            this.position = packet.getPosition();
            this.streamIndex = packet.getStreamIndex();

            data = packet.getData().getByteArray(0, size);
        }

        public IPacket getIPacket()
        {
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(size);
            byteBuffer.put(data, 0, size).order(ByteOrder.BIG_ENDIAN);
            IBuffer iBuffer = IBuffer.make(null, data, 0, size);
            IPacket iPacket = IPacket.make(iBuffer);
            iPacket.getByteBuffer().put(byteBuffer);
            iPacket.setDts(dts);
            iPacket.setPts(pts);
            iPacket.setDuration(duration);
            iPacket.setTimeBase(IRational.make(timeBaseNumerator, timeBaseDenominator));
            iPacket.setFlags(flags);
            iPacket.setTimeStamp(timestamp);
            iPacket.setPosition(position);
            iPacket.setStreamIndex(streamIndex);

            return iPacket;
        }
    }
}
