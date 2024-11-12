package com.metapercept.fff_dita_converter.service;

import folioxml.command.Main;
import com.metapercept.fff_dita_converter.model.ResponseModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;

@Service
public class FolioConverterService {

    @Autowired
    private ZipService zipService;

    private static final String UPLOAD_DIR = "uploads/";
    private static final String EXPORT_DIR = "exports/";

    public ResponseEntity<ResponseModel> convertFolioFiles() throws IOException {

        // Find the uploaded zip file
        File zipFile = zipService.findZipFile();
        if (zipFile == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseModel("No zip file found in the upload directory.", null));
        }

        // Extract the uploaded zip file
        String extractedDir = zipService.extractZipFile(zipFile);

        // Find the .FFF file
        String fffFilePath = findFFFFile(extractedDir);
        if (fffFilePath == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseModel("No .FFF file found in the extracted zip folder.", null));
        }

        // Generate config.yaml
        String configFilePath = generateConfigYaml(extractedDir, fffFilePath);

        // Process .ob files and entire "Data" folder
        processFiles(extractedDir);

        // Run the FolioXML export process
        try {
            runFolioXmlExport(configFilePath);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResponseModel("FolioXML export failed with exit code: " + e.getMessage(), null));
        }

        return ResponseEntity.ok(new ResponseModel("Folio files converted successfully.", null));
    }

    private String findFFFFile(String extractedDir) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(extractedDir), "*.FFF")) {
            for (Path entry : stream) {
                return entry.toString();
            }
        }
        return null;
    }

    private String generateConfigYaml(String extractedDir, String fffFilePath) throws IOException {
        String configFilePath = extractedDir + "config.yaml";
        String content = "myconfig:\n" +
                "  export_xml: true\n" + //Creates a single XML file with all the content (excluding the stylesheet, images, and logs)  (default=true)
                "  skip_normal_records: false\n" + //affects xml only - only writes out hierarchy-affecting levels, in order to make a shorter file. (default=false)
                "  nest_file_elements: true\n" + //affects XML only - Disables nested syntax for <file> elements in xml, uses flat structure. Nesting uses same hierarchy as folio. (default=true)
                "  indent_xml: true\n" + //Indentation can introduce undesired visual artificacts/spacing, and should only be used for human consumption. (default=true)
                "  export_inventory: true\n" + //Lowers performance - tracks unique elements in memory, generates a textual report at the end. (default=true)
                "  export_hidden_text: true\n" + //Halves performance. Writes out a log of text that would be hidden by the generated CSS (either via display:none or zero-contrast coloring). (default=true)
                "  resolve_jump_links: true\n" + //Disable fixing up jump links (default=true)
                "  resolve_query_links: true\n" + //Disable simulating queries and linking them to the first result. If this and resolve_jump_links are false, no Lucene index is required. (default=true)
                "  export_html: false\n" + //exports lots of browsable HTML files according to the splitting and naming rules. (default=true)
                "  use_highslide: true\n" + //affects both XML and HTML. Required for popups/notes to keep working. (default=true)
                "  add_nav_links: true\n" + //html only # Adds prev/next links at the beginning and end of each HTML generated. (default=true)
                "  faux_tabs: false\n" + //Enable faux tabs (default=false)
                "  faux_tabs_window_min: 80\n" + // We have to deal with centered and right-aligned tabs.
                "  faux_tabs_window_max: 120\n" + //These provide the default and maximum (character count) bounds with which to simulate them.
                //Here we can manually map broken URLs (and cross-infobase links) to new places
                "  link_mapper:\n" +
                "    urls:\n" +
                "      'C:/Files/Data.pdf': 'https://othersite/data'\n" +
                "    infobases:\n" +
                "      'C:/Files/Other.NFO': 'http://othersite/other'\n" +
                "      'C:/Files/Other2.NFO': 'http://othersite/other2'\n" +
                //This is how we trash stuff we don't care about
                "  pull:\n" + //log_pulled_elements.txt and log_dropped_elements.txt are created
                "    program_links: true\n" +
                "    menu_links: true\n" +
                "    drop_notes: false\n" + //It can be cleaner to drop notes/popups than preserve them with highslide & javascript. Dropped data is logged.
                "    drop_popups: false\n" + //When use_highslide is false, the popups would otherwise be invalid HTML
                "    ole_objects: false\n" +
                "    metafile_objects: false\n" +
                "    links_to_infobases:\n" +
                "      - 'C:/Files/Obsolete.NFO'\n" +
                "      - 'C:/Files/Obsolete2.NFO'\n" +
                //You must convert all your infobases that link to each other at once, otherwise those links will not be preserved.
                //In addition, unique IDs will overlap between infobases converted separately, causing potential issues in your final data store.
                "  infobases:\n" +
                "    - id: info_a\n" +
                "      path: \"" + fffFilePath.replace("\\", "/") + "\"\n" +

                //Structure affects how we split the infobase into parts and identify those parts.
                //We can specify a custom provider class

                "  structure_class: \"folioxml.export.structure.IdSlugProvider\"\n" +
                "  structure_class_params: [ \"null\",  \"null\", 0, 1, 1]\n" +
                //structure_class_params: String levelRegex, String splitOnFieldName, Integer idKind, Integer root_index, Integer start_index

                //levelRegex lets us split based on predefined folio levels like "Heading 1|Heading 2"
                //splitOnFieldName lets us split whenever a record contains the given field.
                //idKind values
                //0 (heading-based slugs), 1 (integers), 2 (nested integers 2.3.1), 3 (guids), or 4 (folio IDs). Schemes 5-9 use the contents of splitOnFieldName and fall back to 0-4 if missing or non-unique.
                //root_index and start_index are used for idKinds 2 and 3 (as well as 7 and 8, of course).

                "  asset_start_index: 1\n" + //What index do we start with for asset IDs.

                //Only set this to true if you also set export_locations: images: url to a template that can respond based on numeric ID instead of filename. See AssetInventory.xml
                "  asset_use_index_in_url: true\n" +

                "  export_locations:\n" +
                "    default:\n" +
                "      path: \"" + EXPORT_DIR.replace("\\", "/") + "{id}-{stamp}/{input}\"\n" + //Used for text, xml files, css, and logs
                "    image:\n" +
                "      path: \"" + EXPORT_DIR.replace("\\", "/") + "{input}\"\n" + //Adjust this to represent the absolute path where the static images will be stored. Control {input} with asset_use_index_in_url
                "    luceneindex:\n" +
                "      path: \"" + EXPORT_DIR.replace("\\", "/") + "indexes/myconfig/\"\n" + //Just a temp directory.
                "    html:\n" +
                "      path: \"" + EXPORT_DIR.replace("\\", "/") + "{id}-{stamp}/html/{input}.html\"\n"; //Adjust this to represent the final hosted location of your HTML pages, as an absolute path. Adjust idKind to change {input}

        try (FileWriter writer = new FileWriter(configFilePath)) {
            writer.write(content);
        }

        return configFilePath;
    }

    private void runFolioXmlExport(String configFilePath) throws IOException {
        String exportName = "myconfig"; // Adjust if needed

        String[] commandArgs = {
                "--config", configFilePath,
                "--export", exportName
        };

        int result = Main.run(commandArgs);
        if (result != 0) {
            throw new IOException("FolioXML export failed with exit code: " + result);
        }
    }

    private void processFiles(String extractedDir) throws IOException {
        Path dataDir = Paths.get("output/finalOutput/cpa/data");
        Path imagesDir = Paths.get("output/finalOutput/cpa/images");
        Files.createDirectories(dataDir);
        Files.createDirectories(imagesDir);

        Files.walk(Paths.get(extractedDir))
                .filter(Files::isRegularFile)
                .forEach(file -> {
                    try {
                        String fileName = file.getFileName().toString();
                        Path parentDir = file.getParent().getFileName();

                        // Check if the file is inside the "Data" folder
                        if ("Data".equalsIgnoreCase(parentDir.toString())) {
                            // Replace spaces with underscores in the filename
                            String newFileName = fileName.replaceAll(" ", "_");

                            // Copy the file with the updated name to the final output data directory
                            Files.copy(file, dataDir.resolve(newFileName), StandardCopyOption.REPLACE_EXISTING);

                        } else if (fileName.endsWith(".OB")) {
                            String newFileName = fileName.substring(0, fileName.length() - 3) + ".jpg";
                            Files.copy(file, imagesDir.resolve(newFileName), StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }
}
