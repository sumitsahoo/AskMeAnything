package com.sumit.askmeanything.api;

import com.sumit.askmeanything.model.ResultPod;
import com.sumit.askmeanything.parser.ResultPodXmlParser;

import org.apache.commons.lang3.StringUtils;

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

    public static final String APP_ID = "YOUR_APP_ID_HERE";
    public static final String BASE_URL = "http://api.wolframalpha.com/v2/query?";

    // Get query results

    public static ArrayList<ResultPod> getQueryResult(String query) {

        String resultXml = makeRestCall(getFormattedUrl(query));

        if (resultXml != null)
            return ResultPodXmlParser.parseResultXml(resultXml, query);

        return null;
    }

    // Do URL formatting before making REST call

    public static String getFormattedUrl(String query) {

        // URL Example Below :
        // http://api.wolframalpha.com/v2/query?input=whoami&appid=YOUR_APP_ID
        // White spaces needs to be encoded before making the REST call

        // Remove .gif .png .jpg .jpeg from search string
        // Add more checks for extensions if needed

        String finalQuery = StringUtils.remove(query, " gif");
        finalQuery = StringUtils.remove(finalQuery, ".gif");
        finalQuery = StringUtils.remove(finalQuery, " png");
        finalQuery = StringUtils.remove(finalQuery, " .png");
        finalQuery = StringUtils.remove(finalQuery, " jpg");
        finalQuery = StringUtils.remove(finalQuery, " .jpg");
        finalQuery = StringUtils.remove(finalQuery, " jpeg");
        finalQuery = StringUtils.remove(finalQuery, " .jpeg");

        // If searched for only extension then re add the query

        if (StringUtils.isEmpty(finalQuery))
            finalQuery = query;

        try {
            return BASE_URL + "input=" + URLEncoder.encode(finalQuery, "utf-8") + "&appid=" + APP_ID;
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
            if (response != null)
                return response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }
}
