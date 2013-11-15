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
    private NetCDFReader ncrRecharge, ncrsatDegUpp, ncrsatDegLow;

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

    public ImauDatasetManager(File discharge, File recharge, File satDegUpp, File satDegLow) {
        ncr1 = new NetCDFReader(discharge);
        
        ncrRecharge = new NetCDFReader(recharge);
        ncrsatDegUpp = new NetCDFReader(satDegUpp);
        ncrsatDegLow = new NetCDFReader(satDegLow);

        availableFrameSequenceNumbers = new ArrayList<Integer>();

        for (int i = 0; i < ncr1.getAvailableFrames(); i++) {
            if (i < ncrRecharge.getAvailableFrames()
                    && i < ncrsatDegUpp.getAvailableFrames() && i < ncrsatDegLow.getAvailableFrames()) {
                availableFrameSequenceNumbers.add(i);
            }
        }
        availableVariables = new ArrayList<String>();
        availableVariables.add("discharge");
        availableVariables.add("recharge");
        availableVariables.add("satDegUpp");
        availableVariables.add("satDegLow");

        latArraySize = ncr1.getLatSize();

        if (latArraySize != ncrRecharge.getLatSize()
                || latArraySize != ncrsatDegUpp.getLatSize() || latArraySize != ncrsatDegLow.getLatSize()) {
            logger.debug("LAT ARRAY SIZES NOT EQUAL");
        }

        lonArraySize = ncr1.getLonSize();

        if (lonArraySize != ncrRecharge.getLonSize()
                || lonArraySize != ncrsatDegUpp.getLonSize() || lonArraySize != ncrsatDegLow.getLonSize()) {
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
        } else if (varName.compareTo("gwRecharge") == 0) {
            currentReader = ncrRecharge;
        } else if (varName.compareTo("satDegUpp") == 0) {
            currentReader = ncrsatDegUpp;
        } else if (varName.compareTo("satDegLow") == 0) {
            currentReader = ncrsatDegLow;
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
