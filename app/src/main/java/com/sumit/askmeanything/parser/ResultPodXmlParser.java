package com.sumit.askmeanything.parser;

import com.sumit.askmeanything.Utils;
import com.sumit.askmeanything.api.BingImageSearchAPI;
import com.sumit.askmeanything.model.ResultPod;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Created by sumit on 6/9/2016.
 */
public class ResultPodXmlParser {
    public static ArrayList<ResultPod> parseResultXml(String resultXml, String query) {

        ArrayList<ResultPod> resultPods = new ArrayList<>();
        int totalResultPods = 0;

        try {
            Document document = loadXMLFromString(resultXml);
            document.getDocumentElement().normalize();

            Element rootElement = (Element) (document.getElementsByTagName("queryresult")).item(0);
            totalResultPods = Integer.parseInt(rootElement.getAttribute("numpods"));

            NodeList pods = rootElement.getElementsByTagName("pod");

            for (int count = 0; count < pods.getLength(); count++) {

                Node pod = pods.item(count);
                Element subPodElement = (Element) ((Element) pod).getElementsByTagName("subpod").item(0);
                Element descriptionElement = (Element) (subPodElement.getElementsByTagName("plaintext").item(0));

                ResultPod resultPod = new ResultPod();

                resultPod.setDefaultCard(false);

                // Set result pod title and description

                resultPod.setTitle(((Element) pod).getAttribute("title"));

                if (StringUtils.isNotEmpty(descriptionElement.getTextContent()))
                    resultPod.setDescription(descriptionElement.getTextContent());
                else resultPod.setDescription("More information not available at the moment.");

                // Set image source URL if any

                Element imageElement = (Element) (subPodElement.getElementsByTagName("imagesource").item(0));
                if (imageElement != null && StringUtils.isNotEmpty(imageElement.getTextContent())) {

                    // If image is present in result pods then add either get from Wikipedia or Bing Image Search API
                    // Parse Wikipedia image link

                    if (StringUtils.containsIgnoreCase(imageElement.getTextContent(), "wikipedia.org") && (!StringUtils.containsIgnoreCase(query, " gif") || !StringUtils.containsIgnoreCase(query, ".gif")))
                        resultPod.setImageSource(Utils.getWikiImageURL(imageElement.getTextContent()));
                    else {

                        // If image is not from Wikipedia then search using Bing Image API instead

                        String imageUrl = BingImageSearchAPI.getImageUrl(query);

                        if (StringUtils.isNotEmpty(imageUrl))
                            resultPod.setImageSource(imageUrl);
                    }
                }

                // Do not add information if both image source and description is missing

                if (StringUtils.isNotEmpty(resultPod.getImageSource()) || StringUtils.isNotEmpty(resultPod.getDescription()))
                    resultPods.add(resultPod);
            }

            if (resultPods.size() > 0)
                return resultPods;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Document loadXMLFromString(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource source = new InputSource(new StringReader(xml));
        return builder.parse(source);
    }
}
