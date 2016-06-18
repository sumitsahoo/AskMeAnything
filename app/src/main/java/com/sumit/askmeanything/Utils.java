package com.sumit.askmeanything;

import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by sumit on 6/10/2016.
 */
public class Utils {

    public static final String WIKI_IMAGE_BASE_URL = "https://upload.wikimedia.org/wikipedia/commons/";

    public static String getWikiImageURL(String wikiFileUrl) {
        String imageUrl = null;

        String imageName = StringUtils.substringAfter(wikiFileUrl, "File:");

        // General url format : https://upload.wikimedia.org/wikipedia/commons/a/ab/image_name.ext
        // a : 1st char of MD5 of the file name
        // b : 2nd chat of MD5 of the file name

        String fileMD5 = calculateMD5(imageName);

        imageUrl = WIKI_IMAGE_BASE_URL + fileMD5.charAt(0) + "/" + fileMD5.charAt(0) + fileMD5.charAt(1) + "/" + imageName;

        return imageUrl;
    }

    public static String calculateMD5(String wikiImageFileName) {
        MessageDigest md = null;
        StringBuffer stringBuffer = null;

        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        md.update(wikiImageFileName.getBytes());

        byte byteData[] = md.digest();

        //convert the byte to hex format
        stringBuffer = new StringBuffer();

        for (int i = 0; i < byteData.length; i++) {
            stringBuffer.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
        }

        return stringBuffer.toString();
    }

    // Show multiline snackbar with message and action

    public static void showMultiLineSnackBar(CoordinatorLayout coordinatorLayout, String contentMessage, String actionMessage, View.OnClickListener mOnClickListener) {

        Snackbar snackbar = Snackbar
                .make(coordinatorLayout, contentMessage, Snackbar.LENGTH_INDEFINITE)
                .setAction(actionMessage, mOnClickListener);
        snackbar.setActionTextColor(Color.YELLOW);

        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(Color.DKGRAY);

        TextView textView = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setMaxLines(6);

        //textView.setTextColor(Color.WHITE);
        //textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);

        //TextView snackbarAction = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_action);
        //snackbarAction.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);

        snackbar.show();
    }

    // Check network connectivity

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
