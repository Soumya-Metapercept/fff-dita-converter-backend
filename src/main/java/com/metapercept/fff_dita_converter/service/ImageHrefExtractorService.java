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
public class ImageHrefExtractorService {

    private static final String TEMP_DIR = "temp/";

    private final SharedConfig sharedConfig;

    @Autowired
    public ImageHrefExtractorService(SharedConfig sharedConfig) {
        this.sharedConfig = sharedConfig;
    }

    public String extractImageHrefs() throws IOException {

        String imageJsonFilePath = TEMP_DIR + "imageJson.json";

        // Ensure the temporary directory exists
        Files.createDirectories(Paths.get(TEMP_DIR));

        // Get the myconfig.xml file path from SharedConfig
        String myconfigXmlFilePath = sharedConfig.getMyConfigXmlFilePath();
        if (myconfigXmlFilePath == null || myconfigXmlFilePath.isEmpty()) {
            throw new IOException("myconfig.xml file path is not set.");
        }
        File myconfigXmlFile = new File(myconfigXmlFilePath);

        // Prepare JSON array for images
        JsonArray imageJsonArray = new JsonArray();

        try {
            // Parse the XML file
            Document doc = Jsoup.parse(myconfigXmlFile, "UTF-8");

            // Find all <object> tags
            Elements objectTags = doc.select("object");

            for (Element objectTag : objectTags) {
                // Traverse up the tree to find the nearest ancestor <file> tag
                Element parent = objectTag.parent();
                String uriFragment = "";
                while (parent != null) {
                    if (parent.tagName().equals("file")) {
                        uriFragment = parent.attr("uri_fragment");
                        break;
                    }
                    parent = parent.parent();
                }

                // If the <object> tag is in a valid <file> context and src ends with .OB
                String src = objectTag.attr("src");
                if (!uriFragment.isEmpty() && src.endsWith(".OB")) {
                    String imageFileName = src.substring(src.lastIndexOf("\\") + 1).replace(".OB", ".jpg");

                    JsonObject imageJsonObject = new JsonObject();
                    imageJsonObject.add("orgFileID", new JsonPrimitive(uriFragment));
                    imageJsonObject.add("imageFileName", new JsonPrimitive(imageFileName));

                    imageJsonArray.add(imageJsonObject);
                }
            }

            // Write the image JSON array to the image file
            try (FileWriter fileWriter = new FileWriter(imageJsonFilePath)) {
                Gson gson = new Gson();
                gson.toJson(imageJsonArray, fileWriter);
            }

            System.out.println("Image JSON file created successfully: " + imageJsonFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return imageJsonFilePath;
    }
}
