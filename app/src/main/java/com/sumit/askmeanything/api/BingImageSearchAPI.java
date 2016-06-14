package com.sumit.askmeanything.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sumit.askmeanything.parser.BingImageSearchJsonParser;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by sumit on 6/14/2016.
 */
public class BingImageSearchAPI {

    public static final String SUBSCRIPTION_KEY = "YOUR_KEY_HERE";

    public static String getImageUrl(String query) {

        String imageUrl = null;
        HttpUrl url = null;

        try {

            // Prepare Bing Search URL with parameters
            // End Point URL : https://bingapis.azure-api.net/api/v5/images/search

            url = new HttpUrl.Builder()
                    .scheme("https")
                    .host("bingapis.azure-api.net")
                    .addPathSegment("api")
                    .addPathSegment("v5")
                    .addPathSegment("images")
                    .addPathSegment("search")
                    .addQueryParameter("q", URLEncoder.encode(query, "utf-8"))
                    .addQueryParameter("count", "1")                    // Only one image needed to display
                    .addQueryParameter("offset", "0")
                    .addQueryParameter("mkt", "en-us")
                    .addQueryParameter("size", "medium")                // Keep image size less to load quickly (consider slow network :P)
                    .addQueryParameter("aspect", "square")
                    .addQueryParameter("safeSearch", "moderate")        // Filter out images that are not appropriate
                    .build();

            // Build request and add subscription key header

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .addHeader("Ocp-Apim-Subscription-Key", SUBSCRIPTION_KEY)
                    .url(url)
                    .build();

            Response response = null;

            // Initiate REST call

            response = client.newCall(request).execute();
            if (response != null) {
                JsonObject responseObject = new JsonParser().parse(response.body().string()).getAsJsonObject();
                imageUrl = BingImageSearchJsonParser.getImageUrlFromResponseJson(responseObject);
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return imageUrl;
    }

}
