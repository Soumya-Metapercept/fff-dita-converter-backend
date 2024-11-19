package com.metapercept.fff_dita_converter.service;

import com.metapercept.fff_dita_converter.config.SharedConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import net.sf.saxon.TransformerFactoryImpl;

@Service
public class XSLTransformerService {
    private static final int THREAD_POOL_SIZE = 4;  // Adjust based on your system capabilities

    @Value("${xslt.path}")
    private String xsltPath;

    private final ResourceLoader resourceLoader;
    private final SharedConfig sharedConfig;
 
    public XSLTransformerService(ResourceLoader resourceLoader, SharedConfig sharedConfig) {
        this.resourceLoader = resourceLoader;
        this.sharedConfig = sharedConfig;
    }

    public void convertXMLToDITA() throws Exception {
        long startTime = System.currentTimeMillis();
        try {
            // Paths to the XSLT files
            Resource cpaDelayerXSLT = resourceLoader.getResource(xsltPath + "cpa_delayer.xsl");
            Resource xmlToDitaXSLT = resourceLoader.getResource(xsltPath + "XmlToDita.xsl");
            Resource bullet_list1XSLT = resourceLoader.getResource(xsltPath + "bullet_list1.xsl");
            Resource bullet_list2XSLT = resourceLoader.getResource(xsltPath + "bullet_list2.xsl");
            Resource tableXSLT = resourceLoader.getResource(xsltPath + "table.xsl");
            Resource cleanupXSLT = resourceLoader.getResource(xsltPath + "cleanup.xsl");
            Resource cpaDitamapXSLT = resourceLoader.getResource(xsltPath + "cpa_ditamap.xsl");
//            Resource OLtoULXSLT = resourceLoader.getResource(xsltPath + "OLtoUL.xsl");

            // Get the myconfig.xml file path from SharedConfig
            String myconfigXmlFilePath = sharedConfig.getMyConfigXmlFilePath();
            if (myconfigXmlFilePath == null || myconfigXmlFilePath.isEmpty()) {
                throw new IOException("myconfig.xml file path is not set.");
            }
            File myconfigXmlFile = new File(myconfigXmlFilePath);

            // Output directory for each transformation step
            File intermediateDir1 = new File("output/intermediate1/");
            File intermediateDir2 = new File("output/intermediate2/");
            File intermediateDir3 = new File("output/intermediate3/");
            File intermediateDir4 = new File("output/intermediate4/");
            File intermediateDir5 = new File("output/intermediate5/");
            File finalOutputDir = new File("output/finalOutput/");
//            File fixesOutputDir = new File("output/fixesOutput/");

            // Temporary directory for final transformation
            File tempOutputDir = new File("output/tempOutput/");

            // Ensure directories exist
            intermediateDir1.mkdirs();
            intermediateDir2.mkdirs();
            intermediateDir3.mkdirs();
            intermediateDir4.mkdirs();
            intermediateDir5.mkdirs();
            finalOutputDir.mkdirs();
            tempOutputDir.mkdirs();
//            fixesOutputDir.mkdirs();

            System.out.println("Starting transformation process...");

            // Perform the initial transformation to get the DITA files
            transform(myconfigXmlFile, cpaDelayerXSLT.getFile(), intermediateDir1, "cpa_delayer.xsl");

            // Log the files generated in intermediateDir1
            logDirectoryContents(intermediateDir1, ".dita");

            // Parallel processing using a thread pool
            ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

            // Transform each DITA file through the subsequent steps
            processDirectoryRecursively(executor, intermediateDir1, xmlToDitaXSLT.getFile(), intermediateDir2, ".dita", "XmlToDita.xsl");
            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            logDirectoryContents(intermediateDir2, ".dita");

            executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
            processDirectoryRecursively(executor, intermediateDir2, bullet_list1XSLT.getFile(), intermediateDir3, ".dita", "bullet_list1.xsl");
            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            logDirectoryContents(intermediateDir3, ".dita");

            executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
            processDirectoryRecursively(executor, intermediateDir3, bullet_list2XSLT.getFile(), intermediateDir4, ".dita", "bullet_list2.xsl");
            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            logDirectoryContents(intermediateDir4, ".dita");

            executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
            processDirectoryRecursively(executor, intermediateDir4, tableXSLT.getFile(), intermediateDir5, ".dita", "table.xsl");
            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            logDirectoryContents(intermediateDir5, ".dita");

            executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
            processDirectoryRecursively(executor, intermediateDir5, cleanupXSLT.getFile(), finalOutputDir, ".dita", "cleanup.xsl");
            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            logDirectoryContents(finalOutputDir, ".dita");

            //Temporary Fixes processing XSLT
//            executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
//            processDirectoryRecursively(executor, intermediateDir5, cleanupXSLT.getFile(), finalOutputDir, ".dita", "cleanup.xsl");
//            executor.shutdown();
//            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
//            logDirectoryContents(finalOutputDir, ".dita");

            // Perform the final transformation to generate the ditamap into a temporary directory
            transform(myconfigXmlFile, cpaDitamapXSLT.getFile(), tempOutputDir, "cpa_ditamap.xsl");


            // Merge the tempOutputDir with finalOutputDir
            mergeDirectories(tempOutputDir, finalOutputDir);

            System.out.println("All transformations completed successfully.");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            System.out.println("Total time taken: " + (totalTime / 1000) + " seconds.");
        }
    }

