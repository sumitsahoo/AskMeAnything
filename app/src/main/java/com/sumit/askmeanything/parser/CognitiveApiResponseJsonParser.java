package com.sumit.askmeanything.parser;

import android.net.Uri;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sumit.askmeanything.model.ResultPod;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sumit on 6/14/2016.
 */
public class CognitiveApiResponseJsonParser {

    public static ArrayList<String> parseImageUrlFromResponseJson(JsonObject jsonObject) {

        ArrayList<String> imageUrls = null;

        JsonArray jsonValueArray = jsonObject.getAsJsonArray("value");

        if (jsonValueArray != null && jsonValueArray.size() > 0) {

            imageUrls = new ArrayList<>();

            for (JsonElement resultObject : jsonValueArray) {
                imageUrls.add(((JsonObject) resultObject).get("contentUrl").getAsString());
            }
        }

        return imageUrls;
    }

    public static List<ResultPod> parseImageDescriptionFromResponseJson(JsonObject jsonObject, Uri imageFileUri) {

        String imageDescription = null;
        String tags = null;

        String celebrityName = null;

        int age = -1;
        String gender = null;

        String dominantColors = null;
        boolean isBWImage = false;

        // Description

        JsonObject descriptionObject = jsonObject.getAsJsonObject("description");
        JsonObject captionObject = (JsonObject) descriptionObject.getAsJsonArray("captions").get(0);

        imageDescription = captionObject.get("text").getAsString();

        // Celebrity Name

        JsonArray celebrityArray = jsonObject.getAsJsonArray("categories");

        if (celebrityArray != null && celebrityArray.size() > 0) {
            JsonObject celebrityObject = celebrityArray.get(0).getAsJsonObject().getAsJsonObject("detail");

            if (celebrityObject != null) {
                try {
                    celebrityName = ((JsonObject) celebrityObject.getAsJsonArray("celebrities").get(0)).get("name").getAsString();
                } catch (IndexOutOfBoundsException e) {
                    celebrityName = null;
                }
            }
        }

        // Faces : Age and Gender

        JsonArray faceArray = jsonObject.getAsJsonArray("faces");

        if (faceArray != null && faceArray.size() > 0) {
            JsonObject faceObject = (JsonObject) faceArray.get(0);

            if (faceObject.get("age") != null)
                age = faceObject.get("age").getAsInt();

            if (faceObject.get("gender") != null)
                gender = faceObject.get("gender").getAsString();
        }

        // Dominant Colors

        JsonObject colorObject = jsonObject.getAsJsonObject("color");

        if (colorObject != null) {
            JsonArray colorArray = colorObject.getAsJsonArray("dominantColors");

            if (colorArray != null && colorArray.size() > 0) {
                for (JsonElement dominantColorElement : colorArray) {
                    if (dominantColors == null) {
                        dominantColors = dominantColorElement.getAsString();
                    } else {
                        dominantColors += ", " + dominantColorElement.getAsString();
                    }
                }
            }
        }

        // Tags

        JsonArray tagsArray = descriptionObject.getAsJsonArray("tags");

        for (JsonElement tagElement : tagsArray) {
            if (tags == null) {
                tags = tagElement.getAsString();
            } else tags += ", " + tagElement.getAsString();
        }

        // Prepare full image description

        List<ResultPod> resultPods = new ArrayList<>();

        ResultPod descriptionCard = new ResultPod();
        descriptionCard.setDescription(StringUtils.capitalize(imageDescription));
        descriptionCard.setTitle("Image Description");
        descriptionCard.setDefaultCard(true);
        descriptionCard.setImageSource(imageFileUri.toString());

        ResultPod personCard = new ResultPod();
        personCard.setDefaultCard(false);
        personCard.setTitle("Person Details");

        String personDetails = StringUtils.isNotEmpty(celebrityName) ? "Name : " + celebrityName : "";
        if (StringUtils.isNotEmpty(celebrityName))
            personDetails += StringUtils.isNotEmpty(gender) ? "\nDetected Gender : " + gender : "";
        else personDetails = StringUtils.isNotEmpty(gender) ? "Detected Gender : " + gender : "";
        personDetails += age != -1 ? "\nDetected Age : " + age : "";

        personCard.setDescription(personDetails);

        ResultPod colorCard = new ResultPod();
        colorCard.setTitle("Dominant Colors");
        colorCard.setDescription(dominantColors);
        colorCard.setDefaultCard(false);

        ResultPod tagCard = new ResultPod();
        tagCard.setTitle("Image Tags");
        tagCard.setDefaultCard(false);
        tagCard.setDescription(tags);

        // Add results to ArrayList

        resultPods.add(descriptionCard);

        if (StringUtils.isNotEmpty(celebrityName) || StringUtils.isNotEmpty(gender) || age != -1)
            resultPods.add(personCard);

        if (StringUtils.isNotEmpty(dominantColors))
            resultPods.add(colorCard);

        if (StringUtils.isNotEmpty(tags))
            resultPods.add(tagCard);

        return resultPods;
    }

    // Parse OCR Response JSON

    public static List<ResultPod> parseOcrTextFromResponseJson(JsonObject jsonObject, Uri imageFileUri) {

        ArrayList<ResultPod> resultPods = null;
        String ocrResponseText = null;

        JsonArray regionArray = jsonObject.getAsJsonArray("regions");

        if (regionArray != null && regionArray.size() > 0) {
            for (JsonElement regionObject : regionArray) {
                JsonArray linesArray = ((JsonObject) regionObject).getAsJsonArray("lines");

                if (linesArray != null && linesArray.size() > 0) {
                    for (JsonElement lineObject : linesArray) {
                        JsonArray wordsArray = ((JsonObject) lineObject).getAsJsonArray("words");

                        if (wordsArray != null && wordsArray.size() > 0) {

                            int wordCount = 0;

                            for (JsonElement wordObject : wordsArray) {

                                if (StringUtils.isEmpty(ocrResponseText)) {
                                    ocrResponseText = ((JsonObject) wordObject).get("text").getAsString();
                                } else {
                                    if (wordCount == 0)
                                        ocrResponseText += ((JsonObject) wordObject).get("text").getAsString();
                                    else
                                        ocrResponseText += " " + ((JsonObject) wordObject).get("text").getAsString();
                                }

                                wordCount++;
                            }
                        }

                        ocrResponseText += "\n";
                    }
                }
            }
        }

        if (StringUtils.isNotEmpty(ocrResponseText)) {
            ResultPod resultPod = new ResultPod();
            resultPod.setDefaultCard(true);
            resultPod.setTitle("OCR Text");
            resultPod.setImageSource(imageFileUri.toString());
            resultPod.setDescription(ocrResponseText);

            resultPods = new ArrayList<>();
            resultPods.add(resultPod);
        }

        return resultPods;
    }
}
