package com.sumit.askmeanything;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.sumit.askmeanything.api.WolframAlphaAPI;
import com.sumit.askmeanything.model.ResultPod;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private Context context;
    private CoordinatorLayout coordinatorLayout;
    private EditText editTextSearchQuery;
    private TextView textViewShowResult;
    private ImageButton imageButtonVoiceInput;
    private FloatingActionButton fab;
    private ProgressDialog progressDialog;
    private TextToSpeech textToSpeech;

    private final int REQ_CODE_SPEECH_INPUT = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;

        initViews();

    }

    private void initViews() {

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout);

        // Setup progress bar

        progressDialog = new ProgressDialog(context);
        progressDialog.setMessage(getString(R.string.wait_message));
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);

        // Setup views

        editTextSearchQuery = (EditText) findViewById(R.id.edit_search_query);
        imageButtonVoiceInput = (ImageButton) findViewById(R.id.imagebutton_voice_input);
        textViewShowResult = (TextView) findViewById(R.id.text_show_result);

        imageButtonVoiceInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                promptSpeechInput();
            }
        });

        fab = (FloatingActionButton) findViewById(R.id.fab);

        // Add action listeners

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initiateSearch();
            }
        });

    }

    private void initTextToSpeech() {
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.US);
                }
            }
        });
    }

    // Show google speech input dialog

    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_prompt));
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            showInformation(getString(R.string.speech_not_supported), getString(R.string.dismiss));
        }
    }

    // Receive voice input

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    editTextSearchQuery.setText(result.get(0));
                }
                break;
            }

        }
    }

    private void initiateSearch() {

        String query = editTextSearchQuery.getText().toString();

        new AsyncTask<String, Void, ArrayList<ResultPod>>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                toggleProgressBar(true);
            }

            @Override
            protected ArrayList<ResultPod> doInBackground(String... params) {

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
                } else if (resultPods == null && StringUtils.isEmpty(editTextSearchQuery.getText().toString())) {
                    showInformation(getString(R.string.enter_search_query), getString(R.string.okay));
                } else
                    showInformation(getString(R.string.error_unable_to_search), getString(R.string.dismiss));
            }
        }.execute(query);
    }

    private void populateResult(ArrayList<ResultPod> resultPods) {

        String result = null;
        int count = 1;

        String mainResult = null;

        for (ResultPod resultPod : resultPods) {

            if (result == null) {
                result = getString(R.string.response_title) + " (" + count + ") : " + resultPod.getTitle();
            } else {
                result += "\n\n" + getString(R.string.response_title) + " (" + count + ") : " + resultPod.getTitle();
            }

            result += "\n" + getString(R.string.response_description) + " (" + count + ") : " + resultPod.getDescription();

            // Generally result pod 2 will have main answer to the query

            if(count == 2)
                mainResult = resultPod.getDescription();

            count++;
        }

        textViewShowResult.setText(result);

        if(!textToSpeech.isSpeaking() && StringUtils.isEmpty(mainResult))
            textToSpeech.speak(mainResult, TextToSpeech.QUEUE_FLUSH, null);

    }

    private void toggleProgressBar(boolean isShow) {

        if (isShow) {
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
        return true;
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
            return true;
        } else if (id == R.id.action_clear) {
            editTextSearchQuery.setText("");
            textViewShowResult.setText("");
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Stop Speech Engine

        if(textToSpeech != null){
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
