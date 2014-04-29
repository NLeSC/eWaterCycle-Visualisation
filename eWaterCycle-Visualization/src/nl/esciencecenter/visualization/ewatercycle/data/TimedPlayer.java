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

    private int numberOfFramesPassedBetweenTimesteps = 0;

    public TimedPlayer(CustomJSlider timeBar2, JFormattedTextField frameCounter) {
        this.timeBar = timeBar2;
        this.frameCounter = frameCounter;
        this.inputHandler = WaterCycleInputHandler.getInstance();
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
            // setScreenshotFileName(frameNumber + ".png");

            if ((currentState == states.PLAYING) || (currentState == states.REDRAWING)
                    || (currentState == states.MOVIEMAKING) || (currentState == states.REVIEW)) {
                try {
                    // if (!isScreenshotNeeded()) {
                    startTime = System.currentTimeMillis();

                    if (currentState == states.MOVIEMAKING || currentState == states.REVIEW) {
                        if (!isScreenshotNeeded()
                                && numberOfFramesPassedBetweenTimesteps != settings.getNumberOfScreenshotsPerTimeStep()) {
                            int movieFrameNumber = (frameNumber * settings.getNumberOfScreenshotsPerTimeStep())
                                    + numberOfFramesPassedBetweenTimesteps;
                            for (Orientation o : orientationList) {
                                if (o.getFrameNumber() == movieFrameNumber) {
                                    Float3Vector rotation = new Float3Vector(o.getRotation().getX(), o.getRotation()
                                            .getY(), 0f);
                                    float viewDist = o.getViewDist();
                                    inputHandler.setRotation(rotation);
                                    inputHandler.setViewDist(viewDist);

                                }
                            }
                            if (currentState == states.MOVIEMAKING) {
                                String ssFileName = String.format("%06d.png", movieFrameNumber);
                                System.out.println("Writing screenshot: " + ssFileName);
                                // setScreenshotFileName(ssFileName);
                                // setScreenshotNeeded(true);
                                makeScreenShot(ssFileName);
                            }
                        }

                        numberOfFramesPassedBetweenTimesteps++;
                    }

                    // Forward frame
                    if (currentState != states.REDRAWING) {
                        int newFrameNumber;
                        try {
                            if (numberOfFramesPassedBetweenTimesteps == settings.getNumberOfScreenshotsPerTimeStep()) {
                                if (effTexStorage.doneWithLastRequest()) {
                                    logger.debug("Done with last request");
                                    newFrameNumber = dsManager.getNextFrameNumber(frameNumber);

                                    updateFrame(newFrameNumber, false);
                                } else {
                                    logger.debug("Not done with last request");
                                }
                                numberOfFramesPassedBetweenTimesteps = 0;
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
                for (int i = 0; i < settings.getNumberOfScreenshotsPerTimeStep(); i++) {
                    intermediateFrameNumbers.add((currentFrameNumber * settings.getNumberOfScreenshotsPerTimeStep())
                            + i);
                }
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

                int startFrameNumber = currentKeyFrame.getFrameNumber() * settings.getNumberOfScreenshotsPerTimeStep();
                int stopFrameNumber = nextKeyFrame.getFrameNumber() * settings.getNumberOfScreenshotsPerTimeStep();
                int numberOfInterpolationFrames = 0;
                for (int currentFrameNumber : intermediateFrameNumbers) {
                    if (currentFrameNumber >= startFrameNumber && currentFrameNumber < stopFrameNumber) {
                        numberOfInterpolationFrames++;
                    }
                }

                Float3Vector startLocation = new Float3Vector(currentKeyFrame.getRotation().getX(), currentKeyFrame
                        .getRotation().getY(), currentKeyFrame.getViewDist());
                Float3Vector endLocation = new Float3Vector(nextKeyFrame.getRotation().getX(), nextKeyFrame
                        .getRotation().getY(), nextKeyFrame.getViewDist());

                if (startLocation.getX() - endLocation.getX() < 0f) {
                    startLocation.setX(startLocation.getX() + 360f);
                }

                if (startLocation.getY() - endLocation.getY() < 0f) {
                    startLocation.setY(startLocation.getY() + 360f);
                }

                Float3Vector still = new Float3Vector();

                Float3Vector[] curveSteps = FloatVectorMath.degreesBezierCurve(numberOfInterpolationFrames,
                        startLocation, still, still, endLocation);

                // Patch for zoom
                // Float4Vector startZoom = new
                // Float4Vector(currentKeyFrame.getViewDist(), 0f, 0f, 1f);
                // Float4Vector endZoom = new
                // Float4Vector(nextKeyFrame.getViewDist(), 0f, 0f, 1f);
                //
                // Float4Vector[] zoomSteps =
                // FloatVectorMath.bezierCurve(numberOfInterpolationFrames,
                // startZoom,
                // still, endControl, endZoom);

                System.out.println("O : "
                        + new Float4Vector(currentKeyFrame.getRotation(), currentKeyFrame.getViewDist()));

                for (int j = 0; j < numberOfInterpolationFrames; j++) {
                    int currentFrameNumber = intermediateFrameNumbers.get(currentKeyFrame.getFrameNumber() + j);
                    Orientation newOrientation = new Orientation(currentFrameNumber, curveSteps[j],
                            curveSteps[j].getZ());
                    orientationList.add(newOrientation);

                    System.out.println("S+" + j + ": " + new Float4Vector(curveSteps[j], curveSteps[j].getX()));
                }

                System.out.println("D : " + new Float4Vector(nextKeyFrame.getRotation(), nextKeyFrame.getViewDist()));

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
