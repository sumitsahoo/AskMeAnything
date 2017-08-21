package com.sumit.askmeanything.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.sumit.askmeanything.Utils;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


/**
 * Created by Sumit 8/18/2017.
 */

public class GoogleCustomSearchAPI {

    public static ArrayList<String> getImageUrl(String query, int imageCount) {

        ArrayList<String> imageUrls = null;

        String baseUrl = "https://www.googleapis.com/customsearch/v1";
        final String url = baseUrl + "?q=" + query + "&cx=" + Utils.GOOGLE_CUSTOM_SEARCH_CX_CODE + "&fileType=jpg&imgSize=medium&imgType=photo&num=" + imageCount + "&safe=high&searchType=image&key=" + Utils.GOOGLE_CUSTOM_SEARCH_API_KEY;

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try {
            Response response = client.newCall(request).execute();

            if (response != null) {
                JsonObject responseJson = new JsonParser().parse(response.body().string()).getAsJsonObject();
                JsonArray imageResultArray = responseJson.getAsJsonArray("items");

                if (imageResultArray != null && imageResultArray.size() > 0) {
                    imageUrls = new ArrayList<>();
                    for (JsonElement jsonElement : imageResultArray) {
                        imageUrls.add(((JsonObject) jsonElement).get("link").getAsString());
                    }
                }
            }

            if (imageUrls != null)
                return imageUrls;

        } catch (IOException | JsonParseException e) {
            e.printStackTrace();
        }

        return null;
    }
}
