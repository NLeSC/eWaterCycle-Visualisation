package nl.esciencecenter.visualization.ewatercycle;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class CacheFileManager {
    private final File cacheFile;

    public CacheFileManager(String path) {
        cacheFile = new File(path + File.separator + ".visualizationCache");
    }

    public float readMin(String variableName) {
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

    public void writeMin(String variableName, float value) {
        if (!cacheFile.exists()) {
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

    public float readMax(String variableName) {
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

    public void writeMax(String variableName, float value) {
        if (!cacheFile.exists()) {
            try {
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

    public String readColormap(String variableName) {
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

    public void writeColormap(String variableName, String value) {
        try {
            if (!cacheFile.exists()) {
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
