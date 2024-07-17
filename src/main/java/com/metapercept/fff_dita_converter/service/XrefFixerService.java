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
public class XrefFixerService {
    private static final String MAIN_FOLDER = "output/finalOutput";

    public void fixXrefs(String jsonFilePath) throws IOException, InterruptedException, ExecutionException {
        long startTime = System.currentTimeMillis();

        // Read the JSON file
        JsonArray mappings = JsonParser.parseReader(new FileReader(jsonFilePath)).getAsJsonArray();

        // Create a map for fast lookup based on orgFileID
        Map<String, List<Map<String, String>>> orgFileIdToMappings = new HashMap<>();
        for (JsonElement element : mappings) {
            JsonObject obj = element.getAsJsonObject();
            String orgFileID = obj.get("orgFileID").getAsString().substring(1); // Remove the leading '#'
            String refFileId = obj.get("refFileId").getAsString().substring(1); // Remove the leading '#'
            String refFileName = obj.get("refFileName").getAsString();
            String refFileNo = obj.get("refFileNo.").getAsString().substring(1); // Remove the leading '#'

            Map<String, String> mapping = new HashMap<>();
            mapping.put("refFileId", refFileId);
            mapping.put("refFileName", refFileName);
            mapping.put("refFileNo", refFileNo);

            orgFileIdToMappings.computeIfAbsent(orgFileID, k -> new ArrayList<>()).add(mapping);
        }

        // Walk the file tree once and collect all relevant files
        Map<String, Path> fileMap = Files.walk(Paths.get(MAIN_FOLDER))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".dita"))
                .collect(Collectors.toMap(
                        path -> path.getFileName().toString(),
                        path -> path
                ));

        // Process each orgFileID
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<?>> futures = new ArrayList<>();

        for (String orgFileID : orgFileIdToMappings.keySet()) {
            futures.add(executorService.submit(() -> {
                // Find the orgFile based on orgFileID
                String orgFileName = findFileNameById(fileMap, orgFileID);

                if (orgFileName != null) {
                    Path orgFilePath = fileMap.get(orgFileName);
                    updateXref(orgFilePath.toFile(), orgFileIdToMappings.get(orgFileID), fileMap);
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

    private void updateXref(File orgFile, List<Map<String, String>> mappings, Map<String, Path> fileMap) {
        try {
            // Read the file content into a String
            String orgFileContent = Files.readString(orgFile.toPath());

            // Parse the orgFile content as XML to avoid adding unwanted html, head, and body tags
            Document doc = Jsoup.parse(orgFileContent, "", Parser.xmlParser());
            boolean changesMade = false;

            // Find all <pwc-xref> tags with href attributes
            Elements xrefTags = doc.select("pwc-xref[href]");
            for (Element xrefTag : xrefTags) {
                String href = xrefTag.attr("href");
                String currentRefFileId = href.substring(href.indexOf("#") + 1);

                for (Map<String, String> mapping : mappings) {
                    if (currentRefFileId.equals(mapping.get("refFileId"))) {
                        // Find the refFile based on refFileNo
                        String refFileName = findFileNameById(fileMap, mapping.get("refFileNo"));

                        if (refFileName != null) {
                            Path refFilePath = fileMap.get(refFileName);

                            // Read the refFile content into a String
                            String refFileContent = Files.readString(refFilePath);

                            // Parse the refFile content to find the pwc-topic tag with matching id only to refFileId
                            Document refDoc = Jsoup.parse(refFileContent, "", Parser.xmlParser());
                            Element pwcTopicTag = refDoc.selectFirst("pwc-topic[id=" + mapping.get("refFileId") + "]");

                            String newHref;
                            if (pwcTopicTag != null) {
                                // If id matches refFileId
                                newHref = refFilePath.toString().replace("\\", "/") + "#" + pwcTopicTag.attr("id");
                            } else {
                                // If no match, append refFileId at the end
                                pwcTopicTag = refDoc.selectFirst("pwc-topic");
                                newHref = refFilePath.toString().replace("\\", "/") + "#" + (pwcTopicTag != null ? pwcTopicTag.attr("id") + "/" : "") + mapping.get("refFileId");
                            }

                            // Get the relative path
                            Path orgFilePath = orgFile.toPath().getParent();
                            Path relativePath = orgFilePath.relativize(Paths.get(newHref));

                            // Log the change
                            String oldHref = xrefTag.attr("href");
                            String updatedHref = relativePath.toString().replace("\\", "/");
                            System.out.println("File: " + orgFile.getName());
                            System.out.println("Updated <pwc-xref> tag: ");
                            System.out.println("Old href: " + oldHref);
                            System.out.println("New href: " + updatedHref);

                            // Update the href attribute
                            xrefTag.attr("href", updatedHref);
                            changesMade = true;
                            break;
                        }
                    }
                }
            }

            // Write the updated document back to the orgFile if changes were made
            if (changesMade) {
                // Write the updated document back to the orgFile, maintaining the original XML format
                try (FileWriter writer = new FileWriter(orgFile)) {
                    writer.write(doc.outerHtml());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
