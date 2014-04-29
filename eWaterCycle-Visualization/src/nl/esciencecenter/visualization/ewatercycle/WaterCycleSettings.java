package nl.esciencecenter.visualization.ewatercycle;

import java.util.ArrayList;
import java.util.HashMap;

import nl.esciencecenter.neon.swing.ColormapInterpreter;
import nl.esciencecenter.neon.util.TypedProperties;
import nl.esciencecenter.visualization.ewatercycle.data.SurfaceTextureDescription;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WaterCycleSettings {
    private final Logger logger = LoggerFactory.getLogger(WaterCycleSettings.class);

    private static class SingletonHolder {
        public final static WaterCycleSettings instance = new WaterCycleSettings();
    }

    public static WaterCycleSettings getInstance() {
        return SingletonHolder.instance;
    }

    public enum GlobeMode {
        FIRST_DATASET, SECOND_DATASET, DIFF
    };

    public enum Months {
        Jan, Feb, Mar, Apr, May, Jun, Jul, Aug, Sep, Oct, Nov, Dec
    };

    private boolean STEREO_RENDERING = true;
    private boolean STEREO_SWITCHED = true;

    private float STEREO_OCULAR_DISTANCE_MIN = 0f;
    private float STEREO_OCULAR_DISTANCE_DEF = .2f;
    private float STEREO_OCULAR_DISTANCE_MAX = 1f;

    // Size settings for default startup and screenshots
    private int DEFAULT_SCREEN_WIDTH = 1024;
    private int DEFAULT_SCREEN_HEIGHT = 768;

    private int INTERFACE_HEIGHT = 88;
    private int INTERFACE_WIDTH = 210;

    private final int SCREENSHOT_SCREEN_WIDTH = 1280;
    private final int SCREENSHOT_SCREEN_HEIGHT = 720;

    // Settings for the initial view
    private int INITIAL_SIMULATION_FRAME = 0;
    private float INITIAL_ROTATION_X = 17f;
    private float INITIAL_ROTATION_Y = -25f;
    private float INITIAL_ZOOM = -390.0f;

    // Setting per movie frame
    private boolean MOVIE_ROTATE = true;
    private float MOVIE_ROTATION_SPEED_MIN = -1f;
    private float MOVIE_ROTATION_SPEED_MAX = 1f;
    private float MOVIE_ROTATION_SPEED_DEF = -0.25f;

    // Settings for the gas cloud octree
    private int MAX_OCTREE_DEPTH = 25;
    private float OCTREE_EDGES = 800f;

    // Settings that should never change, but are listed here to make sure they
    // can be found if necessary
    private int MAX_EXPECTED_MODELS = 1000;

    protected String SCREENSHOT_PATH = System.getProperty("user.dir") + "/";

    private long WAITTIME_FOR_RETRY = 10000;
    private long WAITTIME_FOR_MOVIE = 1000;
    private int TIME_STEP_SIZE = 1;
    private float EPSILON = 1.0E-7f;

    private int FILE_EXTENSION_LENGTH = 2;
    private int FILE_NUMBER_LENGTH = 4;

    private final String[] ACCEPTABLE_POSTFIXES = { ".nc" };

    private String CURRENT_POSTFIX = "nc";

    private int PREPROCESSING_AMOUNT = 2;

    private final HashMap<String, Float> minValues;
    private final HashMap<String, Float> diffMinValues;
    private final HashMap<String, Float> maxValues;
    private final HashMap<String, Float> diffMaxValues;
    private final HashMap<String, Float> currentMinValues;
    private final HashMap<String, Float> currentDiffMinValues;
    private final HashMap<String, Float> currentMaxValues;
    private final HashMap<String, Float> currentDiffMaxValues;
    private final HashMap<String, String> currentColormap;
    private final HashMap<String, Boolean> logarithmicScale;

    private int DEPTH_MIN = 0;
    private int DEPTH_DEF = 0;
    private int DEPTH_MAX = 41;

    private int WINDOW_SELECTION = 0;

    private boolean IMAGE_STREAM_OUTPUT = false;
    private final int SAGE_FRAMES_PER_SECOND = 10;
    private boolean IMAGE_STREAM_GL_ONLY = true;

    private float HEIGHT_DISTORION = 0f;
    private final float HEIGHT_DISTORION_MIN = 0f;
    private final float HEIGHT_DISTORION_MAX = .01f;

    private String SAGE_DIRECTORY = "/home/maarten/sage-code/sage";

    private final boolean TOUCH_CONNECTED = false;

    private SurfaceTextureDescription[] screenDescriptions;

    private final String grid_width_dimension_substring = "lon";
    private final String grid_height_dimension_substring = "lat";

    private int number_of_screens_col = 2;
    private int number_of_screens_row = 2;

    private CacheFileManager cacheFileManager;
    private boolean requestedNewConfiguration;

    private int numberOfScreenshotsPerTimeStep = 6;

    private WaterCycleSettings() {
        super();
        minValues = new HashMap<String, Float>();
        maxValues = new HashMap<String, Float>();
        currentMinValues = new HashMap<String, Float>();
        currentMaxValues = new HashMap<String, Float>();
        diffMinValues = new HashMap<String, Float>();
        diffMaxValues = new HashMap<String, Float>();
        currentDiffMinValues = new HashMap<String, Float>();
        currentDiffMaxValues = new HashMap<String, Float>();
        currentColormap = new HashMap<String, String>();
        logarithmicScale = new HashMap<String, Boolean>();

        try {
            final TypedProperties props = new TypedProperties();
            props.loadFromClassPath("settings.properties");

            STEREO_RENDERING = props.getBooleanProperty("STEREO_RENDERING");
            STEREO_SWITCHED = props.getBooleanProperty("STEREO_SWITCHED");

            STEREO_OCULAR_DISTANCE_MIN = props.getFloatProperty("STEREO_OCULAR_DISTANCE_MIN");
            STEREO_OCULAR_DISTANCE_MAX = props.getFloatProperty("STEREO_OCULAR_DISTANCE_MAX");
            STEREO_OCULAR_DISTANCE_DEF = props.getFloatProperty("STEREO_OCULAR_DISTANCE_DEF");

            // Size settings for default startup and screenshots
            DEFAULT_SCREEN_WIDTH = props.getIntProperty("DEFAULT_SCREEN_WIDTH");
            DEFAULT_SCREEN_HEIGHT = props.getIntProperty("DEFAULT_SCREEN_HEIGHT");
            INTERFACE_WIDTH = props.getIntProperty("INTERFACE_WIDTH");
            INTERFACE_HEIGHT = props.getIntProperty("INTERFACE_HEIGHT");

            // SCREENSHOT_SCREEN_WIDTH = props
            // .getIntProperty("SCREENSHOT_SCREEN_WIDTH");
            // SCREENSHOT_SCREEN_HEIGHT = props
            // .getIntProperty("SCREENSHOT_SCREEN_HEIGHT");

            // Settings for the initial view
            INITIAL_SIMULATION_FRAME = props.getIntProperty("INITIAL_SIMULATION_FRAME");
            INITIAL_ROTATION_X = props.getFloatProperty("INITIAL_ROTATION_X");
            INITIAL_ROTATION_Y = props.getFloatProperty("INITIAL_ROTATION_Y");
            INITIAL_ZOOM = props.getFloatProperty("INITIAL_ZOOM");
            TIME_STEP_SIZE = props.getIntProperty("TIME_STEP_SIZE");

            // Setting per movie frame
            MOVIE_ROTATE = props.getBooleanProperty("MOVIE_ROTATE");
            MOVIE_ROTATION_SPEED_MIN = props.getFloatProperty("MOVIE_ROTATION_SPEED_MIN");
            MOVIE_ROTATION_SPEED_MAX = props.getFloatProperty("MOVIE_ROTATION_SPEED_MAX");
            MOVIE_ROTATION_SPEED_DEF = props.getFloatProperty("MOVIE_ROTATION_SPEED_DEF");

            // Settings for the gas cloud octree
            MAX_OCTREE_DEPTH = props.getIntProperty("MAX_OCTREE_DEPTH");
            OCTREE_EDGES = props.getFloatProperty("OCTREE_EDGES");

            // Settings that should never change, but are listed here to make
            // sure
            // they
            // can be found if necessary
            MAX_EXPECTED_MODELS = props.getIntProperty("MAX_EXPECTED_MODELS");

            // SCREENSHOT_PATH = props.getProperty("SCREENSHOT_PATH");

            WAITTIME_FOR_RETRY = props.getLongProperty("WAITTIME_FOR_RETRY");
            WAITTIME_FOR_MOVIE = props.getLongProperty("WAITTIME_FOR_MOVIE");

            System.out.println(IMAGE_STREAM_OUTPUT ? "true" : "false");

            setIMAGE_STREAM_OUTPUT(props.getBooleanProperty("IMAGE_STREAM_OUTPUT"));

            System.out.println(IMAGE_STREAM_OUTPUT ? "true" : "false");

            // eWaterCycle
            minValues.put("discharge", props.getFloatProperty("MIN_discharge"));
            maxValues.put("discharge", props.getFloatProperty("MAX_discharge"));
            currentMinValues.put("discharge", props.getFloatProperty("SET_MIN_discharge"));
            currentMaxValues.put("discharge", props.getFloatProperty("SET_MAX_discharge"));
            diffMinValues.put("discharge", props.getFloatProperty("DIFF_MIN_discharge"));
            diffMaxValues.put("discharge", props.getFloatProperty("DIFF_MAX_discharge"));
            currentDiffMinValues.put("discharge", props.getFloatProperty("SET_DIFF_MIN_discharge"));
            currentDiffMaxValues.put("discharge", props.getFloatProperty("SET_DIFF_MAX_discharge"));
            currentColormap.put("discharge", "realistic");

            minValues.put("snowCoverSWE", props.getFloatProperty("MIN_snowCoverSWE"));
            maxValues.put("snowCoverSWE", props.getFloatProperty("MAX_snowCoverSWE"));
            currentMinValues.put("snowCoverSWE", props.getFloatProperty("SET_MIN_snowCoverSWE"));
            currentMaxValues.put("snowCoverSWE", props.getFloatProperty("SET_MAX_snowCoverSWE"));
            diffMinValues.put("snowCoverSWE", props.getFloatProperty("DIFF_MIN_snowCoverSWE"));
            diffMaxValues.put("snowCoverSWE", props.getFloatProperty("DIFF_MAX_snowCoverSWE"));
            currentDiffMinValues.put("snowCoverSWE", props.getFloatProperty("SET_DIFF_MIN_snowCoverSWE"));
            currentDiffMaxValues.put("snowCoverSWE", props.getFloatProperty("SET_DIFF_MAX_snowCoverSWE"));
            currentColormap.put("snowCoverSWE", "snow");

            minValues.put("storUpp000005", props.getFloatProperty("MIN_storUpp000005"));
            maxValues.put("storUpp000005", props.getFloatProperty("MAX_storUpp000005"));
            currentMinValues.put("storUpp000005", props.getFloatProperty("SET_MIN_storUpp000005"));
            currentMaxValues.put("storUpp000005", props.getFloatProperty("SET_MAX_storUpp000005"));
            diffMinValues.put("storUpp000005", props.getFloatProperty("DIFF_MIN_storUpp000005"));
            diffMaxValues.put("storUpp000005", props.getFloatProperty("DIFF_MAX_storUpp000005"));
            currentDiffMinValues.put("storUpp000005", props.getFloatProperty("SET_DIFF_MIN_storUpp000005"));
            currentDiffMaxValues.put("storUpp000005", props.getFloatProperty("SET_DIFF_MAX_storUpp000005"));
            currentColormap.put("storUpp000005", "moisture");

            minValues.put("storUpp005030", props.getFloatProperty("MIN_storUpp005030"));
            maxValues.put("storUpp005030", props.getFloatProperty("MAX_storUpp005030"));
            currentMinValues.put("storUpp005030", props.getFloatProperty("SET_MIN_storUpp005030"));
            currentMaxValues.put("storUpp005030", props.getFloatProperty("SET_MAX_storUpp005030"));
            diffMinValues.put("storUpp005030", props.getFloatProperty("DIFF_MIN_storUpp005030"));
            diffMaxValues.put("storUpp005030", props.getFloatProperty("DIFF_MAX_storUpp005030"));
            currentDiffMinValues.put("storUpp005030", props.getFloatProperty("SET_DIFF_MIN_storUpp005030"));
            currentDiffMaxValues.put("storUpp005030", props.getFloatProperty("SET_DIFF_MAX_storUpp005030"));
            currentColormap.put("storUpp005030", "moisture");

            minValues.put("storLow030150", props.getFloatProperty("MIN_storLow030150"));
            maxValues.put("storLow030150", props.getFloatProperty("MAX_storLow030150"));
            currentMinValues.put("storLow030150", props.getFloatProperty("SET_MIN_storLow030150"));
            currentMaxValues.put("storLow030150", props.getFloatProperty("SET_MAX_storLow030150"));
            diffMinValues.put("storLow030150", props.getFloatProperty("DIFF_MIN_storLow030150"));
            diffMaxValues.put("storLow030150", props.getFloatProperty("DIFF_MAX_storLow030150"));
            currentDiffMinValues.put("storLow030150", props.getFloatProperty("SET_DIFF_MIN_storLow030150"));
            currentDiffMaxValues.put("storLow030150", props.getFloatProperty("SET_DIFF_MAX_storLow030150"));
            currentColormap.put("storLow030150", "moisture");

            minValues.put("satDegUpp000005", props.getFloatProperty("MIN_satDegUpp000005"));
            maxValues.put("satDegUpp000005", props.getFloatProperty("MAX_satDegUpp000005"));
            currentMinValues.put("satDegUpp000005", props.getFloatProperty("SET_MIN_satDegUpp000005"));
            currentMaxValues.put("satDegUpp000005", props.getFloatProperty("SET_MAX_satDegUpp000005"));
            diffMinValues.put("satDegUpp000005", props.getFloatProperty("DIFF_MIN_satDegUpp000005"));
            diffMaxValues.put("satDegUpp000005", props.getFloatProperty("DIFF_MAX_satDegUpp000005"));
            currentDiffMinValues.put("satDegUpp000005", props.getFloatProperty("SET_DIFF_MIN_satDegUpp000005"));
            currentDiffMaxValues.put("satDegUpp000005", props.getFloatProperty("SET_DIFF_MAX_satDegUpp000005"));
            currentColormap.put("satDegUpp000005", "moisture");

            minValues.put("satDegUpp005030", props.getFloatProperty("MIN_satDegUpp005030"));
            maxValues.put("satDegUpp005030", props.getFloatProperty("MAX_satDegUpp005030"));
            currentMinValues.put("satDegUpp005030", props.getFloatProperty("SET_MIN_satDegUpp005030"));
            currentMaxValues.put("satDegUpp005030", props.getFloatProperty("SET_MAX_satDegUpp005030"));
            diffMinValues.put("satDegUpp005030", props.getFloatProperty("DIFF_MIN_satDegUpp005030"));
            diffMaxValues.put("satDegUpp005030", props.getFloatProperty("DIFF_MAX_satDegUpp005030"));
            currentDiffMinValues.put("satDegUpp005030", props.getFloatProperty("SET_DIFF_MIN_satDegUpp005030"));
            currentDiffMaxValues.put("satDegUpp005030", props.getFloatProperty("SET_DIFF_MAX_satDegUpp005030"));
            currentColormap.put("satDegUpp005030", "moisture");

            minValues.put("satDegLow030150", props.getFloatProperty("MIN_satDegLow030150"));
            maxValues.put("satDegLow030150", props.getFloatProperty("MAX_satDegLow030150"));
            currentMinValues.put("satDegLow030150", props.getFloatProperty("SET_MIN_satDegLow030150"));
            currentMaxValues.put("satDegLow030150", props.getFloatProperty("SET_MAX_satDegLow030150"));
            diffMinValues.put("satDegLow030150", props.getFloatProperty("DIFF_MIN_satDegLow030150"));
            diffMaxValues.put("satDegLow030150", props.getFloatProperty("DIFF_MAX_satDegLow030150"));
            currentDiffMinValues.put("satDegLow030150", props.getFloatProperty("SET_DIFF_MIN_satDegLow030150"));
            currentDiffMaxValues.put("satDegLow030150", props.getFloatProperty("SET_DIFF_MAX_satDegLow030150"));
            currentColormap.put("satDegLow030150", "moisture");

            minValues.put("precipitation", props.getFloatProperty("MIN_precipitation"));
            maxValues.put("precipitation", props.getFloatProperty("MAX_precipitation"));
            currentMinValues.put("precipitation", props.getFloatProperty("SET_MIN_precipitation"));
            currentMaxValues.put("precipitation", props.getFloatProperty("SET_MAX_precipitation"));
            diffMinValues.put("precipitation", props.getFloatProperty("DIFF_MIN_precipitation"));
            diffMaxValues.put("precipitation", props.getFloatProperty("DIFF_MAX_precipitation"));
            currentDiffMinValues.put("precipitation", props.getFloatProperty("SET_DIFF_MIN_precipitation"));
            currentDiffMaxValues.put("precipitation", props.getFloatProperty("SET_DIFF_MAX_precipitation"));
            currentColormap.put("precipitation", "hotres");

            minValues.put("temperature", props.getFloatProperty("MIN_temperature"));
            maxValues.put("temperature", props.getFloatProperty("MAX_temperature"));
            currentMinValues.put("temperature", props.getFloatProperty("SET_MIN_temperature"));
            currentMaxValues.put("temperature", props.getFloatProperty("SET_MAX_temperature"));
            diffMinValues.put("temperature", props.getFloatProperty("DIFF_MIN_temperature"));
            diffMaxValues.put("temperature", props.getFloatProperty("DIFF_MAX_temperature"));
            currentDiffMinValues.put("temperature", props.getFloatProperty("SET_DIFF_MIN_temperature"));
            currentDiffMaxValues.put("temperature", props.getFloatProperty("SET_DIFF_MAX_temperature"));
            currentColormap.put("temperature", "realistic");

            minValues.put("runoff", props.getFloatProperty("MIN_runoff"));
            maxValues.put("runoff", props.getFloatProperty("MAX_runoff"));
            currentMinValues.put("runoff", props.getFloatProperty("SET_MIN_runoff"));
            currentMaxValues.put("runoff", props.getFloatProperty("SET_MAX_runoff"));
            diffMinValues.put("runoff", props.getFloatProperty("DIFF_MIN_runoff"));
            diffMaxValues.put("runoff", props.getFloatProperty("DIFF_MAX_runoff"));
            currentDiffMinValues.put("runoff", props.getFloatProperty("SET_DIFF_MIN_runoff"));
            currentDiffMaxValues.put("runoff", props.getFloatProperty("SET_DIFF_MAX_runoff"));
            currentColormap.put("runoff", "hotres");

            minValues.put("runoff", props.getFloatProperty("MIN_runoff"));
            maxValues.put("runoff", props.getFloatProperty("MAX_runoff"));
            currentMinValues.put("runoff", props.getFloatProperty("SET_MIN_runoff"));
            currentMaxValues.put("runoff", props.getFloatProperty("SET_MAX_runoff"));
            diffMinValues.put("runoff", props.getFloatProperty("DIFF_MIN_runoff"));
            diffMaxValues.put("runoff", props.getFloatProperty("DIFF_MAX_runoff"));
            currentDiffMinValues.put("runoff", props.getFloatProperty("SET_DIFF_MIN_runoff"));
            currentDiffMaxValues.put("runoff", props.getFloatProperty("SET_DIFF_MAX_runoff"));
            currentColormap.put("runoff", "hotres");

            // grid_width_dimension_substring = props
            // .getProperty("grid_width_dimension_substring");
            // grid_height_dimension_substring = props
            // .getProperty("grid_height_dimension_substring");

        } catch (NumberFormatException e) {
            logger.warn(e.getMessage());
        }

        initializeScreenDescriptions();
    }

    private synchronized void initializeScreenDescriptions() {
        screenDescriptions = new SurfaceTextureDescription[number_of_screens_col * number_of_screens_row];
    }

    public synchronized void setWaittimeBeforeRetry(long value) {
        WAITTIME_FOR_RETRY = value;
    }

    public synchronized void setWaittimeMovie(long value) {
        WAITTIME_FOR_MOVIE = value;
    }

    public synchronized void setEpsilon(float value) {
        EPSILON = value;
    }

    public synchronized void setFileExtensionLength(int value) {
        FILE_EXTENSION_LENGTH = value;
    }

    public synchronized void setFileNumberLength(int value) {
        FILE_NUMBER_LENGTH = value;
    }

    public synchronized void setCurrentExtension(String value) {
        CURRENT_POSTFIX = value;
    }

    public long getWaittimeBeforeRetry() {
        return WAITTIME_FOR_RETRY;
    }

    public long getWaittimeMovie() {
        return WAITTIME_FOR_MOVIE;
    }

    public synchronized float getEpsilon() {
        return EPSILON;
    }

    public synchronized int getFileExtensionLength() {
        return FILE_EXTENSION_LENGTH;
    }

    public synchronized int getFileNumberLength() {
        return FILE_NUMBER_LENGTH;
    }

    public synchronized String[] getAcceptableExtensions() {
        return ACCEPTABLE_POSTFIXES;
    }

    public synchronized String getCurrentExtension() {
        return CURRENT_POSTFIX;
    }

    public synchronized int getPreprocessAmount() {
        return PREPROCESSING_AMOUNT;
    }

    public synchronized void setPreprocessAmount(int value) {
        PREPROCESSING_AMOUNT = value;
    }

    public synchronized int getDepthMin() {
        return DEPTH_MIN;
    }

    public synchronized void setDepthMin(int value) {
        DEPTH_MIN = value;
    }

    public synchronized int getDepthDef() {
        return DEPTH_DEF;
    }

    public synchronized void setFrameNumber(int value) {
        for (int i = 0; i < number_of_screens_col * number_of_screens_row; i++) {
            SurfaceTextureDescription currentState = screenDescriptions[i];
            screenDescriptions[i] = new SurfaceTextureDescription(value, currentState.getDepth(),
                    currentState.getVarName(), currentState.getColorMap(), currentState.isDynamicDimensions(),
                    currentState.isDiff(), currentState.isSecondSet(), currentState.getLowerBound(),
                    currentState.getUpperBound(), currentState.isLogScale());
        }
        setRequestedNewConfiguration(true);
    }

    public synchronized void setDepth(int value) {
        for (int i = 0; i < number_of_screens_col * number_of_screens_row; i++) {
            SurfaceTextureDescription currentState = screenDescriptions[i];
            screenDescriptions[i] = new SurfaceTextureDescription(currentState.getFrameNumber(), value,
                    currentState.getVarName(), currentState.getColorMap(), currentState.isDynamicDimensions(),
                    currentState.isDiff(), currentState.isSecondSet(), currentState.getLowerBound(),
                    currentState.getUpperBound(), currentState.isLogScale());
        }

        DEPTH_DEF = value;
        setRequestedNewConfiguration(true);
    }

    public synchronized int getDepthMax() {
        return DEPTH_MAX;
    }

    public synchronized void setDepthMax(int value) {
        DEPTH_MAX = value;
    }

    public synchronized void setWindowSelection(int i) {
        WINDOW_SELECTION = i;
    }

    public synchronized int getWindowSelection() {
        return WINDOW_SELECTION;
    }

    public synchronized String selectionToString(int windowSelection) {
        if (windowSelection == 1) {
            return "Left Top";
        } else if (windowSelection == 2) {
            return "Right Top";
        } else if (windowSelection == 3) {
            return "Left Bottom";
        } else if (windowSelection == 4) {
            return "Right Bottom";
        }

        return "All";
    }

    public synchronized void setDataMode(int screenNumber, boolean dynamic, boolean diff, boolean secondSet) {
        SurfaceTextureDescription state = screenDescriptions[screenNumber];

        SurfaceTextureDescription result;
        result = new SurfaceTextureDescription(state.getFrameNumber(), state.getDepth(), state.getVarName(),
                state.getColorMap(), dynamic, diff, secondSet, state.getLowerBound(), state.getUpperBound(),
                state.isLogScale());
        screenDescriptions[screenNumber] = result;
        setRequestedNewConfiguration(true);
    }

    public synchronized void setVariable(int screenNumber, String variable) {
        SurfaceTextureDescription state = screenDescriptions[screenNumber];
        SurfaceTextureDescription result = new SurfaceTextureDescription(state.getFrameNumber(), state.getDepth(),
                variable, getCurrentColormap(variable), state.isDynamicDimensions(), state.isDiff(),
                state.isSecondSet(), state.getLowerBound(), state.getUpperBound(), state.isLogScale());
        screenDescriptions[screenNumber] = result;
        setRequestedNewConfiguration(true);
    }

    public synchronized SurfaceTextureDescription getSurfaceDescription(int screenNumber) {
        return screenDescriptions[screenNumber];
    }

    public synchronized void setColorMap(int screenNumber, String selectedColorMap) {
        SurfaceTextureDescription state = screenDescriptions[screenNumber];
        SurfaceTextureDescription result = new SurfaceTextureDescription(state.getFrameNumber(), state.getDepth(),
                state.getVarName(), selectedColorMap, state.isDynamicDimensions(), state.isDiff(), state.isSecondSet(),
                state.getLowerBound(), state.getUpperBound(), state.isLogScale());
        screenDescriptions[screenNumber] = result;
        currentColormap.put(state.getVarName(), selectedColorMap);

        if (cacheFileManager != null) {
            cacheFileManager.writeColormap(state.getVarName(), selectedColorMap);
        }
        setRequestedNewConfiguration(true);
    }

    public synchronized boolean isIMAGE_STREAM_OUTPUT() {
        return IMAGE_STREAM_OUTPUT;
    }

    public synchronized void setIMAGE_STREAM_OUTPUT(boolean value) {
        IMAGE_STREAM_OUTPUT = value;
    }

    public synchronized String getSAGE_DIRECTORY() {
        return SAGE_DIRECTORY;
    }

    public synchronized void setSAGE_DIRECTORY(String sAGE_DIRECTORY) {
        SAGE_DIRECTORY = sAGE_DIRECTORY;
    }

    public synchronized boolean isIMAGE_STREAM_GL_ONLY() {
        return IMAGE_STREAM_GL_ONLY;
    }

    public synchronized void setIMAGE_STREAM_GL_ONLY(boolean iMAGE_STREAM_GL_ONLY) {
        IMAGE_STREAM_GL_ONLY = iMAGE_STREAM_GL_ONLY;
    }

    public synchronized float getHeightDistortion() {
        return HEIGHT_DISTORION;
    }

    public synchronized float getHeightDistortionMin() {
        return HEIGHT_DISTORION_MIN;
    }

    public synchronized float getHeightDistortionMax() {
        return HEIGHT_DISTORION_MAX;
    }

    public synchronized void setHeightDistortion(float value) {
        HEIGHT_DISTORION = value;
    }

    public synchronized boolean isTouchConnected() {
        return TOUCH_CONNECTED;
    }

    public synchronized String getFancyDate(int frameNumber) {
        String result = "Month: " + frameNumber;

        return result;
    }

    public synchronized int getSageFramesPerSecond() {
        return SAGE_FRAMES_PER_SECOND;
    }

    public synchronized int getTimestep() {
        return TIME_STEP_SIZE;
    }

    public synchronized void setTimestep(int value) {
        System.out.println("Timestep set to: " + value);
        TIME_STEP_SIZE = value;
    }

    public synchronized boolean getStereo() {
        return STEREO_RENDERING;
    }

    public synchronized void setStereo(int stateChange) {
        if (stateChange == 1)
            STEREO_RENDERING = true;
        if (stateChange == 2)
            STEREO_RENDERING = false;
    }

    public synchronized boolean getStereoSwitched() {
        return STEREO_SWITCHED;
    }

    public synchronized void setStereoSwitched(int stateChange) {
        if (stateChange == 1)
            STEREO_SWITCHED = true;
        if (stateChange == 2)
            STEREO_SWITCHED = false;
    }

    public synchronized float getStereoOcularDistanceMin() {
        return STEREO_OCULAR_DISTANCE_MIN;
    }

    public synchronized float getStereoOcularDistanceMax() {
        return STEREO_OCULAR_DISTANCE_MAX;
    }

    public synchronized float getStereoOcularDistance() {
        return STEREO_OCULAR_DISTANCE_DEF;
    }

    public synchronized void setStereoOcularDistance(float value) {
        STEREO_OCULAR_DISTANCE_DEF = value;
    }

    public synchronized int getDefaultScreenWidth() {
        return DEFAULT_SCREEN_WIDTH;
    }

    public synchronized int getDefaultScreenHeight() {
        return DEFAULT_SCREEN_HEIGHT;
    }

    public synchronized int getScreenshotScreenWidth() {
        return SCREENSHOT_SCREEN_WIDTH;
    }

    public synchronized int getScreenshotScreenHeight() {
        return SCREENSHOT_SCREEN_HEIGHT;
    }

    public synchronized int getMaxOctreeDepth() {
        return MAX_OCTREE_DEPTH;
    }

    public synchronized float getOctreeEdges() {
        return OCTREE_EDGES;
    }

    public synchronized int getMaxExpectedModels() {
        return MAX_EXPECTED_MODELS;
    }

    public synchronized float getInitialRotationX() {
        return INITIAL_ROTATION_X;
    }

    public synchronized float getInitialRotationY() {
        return INITIAL_ROTATION_Y;
    }

    public synchronized float getInitialZoom() {
        return INITIAL_ZOOM;
    }

    public synchronized void setMovieRotate(int stateChange) {
        if (stateChange == 1)
            MOVIE_ROTATE = true;
        if (stateChange == 2)
            MOVIE_ROTATE = false;
    }

    public synchronized boolean getMovieRotate() {
        return MOVIE_ROTATE;
    }

    public synchronized void setMovieRotationSpeed(float value) {
        MOVIE_ROTATION_SPEED_DEF = value;
    }

    public synchronized float getMovieRotationSpeedMin() {
        return MOVIE_ROTATION_SPEED_MIN;
    }

    public synchronized float getMovieRotationSpeedMax() {
        return MOVIE_ROTATION_SPEED_MAX;
    }

    public synchronized float getMovieRotationSpeedDef() {
        return MOVIE_ROTATION_SPEED_DEF;
    }

    public synchronized int getInitialSimulationFrame() {
        return INITIAL_SIMULATION_FRAME;
    }

    public synchronized void setInitial_simulation_frame(int initialSimulationFrame) {
        INITIAL_SIMULATION_FRAME = initialSimulationFrame;
    }

    public synchronized void setInitial_rotation_x(float initialRotationX) {
        INITIAL_ROTATION_X = initialRotationX;
    }

    public synchronized void setInitial_rotation_y(float initialRotationY) {
        INITIAL_ROTATION_Y = initialRotationY;
    }

    public synchronized String getScreenshotPath() {
        return SCREENSHOT_PATH;
    }

    public synchronized void setScreenshotPath(String newPath) {
        SCREENSHOT_PATH = newPath;
    }

    public synchronized void setVariableRange(int screenNumber, String varName, int sliderLowerValue,
            int sliderUpperValue) {
        float diff = getVarMax(varName) - getVarMin(varName);

        currentMinValues.put(varName, (sliderLowerValue / 100f) * diff + getVarMin(varName));
        currentMaxValues.put(varName, (sliderUpperValue / 100f) * diff + getVarMin(varName));
        float minFloatValue = getCurrentVarMin(varName);
        float maxFloatValue = getCurrentVarMax(varName);

        SurfaceTextureDescription state = screenDescriptions[screenNumber];
        SurfaceTextureDescription result = new SurfaceTextureDescription(state.getFrameNumber(), state.getDepth(),
                state.getVarName(), state.getColorMap(), state.isDynamicDimensions(), state.isDiff(),
                state.isSecondSet(), minFloatValue, maxFloatValue, state.isLogScale());
        screenDescriptions[screenNumber] = result;
        setRequestedNewConfiguration(true);
    }

    public synchronized int getRangeSliderLowerValue(int screenNumber) {
        SurfaceTextureDescription state = screenDescriptions[screenNumber];

        float min = getVarMin(state.getVarName());
        float max = getVarMax(state.getVarName());
        float currentMin = getCurrentVarMin(state.getVarName());

        float diff = max - min;
        float result = (currentMin - min) / diff;

        return (int) (result * 100) - 1;
    }

    public synchronized int getRangeSliderUpperValue(int screenNumber) {
        SurfaceTextureDescription state = screenDescriptions[screenNumber];

        float min = getVarMin(state.getVarName());
        float max = getVarMax(state.getVarName());
        float currentMax = getCurrentVarMax(state.getVarName());

        float diff = max - min;
        float result = (currentMax - min) / diff;

        return (int) (result * 100) - 1;
    }

    public synchronized String getWidthSubstring() {
        return grid_width_dimension_substring;
    }

    public synchronized String getHeightSubstring() {
        return grid_height_dimension_substring;
    }

    public synchronized int getNumScreensRows() {
        return number_of_screens_row;
    }

    public synchronized int getNumScreensCols() {
        return number_of_screens_col;
    }

    public synchronized void setNumberOfScreens(int rows, int columns) {
        number_of_screens_row = rows;
        number_of_screens_col = columns;

        initializeScreenDescriptions();
    }

    public synchronized int getInterfaceWidth() {
        return INTERFACE_WIDTH;
    }

    public synchronized int getInterfaceHeight() {
        return INTERFACE_HEIGHT;
    }

    public synchronized void initDefaultVariables(ArrayList<String> variables) {
        screenDescriptions = new SurfaceTextureDescription[number_of_screens_col * number_of_screens_row];

        if (variables.size() != 0) {
            for (int j = 0; j < number_of_screens_col * number_of_screens_row; j++) {
                String var;
                if (j < variables.size()) {
                    var = variables.get(j);
                } else {
                    var = variables.get(0);
                }
                screenDescriptions[j] = new SurfaceTextureDescription(INITIAL_SIMULATION_FRAME, 0, var,
                        getCurrentColormap(var), false, false, false, getCurrentVarMin(var), getCurrentVarMax(var),
                        false);

                logger.debug(screenDescriptions[j].toString());
            }
        }
    }

    public synchronized String getCurrentColormap(String key) {
        String colormap = "";
        if (currentColormap.containsKey(key)) {
            colormap = currentColormap.get(key);
        } else if (cacheFileManager != null) {
            colormap = cacheFileManager.readColormap(key);
        }

        if (colormap.compareTo("") == 0) {
            colormap = ColormapInterpreter.getColormapNames()[0];
        }

        return colormap;
    }

    public synchronized float getVarMin(String key) {
        float value;
        if (minValues.containsKey(key)) {
            value = minValues.get(key);
        } else {
            value = Float.NaN;
        }

        return value;
    }

    public synchronized float getVarMax(String key) {
        float value;
        if (maxValues.containsKey(key)) {
            value = maxValues.get(key);
        } else {
            value = Float.NaN;
        }

        return value;
    }

    public synchronized float getCurrentVarDiffMin(String key) {
        float value;
        if (currentDiffMinValues.containsKey(key)) {
            value = currentDiffMinValues.get(key);
        } else {
            value = minValues.get(key);
        }

        return value;
    }

    public synchronized float getCurrentVarDiffMax(String key) {
        float value;
        if (currentDiffMaxValues.containsKey(key)) {
            value = currentDiffMaxValues.get(key);
        } else {
            value = maxValues.get(key);
        }

        return value;
    }

    public synchronized float getCurrentVarMin(String key) {
        float value;
        if (currentMinValues.containsKey(key)) {
            value = currentMinValues.get(key);
        } else {
            value = minValues.get(key);
        }

        return value;
    }

    public synchronized float getCurrentVarMax(String key) {
        float value;
        if (currentMaxValues.containsKey(key)) {
            value = currentMaxValues.get(key);
        } else {
            value = maxValues.get(key);
        }

        return value;
    }

    public synchronized void setVarMin(String key, float currentMin) {
        minValues.put(key, currentMin);
        currentMinValues.put(key, currentMin);
        setRequestedNewConfiguration(true);
    }

    public synchronized void setVarMax(String key, float currentMax) {
        maxValues.put(key, currentMax);
        currentMaxValues.put(key, currentMax);
        setRequestedNewConfiguration(true);

    }

    public synchronized void setLogScale(int screenNumber, boolean selected) {
        SurfaceTextureDescription state = screenDescriptions[screenNumber];
        SurfaceTextureDescription result = new SurfaceTextureDescription(state.getFrameNumber(), state.getDepth(),
                state.getVarName(), state.getColorMap(), state.isDynamicDimensions(), state.isDiff(),
                state.isSecondSet(), state.getLowerBound(), state.getUpperBound(), selected);
        screenDescriptions[screenNumber] = result;
        setRequestedNewConfiguration(true);
    }

    public synchronized void setCacheFileManager(CacheFileManager cacheFileManager) {
        this.cacheFileManager = cacheFileManager;
    }

    public synchronized CacheFileManager getCacheFileManager() {
        return cacheFileManager;
    }

    public synchronized boolean isRequestedNewConfiguration() {
        return requestedNewConfiguration;
    }

    public synchronized void setRequestedNewConfiguration(boolean requestedNewConfiguration) {
        this.requestedNewConfiguration = requestedNewConfiguration;
    }

    public int getNumberOfScreenshotsPerTimeStep() {
        return numberOfScreenshotsPerTimeStep;
    }

    public void setNumberOfScreenshotsPerTimeStep(int numberOfScreenshotsPerTimeStep) {
        this.numberOfScreenshotsPerTimeStep = numberOfScreenshotsPerTimeStep;
    }
}
