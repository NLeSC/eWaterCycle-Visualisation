package nl.esciencecenter.visualization.ewatercycle;

/*
 * JOCL - Java bindings for OpenCL
 * 
 * Copyright 2009 Marco Hutter - http://www.jocl.org/
 */

import static org.jocl.CL.CL_CONTEXT_PLATFORM;
import static org.jocl.CL.CL_DEVICE_TYPE_ALL;
import static org.jocl.CL.CL_MEM_READ_WRITE;
import static org.jocl.CL.CL_MEM_WRITE_ONLY;
import static org.jocl.CL.CL_TRUE;
import static org.jocl.CL.clBuildProgram;
import static org.jocl.CL.clCreateBuffer;
import static org.jocl.CL.clCreateCommandQueue;
import static org.jocl.CL.clCreateContext;
import static org.jocl.CL.clCreateKernel;
import static org.jocl.CL.clCreateProgramWithSource;
import static org.jocl.CL.clEnqueueNDRangeKernel;
import static org.jocl.CL.clEnqueueReadBuffer;
import static org.jocl.CL.clEnqueueWriteBuffer;
import static org.jocl.CL.clGetDeviceIDs;
import static org.jocl.CL.clGetPlatformIDs;
import static org.jocl.CL.clSetKernelArg;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import nl.esciencecenter.neon.swing.ColormapInterpreter.Dimensions;

import org.jocl.CL;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_context_properties;
import org.jocl.cl_device_id;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;
import org.jocl.cl_platform_id;
import org.jocl.cl_program;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class that uses a simple OpenCL kernel to compute the Mandelbrot set and
 * displays it in an image
 */
public class JOCLColormapper {
    private final static Logger logger = LoggerFactory.getLogger(JOCLColormapper.class);

    /**
     * Extension filter for the filtering of filenames in directory structures.
     * 
     * @author Maarten van Meersbergen <m.van.meersbergen@esciencecenter.nl>
     * 
     */
    static class ExtFilter implements FilenameFilter {
        private final String ext;

        /**
         * Basic constructor for ExtFilter.
         * 
         * @param ext
         *            The extension to filter for.
         */
        public ExtFilter(String ext) {
            this.ext = ext;
        }

        @Override
        public boolean accept(File dir, String name) {
            return (name.endsWith(ext));
        }
    }

    /** Storage for the colormaps. */
    private static Map<String, int[]> colorMaps;

    /**
     * The image which will contain the Mandelbrot pixel data
     */
    // private final BufferedImage image;

    /**
     * The width of the image
     */
    private int width = 0;

    /**
     * The height of the image
     */
    private int height = 0;

    /**
     * The OpenCL context
     */
    private cl_context context;

    /**
     * The OpenCL command queue
     */
    private cl_command_queue commandQueue;

    /**
     * The OpenCL kernel which will actually compute the Mandelbrot set and
     * store the pixel data in a CL memory object
     */
    private cl_kernel kernel;

    /**
     * The OpenCL memory object which stores the pixel data
     */
    private cl_mem outputMem;

    /**
     * The OpenCL memory object which stores the pixel data
     */
    private cl_mem dataMem;

    /**
     * An OpenCL memory object which stores a nifty color map, encoded as
     * integers combining the RGB components of the colors.
     */
    private cl_mem colorMapMem;

    //
    // /**
    // * The color map which will be copied to OpenCL for filling the PBO.
    // */
    // private int colorMap[];
    //
    // private int colormapLength;

    /**
     * Creates the JOCLSimpleMandelbrot sample with the given width and height
     */
    public JOCLColormapper(int width, int height) {
        this.width = width;
        this.height = height;

        // Create the image and the component that will paint the image
        // image = new BufferedImage(sizeX, sizeY, BufferedImage.TYPE_INT_RGB);

        // Initialize OpenCL
        initCL(width, height);

        // Initial image update
        // updateImage();
    }

    /**
     * Initialize OpenCL: Create the context, the command queue and the kernel.
     */
    private void initCL(int width, int height) {
        final int platformIndex = 0;
        final long deviceType = CL_DEVICE_TYPE_ALL;
        final int deviceIndex = 0;

        // Enable exceptions and subsequently omit error checks in this sample
        CL.setExceptionsEnabled(true);

        // Obtain the number of platforms
        int numPlatformsArray[] = new int[1];
        clGetPlatformIDs(0, null, numPlatformsArray);
        int numPlatforms = numPlatformsArray[0];

        // Obtain a platform ID
        cl_platform_id platforms[] = new cl_platform_id[numPlatforms];
        clGetPlatformIDs(platforms.length, platforms, null);
        cl_platform_id platform = platforms[platformIndex];

        // Initialize the context properties
        cl_context_properties contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform);

