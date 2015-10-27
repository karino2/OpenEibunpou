package com.livejournal.karino2.openeibunpou;

import android.database.Cursor;
import android.os.Handler;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by karino on 10/24/15.
 */
public class Sync {
    Database database;
    Server server;

    public void loadStage(String stageName) {
        database.insertOneStageSyncRequest(stageName);
        database.insertCompletionRequest(stageName);
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

        // stateListener.notifyOneStageUpdate(stageName);
    }

    public void syncCompletion(String stageName) {
        database.insertCompletionRequest(stageName);
        // might be race condition. We'll fix when we make Sync to Service.
        handlePendingRequest();
    }

    public void updateCompletion(String stageName, String json) {
        database.insertUpdateCompletionRequest(json);
        database.insertCompletionRequest(stageName);
        // TODO: this might gc-ed.
        handlePendingRequest();
    }

    public void updateStageCompletion(String stageName, int completion) {
        database.updateStageCompletion(stageName, completion);
    }

    class QuestionCompletionRecord {
        String year;
        String sub;
        int comp;
        long date;
    }
    /*
    year, sub, comp, date
     */
    private void onQuestionsCompletionArrived(String stageName, String responseText) {
        Gson gson = new Gson();
        Type collectionType = new TypeToken<Collection<QuestionCompletionRecord>>(){}.getType();
        List<QuestionCompletionRecord> records = gson.fromJson(responseText, collectionType);
        for(QuestionCompletionRecord rec : records) {
            database.insertQuestionCompletion(stageName, rec.sub, rec.comp, rec.date);
        }
        stateListener.notifyOneStageUpdate(stageName);
    }

    public interface OnCookieErrorListener {
        void onError(String msg);
    }

    public void requestGetCookie(String authToken, final OnCookieErrorListener onError) {
        server.requestGetCookie(authToken, new Server.OnContentReadyListener() {
            @Override
            public void onReady(String responseText) {
            }

            @Override
            public void onFail(String message) {
                onError.onError("GetCookie fail: " + message);
            }
        });
    }



    class PendingTaskAction implements Runnable {
        List<Database.PendingCommand> cmds;

        PendingTaskAction(List<Database.PendingCommand> pendingCmds) {
            cmds = pendingCmds;
        }

        void handleNext() {
            cmds.remove(0);

            if(cmds.size() == 0) {
                return;
            }

            handler.post(this);
        }


        Database.PendingCommand current;

        @Override
        public void run() {
            current = cmds.get(0);
            if(current.cmd == Database.CMD_ID_SYNC_QUESTIONS) {
                server.executeGet("/questions/" + current.body, new Server.OnContentReadyListener() {
                    @Override
                    public void onReady(String responseText) {
                        onQuestionsArrived(current.body, responseText);
                        database.deletePendingOneStageSyncRequest(current.body);
                        handleNext();
                    }

                    @Override
                    public void onFail(String message) {
                        stateListener.notifyOneStageSyncError(current.body, message);
                        database.deletePendingOneStageSyncRequest(current.body);
                        handleNext();
                    }
                });
            } else if(current.cmd == Database.CMD_ID_SYNC_COMPLETION) {
                server.executeGet("/compyear/" + current.body + "/" + database.lastCompletionTick(current.body), new Server.OnContentReadyListener() {
                    @Override
                    public void onReady(String responseText) {
                        onQuestionsCompletionArrived(current.body, responseText);
                        database.deletePendingCompletionRequest(current.body);
                        handleNext();
                    }

                    @Override
                    public void onFail(String message) {
                        stateListener.notifyOneStageSyncError(current.body, message);
                        database.deletePendingCompletionRequest(current.body);
                        handleNext();
                    }
                });
            } else if(current.cmd == Database.CMD_ID_UPDATE_COMPLETION) {
                Map<String, String> postParams = new HashMap<String, String>();
                postParams.put("json", current.body);
                server.executePost("/cqupdate", postParams, new Server.OnContentReadyListener() {
                    @Override
                    public void onReady(String responseText) {
                        database.deletePendingCommandById(current.id);
                        handleNext();
                    }

                    @Override
                    public void onFail(String message) {
                        stateListener.notifyError("completion update fail: " + message);
                        // should I remove pending command? retry later?
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

    public Sync(Database db, Server sv) {
        this(db, sv, new StateListener() {
            @Override
            public void notifyStagesUpdate() {

            }

            @Override
            public void notifyOneStageUpdate(String stageName) {

            }

            @Override
            public void notifyOneStageSyncError(String stageName, String msg) {

            }

            @Override
            public void notifyError(String msg) {

            }
        });
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
