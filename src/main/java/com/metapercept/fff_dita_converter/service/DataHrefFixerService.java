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
public class DataHrefFixerService {
    private static final String MAIN_FOLDER = "output/finalOutput";
    private static final String DATA_FOLDER = "output/finalOutput/cpa/data";

    public void fixDataHrefs(String dataJsonFilePath) throws IOException, InterruptedException, ExecutionException {
        long startTime = System.currentTimeMillis();

        // Parse the data JSON file
        JsonArray dataMappings = JsonParser.parseReader(new FileReader(dataJsonFilePath)).getAsJsonArray();
        Map<String, List<String>> orgFileIdToDataFileNamesMap = new HashMap<>();

        // Organize data mappings (support multiple data files per orgFileID)
        for (JsonElement element : dataMappings) {
            JsonObject obj = element.getAsJsonObject();
            String orgFileID = getStringFromJsonObject(obj, "orgFileID", true);
            String dataFileName = getStringFromJsonObject(obj, "dataFileName", false);

            if (orgFileID != null && dataFileName != null) {
                orgFileIdToDataFileNamesMap.computeIfAbsent(orgFileID, k -> new ArrayList<>()).add(dataFileName);
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
        for (String orgFileID : orgFileIdToDataFileNamesMap.keySet()) {
            futures.add(executorService.submit(() -> {
                String orgFileName = findFileNameById(fileMap, orgFileID);

                if (orgFileName != null) {
                    Path orgFilePath = fileMap.get(orgFileName);
                    updateAllDataHrefs(orgFilePath.toFile(), orgFileIdToDataFileNamesMap.get(orgFileID));
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

    private void updateAllDataHrefs(File orgFile, List<String> dataFileNames) {
        try {
            // Read the DITA file
            String orgFileContent = Files.readString(orgFile.toPath());
            Document doc = Jsoup.parse(orgFileContent, "", Parser.xmlParser());
            boolean changesMade = false;

            // Find all <pwc-xref> tags with the 'format' attribute
            Elements xrefTags = doc.select("pwc-xref[format]");

            // Update all <pwc-xref> hrefs that match the dataFileNames
            for (Element xrefTag : xrefTags) {
                String href = xrefTag.attr("href");

                for (String dataFileName : dataFileNames) {
                    if (href.equals(dataFileName)) {
                        // Calculate the relative path to the data file
                        Path orgFilePath = orgFile.toPath().getParent();
                        Path dataPath = Paths.get(DATA_FOLDER, dataFileName);
                        Path relativePath = orgFilePath.relativize(dataPath);

                        // Update the href attribute
                        xrefTag.attr("href", relativePath.toString().replace("\\", "/"));
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
