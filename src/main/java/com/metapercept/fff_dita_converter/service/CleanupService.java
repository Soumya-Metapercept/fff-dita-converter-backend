package com.metapercept.fff_dita_converter.service;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class CleanupService {
public void cleanUpDirectories(String zipFilePath) {
    try {
        System.out.println("Starting cleanup...");

        // Define directories to be cleaned
        String[] directories = {"uploads", "exports", "temp", "output"};

        for (String dir : directories) {
            File directory = new File(dir);
            if (directory.exists()) {
                Files.walk(directory.toPath())
                        .map(Path::toFile)
                        .sorted((o1, o2) -> -o1.compareTo(o2)) // Sort to delete files before directories
                        .forEach(File::delete);
                System.out.println("Deleted directory: " + dir);
            }
        }

        // Delete the zip file if specified
        if (zipFilePath != null) {
            Files.deleteIfExists(Paths.get(zipFilePath));
            System.out.println("Deleted zip file: " + zipFilePath);
        }

        System.out.println("Cleanup completed.");
    } catch (IOException e) {
        e.printStackTrace();
    }
}
}
