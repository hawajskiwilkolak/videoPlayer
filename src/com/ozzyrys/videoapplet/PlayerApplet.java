package com.ozzyrys.videoapplet;

import com.ozzyrys.videoapplet.streaming.IStreamFrameListener;
import com.ozzyrys.videoapplet.ui.VideoPanel;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JApplet;
import javax.swing.SwingUtilities;
import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Klasa appletu ktora posredniczy miedzy przegladarka a wlasciwym programem.
 *
 * @author ozzyrys
 */
public class PlayerApplet extends JApplet
{
    protected final static Logger logger = LoggerFactory.getLogger(PlayerApplet.class);

    private static Player player;
    private PlayerFile filePlayer;
    protected static VideoPanel videoPanel;
    private static SourceDataLine line;

    public static final Dimension DEFAULT_RESOLUTION = new Dimension(640, 480);
    protected static Dimension resolution;

    private static HashMap<String, String> vars;

    private class Variables
    {
        public static final String HOST = "host";
        public static final String PORT = "port";
        public static final String RECORDING = "recording";
        public static final String STREAMING = "streaming";
        public static final String AUDIO = "audio";
    }

    @Override
    public void init()
    {
        super.init();
        resolution = new Dimension(this.getWidth(), this.getHeight());
        try
        {
            SwingUtilities.invokeAndWait(new Runnable()
            {
                @Override
                public void run()
                {
                    videoPanel = new VideoPanel();
                    videoPanel.setPreferredSize(resolution);
                    add(videoPanel);
                    setPreferredSize(resolution);
                    setVisible(true);
                }
            });
        }
        catch (InterruptedException | InvocationTargetException e)
        {
            logger.error("createGUI didn't complete successfully");
        }

        vars = new HashMap<>();
        vars.put(Variables.HOST, "");
        vars.put(Variables.PORT, "");
        vars.put(Variables.STREAMING, "false");
        vars.put(Variables.RECORDING, "false");
        vars.put(Variables.AUDIO, "false");
    }

