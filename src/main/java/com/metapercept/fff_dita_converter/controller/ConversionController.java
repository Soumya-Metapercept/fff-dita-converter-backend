package com.metapercept.fff_dita_converter.controller;

import com.metapercept.fff_dita_converter.config.SharedConfig;
import com.metapercept.fff_dita_converter.model.ResponseModel;
import com.metapercept.fff_dita_converter.service.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;

import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api")
public class ConversionController {

    @Autowired
    private FolioConverterService folioConverterService;
    @Autowired
    private XSLTransformerService xslTransformerService;
    @Autowired
    private XrefExtractorService xrefExtractorService;
    @Autowired
    private XrefFixerService xrefFixerService;
    @Autowired
    private ImageHrefExtractorService imageHrefExtractorService;
    @Autowired
    private ImageHrefFixerService imageHrefFixerService;
    @Autowired
    private DataHrefExtractorService dataHrefExtractorService;
    @Autowired
    private DataHrefFixerService dataHrefFixerService;
    @Autowired
    private ZipService zipService;
    @Autowired
    private SharedConfig sharedConfig;
    @Autowired
    private CleanupService cleanupService;
    @Autowired
    private XmlFileNameUpdaterService xmlFileNameUpdaterService;
    @Autowired
    private PopupExtractorService popupExtractorService;
    @Autowired
    private PopupFixerService popupFixerService;


    @GetMapping("/convert")
       public ResponseEntity<ResponseModel> convert() {
        long startTime = System.currentTimeMillis();
        try {
            // Clean up all directories except "uploads"
            cleanupService.cleanUpDirectoriesExclude("uploads");

            // Convert Folio files to XML
            System.out.println("Starting conversion process...");
            ResponseEntity<ResponseModel> folioResponse = folioConverterService.convertFolioFiles();
            if (folioResponse.getStatusCode().isError()) {
                return folioResponse;
            }

            // Find the myconfig.xml file
            File myconfigXmlFile = xslTransformerService.findMyConfigXmlFile(new File("exports/"));
            if (myconfigXmlFile == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ResponseModel("myconfig.xml file not found.", null));
            }

            // Set the myconfig.xml file path in SharedConfig
            sharedConfig.setMyConfigXmlFilePath(myconfigXmlFile.getAbsolutePath());

            // **Run XmlFileNameUpdaterService to update the file names**
            xmlFileNameUpdaterService.updateFileNamesInConfig(myconfigXmlFile.getAbsolutePath());

            // Extract Xrefs from the myconfig.xml file
            String linkJsonFilePath = xrefExtractorService.extractXrefs();

            // Extract Image Hrefs from the myconfig.xml file
            String imageJsonFilePath = imageHrefExtractorService.extractImageHrefs();

            // Extract Data Hrefs from the myconfig.xml file
            String dataJsonFilePath = dataHrefExtractorService.extractDataHrefs();

            // Extract PopUp from the myconfig.xml file
            String popupJsonFilePath = popupExtractorService.extractPopupData();

            // Convert XML to DITA
            xslTransformerService.convertXMLToDITA();

            // Fix Xrefs in DITA files
            xrefFixerService.fixXrefs(linkJsonFilePath);

            // Fix PopupXrefs in DITA files
            popupFixerService.fixPopups(popupJsonFilePath);

            // Fix Image Hrefs in DITA files
            imageHrefFixerService.fixImageHrefs(imageJsonFilePath);

            // Fix Data Hrefs in DITA files
            dataHrefFixerService.fixDataHrefs(dataJsonFilePath);

            // Create ZIP file at the end of the conversion process
            String sourceDirPath = "output/finalOutput";
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
            String dateTime = LocalDateTime.now().format(formatter);
            String zipFilePath = "output/finalOutput_" + dateTime + ".zip";

            File sourceDir = new File(sourceDirPath);
            if (!sourceDir.exists() || sourceDir.listFiles().length == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ResponseModel("No files found to zip.", null));
            }

            // Create the ZIP file
            System.out.println("Creating ZIP file...");
            zipService.createZipFile(sourceDirPath, zipFilePath);
            System.out.println("Created ZIP file.");

            // Store the ZIP file path in sharedConfig so it can be accessed by the download API
            sharedConfig.setZipFilePath(zipFilePath);

           ResponseModel response = new ResponseModel("Conversion done successfully.", null);
           return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
           ResponseModel response = new ResponseModel("Mission failed.", null);
           return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } finally {
            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            System.out.println("Total time taken for FFF-DITA conversion: " + (totalTime / 1000) + " seconds.");
        }
    }
}
