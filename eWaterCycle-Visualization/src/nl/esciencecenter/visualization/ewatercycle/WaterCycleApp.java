package nl.esciencecenter.visualization.ewatercycle;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import nl.esciencecenter.neon.NeonNewtWindow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WaterCycleApp {
    private final static WaterCycleSettings settings = WaterCycleSettings.getInstance();
    private final static Logger logger = LoggerFactory.getLogger(WaterCycleApp.class);

    private static JFrame frame;
    private static WaterCyclePanel panel;
    private static WaterCycleWindow imauWindow;

    public static void main(String[] arguments) {
        // Create the Swing interface elements
        panel = new WaterCyclePanel();

        // Create the GLEventListener
        imauWindow = new WaterCycleWindow(WaterCycleInputHandler.getInstance());

        new NeonNewtWindow(true, imauWindow.getInputHandler(), imauWindow, settings.getDefaultScreenWidth(),
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
                    frame.getContentPane().add(panel);
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
        return panel.getCanvasLocation();
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
