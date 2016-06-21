package com.sumit.askmeanything;

import android.Manifest;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
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
import android.support.v4.view.MenuItemCompat;
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
import com.sumit.askmeanything.api.MicrosoftCognitiveAPI;
import com.sumit.askmeanything.api.WolframAlphaAPI;
import com.sumit.askmeanything.contentprovider.SearchSuggestionProvider;
import com.sumit.askmeanything.model.ResultPod;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_CODE = 100;
    private Context context;
    private CoordinatorLayout coordinatorLayout;
    private FloatingActionButton fab;
    private ProgressDialog progressDialog;
    private TextToSpeech textToSpeech;
    private SearchView searchView;
    private RecyclerView recyclerView;
    private LinearLayoutManager linearLayoutManager;
    private ResultPodAdapter resultPodAdapter;
    private Uri imageFileUri;
    private int cameraPermissionCheck = PackageManager.PERMISSION_DENIED;
    private int externalStoragePermissionCheck = PackageManager.PERMISSION_DENIED;

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
                    initiateSearch(searchView.getQuery().toString(), SearchType.QUERY);
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
            initiateSearch(query, SearchType.QUERY);
        }
    }

    private void initiateSearch(String query, final SearchType searchType) {

        stopTextToSpeech();

        searchView.clearFocus();

        new AsyncTask<String, Void, ArrayList<ResultPod>>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                toggleProgressBar(true);
            }

            @Override
            protected ArrayList<ResultPod> doInBackground(String... params) {

                if (!Utils.isNetworkAvailable(context))
                    return null;

                if (searchType == SearchType.QUERY && !StringUtils.isEmpty(params[0])) {
                    return WolframAlphaAPI.getQueryResult(params[0]);
                } else if (searchType == SearchType.IMAGE_DESCRIPTION && imageFileUri != null) {
                    return MicrosoftCognitiveAPI.getImageDescription(imageFileUri, context);
                } else if (searchType == SearchType.OCR && imageFileUri != null) {
                    return MicrosoftCognitiveAPI.getOCRText(imageFileUri, context);
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
        }.execute(query);
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
                initiateSearch(query, SearchType.QUERY);
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
            showInformation(getString(R.string.about_dev), getString(R.string.okay));
            searchView.clearFocus();
            return true;
        } else if (id == R.id.action_clear_history) {
            clearSearchHistory();
            searchView.clearFocus();
        } else if (id == R.id.action_clear_result) {
            clearResult();
            searchView.clearFocus();
        } else if (id == R.id.action_recognize_photo) {

            stopTextToSpeech();

            if (cameraPermissionCheck == PackageManager.PERMISSION_GRANTED && externalStoragePermissionCheck == PackageManager.PERMISSION_GRANTED) {
                startImageCapture(SearchType.IMAGE_DESCRIPTION);
            } else {
                verifyAndRequestPermission();
            }
        } else if (id == R.id.action_run_ocr) {
            stopTextToSpeech();

            if (cameraPermissionCheck == PackageManager.PERMISSION_GRANTED && externalStoragePermissionCheck == PackageManager.PERMISSION_GRANTED) {
                startImageCapture(SearchType.OCR);
            } else {
                verifyAndRequestPermission();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private void startImageCapture(SearchType searchType) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        imageFileUri = Uri.fromFile(getOutputMediaFile());
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageFileUri);

        startActivityForResult(intent, searchType.value);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == SearchType.IMAGE_DESCRIPTION.value && resultCode == RESULT_OK && imageFileUri != null) {
            // Start searching for the description for the image taken
            initiateSearch("", SearchType.IMAGE_DESCRIPTION);
        } else if (requestCode == SearchType.OCR.value && resultCode == RESULT_OK && imageFileUri != null) {
            // Start OCR
            initiateSearch("", SearchType.OCR);
        }
    }

    private void clearResult() {

        stopTextToSpeech();
        recyclerView.setAdapter(null);
        loadDefaultCard();
    }

    @Override
    protected void onPause() {
        super.onPause();

        toggleProgressBar(false);

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

    public enum SearchType {
        QUERY(1), IMAGE_DESCRIPTION(2), OCR(3);
        private int value;

        SearchType(int value) {
            this.value = value;
        }
    }
}
