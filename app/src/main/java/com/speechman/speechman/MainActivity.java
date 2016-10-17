package com.speechman.speechman;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.speech.RecognizerIntent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private final int SPEECH_CODE = 100;

    private TextView speechOutputTextview = null;
    private ImageButton micButton = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        speechOutputTextview = (TextView)findViewById(R.id.spokenWordsTextview);
        micButton = (ImageButton)findViewById(R.id.micIntentButton);

        micButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your command to the ESP8266");
                startActivityForResult(intent, SPEECH_CODE);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == SPEECH_CODE) {
            if (resultCode == RESULT_OK && null != data) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                String speechText = result.get(0); //most confidence here
                speechOutputTextview.setText(speechText);

                new SendCommandTask(this, speechText).execute();  //executes command to ESP8266
            }
        }
    }

    private class SendCommandTask extends AsyncTask<Void, Void, String> {

        Context activity;
        ProgressDialog pdialog;
        String command;

        SendCommandTask(Context a, String command) {
            activity = a;
            this.command = command;
        }

        @Override
        protected void onPreExecute() {
            pdialog = new ProgressDialog(activity);
            pdialog.setMessage("Sending command...");
            pdialog.show();
        }

        @Override
        protected String doInBackground(Void... params) {

            URL urlObj = null;

            try {
                urlObj = new URL("http://9d59468f.ngrok.io/" + command.replaceAll(" ", "%20"));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            if (urlObj != null) { //make the connection and return response
                HttpURLConnection urlConnection = null;
                try {
                    urlConnection = (HttpURLConnection) urlObj.openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.connect();
                    BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder res = new StringBuilder();
                    String readLine;
                    while((readLine = br.readLine()) != null) {
                        res.append(readLine);
                    }
                    return res.toString();

                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(activity, "IO Exception", Toast.LENGTH_LONG).show();
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(String res) {
            super.onPostExecute(res);

            if (pdialog != null && pdialog.isShowing()) {
                pdialog.dismiss();
            }

            Toast.makeText(activity, "Result: " + res, Toast.LENGTH_SHORT).show();
        }
    }
}

