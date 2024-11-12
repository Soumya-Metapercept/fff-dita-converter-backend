// File: src/main/java/com/metapercept/fff_dita_converter/service/PopupFixerService.java

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
public class PopupFixerService {
    private static final String MAIN_FOLDER = "output/finalOutput";

    public void fixPopups(String jsonFilePath) throws IOException, InterruptedException, ExecutionException {
        long startTime = System.currentTimeMillis();

        // Read the JSON file
        JsonObject popupData = JsonParser.parseReader(new FileReader(jsonFilePath)).getAsJsonObject();

        // Create mappings for <div> and <a> data
        Map<String, String> divMap = new HashMap<>();
        Map<String, JsonObject> aTagData = new HashMap<>();
        Set<String> relevantFileIds = new HashSet<>();

        JsonArray divArray = popupData.getAsJsonArray("div");
        for (JsonElement element : divArray) {
            JsonObject obj = element.getAsJsonObject();
            String tagId = obj.get("tagId").getAsString();
            String orgFileId = obj.get("orgFileId").getAsString().replace("#", "");  // Remove '#' from orgFileId
            divMap.put(tagId, orgFileId);
            relevantFileIds.add(orgFileId);  // Track relevant files by orgFileId
        }

        JsonArray aArray = popupData.getAsJsonArray("a");
        for (JsonElement element : aArray) {
            JsonObject obj = element.getAsJsonObject();
            String refId = obj.get("refId").getAsString().replace("#", "");  // Remove '#' from refId
            obj.addProperty("refParentId", obj.get("refParentId").getAsString().replace("#", ""));  // Remove '#' from refParentId
            aTagData.put(refId, obj);
            relevantFileIds.add(obj.get("refParentId").getAsString());  // Track relevant files by refParentId
        }

        // Collect paths of only relevant .dita files based on relevantFileIds
        Map<String, Path> fileMap = Files.walk(Paths.get(MAIN_FOLDER))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".dita"))
                .filter(path -> {
                    String fileName = path.getFileName().toString();
                    return relevantFileIds.stream().anyMatch(fileName::contains);
                })
                .collect(Collectors.toMap(
                        path -> path.getFileName().toString(),
                        path -> path
                ));

        // Use ExecutorService to process only relevant files concurrently
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<?>> futures = new ArrayList<>();

        for (Path ditaFilePath : fileMap.values()) {
            futures.add(executorService.submit(() -> {
                try {
                    System.out.println("Processing file: " + ditaFilePath.getFileName());
                    updatePopups(ditaFilePath.toFile(), aTagData, divMap, fileMap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }));
        }

        for (Future<?> future : futures) {
            future.get();
        }

        executorService.shutdown();

        long endTime = System.currentTimeMillis();
        System.out.println("Total time taken: " + (endTime - startTime) / 1000 + " seconds.");
    }

    private Path findFilePathById(Map<String, Path> fileMap, String id) {
        return fileMap.values().stream()
                .filter(path -> path.getFileName().toString().endsWith("_" + id + ".dita"))
                .findFirst()
                .orElse(null);
    }

    private void updatePopups(File ditaFile, Map<String, JsonObject> aTagData, Map<String, String> divMap, Map<String, Path> fileMap) throws IOException {
        // Read the DITA file content
        String fileContent = Files.readString(ditaFile.toPath());
        Document doc = Jsoup.parse(fileContent, "", Parser.xmlParser());
        boolean changesMade = false;

        // Get the pwc-topic id of the current DITA file
        Element pwcTopic = doc.selectFirst("pwc-topic");
        String pwcTopicId = pwcTopic != null ? pwcTopic.id() : "";

        // Find and update all <pwc-xref> tags with href matching refId in aTagData
        Elements xrefTags = doc.select("pwc-xref[href]");
        for (Element xrefTag : xrefTags) {
            String href = xrefTag.attr("href");
            String currentRefFileId = href.contains("#") ? href.substring(href.indexOf("#") + 1) : "";

            JsonObject aTagInfo = aTagData.get(currentRefFileId);
            if (aTagInfo != null) {
                String refParentId = aTagInfo.get("refParentId").getAsString();
                String refContent = aTagInfo.get("refContent").getAsString();

                Path refFilePath = findFilePathById(fileMap, refParentId);
                if (refFilePath != null) {
                    // Parse the referenced file to find pwc-topic id
                    Document refDoc = Jsoup.parse(Files.readString(refFilePath), "", Parser.xmlParser());
                    Element refTopicTag = refDoc.selectFirst("pwc-topic");
                    String refTopicId = refTopicTag != null ? refTopicTag.id() : "";

                    String newHref;
                    if (ditaFile.equals(refFilePath.toFile())) {
                        newHref = ditaFile.getName() + "#" + refTopicId + "/" + currentRefFileId;
                    } else {
                        Path relativePath = ditaFile.toPath().getParent().relativize(refFilePath);
                        newHref = relativePath.toString().replace("\\", "/") + "#" + refTopicId + "/" + currentRefFileId;
                    }

                    // Log the change being made to the <pwc-xref> tag
                    System.out.println("Updating <pwc-xref> in file: " + ditaFile.getName());
                    System.out.println("Old href: " + href);
                    System.out.println("New href: " + newHref);
                    xrefTag.attr("href", newHref);
                    changesMade = true;
                }
            }
        }

        // Update all <fn> tags with matching tagId in divMap
        for (Map.Entry<String, String> entry : divMap.entrySet()) {
            String tagId = entry.getKey();
            String orgFileId = entry.getValue();

            Element fnTag = doc.selectFirst("fn[id='" + tagId + "']");
            if (fnTag != null && aTagData.containsKey(tagId)) {
                String refContent = aTagData.get(tagId).get("refContent").getAsString();

                System.out.println("Updating <fn> tag in file: " + ditaFile.getName());
                System.out.println("Old callout: " + fnTag.attr("callout"));
                System.out.println("New callout: " + refContent);
                fnTag.attr("callout", refContent);
                changesMade = true;
            }
        }

        // Write changes back to the file if any modifications were made
        if (changesMade) {
            System.out.println("Changes made to file: " + ditaFile.getName() + " - Saving changes.");
            try (FileWriter writer = new FileWriter(ditaFile)) {
                writer.write(doc.outerHtml());
            }
        } else {
            System.out.println("No changes made to file: " + ditaFile.getName());
        }
    }
}
