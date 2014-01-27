package nl.esciencecenter.visualization.ewatercycle.data;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import nl.esciencecenter.neon.swing.ColormapInterpreter;
import nl.esciencecenter.neon.swing.ColormapInterpreter.Color;
import nl.esciencecenter.neon.swing.ColormapInterpreter.Dimensions;
import nl.esciencecenter.visualization.ewatercycle.WaterCycleSettings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import com.jogamp.common.nio.Buffers;

public class NetCDFReader {
    private final static Logger logger = LoggerFactory.getLogger(NetCDFReader.class);
    private final WaterCycleSettings settings = WaterCycleSettings.getInstance();

    private final NetcdfFile ncfile;
    private final HashMap<String, Variable> variables;
    private final HashMap<String, String> units;
    private final HashMap<String, List<Dimension>> dimensions;
    private final HashMap<String, List<Integer>> shapes;
    private final HashMap<String, Float> fillValues;

    public NetCDFReader(File file) {
        this.ncfile = open(file);

        variables = new HashMap<String, Variable>();
        units = new HashMap<String, String>();
        dimensions = new HashMap<String, List<Dimension>>();
        shapes = new HashMap<String, List<Integer>>();
        fillValues = new HashMap<String, Float>();

        List<Variable> vars = ncfile.getVariables();
        List<Dimension> dims = ncfile.getDimensions();

        for (Variable v : vars) {
            String name = v.getFullName();

            boolean variableIsActuallyADimension = false;
            for (Dimension d : dims) {
                if (d.getName().compareTo(name) == 0) {
                    variableIsActuallyADimension = true;
                }
            }

            ArrayList<Integer> shape = new ArrayList<Integer>();
            for (int i : v.getShape()) {
                shape.add(i);
            }
            shapes.put(name, shape);

            if (!variableIsActuallyADimension) {
                variables.put(name, v);
                units.put(name, v.getUnitsString());

                for (Attribute a : v.getAttributes()) {
                    if (a.getName().compareTo("_FillValue") == 0) {
                        float fillValue = a.getNumericValue().floatValue();
                        fillValues.put(name, fillValue);
                    }
                }
            } else {
                dimensions.put(name, v.getDimensions());
            }
        }
    }

    public int getAvailableFrames(String varName) {
        return shapes.get(varName).get(0);
    }

    public int getAvailableFrames() {
        int value = -1;
        for (Entry<String, List<Integer>> shapeEntry : shapes.entrySet()) {
            String name = shapeEntry.getKey();
            if (name.compareTo("time") == 0) {
                List<Integer> shape = shapeEntry.getValue();
                for (int i : shape) {
                    value = i;
                }
            }
        }

        return value;
    }

    public int getLatSize() {
        int value = -1;
        for (Entry<String, List<Integer>> shapeEntry : shapes.entrySet()) {
            String name = shapeEntry.getKey();
            if (name.compareTo("lat") == 0) {
                List<Integer> shape = shapeEntry.getValue();
                for (int i : shape) {
                    value = i;
                }
            }
        }

        return value;
    }

    public int getLonSize() {
        int value = -1;
        for (Entry<String, List<Integer>> shapeEntry : shapes.entrySet()) {
            String name = shapeEntry.getKey();
            if (name.compareTo("lon") == 0) {
                List<Integer> shape = shapeEntry.getValue();
                for (int i : shape) {
                    value = i;
                }
            }
        }

        return value;
    }

    public ArrayList<String> getVariableNames() {
        ArrayList<String> result = new ArrayList<String>();
        for (String s : variables.keySet()) {
            result.add(s);
            System.out.println(s);
        }
        return result;
    }

    public String getUnits(String varName) {
        return units.get(varName);
    }

    public ByteBuffer getImage(String colorMapname, String variableName, int time) {
        Variable variable = variables.get(variableName);
        // int times = shapes.get(variableName).get(0);
        int lats = shapes.get(variableName).get(1);
        int lons = shapes.get(variableName).get(2);

        logger.debug("creating image size: " + lats + "x" + lons);

        ByteBuffer result = Buffers.newDirectByteBuffer(lats * lons * 4);
        result.rewind();

        try {
            Array netCDFArray = variable.slice(0, time).read();
            float[] data = (float[]) netCDFArray.get1DJavaArray(float.class);

            Dimensions colormapDims = new Dimensions(settings.getCurrentVarMin(variableName),
                    settings.getCurrentVarMax(variableName));

            for (int lat = 0; lat < lats; lat++) {
                for (int lon = lons - 1; lon >= 0; lon--) {
                    Color color = ColormapInterpreter.getColor(colorMapname, colormapDims, data[lat * lons + lon],
                            fillValues.get(variableName));
                    result.put((byte) (color.getRed() * 255));
                    result.put((byte) (color.getGreen() * 255));
                    result.put((byte) (color.getBlue() * 255));
                    result.put((byte) 0);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidRangeException e) {
            e.printStackTrace();
        }

        result.rewind();

        return result;
    }

    @Override
    public String toString() {
        String result = "";
        for (String name : variables.keySet()) {
            result += "Variable: " + name + "\n";

            String dimensionNames = "";
            for (Dimension d : dimensions.get(name)) {
                dimensionNames += d.getName() + " ";
            }
            result += "Dims: " + dimensionNames + "\n";
            result += "Shape: " + shapes.get(name) + "\n";
        }
        return result;
    }

    private NetcdfFile open(File file) {
        NetcdfFile ncfile = null;
        try {
            ncfile = NetcdfFile.open(file.getAbsolutePath());
        } catch (IOException ioe) {
            logger.error("trying to open " + file.getAbsolutePath(), ioe);
        }
        return ncfile;
    }

    public void close() {
        try {
            this.ncfile.close();
        } catch (IOException ioe) {
            logger.error("trying to close " + ncfile.toString(), ioe);
        }
    }
}
