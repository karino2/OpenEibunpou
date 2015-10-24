package com.livejournal.karino2.openeibunpou;

import com.google.gson.Gson;

/**
 * Created by karino on 10/24/15.
 */
public class Sync {
    Database database;
    Server server;

    public interface StateListener {
        void notifyStagesUpdate();
        void notifyError(String msg);
    }

    StateListener stateListener;

    public Sync(Database db, Server sv, StateListener listener) {
        database = db;
        server = sv;
        stateListener = listener;
    }

    public void syncStages() {
        // database.insertStageTableSyncRequest();
        server.executeGet("/years", new Server.OnContentReadyListener() {
            @Override
            public void onReady(String responseText) {
                Gson gson = new Gson();
                String[] names = gson.fromJson(responseText, String[].class);
                for(String name : names) {
                    database.insertStage(name);
                }
                stateListener.notifyStagesUpdate();
            }

            @Override
            public void onFail(String message) {
                stateListener.notifyError(message);
            }
        });
    }


}
