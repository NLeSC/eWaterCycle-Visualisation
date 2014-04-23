package nl.esciencecenter.visualization.ewatercycle.data;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.media.opengl.GL3;

import nl.esciencecenter.neon.swing.ColormapInterpreter;
import nl.esciencecenter.neon.swing.ColormapInterpreter.Color;
import nl.esciencecenter.neon.swing.ColormapInterpreter.Dimensions;
import nl.esciencecenter.visualization.ewatercycle.WaterCycleSettings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatasetManager {
    private final static Logger logger = LoggerFactory.getLogger(DatasetManager.class);
    private final WaterCycleSettings settings = WaterCycleSettings.getInstance();

    private ArrayList<Integer> availableFrameSequenceNumbers;
    private HashMap<String, NetCDFReader> readers;
    private EfficientTextureStorage effTexStorage;

    private int latArraySize;
    private int lonArraySize;

    ExecutorService executor;

    // private final List<SurfaceTextureDescription> queue;

    private class Worker implements Runnable {
        private final SurfaceTextureDescription desc;

        public Worker(SurfaceTextureDescription desc) {
            this.desc = desc;
        }

        @Override
        public void run() {
            int frameNumber = desc.getFrameNumber();
            String varName = desc.getVarName();

            NetCDFReader currentReader = readers.get(varName);

            if (frameNumber < 0 || frameNumber > currentReader.getAvailableFrames(varName)) {
                logger.debug("buildImages : Requested frameNumber  " + frameNumber + " out of range.");
            }

            ByteBuffer surfaceBuffer = currentReader.getImage(desc.getColorMap(), varName, frameNumber,
                    desc.isLogScale());

            Dimensions colormapDims = new Dimensions(settings.getCurrentVarMin(varName),
                    settings.getCurrentVarMax(varName));

            int height = 500;
            int width = 1;
            ByteBuffer legendBuf = ByteBuffer.allocate(height * width * 4);

            for (int row = height - 1; row >= 0; row--) {
                float index = row / (float) height;
                float var = (index * colormapDims.getDiff()) + colormapDims.getMin();

                Color c = ColormapInterpreter.getColor(desc.getColorMap(), colormapDims, var);

                for (int col = 0; col < width; col++) {
                    legendBuf.put((byte) (255 * c.getRed()));
                    legendBuf.put((byte) (255 * c.getGreen()));
                    legendBuf.put((byte) (255 * c.getBlue()));
                }
            }

            legendBuf.flip();

            effTexStorage.setImageCombo(desc, surfaceBuffer, legendBuf);
        }

    }

    public DatasetManager(File[] files) {
        // queue = new ArrayList<SurfaceTextureDescription>();
        executor = Executors.newFixedThreadPool(4);

        init(files);
    }

    public synchronized void shutdown() {
        executor.shutdown();

        while (!executor.isTerminated()) {
        }
    }

    private synchronized void init(File[] files) {
        availableFrameSequenceNumbers = new ArrayList<Integer>();
        readers = new HashMap<String, NetCDFReader>();

        latArraySize = 0;
        lonArraySize = 0;
        int frames = 0;

        for (File file : files) {
            boolean accept = true;
            boolean init = false;

            NetCDFReader ncr = new NetCDFReader(file);

            // If this is the first file, use it to set the standard
            if (latArraySize == 0) {
                latArraySize = ncr.getLatSize();
            }

            if (lonArraySize == 0) {
                lonArraySize = ncr.getLonSize();
            }

            if (frames == 0) {
                frames = ncr.getAvailableFrames();
                init = true;
            }

            // If it is a subsequent file, check if it adheres to the standards
            // set by the first file.
            if (latArraySize != ncr.getLatSize()) {
                logger.debug("LAT ARRAY SIZES NOT EQUAL");
                accept = false;
            }

            if (lonArraySize != ncr.getLonSize()) {
                logger.debug("LON ARRAY SIZES NOT EQUAL");
                accept = false;
            }

            if (frames != ncr.getAvailableFrames()) {
                logger.debug("NUMBER OF FRAMES NOT EQUAL");
                accept = false;
            }

            // If it does adhere to the standard, add the variables to the
            // datastore and associate them with the netcdf readers they came
            // from.
            if (accept) {
                ArrayList<String> varNames = ncr.getVariableNames();
                for (String varName : varNames) {
                    readers.put(varName, ncr);
                    System.out.println(varName + " added to available variables.");

                    // And determine the bounds
                    ncr.determineMinMax(varName);
                }
                if (init) {
                    int numFrames = ncr.getAvailableFrames();
                    for (int i = 0; i < numFrames; i++) {
                        availableFrameSequenceNumbers.add(i);
                    }
                }
            }
        }

        effTexStorage = new EfficientTextureStorage(this, settings.getNumScreensRows() * settings.getNumScreensCols(),
                lonArraySize, latArraySize, GL3.GL_TEXTURE4, GL3.GL_TEXTURE5);

    }

    public synchronized void buildImages(SurfaceTextureDescription desc) {
        // for (int i = 0; i < 10; i++) {
        Runnable worker = new Worker(desc);
        executor.execute(worker);
        // }
        // int frameNumber = desc.getFrameNumber();
        // String varName = desc.getVarName();
        //
        // NetCDFReader currentReader = readers.get(varName);
        //
        // if (frameNumber < 0 || frameNumber >
        // currentReader.getAvailableFrames(varName)) {
        // logger.debug("buildImages : Requested frameNumber  " + frameNumber +
        // " out of range.");
        // }
        //
        // ByteBuffer surfaceBuffer = currentReader.getImage(desc.getColorMap(),
        // varName, frameNumber, desc.isLogScale());
        // effTexStorage.setSurfaceImage(desc, surfaceBuffer);
        //
        // Dimensions colormapDims = new
        // Dimensions(settings.getCurrentVarMin(varName),
        // settings.getCurrentVarMax(varName));
        //
        // int height = 500;
        // int width = 1;
        // ByteBuffer outBuf = ByteBuffer.allocate(height * width * 4);
        //
        // for (int row = height - 1; row >= 0; row--) {
        // float index = row / (float) height;
        // float var = (index * colormapDims.getDiff()) + colormapDims.getMin();
        //
        // Color c = ColormapInterpreter.getColor(desc.getColorMap(),
        // colormapDims, var);
        //
        // for (int col = 0; col < width; col++) {
        // outBuf.put((byte) (255 * c.getRed()));
        // outBuf.put((byte) (255 * c.getGreen()));
        // outBuf.put((byte) (255 * c.getBlue()));
        // }
        // }
        //
        // outBuf.flip();
        //
        // effTexStorage.setLegendImage(desc, outBuf);
    }

    public synchronized EfficientTextureStorage getEfficientTextureStorage() {
        return effTexStorage;
    }

    public synchronized int getFrameNumberOfIndex(int index) {
        return availableFrameSequenceNumbers.get(index);
    }

    public synchronized int getIndexOfFrameNumber(int frameNumber) {
        return availableFrameSequenceNumbers.indexOf(frameNumber);
    }

    public synchronized int getPreviousFrameNumber(int frameNumber) throws IOException {
        int nextNumber = getIndexOfFrameNumber(frameNumber) - 1;

        if (nextNumber >= 0 && nextNumber < availableFrameSequenceNumbers.size()) {
            return getFrameNumberOfIndex(nextNumber);
        } else {
            throw new IOException("Frame number not available: " + nextNumber);
        }
    }

    public synchronized int getNextFrameNumber(int frameNumber) throws IOException {
        int nextNumber = getIndexOfFrameNumber(frameNumber) + 1;

        if (nextNumber >= 0 && nextNumber < availableFrameSequenceNumbers.size()) {
            return getFrameNumberOfIndex(nextNumber);
        } else {
            throw new IOException("Frame number not available: " + nextNumber);
        }
    }

    public synchronized int getNumFrames() {
        return availableFrameSequenceNumbers.size();
    }

    public synchronized ArrayList<String> getVariables() {
        ArrayList<String> result = new ArrayList<String>();
        for (Map.Entry<String, NetCDFReader> readerEntry : readers.entrySet()) {
            result.add(readerEntry.getKey());
        }
        return result;
    }

    public synchronized String getVariableUnits(String varName) {
        return readers.get(varName).getUnits(varName);
    }

    public synchronized int getImageWidth() {
        return lonArraySize;
    }

    public synchronized int getImageHeight() {
        return latArraySize;
    }

    public synchronized float getMinValueContainedInDataset(String varName) {
        return readers.get(varName).getMinValue(varName);
    }

    public synchronized float getMaxValueContainedInDataset(String varName) {
        return readers.get(varName).getMaxValue(varName);
    }
}