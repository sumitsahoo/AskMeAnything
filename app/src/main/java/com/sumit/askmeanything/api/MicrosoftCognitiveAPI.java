package com.sumit.askmeanything.api;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sumit.askmeanything.Utils;
import com.sumit.askmeanything.model.ResultPod;
import com.sumit.askmeanything.parser.CognitiveApiResponseJsonParser;

import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by sumit on 6/14/2016.
 */
public class MicrosoftCognitiveAPI {

    public static ArrayList<String> getImageUrl(String query, int imageCount) {

        // Bing image search API reference : https://msdn.microsoft.com/en-us/library/dn760791.aspx

        ArrayList<String> imageUrls = null;
        HttpUrl url = null;

        try {

            // Prepare Bing Search URL with parameters
            // End Point URL : https://api.cognitive.microsoft.com/bing/v5.0/images/search?q=sailing+dinghies&mkt=en-us

            url = new HttpUrl.Builder()
                    .scheme("https")
                    .host("api.cognitive.microsoft.com")
                    .addPathSegment("bing")
                    .addPathSegment("v5.0")
                    .addPathSegment("images")
                    .addPathSegment("search")
                    .addQueryParameter("q", URLEncoder.encode(query, "utf-8"))
                    .addQueryParameter("count", imageCount + "")        // Max images to search
                    .addQueryParameter("offset", "0")
                    .addQueryParameter("mkt", "en-us")
                    .addQueryParameter("size", "medium")                // Keep image size less to load quickly (consider slow network :P)
                    .addQueryParameter("aspect", "square")
                    .addQueryParameter("safeSearch", "moderate")        // Filter out images that are not appropriate
                    .build();

            // Build request and add subscription key header

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .addHeader("Ocp-Apim-Subscription-Key", Utils.MS_IMAGE_SEARCH_SUBSCRIPTION_KEY)
                    .url(url)
                    .build();

            Response response = null;

            // Initiate REST call

            response = client.newCall(request).execute();
            if (response != null) {
                JsonObject responseObject = new JsonParser().parse(response.body().string()).getAsJsonObject();
                imageUrls = CognitiveApiResponseJsonParser.parseImageUrlFromResponseJson(responseObject);
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return imageUrls;
    }

    // Describe image

    public static ArrayList<ResultPod> getImageDescription(Uri imageFileUri, Context context) {

        // Vision API reference : https://azure.microsoft.com/en-us/services/cognitive-services/computer-vision/

        InputStream inputStream = null;

        try {
            inputStream = context.getContentResolver().openInputStream(imageFileUri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        Bitmap originalImage = BitmapFactory.decodeStream(inputStream);

        HttpUrl url = null;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        originalImage.compress(Bitmap.CompressFormat.JPEG, 70, out);

        try {

            // Prepare Image Recognition URL with parameters
            // End Point URL : https://westcentralus.api.cognitive.microsoft.com/vision/v1.0/analyze

            url = new HttpUrl.Builder()
                    .scheme("https")
                    .host("westcentralus.api.cognitive.microsoft.com")
                    .addPathSegment("vision")
                    .addPathSegment("v1.0")
                    .addPathSegment("analyze")
                    .addQueryParameter("visualFeatures", "description,tags,faces,color")
                    .addQueryParameter("details", "celebrities")
                    .build();

            RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("image", getImageFileName(imageFileUri.toString()), RequestBody.create(Utils.MEDIA_TYPE_BINARY_JPG, out.toByteArray()))
                    .build();


            // Build request and add subscription key header

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .addHeader("Ocp-Apim-Subscription-Key", Utils.MS_COMPUTER_VISION_SUBSCRIPTION_KEY)
                    .addHeader("Content-Type", "application/octet-stream")
                    .url(url)
                    .post(requestBody)
                    .build();

            Response response = null;

            // Initiate REST call

            response = client.newCall(request).execute();
            if (response != null) {
                JsonObject responseObject = new JsonParser().parse(response.body().string()).getAsJsonObject();
                return (ArrayList<ResultPod>) CognitiveApiResponseJsonParser.parseImageDescriptionFromResponseJson(responseObject, imageFileUri);
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    // Read text from image

    public static ArrayList<ResultPod> getOCRText(Uri imageFileUri, Context context) {

        InputStream inputStream = null;

        try {
            inputStream = context.getContentResolver().openInputStream(imageFileUri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        Bitmap originalImage = BitmapFactory.decodeStream(inputStream);

        HttpUrl url = null;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        originalImage.compress(Bitmap.CompressFormat.JPEG, 70, out);

        try {

            // Prepare Image Recognition URL with parameters
            // End Point URL : https://westcentralus.api.cognitive.microsoft.com/vision/v1.0/ocr[?language][&detectOrientation ]

            url = new HttpUrl.Builder()
                    .scheme("https")
                    .host("westcentralus.api.cognitive.microsoft.com")
                    .addPathSegment("vision")
                    .addPathSegment("v1.0")
                    .addPathSegment("ocr")
                    .addQueryParameter("language", "en")
                    .build();

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", getImageFileName(imageFileUri.toString()), RequestBody.create(Utils.MEDIA_TYPE_BINARY_JPG, out.toByteArray()))
                    .build();


            // Build request and add subscription key header

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .addHeader("Ocp-Apim-Subscription-Key", Utils.MS_COMPUTER_VISION_SUBSCRIPTION_KEY)
                    .addHeader("Content-Type", "application/octet-stream")
                    .url(url)
                    .post(requestBody)
                    .build();

            Response response = null;

            // Initiate REST call

            response = client.newCall(request).execute();
            if (response != null) {
                JsonObject responseObject = new JsonParser().parse(response.body().string()).getAsJsonObject();
                return (ArrayList<ResultPod>) CognitiveApiResponseJsonParser.parseOcrTextFromResponseJson(responseObject, imageFileUri);
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    // Detect emotion

    public static ArrayList<ResultPod> detectHumanEmotion(Uri imageFileUri, Context context) {

        InputStream inputStream = null;

        try {
            inputStream = context.getContentResolver().openInputStream(imageFileUri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        Bitmap originalImage = BitmapFactory.decodeStream(inputStream);

        HttpUrl url = null;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        originalImage.compress(Bitmap.CompressFormat.JPEG, 70, out);

        try {

            // Prepare Image Recognition URL with parameters
            // End Point URL : https://westus.api.cognitive.microsoft.com/emotion/v1.0/recognize

            url = new HttpUrl.Builder()
                    .scheme("https")
                    .host("westus.api.cognitive.microsoft.com")
                    .addPathSegment("emotion")
                    .addPathSegment("v1.0")
                    .addPathSegment("recognize")
                    .build();

            RequestBody requestBody = RequestBody.create(Utils.MEDIA_TYPE_OCTET_STREAM, out.toByteArray(), 0, out.toByteArray().length);

            // Build request and add subscription key header

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .addHeader("Ocp-Apim-Subscription-Key", Utils.MS_EMOTION_SUBSCRIPTION_KEY)
                    .addHeader("Content-Type", "application/octet-stream")
                    .url(url)
                    .post(requestBody)
                    .build();

            Response response = null;

            // Initiate REST call

            response = client.newCall(request).execute();
            if (response != null) {
                JsonArray responseArray = new JsonParser().parse(response.body().string()).getAsJsonArray();
                return (ArrayList<ResultPod>) CognitiveApiResponseJsonParser.parseEmotionFromResponseJson(responseArray, imageFileUri);
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String getImageFileName(String imageFilePath) {
        return StringUtils.substringAfterLast(imageFilePath, "/");
    }

}
