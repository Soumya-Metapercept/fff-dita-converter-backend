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
    private static final String DATA_FOLDER = "output/finalOutput/data";

    public void fixDataHrefs(String dataJsonFilePath) throws IOException, InterruptedException, ExecutionException {
        long startTime = System.currentTimeMillis();

        JsonArray dataMappings = JsonParser.parseReader(new FileReader(dataJsonFilePath)).getAsJsonArray();
        Map<String, String> orgFileIdToDataFileNameMap = new HashMap<>();

        for (JsonElement element : dataMappings) {
            JsonObject obj = element.getAsJsonObject();

            String orgFileID = getStringFromJsonObject(obj, "orgFileID", true);
            String dataFileName = getStringFromJsonObject(obj, "dataFileName", false);

            if (orgFileID != null && dataFileName != null) {
                orgFileIdToDataFileNameMap.put(orgFileID, dataFileName);
            }
        }

        Map<String, Path> fileMap = Files.walk(Paths.get(MAIN_FOLDER))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".dita"))
                .collect(Collectors.toMap(
                        path -> path.getFileName().toString(),
                        path -> path
                ));

        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<?>> futures = new ArrayList<>();

        for (String orgFileID : orgFileIdToDataFileNameMap.keySet()) {
            futures.add(executorService.submit(() -> {
                String orgFileName = findFileNameById(fileMap, orgFileID);

                if (orgFileName != null) {
                    Path orgFilePath = fileMap.get(orgFileName);
                    updateDataHref(orgFilePath.toFile(), orgFileIdToDataFileNameMap.get(orgFileID));
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

    private void updateDataHref(File orgFile, String dataFileName) {
        try {
            String orgFileContent = Files.readString(orgFile.toPath());
            Document doc = Jsoup.parse(orgFileContent, "", Parser.xmlParser());
            boolean changesMade = false;

            Elements xrefTags = doc.select("pwc-xref[format]");
            for (Element xrefTag : xrefTags) {
                String format = xrefTag.attr("format");
                String href = xrefTag.attr("href");

                if (href.equals(dataFileName)) {
                    Path orgFilePath = orgFile.toPath().getParent();
                    Path dataPath = Paths.get(DATA_FOLDER, dataFileName);
                    Path relativePath = orgFilePath.relativize(dataPath);

                    xrefTag.attr("href", relativePath.toString().replace("\\", "/"));
                    changesMade = true;
                }
            }

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
