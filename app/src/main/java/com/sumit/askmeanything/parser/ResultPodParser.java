package com.sumit.askmeanything.parser;

import com.sumit.askmeanything.model.ResultPod;

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
public class ResultPodParser {
    public static ArrayList<ResultPod> parseResultXml(String resultXml){

        ArrayList<ResultPod> resultPods = new ArrayList<>();
        int totalResultPods = 0;

        try {
            Document document = loadXMLFromString(resultXml);
            document.getDocumentElement().normalize();

            Element rootElement = (Element) (document.getElementsByTagName("queryresult")).item(0);
            totalResultPods = Integer.parseInt(rootElement.getAttribute("numpods"));

            NodeList pods = rootElement.getElementsByTagName("pod");

            for(int count = 0; count < pods.getLength(); count++){

                Node pod = pods.item(count);
                Element subPodElement = (Element) ((Element) pod).getElementsByTagName("subpod").item(0);
                Element descriptionElement = (Element) (subPodElement.getElementsByTagName("plaintext").item(0));

                ResultPod resultPod = new ResultPod();

                resultPod.setTitle(((Element) pod).getAttribute("title"));
                resultPod.setDescription(descriptionElement.getTextContent());

                resultPods.add(resultPod);

            }

            if(resultPods.size() > 0)
                return resultPods;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Document loadXMLFromString(String xml) throws Exception
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource source = new InputSource(new StringReader(xml));
        return builder.parse(source);
    }
}
