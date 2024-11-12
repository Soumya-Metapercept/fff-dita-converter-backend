package com.metapercept.fff_dita_converter.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class ImageHrefFixerService {
    private static final String MAIN_FOLDER = "output/finalOutput";
    private static final String IMAGE_FOLDER = "output/finalOutput/cpa/images";

    public void fixImageHrefs(String imageJsonFilePath) throws IOException, InterruptedException, ExecutionException {
        long startTime = System.currentTimeMillis();

        // Parse the image JSON file
        JsonArray imageMappings = JsonParser.parseReader(new FileReader(imageJsonFilePath)).getAsJsonArray();
        Map<String, List<String>> orgFileIdToImageFileNamesMap = new HashMap<>();

        // Organize image mappings (support multiple image files per orgFileID)
        for (JsonElement element : imageMappings) {
            JsonObject obj = element.getAsJsonObject();
            String orgFileID = getStringFromJsonObject(obj, "orgFileID", true);
            String imageFileName = getStringFromJsonObject(obj, "imageFileName", false);

            if (orgFileID != null && imageFileName != null) {
                orgFileIdToImageFileNamesMap.computeIfAbsent(orgFileID, k -> new ArrayList<>()).add(imageFileName);
            }
        }

        // Walk through DITA files
        Map<String, Path> fileMap = Files.walk(Paths.get(MAIN_FOLDER))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".dita"))
                .collect(Collectors.toMap(
                        path -> path.getFileName().toString(),
                        path -> path
                ));

        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<?>> futures = new ArrayList<>();

        // Process each orgFileID
        for (String orgFileID : orgFileIdToImageFileNamesMap.keySet()) {
            futures.add(executorService.submit(() -> {
                String orgFileName = findFileNameById(fileMap, orgFileID);

                if (orgFileName != null) {
                    Path orgFilePath = fileMap.get(orgFileName);
                    updateAllImageHrefs(orgFilePath.toFile(), orgFileIdToImageFileNamesMap.get(orgFileID));
                } else {
                    System.out.println("File not found with ID: " + orgFileID);
                }
            }));
        }

        for (Future<?> future : futures) {
            future.get();
        }

        executorService.shutdown();

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        System.out.println("Total time taken: " + (totalTime / 1000) + " seconds.");
    }

    private String findFileNameById(Map<String, Path> fileMap, String id) {
        return fileMap.keySet().stream()
                .filter(fileName -> fileName.endsWith("_" + id + ".dita"))
                .findFirst()
                .orElse(null);
    }

    private void updateAllImageHrefs(File orgFile, List<String> imageFileNames) {
        try {
            // Read the DITA file
            String orgFileContent = Files.readString(orgFile.toPath());
            Document doc = Jsoup.parse(orgFileContent, "", Parser.xmlParser());
            boolean changesMade = false;

            // Find all <image> tags
            Elements imageTags = doc.select("image[href]");

            // Update all <image> hrefs that match the imageFileNames
            for (Element imageTag : imageTags) {
                String href = imageTag.attr("href");

                for (String imageFileName : imageFileNames) {
                    if (href.equals(imageFileName)) {
                        // Calculate the relative path to the image file
                        Path orgFilePath = orgFile.toPath().getParent();
                        Path imagePath = Paths.get(IMAGE_FOLDER, imageFileName);
                        Path relativePath = orgFilePath.relativize(imagePath);

                        // Update the href attribute
                        imageTag.attr("href", relativePath.toString().replace("\\", "/"));
                        changesMade = true;
                    }
                }
            }

            // Write the updated document back to the DITA file if changes were made
            if (changesMade) {
                try (FileWriter writer = new FileWriter(orgFile)) {
                    writer.write(doc.outerHtml());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getStringFromJsonObject(JsonObject obj, String key, boolean removeHash) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) {
            return null;
        }
        String value = obj.get(key).getAsString();
        return removeHash && value.startsWith("#") ? value.substring(1) : value;
    }
}
