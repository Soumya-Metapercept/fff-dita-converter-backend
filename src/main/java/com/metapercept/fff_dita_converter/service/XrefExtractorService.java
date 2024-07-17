package com.metapercept.fff_dita_converter.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.metapercept.fff_dita_converter.config.SharedConfig;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Service
public class XrefExtractorService {

    private static final String TEMP_DIR = "temp/";

    private final SharedConfig sharedConfig;

    @Autowired
    public XrefExtractorService(SharedConfig sharedConfig) {
        this.sharedConfig = sharedConfig;
    }

    public String extractXrefs() throws IOException {

        String outputFilePath = TEMP_DIR + "output.json";

        // Ensure the temporary directory exists
        Files.createDirectories(Paths.get(TEMP_DIR));

        // Get the myconfig.xml file path from SharedConfig
        String myconfigXmlFilePath = sharedConfig.getMyConfigXmlFilePath();
        if (myconfigXmlFilePath == null || myconfigXmlFilePath.isEmpty()) {
            throw new IOException("myconfig.xml file path is not set.");
        }
        File myconfigXmlFile = new File(myconfigXmlFilePath);

        try {
            // Parse the XML file
            Document doc = Jsoup.parse(myconfigXmlFile, "UTF-8");

            // Build a lookup table for <a> tags by their id attribute and corresponding <file> attributes
            Map<String, FileAttributes> idToFileAttributesMap = new HashMap<>();
            Elements fileElements = doc.select("file");
            for (Element fileElement : fileElements) {
                String uriFragment = fileElement.attr("uri_fragment");
                String fileHeading = fileElement.attr("heading");
                Elements aTags = fileElement.select("a[id]");
                for (Element aTag : aTags) {
                    String id = aTag.id();
                    idToFileAttributesMap.put(id, new FileAttributes(uriFragment, fileHeading));
                }
            }

            // Prepare a JSON array to hold the results
            JsonArray jsonArray = new JsonArray();

            // Find all <a> tags with class="Jump"
            Elements jumpLinks = doc.select("a.Jump");

            for (Element link : jumpLinks) {
                String href = link.attr("href");

                // Traverse up the tree to find the nearest ancestor <file> tag and get its heading attribute
                Element parent = link.parent();
                String orgFileName = "";
                String orgFileID = "";
                while (parent != null) {
                    if (parent.tagName().equals("file")) {
                        orgFileName = parent.attr("heading");
                        orgFileID = parent.attr("uri_fragment");
                        break;
                    }
                    parent = parent.parent();
                }

                // Split the href to get the required parts
                String refFileId = href.substring(href.indexOf("#") + 1);
                String refFileNo = "";
                String refFileName = "";

                // Log the href and extracted parts
                System.out.println("Processing <a> tag with href: " + href);
                System.out.println("Extracted refFileId: " + refFileId);

                // Find the <a> tag with id=refFileId and get its preceding <file> tag's attributes
                FileAttributes fileAttributes = idToFileAttributesMap.get(refFileId);
                if (fileAttributes != null) {
                    refFileNo = fileAttributes.uriFragment;
                    refFileName = fileAttributes.fileHeading;
                    System.out.println("Found matching <file> tag with uri_fragment: " + refFileNo);
                    System.out.println("Found matching <file> tag with heading: " + refFileName);
                } else {
                    System.out.println("No <file> tag found for refFileId: " + refFileId);
                }

                // Log the found refFileNo and refFileName
                System.out.println("Extracted refFileNo: " + refFileNo);
                System.out.println("Extracted refFileName: " + refFileName);

                // Create a JSON object for each link
                JsonObject jsonObject = new JsonObject();
                jsonObject.add("orgFileName", new JsonPrimitive(orgFileName));
                jsonObject.add("orgFileID", new JsonPrimitive(orgFileID));
                jsonObject.add("refFileName", new JsonPrimitive(refFileName));
                jsonObject.add("refFileId", new JsonPrimitive("#" + refFileId));
                jsonObject.add("refFileNo.", new JsonPrimitive(refFileNo));

                // Add the JSON object to the JSON array
                jsonArray.add(jsonObject);
            }

            // Write the JSON array to the output file
            try (FileWriter fileWriter = new FileWriter(outputFilePath)) {
                Gson gson = new Gson();
                gson.toJson(jsonArray, fileWriter);
            }

            System.out.println("JSON file created successfully: " + outputFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputFilePath;
    }

    static class FileAttributes {
        String uriFragment;
        String fileHeading;

        FileAttributes(String uriFragment, String fileHeading) {
            this.uriFragment = uriFragment;
            this.fileHeading = fileHeading;
        }
    }
}
