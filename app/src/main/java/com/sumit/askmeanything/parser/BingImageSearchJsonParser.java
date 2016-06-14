package com.sumit.askmeanything.parser;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Created by sumit on 6/14/2016.
 */
public class BingImageSearchJsonParser {

    public static String getImageUrlFromResponseJson(JsonObject jsonObject) {

        String imageUrl = null;

        JsonArray jsonValueArray = jsonObject.getAsJsonArray("value");

        // We need only one image so take the top one

        JsonObject resultObject = (JsonObject) jsonValueArray.get(0);

        imageUrl = resultObject.get("contentUrl").getAsString();

        return imageUrl;
    }
}