    private static void transform(File inputFile, File xsltFile, File outputDir, String xsltFileName) throws Exception {
        // Log the transformation step
        System.out.println("Transforming " + inputFile.getPath() + " using " + xsltFileName);

        // Create a source for the XML and XSLT files
        StreamSource xmlSource = new StreamSource(inputFile);
        StreamSource xsltSource = new StreamSource(xsltFile);

        // Create a Saxon transformer factory
        TransformerFactory factory = new TransformerFactoryImpl();

        // Create a transformer for the XSLT file
        Transformer transformer = factory.newTransformer(xsltSource);

        // Create an output stream for the result
        File outputFile = new File(outputDir, inputFile.getName());
        StreamResult result = new StreamResult(outputFile);

        // Perform the transformation
        transformer.transform(xmlSource, result);

        // Log the completion of the transformation step
        System.out.println("Finished transforming " + inputFile.getPath() + " using " + xsltFileName);
    }

    private static void processDirectoryRecursively(ExecutorService executor, File inputDir, File xsltFile, File outputDir, String fileExtension, String xsltFileName) throws Exception {
        // List all files and directories
        File[] files = inputDir.listFiles();

        if (files == null) {
            System.out.println("No files found in directory " + inputDir.getPath());
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                // Create corresponding directory in outputDir
                File nestedOutputDir = new File(outputDir, file.getName());
                nestedOutputDir.mkdirs();
                // Recursively process the nested directory
                processDirectoryRecursively(executor, file, xsltFile, nestedOutputDir, fileExtension, xsltFileName);
            } else if (file.getName().endsWith(fileExtension)) {
                // Process the .dita file in parallel
                executor.submit(() -> {
                    try {
                        transform(file, xsltFile, outputDir, xsltFileName);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }

    private static void mergeDirectories(File sourceDir, File targetDir) throws IOException {
        // List all files and directories in the source directory
        File[] files = sourceDir.listFiles();

        if (files == null) {
            System.out.println("No files found in directory " + sourceDir.getPath());
            return;
        }

        for (File file : files) {
            File targetFile = new File(targetDir, file.getName());
            if (file.isDirectory()) {
                // Recursively merge directories
                mergeDirectories(file, targetFile);
            } else {
                // Copy file to the target directory, overwriting existing files
                Files.copy(file.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static void logDirectoryContents(File directory, String fileExtension) {
        File[] files = directory.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(fileExtension) || new File(dir, name).isDirectory();
            }
        });

        if (files != null && files.length > 0) {
            System.out.println("Files in " + directory.getPath() + ":");
            for (File file : files) {
                System.out.println(" - " + file.getName());
            }
        } else {
            System.out.println("No files with extension " + fileExtension + " found in directory " + directory.getPath());
        }
    }

    public File findMyConfigXmlFile(File directory) throws IOException {
        return Files.walk(directory.toPath())
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().equals("myconfig.xml"))
                .findFirst()
                .map(Path::toFile)
                .orElse(null);
    }
}
