package com.sumit.askmeanything.api;

import android.graphics.Bitmap;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sumit.askmeanything.parser.CognitiveApiResponseJsonParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by sumit on 6/14/2016.
 */
public class MicrosoftCognitiveAPI {

    public static final String IMAGE_SEARCH_SUBSCRIPTION_KEY = "YOUR_SUBSCRIPTION_KEY";
    public static final String COMPUTER_VISION_SUBSCRIPTION_KEY = "YOUR_SUBSCRIPTION_KEY";
    public static final String EMOTION_SUBSCRIPTION_KEY = "YOUR_SUBSCRIPTION_KEY";

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
                    .addHeader("Ocp-Apim-Subscription-Key", IMAGE_SEARCH_SUBSCRIPTION_KEY)
                    .url(url)
                    .build();

            Response response = null;

            // Initiate REST call

            response = client.newCall(request).execute();
            if (response != null) {
                JsonObject responseObject = new JsonParser().parse(response.body().string()).getAsJsonObject();
                imageUrl = CognitiveApiResponseJsonParser.getImageUrlFromResponseJson(responseObject);
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return imageUrl;
    }

    public static String getImageDescription(Bitmap originalImage) {

        String imageDescription = null;
        HttpUrl url = null;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        originalImage.compress(Bitmap.CompressFormat.JPEG, 70, out);
        //Bitmap decodedImage = BitmapFactory.decodeStream(new ByteArrayInputStream(out.toByteArray()));


        try {

            // Prepare Image Recognition URL with parameters
            // End Point URL : https://api.projectoxford.ai/vision/v1.0/analyze[?visualFeatures][&details]

            url = new HttpUrl.Builder()
                    .scheme("https")
                    .host("api.projectoxford.ai")
                    .addPathSegment("vision")
                    .addPathSegment("v1.0")
                    .addPathSegment("analyze")
                    .addQueryParameter("visualFeatures", "description,tags,faces,color")
                    .addQueryParameter("details", "celebrities")
                    .build();

            RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("image", "fileName", RequestBody.create(MediaType.parse("image/jpeg"), out.toByteArray()))
                    .build();


            // Build request and add subscription key header

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .addHeader("Ocp-Apim-Subscription-Key", COMPUTER_VISION_SUBSCRIPTION_KEY)
                    .addHeader("Content-Type", "application/octet-stream")
                    .url(url)
                    .post(requestBody)
                    .build();

            Response response = null;

            // Initiate REST call

            response = client.newCall(request).execute();
            if (response != null) {
                JsonObject responseObject = new JsonParser().parse(response.body().string()).getAsJsonObject();
                imageDescription = CognitiveApiResponseJsonParser.getImageDescription(responseObject);
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return imageDescription;
    }

}
