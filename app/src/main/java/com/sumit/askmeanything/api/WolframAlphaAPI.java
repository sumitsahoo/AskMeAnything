package com.sumit.askmeanything.api;

import com.sumit.askmeanything.model.ResultPod;
import com.sumit.askmeanything.parser.ResultPodParser;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by sumit on 6/9/2016.
 */
public class WolframAlphaAPI {

    public static final String APP_ID = "YOUR_APP_ID";
    public static final String BASE_URL = "http://api.wolframalpha.com/v2/query?";

    // Get query results

    public static ArrayList<ResultPod> getQueryResult(String query) {

        String resultXml = makeRestCall(getFormattedUrl(query));

        if(resultXml != null)
            return ResultPodParser.parseResultXml(resultXml);

        return null;
    }

    // Do URL formatting before making REST call

    public static String getFormattedUrl(String query) {

        // URL Example Below :
        // http://api.wolframalpha.com/v2/query?input=whoami&appid=YOUR_APP_ID
        // White spaces needs to be encoded before making the REST call

        try {
            return BASE_URL + "input=" + URLEncoder.encode(query, "utf-8") + "&appid=" + APP_ID;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return null;
    }

    // Initiate REST call

    public static String makeRestCall(String url) {

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = null;

        try {
            response = client.newCall(request).execute();
            if(response != null)
            return response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }
}
