package com.sumit.askmeanything;

import android.Manifest;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.SearchRecentSuggestions;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.sumit.askmeanything.adapter.ResultPodAdapter;
import com.sumit.askmeanything.api.GoogleCustomSearchAPI;
import com.sumit.askmeanything.api.MicrosoftCognitiveAPI;
import com.sumit.askmeanything.api.WolframAlphaAPI;
import com.sumit.askmeanything.contentprovider.SearchSuggestionProvider;
import com.sumit.askmeanything.model.ResultPod;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_CODE = 100;
    private static final int MAX_IMAGE_SEARCH_LIMIT = 10;

    private Context context;
    private CoordinatorLayout coordinatorLayout;
    private FloatingActionButton fab;
    private ProgressDialog progressDialog;
    private TextToSpeech textToSpeech;
    private SearchView searchView;
    private RecyclerView recyclerView;
    private LinearLayoutManager linearLayoutManager;
    private AlertDialog aboutDialog;
    private ResultPodAdapter resultPodAdapter;
    private Uri imageFileUri;


    private int cameraPermissionCheck = PackageManager.PERMISSION_DENIED;
    private int externalStoragePermissionCheck = PackageManager.PERMISSION_DENIED;

    private final int QUERY = 1;
    private final int IMAGE_DESCRIPTION = 2;
    private final int OCR = 3;
    private final int EMOTION = 4;

    private SearchTask searchTask;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;

        initFrescoLibrary();

        initViews();
        loadDefaultCard();

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1)
            verifyAndRequestPermission();
    }

    // Initialize Fresco Image Library

    private void initFrescoLibrary() {
        ImagePipelineConfig config = ImagePipelineConfig.newBuilder(context)
                //.setBitmapMemoryCacheParamsSupplier(bitmapCacheParamsSupplier)
                //.setCacheKeyFactory(cacheKeyFactory)
                .setDownsampleEnabled(true) // This reduces strain on GPU and keeps UI fluid
                //.setWebpSupportEnabled(true)
                //.setEncodedMemoryCacheParamsSupplier(encodedCacheParamsSupplier)
                //.setExecutorSupplier(executorSupplier)
                //.setImageCacheStatsTracker(imageCacheStatsTracker)
                //.setMainDiskCacheConfig(mainDiskCacheConfig)
                //.setMemoryTrimmableRegistry(memoryTrimmableRegistry)
                //.setNetworkFetchProducer(networkFetchProducer)
                //.setPoolFactory(poolFactory)
                //.setProgressiveJpegConfig(progressiveJpegConfig)
                //.setRequestListeners(requestListeners)
                //.setSmallImageDiskCacheConfig(smallImageDiskCacheConfig)
                .build();

        Fresco.initialize(context, config);
    }

    private void verifyAndRequestPermission() {
        // For marshmallow we need to manually check and prompt for permission

        cameraPermissionCheck = ContextCompat.checkSelfPermission(context,
                Manifest.permission.CAMERA);

        externalStoragePermissionCheck = ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);


        if (cameraPermissionCheck != PackageManager.PERMISSION_GRANTED || externalStoragePermissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_CODE);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                        cameraPermissionCheck = PackageManager.PERMISSION_GRANTED;
                    if (grantResults[1] == PackageManager.PERMISSION_GRANTED)
                        externalStoragePermissionCheck = PackageManager.PERMISSION_GRANTED;

                }
                return;
            }
        }
    }

    private void keepSearchHistory(String query) {
        SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
                SearchSuggestionProvider.AUTHORITY, SearchSuggestionProvider.MODE);
        suggestions.saveRecentQuery(query, null);
    }

    private void clearSearchHistory() {
        SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
                SearchSuggestionProvider.AUTHORITY, SearchSuggestionProvider.MODE);
        suggestions.clearHistory();
    }

    private void initViews() {

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("");
        toolbar.setSubtitle("");

        getSupportActionBar().setTitle("");
        getSupportActionBar().setSubtitle("");

        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout);

        // Setup views

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        // Setting the size as fixed improves the performance
        recyclerView.setHasFixedSize(true);

        linearLayoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(linearLayoutManager);

        fab = (FloatingActionButton) findViewById(R.id.fab);

        // Add action listeners

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (searchView != null) {
                    keepSearchHistory(searchView.getQuery().toString());
                    initiateSearch(searchView.getQuery().toString(), QUERY);
                }
            }
        });

    }

    private void initTextToSpeech() {
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.US);
                }
            }
        });
    }

    private void stopTextToSpeech() {
        if (textToSpeech != null && textToSpeech.isSpeaking())
            textToSpeech.stop();
    }


    @Override
    protected void onNewIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);

            if (searchView != null) {
                searchView.setQuery(query, false);
                searchView.clearFocus();
            }

            keepSearchHistory(query);
            initiateSearch(query, QUERY);
        }
    }

    // Start Async API calls and retrieve search results

    private void initiateSearch(String query, int searchType) {

        if (searchTask != null) {
            searchTask.cancel(true);
            searchTask = null;
        }

        String[] queryParameter = {query, searchType + ""};
        searchTask = new SearchTask();
        searchTask.execute(queryParameter);
    }

    private void loadDefaultCard() {

        ResultPod resultPod = new ResultPod();
        resultPod.setTitle(getString(R.string.welcome));
        resultPod.setDescription(getString(R.string.welcome_description));
        resultPod.setDefaultCard(true);

        List<ResultPod> resultPods = new ArrayList<>();
        resultPods.add(resultPod);

        resultPodAdapter = new ResultPodAdapter(resultPods);
        recyclerView.setAdapter(resultPodAdapter);
    }

    private void populateResult(ArrayList<ResultPod> resultPods) {

        resultPodAdapter = new ResultPodAdapter(resultPods);
        recyclerView.setAdapter(resultPodAdapter);

        // Generally result pod 2 will have main answer to the query
        // If image recognition is used then description will be in 0th element

        String mainResult = null;

        try {
            mainResult = resultPods.get(1).getDescription();
        } catch (Exception e) {
            // This means there is no 2nd pod.
            mainResult = resultPods.get(0).getDescription();
        }

        if (StringUtils.isNotEmpty(mainResult)) {

            stopTextToSpeech();
            textToSpeech.speak(mainResult, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    private void toggleProgressBar(boolean isShow) {

        if (progressDialog != null && isShow && !progressDialog.isShowing()) {
            progressDialog.show();
        } else {
            if (progressDialog != null) {
                progressDialog.dismiss();
            }
        }

    }

    private void showInformation(String messageContent, String actionMessage) {

        Utils.showMultiLineSnackBar(coordinatorLayout, messageContent, actionMessage, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Do action if needed
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        // Get SearchView

        MenuItem menuItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) MenuItemCompat.getActionView(menuItem);
        searchView.setQueryRefinementEnabled(true);
        searchView.setIconifiedByDefault(false);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                keepSearchHistory(query);
                initiateSearch(query, QUERY);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (searchView != null) {
            //searchView.requestFocus();
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        int id = item.getItemId();

        //noinspection SimplifiableIfStatement

        if (id == R.id.action_about) {
            showAboutDialog();
            searchView.clearFocus();
        } else if (id == R.id.action_clear_history) {
            clearSearchHistory();
            deleteImageHistory();
            searchView.clearFocus();
        } else if (id == R.id.action_clear_result) {
            clearResult();
            searchView.clearFocus();
        } else if (id == R.id.action_recognize_photo) {

            stopTextToSpeech();

            if (cameraPermissionCheck == PackageManager.PERMISSION_GRANTED && externalStoragePermissionCheck == PackageManager.PERMISSION_GRANTED) {
                startImageCapture(IMAGE_DESCRIPTION);
            } else {
                verifyAndRequestPermission();
            }
        } else if (id == R.id.action_run_ocr) {
            stopTextToSpeech();

            if (cameraPermissionCheck == PackageManager.PERMISSION_GRANTED && externalStoragePermissionCheck == PackageManager.PERMISSION_GRANTED) {
                startImageCapture(OCR);
            } else {
                verifyAndRequestPermission();
            }
        } else if (id == R.id.action_detect_emotion) {
            if (cameraPermissionCheck == PackageManager.PERMISSION_GRANTED && externalStoragePermissionCheck == PackageManager.PERMISSION_GRANTED) {
                startImageCapture(EMOTION);
            } else {
                verifyAndRequestPermission();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private static File getOutputMediaFile() {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "AskMeAnything");

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return new File(mediaStorageDir.getPath() + File.separator +
                "IMG_" + timeStamp + ".jpg");
    }

    // Delete stored images from directory

    private boolean deleteImageHistory() {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "AskMeAnything");

        if (mediaStorageDir.exists() && mediaStorageDir.isDirectory() && mediaStorageDir.canWrite()) {
            try {
                FileUtils.cleanDirectory(mediaStorageDir);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    private void startImageCapture(int searchType) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // From API 24 onwards file:// Uri sharing is not allowed through intent.
        // Use FileProvider to share Uri instead
        imageFileUri = FileProvider.getUriForFile(MainActivity.this,
                BuildConfig.APPLICATION_ID + ".provider",
                getOutputMediaFile());

        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageFileUri);

        startActivityForResult(intent, searchType);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == IMAGE_DESCRIPTION && resultCode == RESULT_OK && imageFileUri != null) {
            // Start searching for the description for the image taken
            initiateSearch("", IMAGE_DESCRIPTION);
        } else if (requestCode == OCR && resultCode == RESULT_OK && imageFileUri != null) {
            // Start OCR
            initiateSearch("", OCR);
        } else if (requestCode == EMOTION && resultCode == RESULT_OK && imageFileUri != null) {
            // Initiate Emotion Detection
            initiateSearch("", EMOTION);
        }
    }

    private void clearResult() {

        stopTextToSpeech();
        recyclerView.setAdapter(null);
        loadDefaultCard();
        deleteImageHistory();
    }

    private void showAboutDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(getString(R.string.about_dialog_title));
        alertDialogBuilder.setMessage(getString(R.string.about_dev));

        alertDialogBuilder.setPositiveButton(getString(R.string.okay), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        aboutDialog = alertDialogBuilder.create();
        aboutDialog.show();
    }

    @Override
    protected void onPause() {
        super.onPause();

        toggleProgressBar(false);

        // Prevent alert dialog window leak

        if (aboutDialog != null)
            aboutDialog.dismiss();

        // Stop Speech Engine

        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Setup progress bar

        if (progressDialog == null) {
            progressDialog = new ProgressDialog(context);
            progressDialog.setMessage(getString(R.string.wait_message));
            progressDialog.setCancelable(false);
            progressDialog.setIndeterminate(true);
        }

        initTextToSpeech();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Cancel background task if any to prevent memory leak

        if (searchTask != null) {
            searchTask.cancel(true);
            searchTask = null;
        }
    }

    private class SearchTask extends AsyncTask<String, Void, ArrayList<ResultPod>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            searchView.clearFocus();
            stopTextToSpeech();
            toggleProgressBar(true);
        }

        @Override
        protected ArrayList<ResultPod> doInBackground(String... params) {

            if (!Utils.isNetworkAvailable(context))
                return null;

            int searchType = Integer.parseInt(params[1]);

            if (searchType == QUERY && !StringUtils.isEmpty(params[0])) {

                if ((StringUtils.containsIgnoreCase(params[0], " gif") || StringUtils.containsIgnoreCase(params[0], ".gif")) && params[0].trim().length() > 4) {

                    // This means user is trying a gif image search

                    //ArrayList<String> imageUrls = MicrosoftCognitiveAPI.getImageUrl(params[0], MAX_IMAGE_SEARCH_LIMIT);
                    ArrayList<String> imageUrls = GoogleCustomSearchAPI.getImageUrl(params[0], MAX_IMAGE_SEARCH_LIMIT);

                    if (imageUrls != null && imageUrls.size() > 0) {

                        ArrayList<ResultPod> resultPods = new ArrayList<>();

                        for (String imageUrl : imageUrls) {
                            ResultPod resultPod = new ResultPod();
                            resultPod.setDefaultCard(false);
                            resultPod.setImageSource(imageUrl);

                            resultPods.add(resultPod);
                        }

                        return resultPods;
                    }

                } else {

                    // Normal query search i.e. Language processing

                    return WolframAlphaAPI.getQueryResult(params[0]);
                }
            } else if (searchType == IMAGE_DESCRIPTION && imageFileUri != null) {
                return MicrosoftCognitiveAPI.getImageDescription(imageFileUri, context);
            } else if (searchType == OCR && imageFileUri != null) {
                return MicrosoftCognitiveAPI.getOCRText(imageFileUri, context);
            } else if (searchType == EMOTION && imageFileUri != null) {
                return MicrosoftCognitiveAPI.detectHumanEmotion(imageFileUri, context);
            }

            return null;
        }

        @Override
        protected void onPostExecute(ArrayList<ResultPod> resultPods) {
            super.onPostExecute(resultPods);

            toggleProgressBar(false);

            if (resultPods != null && resultPods.size() > 0) {
                populateResult(resultPods);
            } else if (!Utils.isNetworkAvailable(context)) {
                showInformation(getString(R.string.error_network_not_available), getString(R.string.okay));
            } else
                showInformation(getString(R.string.error_unable_to_search), getString(R.string.dismiss));
        }
    }
}
