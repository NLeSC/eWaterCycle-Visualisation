package nl.esciencecenter.visualization.ewatercycle.data;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.media.opengl.GL3;

import nl.esciencecenter.neon.swing.ColormapInterpreter;
import nl.esciencecenter.neon.swing.ColormapInterpreter.Color;
import nl.esciencecenter.neon.swing.ColormapInterpreter.Dimensions;
import nl.esciencecenter.visualization.ewatercycle.WaterCycleSettings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImauDatasetManager {
    private final static Logger logger = LoggerFactory.getLogger(ImauDatasetManager.class);
    private final WaterCycleSettings settings = WaterCycleSettings.getInstance();

    private ArrayList<Integer> availableFrameSequenceNumbers;
    private HashMap<String, NetCDFReader> readers;

    // private NetCDFReader ncr1;
    // private NetCDFReader ncrSnowCoverSWE, ncrStorUpp000005, ncrStorUpp005030,
    // ncrStorLow030150;
    // private NetCDFReader ncrSatDegUpp000005, ncrSatDegUpp005030,
    // ncrSatDegLow030150, ncrPrecipitation, ncrTemperature;

    // private TextureStorage texStorage;
    private EfficientTextureStorage effTexStorage;

    private int latArraySize;
    private int lonArraySize;

    public ImauDatasetManager(File file1) {
        // logger.debug("Opening dataset with initial file: " +
        // file1.getAbsolutePath());
        //
        // ncr1 = new NetCDFReader(file1);
        // logger.debug(ncr1.toString());
        //
        // availableFrameSequenceNumbers = new ArrayList<Integer>();
        // for (int i = 0; i < ncr1.getAvailableFrames(); i++) {
        // availableFrameSequenceNumbers.add(i);
        // }
        // availableVariables = ncr1.getVariableNames();
        //
        // latArraySize = ncr1.getLatSize();
        // lonArraySize = ncr1.getLonSize();
        //
        // texStorage = new TextureStorage(this, settings.getNumScreensRows() *
        // settings.getNumScreensCols(),
        // lonArraySize, latArraySize);

    }

    public ImauDatasetManager(File discharge, File snowCoverSWE, File storUpp000005, File storUpp005030,
            File storLow030150, File satDegUpp000005, File satDegUpp005030, File satDegLow030150, File precipitation,
            File temperature) {
        File[] files = new File[] { discharge, snowCoverSWE, storUpp000005, storUpp005030, storLow030150,
                satDegUpp000005, satDegUpp005030, satDegLow030150, precipitation, temperature };

        init(files);

        // ncr1 = new NetCDFReader(discharge);
        // ncrSnowCoverSWE = new NetCDFReader(snowCoverSWE);
        // ncrStorUpp000005 = new NetCDFReader(storUpp000005);
        // ncrStorUpp005030 = new NetCDFReader(storUpp005030);
        // ncrStorLow030150 = new NetCDFReader(storLow030150);
        // ncrSatDegUpp000005 = new NetCDFReader(satDegUpp000005);
        // ncrSatDegUpp005030 = new NetCDFReader(satDegUpp005030);
        // ncrSatDegLow030150 = new NetCDFReader(satDegLow030150);
        // ncrPrecipitation = new NetCDFReader(precipitation);
        // ncrTemperature = new NetCDFReader(temperature);
        //
        // availableFrameSequenceNumbers = new ArrayList<Integer>();
        //
        // for (int i = 0; i < ncr1.getAvailableFrames(); i++) {
        // if (i < ncrSnowCoverSWE.getAvailableFrames() && i <
        // ncrStorUpp000005.getAvailableFrames()
        // && i < ncrStorUpp005030.getAvailableFrames() && i <
        // ncrStorLow030150.getAvailableFrames()) {
        // availableFrameSequenceNumbers.add(i);
        // }
        // }
        // availableVariables = new ArrayList<String>();
        // availableVariables.add("discharge");
        // availableVariables.add("snowCoverSWE");
        // availableVariables.add("storUpp000005");
        // availableVariables.add("storUpp005030");
        // availableVariables.add("storLow030150");
        // availableVariables.add("satDegUpp000005");
        // availableVariables.add("satDegUpp005030");
        // availableVariables.add("satDegLow030150");
        // availableVariables.add("precipitation");
        // availableVariables.add("temperature");
        //
        // latArraySize = ncr1.getLatSize();
        //
        // if (latArraySize != ncrSnowCoverSWE.getLatSize() || latArraySize !=
        // ncrStorUpp000005.getLatSize()
        // || latArraySize != ncrStorUpp005030.getLatSize() || latArraySize !=
        // ncrStorLow030150.getLatSize()) {
        // logger.debug("LAT ARRAY SIZES NOT EQUAL");
        // }
        //
        // lonArraySize = ncr1.getLonSize();
        //
        // if (lonArraySize != ncrSnowCoverSWE.getLonSize() || lonArraySize !=
        // ncrStorUpp000005.getLonSize()
        // || lonArraySize != ncrStorUpp005030.getLonSize() || lonArraySize !=
        // ncrStorLow030150.getLonSize()) {
        // logger.debug("LON ARRAY SIZES NOT EQUAL");
        // }
        //
        // texStorage = new TextureStorage(this, settings.getNumScreensRows() *
        // settings.getNumScreensCols(),
        // lonArraySize, latArraySize);

    }

    public ImauDatasetManager(File[] files) {
        init(files);
    }

    private void init(File[] files) {
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

        // texStorage = new TextureStorage(this, settings.getNumScreensRows() *
        // settings.getNumScreensCols(),
        // lonArraySize, latArraySize);

        effTexStorage = new EfficientTextureStorage(this, settings.getNumScreensRows() * settings.getNumScreensCols(),
                lonArraySize, latArraySize, GL3.GL_TEXTURE4, GL3.GL_TEXTURE5);

    }

    public void buildImages(SurfaceTextureDescription desc) {
        int frameNumber = desc.getFrameNumber();
        String varName = desc.getVarName();

        NetCDFReader currentReader = readers.get(varName);
        // if (varName.compareTo("discharge") == 0) {
        // currentReader = ncr1;
        // } else if (varName.compareTo("snowCoverSWE") == 0) {
        // currentReader = ncrSnowCoverSWE;
        // } else if (varName.compareTo("storUpp000005") == 0) {
        // currentReader = ncrStorUpp000005;
        // } else if (varName.compareTo("storUpp005030") == 0) {
        // currentReader = ncrStorUpp005030;
        // } else if (varName.compareTo("storLow030150") == 0) {
        // currentReader = ncrStorLow030150;
        // } else if (varName.compareTo("satDegUpp000005") == 0) {
        // currentReader = ncrSatDegUpp000005;
        // } else if (varName.compareTo("satDegUpp005030") == 0) {
        // currentReader = ncrSatDegUpp005030;
        // } else if (varName.compareTo("satDegLow030150") == 0) {
        // currentReader = ncrSatDegLow030150;
        // } else if (varName.compareTo("precipitation") == 0) {
        // currentReader = ncrPrecipitation;
        // } else if (varName.compareTo("temperature") == 0) {
        // currentReader = ncrTemperature;
        // }

        if (frameNumber < 0 || frameNumber > currentReader.getAvailableFrames(varName)) {
            logger.debug("buildImages : Requested frameNumber  " + frameNumber + " out of range.");
        }

        ByteBuffer surfaceBuffer = currentReader.getImage(desc.getColorMap(), varName, frameNumber);
        // texStorage.setSurfaceImage(desc, surfaceBuffer);
        effTexStorage.setSurfaceImage(desc, surfaceBuffer);

        Dimensions colormapDims = new Dimensions(settings.getCurrentVarMin(varName), settings.getCurrentVarMax(varName));

        int height = 500;
        int width = 1;
        ByteBuffer outBuf = ByteBuffer.allocate(height * width * 4);

        for (int row = height - 1; row >= 0; row--) {
            float index = row / (float) height;
            float var = (index * colormapDims.getDiff()) + colormapDims.getMin();

            Color c = ColormapInterpreter.getColor(desc.getColorMap(), colormapDims, var);

            for (int col = 0; col < width; col++) {
                outBuf.put((byte) (255 * c.getRed()));
                outBuf.put((byte) (255 * c.getGreen()));
                outBuf.put((byte) (255 * c.getBlue()));
                outBuf.put((byte) 0);
            }
        }

        outBuf.flip();

        // texStorage.setLegendImage(desc, outBuf);
        effTexStorage.setLegendImage(desc, outBuf);
    }

    // public TextureStorage getTextureStorage() {
    // return texStorage;
    // }

    public EfficientTextureStorage getEfficientTextureStorage() {
        return effTexStorage;
    }

    public int getFrameNumberOfIndex(int index) {
        return availableFrameSequenceNumbers.get(index);
    }

    public int getIndexOfFrameNumber(int frameNumber) {
        return availableFrameSequenceNumbers.indexOf(frameNumber);
    }

    public int getPreviousFrameNumber(int frameNumber) throws IOException {
        int nextNumber = getIndexOfFrameNumber(frameNumber) - 1;

        if (nextNumber >= 0 && nextNumber < availableFrameSequenceNumbers.size()) {
            return getFrameNumberOfIndex(nextNumber);
        } else {
            throw new IOException("Frame number not available: " + nextNumber);
        }
    }

    public int getNextFrameNumber(int frameNumber) throws IOException {
        int nextNumber = getIndexOfFrameNumber(frameNumber) + 1;

        if (nextNumber >= 0 && nextNumber < availableFrameSequenceNumbers.size()) {
            return getFrameNumberOfIndex(nextNumber);
        } else {
            throw new IOException("Frame number not available: " + nextNumber);
        }
    }

    public int getNumFrames() {
        return availableFrameSequenceNumbers.size();
    }

    public ArrayList<String> getVariables() {
        ArrayList<String> result = new ArrayList<String>();
        for (Map.Entry<String, NetCDFReader> readerEntry : readers.entrySet()) {
            result.add(readerEntry.getKey());
        }
        return result;
    }

    public String getVariableUnits(String varName) {
        return readers.get(varName).getUnits(varName);
    }

    public int getImageWidth() {
        return lonArraySize;
    }

    public int getImageHeight() {
        return latArraySize;
    }

    public float getMinValueContainedInDataset(String varName) {
        return readers.get(varName).getMinValue(varName);
    }

    public float getMaxValueContainedInDataset(String varName) {
        return readers.get(varName).getMaxValue(varName);
    }
}
