package com.livejournal.karino2.openeibunpou;

import android.content.Context;
import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


/**
 * Created by karino on 10/24/15.
 */
public class Server {
    public static final String MAIN_URL = "https://openeibunpou.appspot.com";



    public interface OnContentReadyListener {
        void onReady(String responseText);
        void onFail(String message);
    }

    public void executeGet(String methodAndQuery, OnContentReadyListener listener) {
        GetStringTask task = new GetStringTask(MAIN_URL + methodAndQuery, listener);
        task.execute();
    }

    class GetStringTask extends AsyncTask<Object, String, Boolean> {

        OnContentReadyListener resultListener;
        String url;

        GetStringTask(String url, OnContentReadyListener listener) {
            this.url = url;
            resultListener = listener;
        }

        String readBody(InputStream is) throws IOException {
            BufferedReader reader;
            reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder builder = new StringBuilder();
            String line = reader.readLine();
            boolean first = true;
            while (line != null) {
                if (!first)
                    builder.append("\n");
                first = false;
                builder.append(line);
                line = reader.readLine();
            }
            return builder.toString();
        }

        String responseText = null;
        String errorMessage = null;

        @Override
        protected Boolean doInBackground(Object... params) {
            try {
                URL u = new URL(url);
                HttpURLConnection connection = (HttpURLConnection)u.openConnection();
                try {
                    responseText = readBody(connection.getInputStream());
                    return true;
                }
                finally {
                    connection.disconnect();
                }
            } catch (MalformedURLException e) {
                errorMessage = "MalformedURLException: " + e.getMessage();
                return false;
            } catch (IOException e) {
                errorMessage = "IOException: " + e.getMessage();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success)
                resultListener.onReady(responseText);
            else
                resultListener.onFail(errorMessage);
        }
    }

}
