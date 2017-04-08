package com.scipianus.pocketlibrary.utils;

import android.content.res.AssetManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by scipianus on 08-Apr-17.
 */

public class FileUtils {
    public static void transferDataFile(AssetManager assetManager, File dir, String inputPath, String outputPath) {
        if (!dir.exists() && dir.mkdirs()) {
            copyDataFile(assetManager, inputPath, outputPath);
        }
        if (dir.exists()) {
            File file = new File(outputPath);

            if (!file.exists()) {
                copyDataFile(assetManager, inputPath, outputPath);
            }
        }
    }

    private static void copyDataFile(AssetManager assetManager, String inputPath, String outputPath) {
        try {
            InputStream inputStream = assetManager.open(inputPath);
            OutputStream outputStream = new FileOutputStream(outputPath);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }

            outputStream.flush();
            outputStream.close();
            inputStream.close();

            File file = new File(outputPath);
            if (!file.exists()) {
                throw new FileNotFoundException();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
