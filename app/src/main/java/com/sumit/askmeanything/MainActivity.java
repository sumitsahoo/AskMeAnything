package com.sumit.askmeanything;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.sumit.askmeanything.adapter.ResultPodAdapter;
import com.sumit.askmeanything.api.WolframAlphaAPI;
import com.sumit.askmeanything.contentprovider.SearchSuggestionProvider;
import com.sumit.askmeanything.model.ResultPod;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private Context context;
    private CoordinatorLayout coordinatorLayout;
    private FloatingActionButton fab;
    private ProgressDialog progressDialog;
    private TextToSpeech textToSpeech;
    private SearchView searchView;
    private CardView cardView;
    private RecyclerView recyclerView;
    private LinearLayoutManager linearLayoutManager;
    private ResultPodAdapter resultPodAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;

        // Initialize Fresco Image Library

        Fresco.initialize(context);

        initViews();
        loadDefaultCard();
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
                    initiateSearch(searchView.getQuery().toString());
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


    @Override
    protected void onNewIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);

            if (searchView != null) {
                searchView.setQuery(query, false);
                searchView.clearFocus();
            }

            keepSearchHistory(query);
            initiateSearch(query);
        }
    }

    private void initiateSearch(String query) {

        if (textToSpeech != null && textToSpeech.isSpeaking())
            textToSpeech.stop();

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
                if (!StringUtils.isEmpty(params[0]))
                    return WolframAlphaAPI.getQueryResult(params[0]);

                return null;
            }

            @Override
            protected void onPostExecute(ArrayList<ResultPod> resultPods) {
                super.onPostExecute(resultPods);

                toggleProgressBar(false);
                if (resultPods != null) {
                    populateResult(resultPods);
                } else if (!Utils.isNetworkAvailable(context)) {
                    showInformation(getString(R.string.error_network_not_available), getString(R.string.okay));
                } else if (searchView != null && StringUtils.isEmpty(searchView.getQuery().toString()))
                    showInformation(getString(R.string.enter_search_query), getString(R.string.okay));
                else
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

        String mainResult = resultPods.get(1).getDescription();

        if (StringUtils.isNotEmpty(mainResult)) {

            if (textToSpeech.isSpeaking())
                textToSpeech.stop();

            textToSpeech.speak(mainResult, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    private void toggleProgressBar(boolean isShow) {

        if (progressDialog != null && isShow) {
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
                initiateSearch(query);
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
        }

        return super.onOptionsItemSelected(item);
    }

    private void clearResult() {
        recyclerView.setAdapter(null);
        loadDefaultCard();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Setup progress bar

        progressDialog = new ProgressDialog(context);
        progressDialog.setMessage(getString(R.string.wait_message));
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);


        // Stop Speech Engine

        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        initTextToSpeech();
    }
}
