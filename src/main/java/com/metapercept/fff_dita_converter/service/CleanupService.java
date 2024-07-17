package com.metapercept.fff_dita_converter.service;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class CleanupService {

    public void cleanUpDirectories(String zipFilePath) throws IOException {
        // Define the directories to be cleaned
        String[] directories = {"uploads", "exports","temp", "output/finalOutput", "output/intermediate1", "output/intermediate2", "output/intermediate3", "output/tempOutput"};

        for (String dir : directories) {
            cleanDirectory(Paths.get(dir), zipFilePath);
        }
    }

    private void cleanDirectory(Path dirPath, String zipFilePath) throws IOException {
        File dir = dirPath.toFile();

        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    // Skip the zip file
                    if (file.getAbsolutePath().equals(zipFilePath)) {
                        continue;
                    }

                    if (file.isDirectory()) {
                        cleanDirectory(file.toPath(), zipFilePath);
                    }
                    Files.delete(file.toPath());
                }
            }
        }
    }
}
