package com.metapercept.fff_dita_converter.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.util.zip.*;

@Service
public class ZipService {
    private static final String UPLOAD_DIR = "uploads/";
    private static final String FINAL_OUTPUT_DIR = "output/finalOutput/";

    public void saveUploadedFile(MultipartFile file) throws IOException {
        Files.createDirectories(Paths.get(UPLOAD_DIR));
        File zipFile = new File(UPLOAD_DIR + file.getOriginalFilename());
        try (FileOutputStream fos = new FileOutputStream(zipFile)) {
            fos.write(file.getBytes());
        }
    }

    public File findZipFile() {
        File dir = new File(UPLOAD_DIR);
        File[] zipFiles = dir.listFiles((d, name) -> name.endsWith(".zip"));
        if (zipFiles != null && zipFiles.length > 0) {
            return zipFiles[0];
        }
        return null;
    }

    public String extractZipFile(File zipFile) throws IOException {
        String extractedDir = UPLOAD_DIR + "extracted/";
        Files.createDirectories(Paths.get(extractedDir));

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile.toPath()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File newFile = new File(extractedDir + entry.getName());
                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    new File(newFile.getParent()).mkdirs();
                    try (OutputStream outputStream = Files.newOutputStream(newFile.toPath())) {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = zis.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                    }
                }
            }
        }

        return extractedDir;
    }

    public File createZipFile(String sourceDirPath, String zipFilePath) throws IOException {
        Path zipPath = Files.createFile(Paths.get(zipFilePath));
        try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            Path sourcePath = Paths.get(sourceDirPath);
            Files.walk(sourcePath)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(sourcePath.relativize(path).toString());
                        try {
                            zs.putNextEntry(zipEntry);
                            Files.copy(path, zs);
                            zs.closeEntry();
                        } catch (IOException e) {
                            System.err.println(e);
                        }
                    });
        }
        return new File(zipFilePath);
    }

//    public File zipFinalOutput() throws IOException {
//        File sourceDir = new File(FINAL_OUTPUT_DIR);
//        File zipFile = new File(FINAL_OUTPUT_DIR + "final_output.zip");
//
//        try (FileOutputStream fos = new FileOutputStream(zipFile);
//             ZipOutputStream zos = new ZipOutputStream(fos)) {
//
//            Files.walkFileTree(sourceDir.toPath(), new SimpleFileVisitor<Path>() {
//                @Override
//                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
//                    zos.putNextEntry(new ZipEntry(sourceDir.toPath().relativize(file).toString()));
//                    Files.copy(file, zos);
//                    zos.closeEntry();
//                    return FileVisitResult.CONTINUE;
//                }
//
//                @Override
//                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
//                    if (!sourceDir.toPath().equals(dir)) {
//                        zos.putNextEntry(new ZipEntry(sourceDir.toPath().relativize(dir).toString() + "/"));
//                        zos.closeEntry();
//                    }
//                    return FileVisitResult.CONTINUE;
//                }
//            });
//        }
//
//        return zipFile;
//    }

//    public void cleanup() throws IOException {
//        FileUtils.deleteDirectory(new File(UPLOAD_DIR));
//        FileUtils.deleteDirectory(new File(FINAL_OUTPUT_DIR));
//    }
}