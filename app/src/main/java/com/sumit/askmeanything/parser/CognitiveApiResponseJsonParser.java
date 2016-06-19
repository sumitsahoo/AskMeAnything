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

    public static String getImageUrlFromResponseJson(JsonObject jsonObject) {

        String imageUrl = null;

        JsonArray jsonValueArray = jsonObject.getAsJsonArray("value");

        // We need only one image so take the top one

        JsonObject resultObject = (JsonObject) jsonValueArray.get(0);

        imageUrl = resultObject.get("contentUrl").getAsString();

        return imageUrl;
    }

    public static List<ResultPod> getImageDescription(JsonObject jsonObject, Uri imageFileUri) {

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

        ResultPod personalCard = new ResultPod();
        personalCard.setDefaultCard(false);
        personalCard.setTitle("Person Details");

        String personalDetails = StringUtils.isNotEmpty(celebrityName) ? "Celebrity Name : " + celebrityName : "";
        if (StringUtils.isNotEmpty(celebrityName))
            personalDetails += StringUtils.isNotEmpty(gender) ? "\nDetected Gender : " + gender : "";
        else personalDetails += StringUtils.isNotEmpty(gender) ? "Detected Gender : " + gender : "";
        personalDetails += age != -1 ? "\nDetected Age : " + age : "";

        personalCard.setDescription(personalDetails);

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
            resultPods.add(personalCard);

        if (StringUtils.isNotEmpty(dominantColors))
            resultPods.add(colorCard);

        if (StringUtils.isNotEmpty(tags))
            resultPods.add(tagCard);

        return resultPods;
    }
}
