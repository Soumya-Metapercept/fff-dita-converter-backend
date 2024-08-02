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

        // Process .ob and .pdf files
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
                "  export_xml: true\n" +
                "  skip_normal_records: false\n" +
                "  nest_file_elements: true\n" +
                "  indent_xml: true\n" +
                "  export_inventory: true\n" +
                "  export_hidden_text: true\n" +
                "  resolve_jump_links: true\n" +
                "  resolve_query_links: true\n" +
                "  export_html: false\n" +
                "  use_highslide: false\n" +
                "  add_nav_links: true\n" +
                "  faux_tabs: false\n" +
                "  faux_tabs_window_min: 80\n" +
                "  faux_tabs_window_max: 120\n" +
                "  link_mapper:\n" +
                "    urls:\n" +
                "      'C:/Files/Data.pdf': 'https://othersite/data'\n" +
                "    infobases:\n" +
                "      'C:/Files/Other.NFO': 'http://othersite/other'\n" +
                "      'C:/Files/Other2.NFO': 'http://othersite/other2'\n" +
                "  pull:\n" +
                "    program_links: true\n" +
                "    menu_links: true\n" +
                "    drop_notes: true\n" +
                "    drop_popups: true\n" +
                "    ole_objects: false\n" +
                "    metafile_objects: false\n" +
                "    links_to_infobases:\n" +
                "      - 'C:/Files/Obsolete.NFO'\n" +
                "      - 'C:/Files/Obsolete2.NFO'\n" +
                "  infobases:\n" +
                "    - id: info_a\n" +
                "      path: \"" + fffFilePath.replace("\\", "/") + "\"\n" +
                "  structure_class: \"folioxml.export.structure.IdSlugProvider\"\n" +
                "  structure_class_params: [ \"null\",  \"null\", 0, 1, 1]\n" +
                "  asset_start_index: 1\n" +
                "  asset_use_index_in_url: true\n" +
                "  export_locations:\n" +
                "    default:\n" +
                "      path: \"" + EXPORT_DIR.replace("\\", "/") + "{id}-{stamp}/{input}\"\n" +
                "    image:\n" +
                "      path: \"" + EXPORT_DIR.replace("\\", "/") + "{input}\"\n" +
                "    luceneindex:\n" +
                "      path: \"" + EXPORT_DIR.replace("\\", "/") + "indexes/myconfig/\"\n" +
                "    html:\n" +
                "      path: \"" + EXPORT_DIR.replace("\\", "/") + "{id}-{stamp}/html/{input}.html\"\n";

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
        Path dataDir = Paths.get("output/finalOutput/data");
        Path imagesDir = Paths.get("output/finalOutput/images");
        Files.createDirectories(dataDir);
        Files.createDirectories(imagesDir);

        Files.walk(Paths.get(extractedDir))
                .filter(Files::isRegularFile)
                .forEach(file -> {
                    try {
                        String fileName = file.getFileName().toString();
                        if (fileName.endsWith(".pdf")) {
                            Files.copy(file, dataDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
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

