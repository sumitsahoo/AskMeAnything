package com.sumit.askmeanything.parser;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.commons.lang3.StringUtils;

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

    public static String getImageDescription(JsonObject jsonObject) {

        String fullDescription = null;

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
                celebrityName = ((JsonObject) celebrityObject.getAsJsonArray("celebrities").get(0)).get("name").getAsString();
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

        fullDescription = "Description : " + imageDescription;
        fullDescription += StringUtils.isNotEmpty(celebrityName) ? "\n\nCelebrity Name : " + celebrityName : "";
        fullDescription += StringUtils.isNotEmpty(gender) ? "\n\nDetected Gender : " + gender : "";
        fullDescription += age != -1 ? "\nDetected Age : " + age : "";
        fullDescription += StringUtils.isNotEmpty(dominantColors) ? "\n\nDominant Colors : " + dominantColors : "";
        fullDescription += "\n\nTags : " + tags;

        return fullDescription;
    }
}
