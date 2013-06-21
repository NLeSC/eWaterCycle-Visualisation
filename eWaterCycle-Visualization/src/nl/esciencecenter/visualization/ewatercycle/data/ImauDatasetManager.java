package nl.esciencecenter.visualization.ewatercycle.data;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import nl.esciencecenter.esight.swing.ColormapInterpreter;
import nl.esciencecenter.esight.swing.ColormapInterpreter.Color;
import nl.esciencecenter.esight.swing.ColormapInterpreter.Dimensions;
import nl.esciencecenter.visualization.ewatercycle.WaterCycleSettings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImauDatasetManager {
    private final static Logger logger = LoggerFactory.getLogger(ImauDatasetManager.class);
    private final WaterCycleSettings settings = WaterCycleSettings.getInstance();

    private final ArrayList<Integer> availableFrameSequenceNumbers;
    private final ArrayList<String> availableVariables;

    private final NetCDFReader ncr1;
    private NetCDFReader ncrSnowCoverSWE, ncrStorUpp000005, ncrStorUpp005030, ncrStorLow030150;
    private NetCDFReader ncrSatDegUpp000005, ncrSatDegUpp005030, ncrSatDegLow030150, ncrPrecipitation, ncrTemperature;

    private final TextureStorage texStorage;

    private final int latArraySize;
    private final int lonArraySize;

    public ImauDatasetManager(File file1) {
        logger.debug("Opening dataset with initial file: " + file1.getAbsolutePath());

        ncr1 = new NetCDFReader(file1);
        logger.debug(ncr1.toString());

        availableFrameSequenceNumbers = new ArrayList<Integer>();
        for (int i = 0; i < ncr1.getAvailableFrames(); i++) {
            availableFrameSequenceNumbers.add(i);
        }
        availableVariables = ncr1.getVariableNames();

        latArraySize = ncr1.getLatSize();
        lonArraySize = ncr1.getLonSize();

        texStorage = new TextureStorage(this, settings.getNumScreensRows() * settings.getNumScreensCols(),
                lonArraySize, latArraySize);

    }

    public ImauDatasetManager(File discharge, File snowCoverSWE, File storUpp000005, File storUpp005030,
            File storLow030150, File satDegUpp000005, File satDegUpp005030, File satDegLow030150, File precipitation,
            File temperature) {
        ncr1 = new NetCDFReader(discharge);
        ncrSnowCoverSWE = new NetCDFReader(snowCoverSWE);
        ncrStorUpp000005 = new NetCDFReader(storUpp000005);
        ncrStorUpp005030 = new NetCDFReader(storUpp005030);
        ncrStorLow030150 = new NetCDFReader(storLow030150);
        ncrSatDegUpp000005 = new NetCDFReader(satDegUpp000005);
        ncrSatDegUpp005030 = new NetCDFReader(satDegUpp005030);
        ncrSatDegLow030150 = new NetCDFReader(satDegLow030150);
        ncrPrecipitation = new NetCDFReader(precipitation);
        ncrTemperature = new NetCDFReader(temperature);

        availableFrameSequenceNumbers = new ArrayList<Integer>();

        for (int i = 0; i < ncr1.getAvailableFrames(); i++) {
            if (i < ncrSnowCoverSWE.getAvailableFrames() && i < ncrStorUpp000005.getAvailableFrames()
                    && i < ncrStorUpp005030.getAvailableFrames() && i < ncrStorLow030150.getAvailableFrames()) {
                availableFrameSequenceNumbers.add(i);
            }
        }
        availableVariables = new ArrayList<String>();
        availableVariables.add("discharge");
        availableVariables.add("snowCoverSWE");
        availableVariables.add("storUpp000005");
        availableVariables.add("storUpp005030");
        availableVariables.add("storLow030150");
        availableVariables.add("satDegUpp000005");
        availableVariables.add("satDegUpp005030");
        availableVariables.add("satDegLow030150");
        availableVariables.add("precipitation");
        availableVariables.add("temperature");

        latArraySize = ncr1.getLatSize();

        if (latArraySize != ncrSnowCoverSWE.getLatSize() || latArraySize != ncrStorUpp000005.getLatSize()
                || latArraySize != ncrStorUpp005030.getLatSize() || latArraySize != ncrStorLow030150.getLatSize()) {
            logger.debug("LAT ARRAY SIZES NOT EQUAL");
        }

        lonArraySize = ncr1.getLonSize();

        if (lonArraySize != ncrSnowCoverSWE.getLonSize() || lonArraySize != ncrStorUpp000005.getLonSize()
                || lonArraySize != ncrStorUpp005030.getLonSize() || lonArraySize != ncrStorLow030150.getLonSize()) {
            logger.debug("LON ARRAY SIZES NOT EQUAL");
        }

        texStorage = new TextureStorage(this, settings.getNumScreensRows() * settings.getNumScreensCols(),
                lonArraySize, latArraySize);

    }

    public void buildImages(SurfaceTextureDescription desc) {
        int frameNumber = desc.getFrameNumber();
        String varName = desc.getVarName();

        NetCDFReader currentReader = ncr1;
        if (varName.compareTo("discharge") == 0) {
            currentReader = ncr1;
        } else if (varName.compareTo("snowCoverSWE") == 0) {
            currentReader = ncrSnowCoverSWE;
        } else if (varName.compareTo("storUpp000005") == 0) {
            currentReader = ncrStorUpp000005;
        } else if (varName.compareTo("storUpp005030") == 0) {
            currentReader = ncrStorUpp005030;
        } else if (varName.compareTo("storLow030150") == 0) {
            currentReader = ncrStorLow030150;
        } else if (varName.compareTo("satDegUpp000005") == 0) {
            currentReader = ncrSatDegUpp000005;
        } else if (varName.compareTo("satDegUpp005030") == 0) {
            currentReader = ncrSatDegUpp005030;
        } else if (varName.compareTo("satDegLow030150") == 0) {
            currentReader = ncrSatDegLow030150;
        } else if (varName.compareTo("precipitation") == 0) {
            currentReader = ncrPrecipitation;
        } else if (varName.compareTo("temperature") == 0) {
            currentReader = ncrTemperature;
        }

        if (frameNumber < 0 || frameNumber > currentReader.getAvailableFrames(varName)) {
            logger.debug("buildImages : Requested frameNumber  " + frameNumber + " out of range.");
        }

        ByteBuffer surfaceBuffer = currentReader.getImage(desc.getColorMap(), varName, frameNumber);
        texStorage.setSurfaceImage(desc, surfaceBuffer);

        Dimensions colormapDims = new Dimensions(settings.getCurrentVarMin(varName), settings.getCurrentVarMax(varName));

        int height = 500;
        int width = 1;
        ByteBuffer outBuf = ByteBuffer.allocate(height * width * 4);

        for (int row = height - 1; row >= 0; row--) {
            float index = row / (float) height;
            float var = (index * colormapDims.getDiff()) + colormapDims.min;

            Color c = ColormapInterpreter.getColor(desc.getColorMap(), colormapDims, var);

            for (int col = 0; col < width; col++) {
                outBuf.put((byte) (255 * c.red));
                outBuf.put((byte) (255 * c.green));
                outBuf.put((byte) (255 * c.blue));
                outBuf.put((byte) 0);
            }
        }

        outBuf.flip();

        texStorage.setLegendImage(desc, outBuf);
    }

    public TextureStorage getTextureStorage() {
        return texStorage;
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

    public int getNumFiles() {
        return availableFrameSequenceNumbers.size();
    }

    public ArrayList<String> getVariables() {
        return availableVariables;
    }

    public String getVariableUnits(String varName) {

        return ncr1.getUnits(varName);
    }

    public int getImageWidth() {
        return lonArraySize;
    }

    public int getImageHeight() {
        return latArraySize;
    }
}