    @Override
    public void start()
    {
        String host = this.getParameter("host");
        String port = this.getParameter("port");
        String filepath = this.getParameter("filepath");
        String mode = this.getParameter("mode");

        if (mode != null && mode.equals("file") && filepath != null)
        {
            connectToFile(filepath);
        }
        else
        {
            player = new Player(new PlayerApplet.StreamFrameListener());
            line = player.getLine();

            if (filepath != null && !filepath.equals(""))
            {
                startRecording(filepath);
            }
        }
//        host = "127.0.0.1";
//        port = "8169";
        if (host != null && port != null && !host.trim().equals("") && !port.equals(""))
        {
            connectToHost(host.trim(), Integer.parseInt(port));
            vars.put(Variables.HOST, host.trim());
            vars.put(Variables.PORT, port);
        }
        //connectToFile("D:\\server.flv");

        ScheduledExecutorService timer = new ScheduledThreadPoolExecutor(0);
        timer.scheduleAtFixedRate(new Runnable()
        {
            @Override
            public void run()
            {
                videoPanel.repaint();
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop()
    {
        if (player == null)
        {
            return;
        }
        player.disconnect();
        player.stopRecording();
    }

    /**
     * Nawiazywanie polaczenia ze wskazanym serwerem streamingu.
     *
     * @param host
     * @param port
     */
    public void connectToHost(String host, int port)
    {
        if (player == null || vars == null)
        {
            return;
        }
        player.connect(new InetSocketAddress(host.trim(), port));
        vars.put(Variables.STREAMING, "true");
        vars.put(Variables.HOST, host);
        vars.put(Variables.PORT, port + "");
    }

    private void connectToFile(String filepath)
    {
        filePlayer = new PlayerFile(filepath, new PlayerApplet.StreamFrameListener());
    }

    /**
     * Odlaczenie od serwera streamingu.
     */
    public void disconnect()
    {
        if (player == null || vars == null)
        {
            return;
        }
        player.disconnect();
        vars.put(Variables.STREAMING, "false");
    }

    /**
     * Ponowne polaczenie sie z serwerem streamingu z ktorym polaczenie zostalo
     * zerwane ale ktorego host i port nie ulegly zmianie.
     */
    public void reconnect()
    {
        reconnect(vars.get(Variables.HOST), Integer.parseInt(vars.get(Variables.PORT)));
    }

    /**
     * Ponowne polaczenie sie ze wskazanym serwerem streamingu. Jezeli istnialo
     * wczesniej polaczenie to zostanie ono zerwane.
     *
     * @param host
     * @param port
     */
    public void reconnect(String host, int port)
    {
        //if (vars.get(Variables.STREAMING).equals("true"))
        //{
            disconnect();
        //}
        connectToHost(host, port);
    }

    /**
     * Rozpoczyna nagrywanie do wskazanego pliku.
     *
     * @param filename - pelna sciezka absolutna pliku, wraz z rozszerzeniem, do
     * ktorego bedzie nagrywane video.
     */
    public void startRecording(final String filename)
    {
        if (player == null || vars == null)
        {
            return;
        }
        PrivilegedAction action = new PrivilegedAction()
        {
            String path = filename;

            @Override
            public Object run()
            {
                if (new File(path).exists())
                {
                    path = path.substring(0, path.lastIndexOf(".")) + "_" + new SimpleDateFormat("HHmmss").format(Calendar.getInstance().getTime()) + path.substring(path.lastIndexOf("."));
                }
                else
                {
                    new File(path.substring(0, path.lastIndexOf("\\"))).mkdirs();
                }
                player.startRecording(path);
                vars.put(Variables.RECORDING, "true");
                return null;
            }
        };

        AccessController.doPrivileged(action);
    }

    /**
     * Zatrzymuje nagrywanie do pliku.
     */
    public void stopRecording()
    {
        if (player == null || vars == null)
        {
            return;
        }
        player.stopRecording();
        vars.put(Variables.RECORDING, "false");
    }

    /**
     * Wlacza odtwarzanie dzwieku ze streamu.
     */
    public void enableAudio()
    {
        if (player == null || vars == null)
        {
            return;
        }
        player.enableAudio();
        vars.put(Variables.AUDIO, "true");
    }

    /**
     * Wylacza odtwarzanie dzwieku ze streamu.
     */
    public void disableAudio()
    {
        if (player == null || vars == null)
        {
            return;
        }
        player.disableAudio();
        vars.put(Variables.AUDIO, "false");
    }

     /**
     * Przelacza (wlacza lub wylacza) odtwarzanie dzwieku ze streamu.
     */
    public void toggleAudio()
    {
        if (player == null || vars == null)
        {
            return;
        }
        player.toggleAudio();
        vars.put(Variables.AUDIO, (player.wantAudio) ? "true" : "false");
    }

    /**
     * Zmienia rozdzielczosc apletu. Obraz video jest pomniejszany lub
     * rozciagany tak aby pasowal do wielkosci apletu.
     *
     * @param width
     * @param height
     */
    public void resizeApplet(int width, int height)
    {
        resolution = new Dimension(width, height);
        this.setSize(resolution);
    }

    /**
     * Generuje i zwraca status appletu z formacie JSON.
     * @return JSON ze statusem appletu.
     */
    public String status()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        for (String k : vars.keySet())
        {
            sb.append("\"")
                    .append(k)
                    .append("\":\"")
                    .append(vars.get(k))
                    .append("\",");
        }
        sb.deleteCharAt(sb.lastIndexOf(","));
        sb.append("}");

        return sb.toString();
    }

    protected static class StreamFrameListener implements IStreamFrameListener
    {
        @Override
        public void onVideoFrameReceived(BufferedImage image)
        {
            if (image != null)
            {
                if (resolution.width != image.getWidth() || resolution.height != image.getHeight())
                {
                    image = Scalr.resize(image, Scalr.Method.SPEED, Scalr.Mode.FIT_TO_WIDTH, resolution.width, resolution.height, Scalr.OP_ANTIALIAS);
                }
            }
            else
            {
                image = videoPanel.getDisableImage("Brak połączenia!");
                vars.put(Variables.STREAMING, "false");
            }
            videoPanel.updateImage(image);
        }

        @Override
        public void onAudioFrameReceived(byte[] audioBuffer)
        {
            if (line != null)
            {
                line.write(audioBuffer, 0, audioBuffer.length);
                if (!player.wantAudio || !player.isStreaming)
                {
                    line.flush();
                }
            }
        }
    }
}
