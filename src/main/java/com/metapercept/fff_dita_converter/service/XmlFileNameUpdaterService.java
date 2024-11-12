package com.metapercept.fff_dita_converter.service;

import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;

@Service
public class XmlFileNameUpdaterService {

    public void updateFileNamesInConfig(String configFilePath) throws Exception {
        // Load the XML document
        File configFile = new File(configFilePath);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(configFile);

        // Use XPath to select all <link> tags with class="Data"
        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();
        XPathExpression expr = xpath.compile("//link[@class='Data']");
        NodeList linkNodes = (NodeList) expr.evaluate(document, XPathConstants.NODESET);

        // Process each <link> node
        for (int i = 0; i < linkNodes.getLength(); i++) {
            Element linkElement = (Element) linkNodes.item(i);

            // Get the 'href' attribute
            String href = linkElement.getAttribute("href");

            // Extract the filename part (after the last '/') and replace spaces with underscores
            int lastSlashIndex = href.lastIndexOf('/');
            String path = href.substring(0, lastSlashIndex + 1);  // path part before the filename
            String filename = href.substring(lastSlashIndex + 1);  // filename part
            String newFilename = filename.replaceAll(" ", "_");    // Replace spaces with underscores

            // Update the href attribute with the new filename
            linkElement.setAttribute("href", path + newFilename);
        }

        // Save the updated XML back to the file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(document);
        StreamResult result = new StreamResult(configFile);
        transformer.transform(source, result);

        System.out.println("File names updated successfully in config.xml.");
    }
}
