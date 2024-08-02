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
    private ZipService zipService;
    @Autowired
    private SharedConfig sharedConfig;
    @Autowired
    private CleanupService cleanupService;
    @Autowired
    private LogStreamingService logService;

    @GetMapping("/convert")
       public ResponseEntity<ResponseModel> convert() {
        long startTime = System.currentTimeMillis();
        try {
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

            // Extract Xrefs from the myconfig.xml file
            String jsonFilePath = xrefExtractorService.extractXrefs();

            // Convert XML to DITA
            xslTransformerService.convertXMLToDITA();
            xrefFixerService.fixXrefs(jsonFilePath);

            // Create ZIP file of final output
//            String sourceDirPath = "output/finalOutput";
//            String zipFilePath = "output/finalOutput.zip";
//            zipService.createZipFile(sourceDirPath, zipFilePath);

            // Set the path to the ZIP file in SharedConfig
//            sharedConfig.setZipFilePath(zipFilePath);

            // Perform the cleanup operation
//            cleanupService.cleanUpDirectories(zipFilePath);

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
//    @GetMapping("/logs")
//
//    public SseEmitter streamLogs() {
//
//        return logService.registerClient();
//
//    }
}
