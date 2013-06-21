package nl.esciencecenter.visualization.ewatercycle;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import nl.esciencecenter.esight.ESightNewtWindow;
import nl.esciencecenter.esight.input.InputHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WaterCycleApp {
    private final static WaterCycleSettings settings = WaterCycleSettings.getInstance();
    private final static Logger logger = LoggerFactory.getLogger(WaterCycleApp.class);

    private static JFrame frame;
    private static WaterCyclePanel imauPanel;
    private static WaterCycleWindow imauWindow;

    public static void main(String[] arguments) {
        String cmdlnfileName = null;
        String cmdlnfileName2 = null;
        String path = "";

        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i].equals("-o")) {
                i++;
                cmdlnfileName = arguments[i];
                final File cmdlnfile = new File(cmdlnfileName);
                path = cmdlnfile.getPath().substring(0, cmdlnfile.getPath().length() - cmdlnfile.getName().length());

            } else if (arguments[i].equals("-o2")) {
                i++;
                cmdlnfileName2 = arguments[i];
            } else if (arguments[i].equals("-resume")) {
                i++;
                WaterCycleApp.settings.setInitial_simulation_frame(Integer.parseInt(arguments[i]));
                i++;
                WaterCycleApp.settings.setInitial_rotation_x(Float.parseFloat(arguments[i]));
                i++;
                WaterCycleApp.settings.setInitial_rotation_y(Float.parseFloat(arguments[i]));
            } else {
                cmdlnfileName = null;
                path = System.getProperty("user.dir");
            }
        }
        logger.debug("PATH:" + path);

        // Create the Swing interface elements
        imauPanel = new WaterCyclePanel(path, cmdlnfileName, cmdlnfileName2);

        // Create the GLEventListener
        imauWindow = new WaterCycleWindow(InputHandler.getInstance());

        new ESightNewtWindow(true, imauWindow.getInputHandler(), imauWindow, settings.getDefaultScreenWidth(),
                settings.getDefaultScreenHeight(), "eWaterCycle Visualization");

        // Create the frame
        final JFrame frame = new JFrame("eWater");
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent arg0) {
                System.exit(0);
            }
        });

        frame.setAlwaysOnTop(true);

        frame.setSize(WaterCycleApp.settings.getInterfaceWidth(), WaterCycleApp.settings.getInterfaceHeight());
        frame.setMinimumSize(new Dimension(200, 600));

        frame.setResizable(true);

        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    frame.getContentPane().add(imauPanel);
                } catch (final Exception e) {
                    e.printStackTrace(System.err);
                    System.exit(1);
                }
            }
        });

        frame.setVisible(true);
    }

    public static BufferedImage getFrameImage() {
        Component component = frame.getContentPane();
        BufferedImage image = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_RGB);

        // call the Component's paint method, using
        // the Graphics object of the image.
        component.paint(image.getGraphics());

        return image;
    }

    public static Dimension getFrameSize() {
        return frame.getContentPane().getSize();
    }

    public static Point getCanvaslocation() {
        return imauPanel.getCanvasLocation();
    }

    public static void feedMouseEventToPanel(int x, int y) {
        Point p = new Point(x, y);
        SwingUtilities.convertPointFromScreen(p, frame.getContentPane());

        System.out.println("x " + x + " y " + y);
        System.out.println("p.x " + p.x + " p.y " + p.y);

        if ((p.x > 0 && p.x < frame.getWidth()) && (p.y > 0 && p.y < frame.getHeight())) {
            Component comp = SwingUtilities.getDeepestComponentAt(frame.getContentPane(), p.x, p.y);

            System.out.println(comp.toString());

            Toolkit.getDefaultToolkit().getSystemEventQueue()
                    .postEvent(new MouseEvent(comp, MouseEvent.MOUSE_PRESSED, 0, 0, p.x, p.y, 1, false));
            Toolkit.getDefaultToolkit().getSystemEventQueue()
                    .postEvent(new MouseEvent(comp, MouseEvent.MOUSE_RELEASED, 0, 0, p.x, p.y, 1, false));
            Toolkit.getDefaultToolkit().getSystemEventQueue()
                    .postEvent(new MouseEvent(comp, MouseEvent.MOUSE_CLICKED, 0, 0, p.x, p.y, 1, false));
        }
    }
}