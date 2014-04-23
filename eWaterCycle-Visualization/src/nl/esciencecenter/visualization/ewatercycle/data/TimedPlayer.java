package nl.esciencecenter.visualization.ewatercycle.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JFormattedTextField;
import javax.swing.JSlider;

import nl.esciencecenter.neon.math.Float3Vector;
import nl.esciencecenter.neon.math.Float4Vector;
import nl.esciencecenter.neon.math.FloatVectorMath;
import nl.esciencecenter.neon.swing.CustomJSlider;
import nl.esciencecenter.visualization.ewatercycle.WaterCycleInputHandler;
import nl.esciencecenter.visualization.ewatercycle.WaterCyclePanel.KeyFrame;
import nl.esciencecenter.visualization.ewatercycle.WaterCycleSettings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimedPlayer implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(TimedPlayer.class);

    public static enum states {
        UNOPENED, UNINITIALIZED, INITIALIZED, STOPPED, REDRAWING, SNAPSHOTTING, MOVIEMAKING, CLEANUP, WAITINGONFRAME, PLAYING, REVIEW
    }

    private class Orientation {
        private final int frameNumber;
        private final Float3Vector rotation;
        private final float viewDist;

        public Orientation(int frameNumber, Float3Vector rotation, float viewDist) {
            this.frameNumber = frameNumber;
            this.rotation = rotation;
            this.viewDist = viewDist;
        }

        public int getFrameNumber() {
            return frameNumber;
        }

        public Float3Vector getRotation() {
            return rotation;
        }

        public float getViewDist() {
            return viewDist;
        }

        @Override
        public String toString() {
            return "#: " + frameNumber + " " + rotation + " " + viewDist;
        }

        @Override
        public int hashCode() {
            int hash = 1;
            hash = hash * 17 + frameNumber;
            return hash;
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof KeyFrame && ((KeyFrame) other).hashCode() == this.hashCode()) {
                return true;
            } else {
                return false;
            }
        }
    }

    private ArrayList<Orientation> orientationList;

    private final WaterCycleSettings settings = WaterCycleSettings.getInstance();

    private states currentState = states.UNOPENED;
    private int frameNumber;

    private final boolean running = true;
    private boolean initialized = false;

    private long startTime, stopTime;

    private final JSlider timeBar;
    private final JFormattedTextField frameCounter;

    private final WaterCycleInputHandler inputHandler;

    private boolean needsScreenshot = false;
    private final String screenshotDirectory = "";
    private String screenshotFilename = "";

    private DatasetManager dsManager;
    private EfficientTextureStorage effTexStorage;

    private final long waittime = settings.getWaittimeMovie();

    private final ArrayList<Float3Vector> bezierPoints, fixedPoints;
    private final ArrayList<Integer> bezierSteps;

    public TimedPlayer(CustomJSlider timeBar2, JFormattedTextField frameCounter) {
        this.timeBar = timeBar2;
        this.frameCounter = frameCounter;
        this.inputHandler = WaterCycleInputHandler.getInstance();

        bezierPoints = new ArrayList<Float3Vector>();

        fixedPoints = new ArrayList<Float3Vector>();
        bezierSteps = new ArrayList<Integer>();

        fixedPoints.add(new Float3Vector(394, 624, -80)); // Europa
        bezierSteps.add(60);
        fixedPoints.add(new Float3Vector(379, 702, -140)); // Gulf Stream
        bezierSteps.add(60);
        fixedPoints.add(new Float3Vector(359, 651, -140)); // Equator
        bezierSteps.add(60);
        fixedPoints.add(new Float3Vector(320, 599, -90)); // South Africa
        bezierSteps.add(60);
        fixedPoints.add(new Float3Vector(339, 540, -140)); // India
        bezierSteps.add(60);
        fixedPoints.add(new Float3Vector(382, 487, -80)); // Japan
        bezierSteps.add(60);
        fixedPoints.add(new Float3Vector(353, 360, -110)); // Panama
        bezierSteps.add(60);
        fixedPoints.add(new Float3Vector(311, 326, -110)); // Argentina
        bezierSteps.add(60);
        fixedPoints.add(new Float3Vector(412, 302, -140)); // Greenland
        bezierSteps.add(60);
        fixedPoints.add(new Float3Vector(394, 264, -80)); // Europa

        Float3Vector lastPoint = fixedPoints.get(0);
        Float3Vector still = new Float3Vector(0, 0, 0);
        for (int i = 1; i < fixedPoints.size(); i++) {
            Float3Vector newPoint = fixedPoints.get(i);

            Float3Vector[] bezierPointsTemp = FloatVectorMath.degreesBezierCurve(bezierSteps.get(i - 1), lastPoint,
                    still, still, newPoint);

            for (int j = 1; j < bezierPointsTemp.length; j++) {
                bezierPoints.add(bezierPointsTemp[j]);
            }

            lastPoint = newPoint;
        }
    }

    public void close() {
        initialized = false;
        frameNumber = 0;
        timeBar.setValue(0);
        frameCounter.setValue(0);
        timeBar.setMaximum(0);
    }

    public void init(File[] files) {
        this.dsManager = new DatasetManager(files);
        this.effTexStorage = dsManager.getEfficientTextureStorage();

        frameNumber = dsManager.getFrameNumberOfIndex(0);
        final int initialMaxBar = dsManager.getNumFrames() - 1;

        timeBar.setMaximum(initialMaxBar);
        timeBar.setMinimum(0);

        initialized = true;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public synchronized boolean isPlaying() {
        if ((currentState == states.PLAYING) || (currentState == states.MOVIEMAKING)) {
            return true;
        }

        return false;
    }

    public synchronized void oneBack() {
        stop();

        try {
            int newFrameNumber = dsManager.getPreviousFrameNumber(frameNumber);
            updateFrame(newFrameNumber, false);
        } catch (IOException e) {
            logger.debug("One back failed.");
        }
    }

    public synchronized void oneForward() {
        stop();

        try {
            int newFrameNumber = dsManager.getNextFrameNumber(frameNumber);
            updateFrame(newFrameNumber, false);
        } catch (IOException e) {
            logger.debug("One forward failed.");
        }
    }

    public synchronized void redraw() {
        if (initialized) {
            updateFrame(frameNumber, true);
            currentState = states.REDRAWING;
        }
    }

    public synchronized void rewind() {
        stop();
        int newFrameNumber = dsManager.getFrameNumberOfIndex(0);
        updateFrame(newFrameNumber, false);
    }

    public synchronized void setScreenshotNeeded(boolean value) {
        needsScreenshot = value;
        notifyAll();
    }

    public synchronized boolean isScreenshotNeeded() {
        return needsScreenshot;
    }

    public synchronized void setScreenshotFileName(String screenshotFilename) {
        this.screenshotFilename = settings.getScreenshotPath() + screenshotFilename;
    }

    public synchronized String getScreenshotFileName() {
        return screenshotFilename;
    }

    public synchronized void makeScreenShot(String screenshotFilename) {
        this.screenshotFilename = settings.getScreenshotPath() + screenshotFilename;
        this.needsScreenshot = true;

        while (this.needsScreenshot) {
            try {
                wait();
            } catch (InterruptedException e) {
                // IGNORE
            }
        }
    }

    @Override
    public synchronized void run() {
        if (!initialized) {
            System.err.println("HDFTimer started while not initialized.");
            System.exit(1);
        }

        inputHandler.setRotation(new Float3Vector(settings.getInitialRotationX(), settings.getInitialRotationY(), 0f));
        inputHandler.setViewDist(settings.getInitialZoom());

        int frame = settings.getInitialSimulationFrame();
        updateFrame(frame, true);

        stop();

        while (running) {
            setScreenshotFileName(frameNumber + ".png");

            if ((currentState == states.PLAYING) || (currentState == states.REDRAWING)
                    || (currentState == states.MOVIEMAKING) || (currentState == states.REVIEW)) {
                try {
                    // if (!isScreenshotNeeded()) {
                    startTime = System.currentTimeMillis();

                    if (currentState == states.MOVIEMAKING || currentState == states.REVIEW) {
                        for (Orientation o : orientationList) {
                            if (o.getFrameNumber() == frameNumber) {
                                Float3Vector rotation = new Float3Vector(o.getRotation());
                                float viewDist = o.getViewDist();
                                inputHandler.setRotation(rotation);
                                inputHandler.setViewDist(viewDist);

                            }
                            if (currentState == states.MOVIEMAKING) {
                                setScreenshotFileName(frameNumber + ".png");
                                setScreenshotNeeded(true);
                            }
                        }
                    }

                    // Forward frame
                    if (currentState != states.REDRAWING) {
                        int newFrameNumber;
                        try {
                            if (effTexStorage.doneWithLastRequest()) {
                                logger.debug("Done with last request");
                                newFrameNumber = dsManager.getNextFrameNumber(frameNumber);

                                updateFrame(newFrameNumber, false);
                            } else {
                                logger.debug("Not done with last request");
                            }
                        } catch (IOException e) {
                            currentState = states.WAITINGONFRAME;
                            stop();
                            logger.debug("nextFrame returned IOException.");
                        }
                    }

                    // Wait for the _rest_ of the timeframe
                    stopTime = System.currentTimeMillis();
                    long spentTime = stopTime - startTime;

                    if (spentTime < waittime) {
                        wait(waittime - spentTime);
                    }
                    // }
                } catch (final InterruptedException e) {
                    System.err.println("Interrupted while playing.");
                }
            } else if (currentState == states.STOPPED) {
                try {
                    wait(100);
                } catch (final InterruptedException e) {
                    System.err.println("Interrupted while stopped.");
                }
            } else if (currentState == states.REDRAWING) {
                currentState = states.STOPPED;
            } else if (currentState == states.WAITINGONFRAME) {
                try {
                    wait(10);
                } catch (final InterruptedException e) {
                    System.err.println("Interrupted while waiting.");
                }
            }
        }
    }

    public synchronized void setFrame(int value, boolean overrideUpdate) {
        stop();

        updateFrame(dsManager.getFrameNumberOfIndex(value), overrideUpdate);
    }

    public synchronized void start() {
        currentState = states.PLAYING;
    }

    public synchronized void stop() {
        currentState = states.STOPPED;
    }

    private synchronized void updateFrame(int newFrameNumber, boolean overrideUpdate) {
        if (dsManager != null) {
            if (newFrameNumber != frameNumber || overrideUpdate) {
                if (!settings.isRequestedNewConfiguration()) {
                    frameNumber = newFrameNumber;
                    settings.setFrameNumber(newFrameNumber);
                    this.timeBar.setValue(dsManager.getIndexOfFrameNumber(newFrameNumber));
                    this.frameCounter.setValue(dsManager.getIndexOfFrameNumber(newFrameNumber));

                    settings.setRequestedNewConfiguration(true);
                }
            }
        }
    }

    // public TextureStorage getTextureStorage() {
    // return texStorage;
    // }

    public EfficientTextureStorage getEfficientTextureStorage() {
        return effTexStorage;
    }

    public ArrayList<String> getVariables() {
        return dsManager.getVariables();
    }

    public String getVariableUnits(String varName) {
        return dsManager.getVariableUnits(varName);
    }

    public float getMinValueContainedInDataset(String varName) {
        return dsManager.getMinValueContainedInDataset(varName);
    }

    public float getMaxValueContainedInDataset(String varName) {
        return dsManager.getMaxValueContainedInDataset(varName);
    }

    public int getImageWidth() {
        return dsManager.getImageWidth();
    }

    public int getImageHeight() {
        return dsManager.getImageHeight();
    }

    public synchronized void startSequence(ArrayList<KeyFrame> keyFrames, boolean record) {
        int startKeyFrameNumber = 0, finalKeyFrameNumber = 0;
        for (KeyFrame keyFrame : keyFrames) {
            int frameNumber = keyFrame.getFrameNumber();
            if (frameNumber > finalKeyFrameNumber) {
                finalKeyFrameNumber = frameNumber;
            }
            if (frameNumber < startKeyFrameNumber) {
                startKeyFrameNumber = frameNumber;
            }
        }

        orientationList = new ArrayList<Orientation>();

        ArrayList<Integer> intermediateFrameNumbers = new ArrayList<Integer>();

        try {
            int currentFrameNumber = startKeyFrameNumber;
            while (currentFrameNumber <= finalKeyFrameNumber) {
                intermediateFrameNumbers.add(currentFrameNumber);
                currentFrameNumber = dsManager.getNextFrameNumber(currentFrameNumber);
            }
        } catch (IOException e) {
            // We're done.
        }

        // Only do interpolation step if we have a current AND a next keyFrame
        // available.
        if (keyFrames.size() > 1) {
            for (int i = 0; i < keyFrames.size() - 1; i++) {
                KeyFrame currentKeyFrame = keyFrames.get(i);
                KeyFrame nextKeyFrame = keyFrames.get(i + 1);

                int startFrameNumber = currentKeyFrame.getFrameNumber();
                int stopFrameNumber = nextKeyFrame.getFrameNumber();
                int numberOfInterpolationFrames = 0;
                for (int currentFrameNumber : intermediateFrameNumbers) {
                    if (currentFrameNumber >= startFrameNumber && currentFrameNumber < stopFrameNumber) {
                        numberOfInterpolationFrames++;
                    }
                }

                Float4Vector startLocation = new Float4Vector(currentKeyFrame.getRotation(), 1f);
                Float4Vector endLocation = new Float4Vector(nextKeyFrame.getRotation(), 1f);

                Float3Vector startControl = new Float3Vector();
                Float3Vector endControl = new Float3Vector();

                Float4Vector[] curveSteps = FloatVectorMath.bezierCurve(numberOfInterpolationFrames, startLocation,
                        startControl, endControl, endLocation);

                // Patch for zoom
                startLocation = new Float4Vector(currentKeyFrame.getViewDist(), 0f, 0f, 1f);
                endLocation = new Float4Vector(nextKeyFrame.getViewDist(), 0f, 0f, 1f);

                Float4Vector[] zoomSteps = FloatVectorMath.bezierCurve(numberOfInterpolationFrames, startLocation,
                        startControl, endControl, endLocation);

                for (int j = 0; j < numberOfInterpolationFrames; j++) {
                    int currentFrameNumber = intermediateFrameNumbers.get(currentKeyFrame.getFrameNumber() + j);
                    Orientation newOrientation = new Orientation(currentFrameNumber, curveSteps[j].stripAlpha(),
                            zoomSteps[j].getX());
                    orientationList.add(newOrientation);
                }
            }
        }

        stop();
        setFrame(startKeyFrameNumber, true);

        if (record) {
            movieMode();
        } else {
            reviewMode();
        }
    }

    public synchronized void movieMode() {
        currentState = states.MOVIEMAKING;
    }

    public synchronized void reviewMode() {
        currentState = states.REVIEW;
    }
}
