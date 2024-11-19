package com.metapercept.fff_dita_converter.service;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class CleanupService {

    public void cleanSpecificDirectory(String directory) throws IOException {
        File dir = new File(directory);
        if (dir.exists()) {
            Files.walk(dir.toPath())
                    .map(Path::toFile)
                    .sorted((o1, o2) -> -o1.compareTo(o2))
                    .forEach(File::delete);
            System.out.println("Cleaned directory: " + directory);
        }
    }

    public void cleanUpDirectoriesExclude(String excludeDir) {
        try {
            System.out.println("Starting cleanup...");
            String[] directories = {"uploads", "exports", "temp", "output"};

            for (String dir : directories) {
                if (!dir.equals(excludeDir)) {
                    cleanSpecificDirectory(dir);
                }
            }
            System.out.println("Cleanup completed.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
