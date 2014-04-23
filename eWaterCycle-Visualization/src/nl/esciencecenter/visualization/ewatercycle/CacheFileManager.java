package nl.esciencecenter.visualization.ewatercycle;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheFileManager {
    private final static Logger logger = LoggerFactory.getLogger(CacheFileManager.class);
    private final File cacheFile;

    public CacheFileManager(String path) {
        logger.debug("User dir: " + System.getProperty("user.dir"));
        logger.debug("Cache Used dir: " + path);
        cacheFile = new File(path + File.separator + ".visualizationCache");
    }

    public synchronized float readMin(String variableName) {
        float result = Float.NaN;

        // Check if we have made a cacheFileManager file earlier
        if (cacheFile.exists()) {
            BufferedReader in;
            String str;

            try {
                in = new BufferedReader(new FileReader(cacheFile));
                while ((str = in.readLine()) != null) {
                    if (str.contains(variableName) && str.contains("min")) {
                        String[] substrings = str.split(" ");
                        result = Float.parseFloat(substrings[2]);
                    }
                }
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    public synchronized void writeMin(String variableName, float value) {
        if (!cacheFile.exists()) {
            cacheFile.getParentFile().mkdirs();
            try {
                cacheFile.createNewFile();
            } catch (IOException e) {

                e.printStackTrace();
            }
        }

        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(cacheFile, true)));
            out.println(variableName + " min " + value);

            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized float readMax(String variableName) {
        float result = Float.NaN;

        // Check if we have made a cacheFileManager file earlier
        if (cacheFile.exists()) {
            BufferedReader in;
            String str;

            try {
                in = new BufferedReader(new FileReader(cacheFile));
                while ((str = in.readLine()) != null) {
                    if (str.contains(variableName) && str.contains("max")) {
                        String[] substrings = str.split(" ");
                        result = Float.parseFloat(substrings[2]);
                    }
                }
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    public synchronized void writeMax(String variableName, float value) {
        if (!cacheFile.exists()) {
            try {
                cacheFile.getParentFile().mkdirs();
                cacheFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(cacheFile, true)));
            out.println(variableName + " max " + value);

            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized String readColormap(String variableName) {
        String result = "";

        // Check if we have made a cacheFileManager file earlier
        if (cacheFile.exists()) {
            BufferedReader in;
            String str;

            try {
                in = new BufferedReader(new FileReader(cacheFile));
                while ((str = in.readLine()) != null) {
                    if (str.contains(variableName) && str.contains("colormap")) {
                        String[] substrings = str.split(" ");
                        result = substrings[2];
                    }
                }
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    public synchronized void writeColormap(String variableName, String value) {
        try {
            if (!cacheFile.exists()) {
                cacheFile.getParentFile().mkdirs();
                cacheFile.createNewFile();
            }

            // if (readColormap(variableName).compareTo("") != 0) {
            // BufferedReader in;
            // String str;
            //
            // PrintWriter out = new PrintWriter(new BufferedWriter(new
            // FileWriter(cacheFile, false)));
            //
            // in = new BufferedReader(new FileReader(cacheFile));
            // while ((str = in.readLine()) != null) {
            // String toBeWritten = str;
            // if (str.contains(variableName) && str.contains("colormap")) {
            // String[] substrings = str.split(" ");
            // toBeWritten = substrings[0] + " colormap " + value;
            // }
            // out.println(toBeWritten);
            // }
            // in.close();
            //
            // out.close();
            // } else {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(cacheFile, true)));
            out.println(variableName + " colormap " + value);
            out.close();
            // }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
