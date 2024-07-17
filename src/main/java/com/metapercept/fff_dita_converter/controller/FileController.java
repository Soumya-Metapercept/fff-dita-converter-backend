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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@RestController
@RequestMapping("/api")

public class FileController {

    @Autowired
    private ZipService zipService;


    @Autowired
    private SharedConfig sharedConfig;

    @PostMapping("/upload")
    public ResponseEntity<ResponseModel> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
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
        String zipFilePath = sharedConfig.getZipFilePath();
        if (zipFilePath == null || zipFilePath.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        File zipFile = new File(zipFilePath);
        if (!zipFile.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        try {
            InputStreamResource resource = new InputStreamResource(new FileInputStream(zipFile));
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + zipFile.getName());

            ResponseEntity<InputStreamResource> response = ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(zipFile.length())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

            // Delete the ZIP file after the response is sent
            zipFile.delete();

            return response;
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

