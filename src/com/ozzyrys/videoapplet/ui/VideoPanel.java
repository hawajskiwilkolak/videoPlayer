package com.ozzyrys.videoapplet.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JPanel;

public class VideoPanel extends JPanel
{
    private static final long serialVersionUID = -7292145875292244144L;

    protected BufferedImage image;
    protected final ExecutorService worker = Executors.newSingleThreadExecutor();
    private long lastFrameTime = 0;

    @Override
    protected void paintComponent(Graphics g)
    {
        Graphics2D g2 = (Graphics2D) g;
        if (image != null && (System.currentTimeMillis() - lastFrameTime < 2000))
        {
            g2.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);
        }
        else if (System.currentTimeMillis() - lastFrameTime > 2000)
        {
            BufferedImage img = getDisableImage("Brak obrazu");
            g2.drawImage(img, 0, 0, img.getWidth(), img.getHeight(), null);
        }
        else
        {
            BufferedImage img = getDisableImage("Nawiązywanie połączenia..");
            g2.drawImage(img, 0, 0, img.getWidth(), img.getHeight(), null);
        }
    }

    public BufferedImage getDisableImage(String text)
    {
        BufferedImage img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D) img.getGraphics();

        g.clearRect(0, 0, getWidth(), getHeight());
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setBackground(Color.BLACK);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());

        int cx = (getWidth() - 70) / 2;
        int cy = (getHeight() - 40) / 2;

        g.setStroke(new BasicStroke(2));
        g.setColor(Color.LIGHT_GRAY);
        g.fillRoundRect(cx, cy, 70, 40, 10, 10);
        g.setColor(Color.WHITE);
        g.fillOval(cx + 5, cy + 5, 30, 30);
        g.setColor(Color.LIGHT_GRAY);
        g.fillOval(cx + 10, cy + 10, 20, 20);
        g.setColor(Color.WHITE);
        g.fillOval(cx + 12, cy + 12, 16, 16);
        g.fillRoundRect(cx + 50, cy + 5, 15, 10, 5, 5);
        g.fillRect(cx + 63, cy + 25, 7, 2);
        g.fillRect(cx + 63, cy + 28, 7, 2);
        g.fillRect(cx + 63, cy + 31, 7, 2);

        FontMetrics metrics = g.getFontMetrics(getFont());
        int w = metrics.stringWidth(text);
        int h = metrics.getHeight();

        g.setColor(Color.LIGHT_GRAY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.drawString(text, (getWidth() - w) / 2, cy - h);

        return img;
    }

    public void updateImage(final BufferedImage update)
    {
        worker.execute(new Runnable()
        {
            @Override
            public void run()
            {
                image = update;
                lastFrameTime = System.currentTimeMillis();
                repaint();
            }
        });
    }

    public void close()
    {
        worker.shutdown();
    }
}
