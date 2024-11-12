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
import java.nio.file.Paths;

@Service
public class PopupExtractorService {

    private static final String TEMP_DIR = "temp/";

    private final SharedConfig sharedConfig;

    @Autowired
    public PopupExtractorService(SharedConfig sharedConfig) {
        this.sharedConfig = sharedConfig;
    }

    public String extractPopupData() throws IOException {

        String outputFilePath = TEMP_DIR + "popupData.json";

        // Ensure the temporary directory exists
        Files.createDirectories(Paths.get(TEMP_DIR));

        // Get the myconfig.xml file path from SharedConfig
        String myconfigXmlFilePath = sharedConfig.getMyConfigXmlFilePath();
        if (myconfigXmlFilePath == null || myconfigXmlFilePath.isEmpty()) {
            throw new IOException("myconfig.xml file path is not set.");
        }
        File myconfigXmlFile = new File(myconfigXmlFilePath);

        // Prepare JSON arrays for storing <div> and <a> tag data
        JsonArray divJsonArray = new JsonArray();
        JsonArray aJsonArray = new JsonArray();

        try {
            // Parse the XML file
            Document doc = Jsoup.parse(myconfigXmlFile, "UTF-8");

            // Process each <div> tag with title="Popup"
            Elements divTags = doc.select("div[title=Popup]");
            for (Element divTag : divTags) {
                // Traverse up the tree to find the nearest ancestor <file> tag
                Element fileAncestor = findNearestFileAncestor(divTag);
                if (fileAncestor != null) {
                    String uriFragment = fileAncestor.attr("uri_fragment");

                    // Process the <div> tag information
                    JsonObject divObject = new JsonObject();
                    String divId = divTag.id().replace("_", "-");
                    divObject.add("tagId", new JsonPrimitive(divId));
                    divObject.add("orgFileId", new JsonPrimitive(uriFragment));
                    divJsonArray.add(divObject);
                }
            }

            // Process each <a> tag with class="highslide popupLink"
            Elements aTags = doc.select("a.highslide.popupLink");
            for (Element aTag : aTags) {
                // Traverse up the tree to find the nearest ancestor <file> tag
                Element fileAncestor = findNearestFileAncestor(aTag);
                if (fileAncestor != null) {
                    String uriFragment = fileAncestor.attr("uri_fragment");

                    // Process the <a> tag information
                    JsonObject aObject = new JsonObject();
                    String onclickValue = aTag.attr("onclick");
                    String refId = extractMainContentId(onclickValue).replace("_", "-");
                    aObject.add("refId", new JsonPrimitive(refId));
                    aObject.add("refParentId", new JsonPrimitive(uriFragment));
                    aObject.add("refContent", new JsonPrimitive(aTag.text()));
                    aJsonArray.add(aObject);
                }
            }

            // Build JSON output
            JsonObject output = new JsonObject();
            output.add("div", divJsonArray);
            output.add("a", aJsonArray);

            // Write JSON data to output file
            try (FileWriter fileWriter = new FileWriter(outputFilePath)) {
                Gson gson = new Gson();
                gson.toJson(output, fileWriter);
            }

            System.out.println("Popup JSON file created successfully: " + outputFilePath);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputFilePath;
    }

    /**
     * Helper method to traverse up the tree and find the nearest <file> ancestor.
     *
     * @param element The starting element (either <div> or <a> tag).
     * @return The nearest ancestor <file> element, or null if none is found.
     */
    private Element findNearestFileAncestor(Element element) {
        Element parent = element.parent();
        while (parent != null) {
            if (parent.tagName().equals("file")) {
                return parent;
            }
            parent = parent.parent();
        }
        return null; // No <file> ancestor found
    }

    /**
     * Helper method to extract maincontentId from the onclick attribute.
     *
     * @param onclickValue The onclick attribute value from the <a> tag.
     * @return The extracted maincontentId value.
     */
    private String extractMainContentId(String onclickValue) {
        int start = onclickValue.indexOf("maincontentId: '") + 16;
        int end = onclickValue.indexOf("'", start);
        return onclickValue.substring(start, end);
    }
}
