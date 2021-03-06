package nl.esciencecenter.visualization.ewatercycle.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

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

    private HashMap<Integer, Orientation> orientationMap;

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
                                && numberOfFramesPassedBetweenTimesteps < settings.getNumberOfScreenshotsPerTimeStep()) {
                            int movieFrameNumber = (frameNumber * settings.getNumberOfScreenshotsPerTimeStep())
                                    + numberOfFramesPassedBetweenTimesteps;
                            // Orientation o =
                            // orientationMap.get(movieFrameNumber);

                            Float3Vector rotation = new Float3Vector(inputHandler.getRotation().getX(), inputHandler
                                    .getRotation().getY() + 0.1f, 0f);

                            // float viewDist = o.getViewDist();

                            // System.out.println("Rot: " + movieFrameNumber +
                            // " = " + rotation);
                            // System.out.println("Dst: " + movieFrameNumber +
                            // " = " + viewDist);

                            inputHandler.setRotation(rotation);
                            // inputHandler.setViewDist(viewDist);

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
                            if ((currentState != states.MOVIEMAKING && currentState != states.REVIEW)
                                    || numberOfFramesPassedBetweenTimesteps == settings
                                            .getNumberOfScreenshotsPerTimeStep()) {
                                if (effTexStorage.doneWithLastRequest()) {
                                    logger.debug("Done with last request");
                                    newFrameNumber = dsManager.getNextFrameNumber(frameNumber);

                                    updateFrame(newFrameNumber, false);
                                } else {
                                    Thread.sleep(10);
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

        orientationMap = new HashMap<Integer, Orientation>();

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
                        .getRotation().getY(), 0f);
                Float3Vector endLocation = new Float3Vector(nextKeyFrame.getRotation().getX(), nextKeyFrame
                        .getRotation().getY(), 0f);

                startLocation.setX(startLocation.getX() % 360f);
                startLocation.setY(startLocation.getY() % 360f);

                endLocation.setX(endLocation.getX() % 360f);
                endLocation.setY(endLocation.getY() % 360f);

                Float3Vector still = new Float3Vector();

                Float3Vector[] curveSteps = degreesBezierCurve(numberOfInterpolationFrames, startLocation, endLocation);

                // Patch for zoom
                Float4Vector startZoom = new Float4Vector(currentKeyFrame.getViewDist(), 0f, 0f, 1f);
                Float4Vector endZoom = new Float4Vector(nextKeyFrame.getViewDist(), 0f, 0f, 1f);

                Float4Vector[] zoomSteps = FloatVectorMath.bezierCurve(numberOfInterpolationFrames, startZoom, still,
                        still, endZoom);

                for (int j = 0; j < numberOfInterpolationFrames; j++) {
                    int currentMovieFrameNumber = intermediateFrameNumbers.get(currentKeyFrame.getFrameNumber()
                            * settings.getNumberOfScreenshotsPerTimeStep() + j);
                    Orientation newOrientation = new Orientation(currentMovieFrameNumber, curveSteps[j],
                            zoomSteps[j].getX());
                    orientationMap.put(currentMovieFrameNumber, newOrientation);
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

    /**
     * Bezier curve interpolation for _rotation_ between two points with control
     * vectors (this could be particle speed at the points). Outputs a number of
     * degrees for rotations.
     * 
     * @param steps
     *            The number of steps on the bezier curve to calculate.
     * @param startLocation
     *            The starting point for this bezier curve.
     * @param startControl
     *            The starting point's control vector.
     * @param endControl
     *            The end point for this bezier curve.
     * @param endLocation
     *            The end point's control vector.
     * @return The array of points on the new bezier curve.
     */
    public static Float3Vector[] degreesBezierCurve(int steps, Float3Vector startLocation, Float3Vector endLocation) {
        Float3Vector[] newBezierPoints = new Float3Vector[steps];
        for (int i = 0; i < steps; i++) {
            newBezierPoints[i] = new Float3Vector();
        }

        float t = 1f / steps;
        float temp = t * t;

        for (int coord = 0; coord < 3; coord++) {
            float p[] = new float[4];
            if (coord == 0) {
                p[0] = startLocation.getX();
                p[1] = startLocation.getX();
                p[2] = endLocation.getX();
                p[3] = endLocation.getX();
            } else if (coord == 1) {
                p[0] = startLocation.getY();
                p[1] = startLocation.getY();
                p[2] = endLocation.getY();
                p[3] = endLocation.getY();
            } else if (coord == 2) {
                p[0] = startLocation.getZ();
                p[1] = startLocation.getZ();
                p[2] = endLocation.getZ();
                p[3] = endLocation.getZ();
            }

            // p[0] = p[0] % 360f;
            // p[1] = p[1] % 360f;
            // p[2] = p[2] % 360f;
            // p[3] = p[3] % 360f;

            // if (p[0] - p[3] < 0f) {
            // p[0] = p[0] + 360f;
            // p[1] = p[0];
            // } else if (p[0] + p[3] > 360f) {
            // p[0] = p[0] - 360f;
            // p[1] = p[0];
            // }

            // p[0] = p[0] % 360f;
            // p[1] = p[1] % 360f;

            // The algorithm itself begins here ==
            float f, fd, fdd, fddd, fdd_per_2, fddd_per_2, fddd_per_6; // NOSONAR

            // I've tried to optimize the amount of
            // multiplications here, but these are exactly
            // the same formulas that were derived earlier
            // for f(0), f'(0)*t etc.
            f = p[0];
            fd = 3 * (p[1] - p[0]) * t;
            fdd_per_2 = 3 * (p[0] - 2 * p[1] + p[2]) * temp;
            fddd_per_2 = 3 * (3 * (p[1] - p[2]) + p[3] - p[0]) * temp * t;

            fddd = fddd_per_2 + fddd_per_2;
            fdd = fdd_per_2 + fdd_per_2;
            fddd_per_6 = fddd_per_2 * (1f / 3);

            for (int loop = 0; loop < steps; loop++) {
                if (coord == 0) {
                    newBezierPoints[loop].setX(f % 360f);
                } else if (coord == 1) {
                    newBezierPoints[loop].setY(f);
                } else if (coord == 2) {
                    newBezierPoints[loop].setZ(f);
                }

                f = f + fd + fdd_per_2 + fddd_per_6;
                fd = fd + fdd + fddd_per_2;
                fdd = fdd + fddd;
                fdd_per_2 = fdd_per_2 + fddd_per_2;
            }
        }

        return newBezierPoints;
    }

    public synchronized void movieMode() {
        currentState = states.MOVIEMAKING;
    }

    public synchronized void reviewMode() {
        currentState = states.REVIEW;
    }
}
