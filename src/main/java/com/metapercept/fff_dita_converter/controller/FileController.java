package com.metapercept.fff_dita_converter.controller;

import com.metapercept.fff_dita_converter.config.SharedConfig;
import com.metapercept.fff_dita_converter.model.ResponseModel;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import com.metapercept.fff_dita_converter.service.ZipService;
import com.metapercept.fff_dita_converter.service.CleanupService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api")

public class FileController {

    @Autowired
    private ZipService zipService;


    @Autowired
    private SharedConfig sharedConfig;

    @Autowired
    private CleanupService cleanupService;

    @PostMapping("/upload")
    public ResponseEntity<ResponseModel> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // Clean only the uploads directory
            cleanupService.cleanSpecificDirectory("uploads");

            zipService.saveUploadedFile(file);
            ResponseModel response = new ResponseModel("File uploaded successfully.", null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            ResponseModel response = new ResponseModel("File upload failed.", null);
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> download() {
        String zipFilePath = sharedConfig.getZipFilePath(); // Get the ZIP file path from sharedConfig

        if (zipFilePath == null || !(new File(zipFilePath).exists())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        try {
            System.out.println("Downloading file...");
            File zipFile = new File(zipFilePath);
            InputStreamResource resource = new InputStreamResource(new FileInputStream(zipFile));
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + zipFile.getName());

            ResponseEntity<InputStreamResource> response = ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(zipFile.length())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

            System.out.println("File downloaded successfully.");

            // Cleanup will be done in a separate thread to ensure the response is sent before deletion
//            new Thread(() -> {
//                try {
//                    Thread.sleep(5000); // Wait for 5 seconds before cleanup
//                    cleanupService.cleanUpDirectories(zipFilePath);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }).start();
//
            return response;
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

