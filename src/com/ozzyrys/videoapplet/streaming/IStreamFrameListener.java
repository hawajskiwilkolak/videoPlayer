package com.ozzyrys.videoapplet.streaming;

import java.awt.image.BufferedImage;

public interface IStreamFrameListener
{
    public void onVideoFrameReceived(BufferedImage image);
    public void onAudioFrameReceived(byte[] audio);
}