        // Obtain the number of devices for the platform
        int numDevicesArray[] = new int[1];
        clGetDeviceIDs(platform, deviceType, 0, null, numDevicesArray);
        int numDevices = numDevicesArray[0];

        // Obtain a device ID
        cl_device_id devices[] = new cl_device_id[numDevices];
        clGetDeviceIDs(platform, deviceType, numDevices, devices, null);
        cl_device_id device = devices[deviceIndex];

        // Create a context for the selected device
        context = clCreateContext(contextProperties, 1, new cl_device_id[] { device }, null, null, null);

        // Create a command-queue for the selected device
        commandQueue = clCreateCommandQueue(context, device, 0, null);

        // Program Setup
        String source = readFile("kernels/Colormapper.cl");

        // Create the program
        cl_program cpProgram = clCreateProgramWithSource(context, 1, new String[] { source }, null, null);

        // Build the program
        clBuildProgram(cpProgram, 0, null, "-cl-mad-enable", null, null);

        // Create the kernel
        kernel = clCreateKernel(cpProgram, "mapColors", null);

        // Create and fill the memory object containing the color maps
        rebuildMaps();
    }

    /**
     * Rebuilds (and re-reads) the storage of colormaps. Outputs succesfully
     * read colormap names to the command line.
     */
    private int rebuildMaps() {
        int entries = 0;
        colorMaps = new HashMap<String, int[]>();

        try {
            String[] colorMapFileNames = getColorMaps();
            for (String fileName : colorMapFileNames) {
                ArrayList<Color> colorList = new ArrayList<Color>();

                BufferedReader in = new BufferedReader(new FileReader("colormaps/" + fileName + ".ncmap"));
                String str;

                while ((str = in.readLine()) != null) {
                    String[] numbers = str.split(" ");
                    colorList.add(new Color(Integer.parseInt(numbers[0]), Integer.parseInt(numbers[1]), Integer
                            .parseInt(numbers[2]), 255));
                }

                in.close();

                if (entries > 0) {
                    if (entries != colorList.size()) {
                        throw new IOException("colormaps not equally sized! " + fileName + " is the offender.");
                    }
                } else {
                    entries = colorList.size();
                }

                colorMaps.put(fileName, initColorMap(colorList));
                logger.info("Colormap " + fileName + " registered for use.");
            }

        } catch (IOException e) {
            logger.error(e.getMessage());
        }

        return entries;
    }

    /**
     * Getter for the list of colormap names in the directory. Used to load
     * these maps.
     * 
     * @return the array containing all of the colormap names in the directory.
     *         These are unchecked.
     */
    private static String[] getColorMaps() {
        final String[] ls = new File("colormaps").list(new ExtFilter("ncmap"));
        final String[] result = new String[ls.length];

        for (int i = 0; i < ls.length; i++) {
            result[i] = ls[i].split("\\.")[0];
        }

        return result;
    }

    /**
     * Helper function which reads the file with the given name and returns the
     * contents of this file as a String. Will exit the application if the file
     * can not be read.
     * 
     * @param fileName
     *            The name of the file to read.
     * @return The contents of the file
     */
    private String readFile(String fileName) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
            StringBuffer sb = new StringBuffer();
            String line = null;
            while (true) {
                line = br.readLine();
                if (line == null) {
                    break;
                }
                sb.append(line).append("\n");
            }
            br.close();
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }

    // /**
    // * Creates the colorMap array which contains RGB colors as integers,
    // * interpolated through the given colors with colors.length * stepSize
    // steps
    // *
    // * @param stepSize
    // * The number of interpolation steps between two colors
    // * @param colors
    // * The colors for the map
    // */
    // public int[] initColorMap(int stepSize, ArrayList<Color> colors) {
    // int[] colorMap = new int[stepSize * colors.size()];
    // int index = 0;
    // for (int i = 0; i < colors.size() - 1; i++) {
    // Color c0 = colors.get(i);
    // int r0 = c0.getRed();
    // int g0 = c0.getGreen();
    // int b0 = c0.getBlue();
    //
    // Color c1 = colors.get(i + 1);
    // int r1 = c1.getRed();
    // int g1 = c1.getGreen();
    // int b1 = c1.getBlue();
    //
    // int dr = r1 - r0;
    // int dg = g1 - g0;
    // int db = b1 - b0;
    //
    // for (int j = 0; j < stepSize; j++) {
    // float alpha = (float) j / (stepSize - 1);
    // int r = (int) (r0 + alpha * dr);
    // int g = (int) (g0 + alpha * dg);
    // int b = (int) (b0 + alpha * db);
    // int rgba = 255 | r >> 8 | g >> 16 | b >> 24;
    // colorMap[index++] = rgba;
    // }
    // }
    // return colorMap;
    // }

    /**
     * Creates the colorMap array which contains RGB colors as integers,
     * interpolated through the given colors with colors.length * stepSize steps
     * 
     * @param stepSize
     *            The number of interpolation steps between two colors
     * @param colors
     *            The colors for the map
     */
    public int[] initColorMap(ArrayList<Color> colors) {
        int[] colorMap = new int[colors.size()];
        for (int i = 0; i < colors.size(); i++) {
            Color c0 = colors.get(i);
            int r = c0.getRed();
            int g = c0.getGreen();
            int b = c0.getBlue();
            int abgr = (255 << 24) | (b << 16) | (g << 8) | r << 0;
            colorMap[i] = abgr;
        }
        return colorMap;
    }

    /**
     * Execute the kernel function and read the resulting pixel data into the
     * BufferedImage
     */
    public synchronized int[] getImage(String colormapName, Dimensions dim, float[] data, float fillValue) {
        // select colormap and write to GPU
        int[] colorMap = colorMaps.get(colormapName);
        if (colorMap == null) {
            System.out.println("Non-existing colormap selected: " + colormapName);
        }
        if (colorMapMem == null) {
            colorMapMem = clCreateBuffer(context, CL_MEM_READ_WRITE, colorMap.length * Sizeof.cl_uint, null, null);
        }
        clEnqueueWriteBuffer(commandQueue, colorMapMem, CL_TRUE, 0, colorMap.length * Sizeof.cl_uint,
                Pointer.to(colorMap), 0, null, null);

        // write data to GPU
        if (dataMem == null) {
            dataMem = clCreateBuffer(context, CL_MEM_READ_WRITE, width * height * Sizeof.cl_float, null, null);
        }
        clEnqueueWriteBuffer(commandQueue, dataMem, CL_TRUE, 0, width * height * Sizeof.cl_float, Pointer.to(data), 0,
                null, null);

        // Define the output buffer
        if (outputMem == null) {
            outputMem = clCreateBuffer(context, CL_MEM_WRITE_ONLY, width * height * Sizeof.cl_uint, null, null);
        }

        // Set work size
        long globalWorkSize[] = new long[2];
        globalWorkSize[0] = width;
        globalWorkSize[1] = height;

        // load uniforms
        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(dataMem));
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(outputMem));
        clSetKernelArg(kernel, 2, Sizeof.cl_mem, Pointer.to(colorMapMem));
        clSetKernelArg(kernel, 3, Sizeof.cl_uint, Pointer.to(new int[] { width }));
        clSetKernelArg(kernel, 4, Sizeof.cl_uint, Pointer.to(new int[] { height }));
        clSetKernelArg(kernel, 5, Sizeof.cl_float, Pointer.to(new float[] { fillValue }));
        clSetKernelArg(kernel, 6, Sizeof.cl_float, Pointer.to(new float[] { dim.getMin() }));
        clSetKernelArg(kernel, 7, Sizeof.cl_float, Pointer.to(new float[] { dim.getMax() }));
        clSetKernelArg(kernel, 8, Sizeof.cl_uint, Pointer.to(new int[] { 0 << 24 | 0 << 16 | 0 << 8 | 0 << 0 }));
        clSetKernelArg(kernel, 9, Sizeof.cl_uint, Pointer.to(new int[] { colorMap.length }));

        // and execute the kernel
        clEnqueueNDRangeKernel(commandQueue, kernel, 2, null, globalWorkSize, null, 0, null, null);

        // Read the output pixel data into the array
        int pixels[] = new int[width * height];
        clEnqueueReadBuffer(commandQueue, outputMem, CL_TRUE, 0, width * height * Sizeof.cl_uint, Pointer.to(pixels),
                0, null, null);

        return pixels;
    }

}