package com.livejournal.karino2.openeibunpou;

import android.database.Cursor;
import android.os.Handler;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

/**
 * Created by karino on 10/24/15.
 */
public class Sync {
    Database database;
    Server server;

    public void loadStage(String stageName) {
        database.insertOneStageSyncRequest(stageName);
        handlePendingRequest();
    }

    void onQuestionsArrived(String stageName, String responseText) {
        Gson gson = new Gson();
        Type collectionType = new TypeToken<Collection<QuestionRecord>>(){}.getType();
        List<QuestionRecord> records = gson.fromJson(responseText, collectionType);
        for(QuestionRecord rec : records) {
            database.insertOneQuestionRecord(stageName, rec, gson);
        }
        database.updateStageLoadState(stageName, 1);
        stateListener.notifyOneStageUpdate(stageName);
    }

    class PendingTaskAction implements Runnable {
        List<Database.PendingCommand> cmds;

        PendingTaskAction(List<Database.PendingCommand> pendingCmds) {
            cmds = pendingCmds;
        }

        void handleNext() {
            database.deletePendingOneStageSyncRequest(current.body);
            cmds.remove(0);

            if(cmds.size() == 0)
                return;

            handler.post(this);
        }


        Database.PendingCommand current;

        @Override
        public void run() {
            current = cmds.get(0);
            if(current.cmd == 1) {
                server.executeGet("/questions/" + current.body, new Server.OnContentReadyListener() {
                    @Override
                    public void onReady(String responseText) {
                        onQuestionsArrived(current.body, responseText);
                        handleNext();
                    }

                    @Override
                    public void onFail(String message) {
                        stateListener.notifyOneStageSyncError(current.body, message);
                        handleNext();
                    }
                });
            } else {
                throw new RuntimeException("Unknown pending cmd " + current.cmd);
            }
        }
    }

    // May be we should move this to other worker thread in the future.
    Handler handler = new Handler();

    public void handlePendingRequest() {
        List<Database.PendingCommand> cmds = database.queryPendingCommand();
        if(cmds.size() == 0) {
            return;
        }
        handler.post(new PendingTaskAction(cmds));
    }

    public interface StateListener {
        void notifyStagesUpdate();
        void notifyOneStageUpdate(String stageName);
        void notifyOneStageSyncError(String stageName, String msg);
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
