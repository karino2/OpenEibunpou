package com.livejournal.karino2.openeibunpou;

import android.net.Uri;
import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Created by karino on 10/24/15.
 */
public class Server {
    public static final String MAIN_URL = "https://openeibunpou.appspot.com";

    static Server s_server;
    public static Server getInstance() {
        if(s_server == null) {
            s_server = new Server();
        }
        return s_server;
    }

    String cookie;

    public void executePost(String method, Map<String, String> params, OnContentReadyListener listener) {
        PostTask task = new PostTask(MAIN_URL +method, params, listener);
        if(cookie == null){
            waitCookieQueues.add(task);
            return;
        }
        task.execute();
    }

    public interface OnContentReadyListener {
        void onReady(String responseText);
        void onFail(String message);
    }

    List<AsyncTask> waitCookieQueues = new ArrayList<AsyncTask>();

    public void executeGet(String methodAndQuery, OnContentReadyListener listener) {
        GetStringTask task = new GetStringTask(MAIN_URL + methodAndQuery, listener);
        if(cookie == null){
            waitCookieQueues.add(task);
            return;
        }
        task.execute();
    }

    public void requestGetCookie(String authToken, final OnContentReadyListener listener) {
        GetCookieTask task = new GetCookieTask(authToken, new OnContentReadyListener() {
            @Override
            public void onReady(String responseText) {
                cookie = responseText;
                listener.onReady(responseText);

                for(AsyncTask task : waitCookieQueues) {
                    task.execute();
                }
                // TODO: race condition.
                waitCookieQueues.clear();
            }

            @Override
            public void onFail(String message) {
                listener.onFail(message);
            }
        });
        task.execute();
    }


    class GetCookieTask extends AsyncTask<Object, String, Boolean> {

        OnContentReadyListener resultListener;
        String url;

        GetCookieTask(String authToken, OnContentReadyListener listener) {
            url = MAIN_URL + "/_ah/login?&auth=" + authToken;
            resultListener = listener;
        }

        String responseText = null;
        String errorMessage = null;

        @Override
        protected Boolean doInBackground(Object... params) {
            try {
                URL u = new URL(url);
                HttpURLConnection connection = (HttpURLConnection)u.openConnection();
                try {

                    String header = connection.getHeaderField("Set-Cookie");

                    if(HttpURLConnection.HTTP_MOVED_TEMP != connection.getResponseCode() ||
                            header.length() == 0)
                    {
                        return false;
                    }
                    responseText = header;
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
                    connection.setRequestProperty("Cookie", cookie);
                    connection.setRequestProperty("X-Same-Domain", "1");
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

    class PostTask extends AsyncTask<Object, String, Boolean> {

        OnContentReadyListener resultListener;
        String url;
        Map<String, String> postParams;

        PostTask(String url, Map<String, String> params, OnContentReadyListener listener) {
            this.url = url;
            resultListener = listener;
            this.postParams = params;
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
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Cookie", cookie);
                    connection.setRequestProperty("X-Same-Domain", "1");
                    connection.setDoInput(true);
                    connection.setDoOutput(true);

                    OutputStream os = connection.getOutputStream();
                    try {
                        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                        writer.write(getPostString());
                        writer.flush();
                        writer.close();

                        responseText = readBody(connection.getInputStream());
                        return true;

                    }finally {
                        os.close();
                    }
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

        private String getPostString() {
            boolean first = true;
            Uri.Builder builder = new Uri.Builder();
            for(Map.Entry<String, String> ent : postParams.entrySet()) {
                builder.appendQueryParameter(ent.getKey(), ent.getValue());
            }
            return builder.build().getEncodedQuery();
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
