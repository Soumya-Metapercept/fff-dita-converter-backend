package com.metapercept.fff_dita_converter.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.metapercept.fff_dita_converter.config.SharedConfig;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class DataHrefExtractorService {

    private static final String TEMP_DIR = "temp/";
    private final SharedConfig sharedConfig;

    @Autowired
    public DataHrefExtractorService(SharedConfig sharedConfig) {
        this.sharedConfig = sharedConfig;
    }

    public String extractDataHrefs() throws IOException {

        String outputFilePath = TEMP_DIR + "dataJson.json";
        Files.createDirectories(Paths.get(TEMP_DIR));

        String myconfigXmlFilePath = sharedConfig.getMyConfigXmlFilePath();
        if (myconfigXmlFilePath == null || myconfigXmlFilePath.isEmpty()) {
            throw new IOException("myconfig.xml file path is not set.");
        }
        File myconfigXmlFile = new File(myconfigXmlFilePath);

        try {
            Document doc = Jsoup.parse(myconfigXmlFile, "UTF-8");

            JsonArray dataJsonArray = new JsonArray();
            Elements linkTags = doc.select("link[class=Data]");

            for (Element linkTag : linkTags) {
                String href = linkTag.attr("href");
                String dataFileName = href.substring(href.lastIndexOf("\\") + 1);

                // Find the nearest preceding <file> tag
                Element parent = linkTag.parent();
                String orgFileID = "";
                while (parent != null) {
                    if (parent.tagName().equals("file")) {
                        orgFileID = parent.attr("uri_fragment");
                        break;
                    }
                    parent = parent.parent();
                }

                // Store the data in JSON format
                if (!orgFileID.isEmpty()) {
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.add("orgFileID", new JsonPrimitive(orgFileID));
                    jsonObject.add("dataFileName", new JsonPrimitive(dataFileName));
                    dataJsonArray.add(jsonObject);
                }
            }

            // Write the JSON array to a file
            try (FileWriter fileWriter = new FileWriter(outputFilePath)) {
                fileWriter.write(dataJsonArray.toString());
            }

            System.out.println("Data JSON file created successfully: " + outputFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputFilePath;
    }
}
