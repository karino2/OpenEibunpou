package com.livejournal.karino2.openeibunpou;

import android.content.Context;
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

    public interface NotifyStageListener {
        void onNotify(String stageName);
    }

    void callNotifyStageListeners(Map<Integer, NotifyStageListener> listenerMap, String stageName) {
        for(NotifyStageListener listener : listenerMap.values()) {
            listener.onNotify(stageName);
        }
    }

    public interface NotifyStageListListener {
        void onUpdate();
    }

    Map<Integer, NotifyStageListListener> stageListListener = new HashMap<>();
    public void addStageListUpdateListener(int id, NotifyStageListListener listener) {
        stageListListener.put(id, listener);
    }
    public void removeStageListUpdateListener(int id) {
        stageListListener.remove(id);
    }
    void callNotifyStageListUpdateListeners() {
        for(NotifyStageListListener listener : stageListListener.values()) {
            listener.onUpdate();
        }
    }

    public interface ErrorListener {
        void onError(String msg);
    }
    Map<Integer, ErrorListener> errorListeners = new HashMap<>();

    public void addErrorListener(int id, ErrorListener listener) {
        errorListeners.put(id, listener);
    }
    public void removeErrorListener(int id) {
        errorListeners.remove(id);
    }
    void callErrorListeners(String msg) {
        for(ErrorListener listener : errorListeners.values()) {
            listener.onError(msg);
        }
    }


    public void loadStage(String stageName) {
        // reverse order because later is more precedent.
        database.insertMyLikeSyncRequest(stageName);
        database.insertUserPostSyncRequest(stageName);
        database.insertCompletionRequest(stageName);
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

        // stateListener.notifyOneStageUpdate(stageName);
    }

    public void syncStageSecond(String stageName) {
        database.insertMyLikeSyncRequest(stageName);
        database.insertUserPostSyncRequest(stageName);
        database.insertCompletionRequest(stageName);
        handlePendingRequest();
    }

    public void updateCompletion(String stageName, String json) {
        // reverse order because later is more precedent.
        database.insertCompletionRequest(stageName);
        database.insertUpdateCompletionRequest(json);
        // TODO: this might gc-ed.
        handlePendingRequest();
    }

    public void updateStageCompletion(String stageName, int completion) {
        database.updateStageCompletion(stageName, completion);
    }


    class PostDto {
        String year;
        String sub;
        int anon;
        String body;
        PostDto(String y, String s, int a, String b) {
            year = y;
            sub = s;
            anon = a;
            body = b;
        }
    }
    public void postComment(String stageName, String subName, String comment) {
        Gson gson = new Gson();
        PostDto dto = new PostDto(stageName, subName, 0, comment);

        // reverse order.
        database.insertUserPostSyncRequest(stageName);
        database.insertPostUserPostRequest(gson.toJson(dto));
        handlePendingRequest();
    }

    public void postLike(long id, int newVal) {
        database.insertMyLikeUpdateRequest(id, newVal);
        handlePendingRequest();
    }

    Map<Integer, NotifyStageListener> completionArrivedListener = new HashMap<>();

    public void addOneStageLoadedListener(int id, NotifyStageListener listener) {
        completionArrivedListener.put(id, listener);
    }

    public void removeOneStageLoadedListener(int id) {
        completionArrivedListener.remove(id);
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
        callNotifyStageListeners(completionArrivedListener, stageName);
    }

    Map<Integer, NotifyStageListener> userPostListeners = new HashMap<>();

    public void addOnUserPostArrivedListener(int id, NotifyStageListener listener) {
        userPostListeners.put(id, listener);
    }

    public void removeUserPostArrivedListener(int id) {
        userPostListeners.remove(id);
    }

    /*
            obj = {
            'id': p.key.id(),
            'owner': owner,
            'anonymous': p.anonymous,
            'sub': p.subQuestionNumber,
            'parent': p.parent,
            'body': p.body,
            'like': p.like,
            'dislike': p.dislike,
            'date': p.date
            }

     */
    private void onUserPostListArrived(String stageName, String responseText) {
        Gson gson = new Gson();
        Type collectionType = new TypeToken<Collection<Database.UserPostRecord>>(){}.getType();
        List<Database.UserPostRecord> records = gson.fromJson(responseText, collectionType);
        for(Database.UserPostRecord rec : records) {
            database.insertUserPost(stageName, rec);
        }
        callNotifyStageListeners(userPostListeners, stageName);
    }


    /*
    postId, val, date
     */
    class MyLikeRecord {
        public long postId;
        public int val;
        public long date;
    }
    private void onMyLikeListArrived(String stageName, String responseText) {
        Gson gson = new Gson();
        Type collectionType = new TypeToken<Collection<MyLikeRecord>>(){}.getType();
        List<MyLikeRecord> records = gson.fromJson(responseText, collectionType);
        for(MyLikeRecord rec : records) {
            database.insertMyLike(rec.postId, rec.val, rec.date);
        }
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
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        handlePendingInternal();
                    }
                });
                return;
            }

            handler.post(this);
        }


        Database.PendingCommand current;

        class PendingUpdateListener implements Server.OnContentReadyListener {
            long cmdId;
            PendingUpdateListener(long id) {
                cmdId = id;
            }

            @Override
            public void onReady(String responseText) {
                database.deletePendingCommandById(cmdId);
                handleNext();
            }

            @Override
            public void onFail(String message) {
                callErrorListeners("completion update fail: " + message);
                // retry later, so not remove command here.
                handleNext();
            }
        }

        @Override
        public void run() {
            current = cmds.get(0);
            switch(current.cmd) {
                case  Database.CMD_ID_SYNC_QUESTIONS:
                {
                    server.executeGet("/questions/" + current.body, new Server.OnContentReadyListener() {
                        @Override
                        public void onReady(String responseText) {
                            onQuestionsArrived(current.body, responseText);
                            database.deletePendingOneStageSyncRequest(current.body);
                            handleNext();
                        }

                        @Override
                        public void onFail(String message) {
                            callErrorListeners("stage: " + current.body + ", " + message);
                            database.deletePendingOneStageSyncRequest(current.body);
                            handleNext();
                        }
                    });
                    break;
                }
                case  Database.CMD_ID_SYNC_COMPLETION:
                {
                    server.executeGet("/compyear/" + current.body + "/" + database.lastCompletionTick(current.body), new Server.OnContentReadyListener() {
                        @Override
                        public void onReady(String responseText) {
                            onQuestionsCompletionArrived(current.body, responseText);
                            database.deletePendingCompletionRequest(current.body);
                            handleNext();
                        }

                        @Override
                        public void onFail(String message) {
                            callErrorListeners("stage: " + current.body + ", " + message);
                            database.deletePendingCompletionRequest(current.body);
                            handleNext();
                        }
                    });
                    break;
                }
                case Database.CMD_ID_UPDATE_COMPLETION:
                {
                    Map<String, String> postParams = new HashMap<String, String>();
                    postParams.put("json", current.body);
                    server.executePost("/cqupdate", postParams, new PendingUpdateListener(current.id));
                    break;
                }
                case Database.CMD_ID_SYNC_USERPOST: {
                    server.executeGet("/posts/" + current.body + "/" + database.lastPostTick(current.body), new Server.OnContentReadyListener() {
                        @Override
                        public void onReady(String responseText) {
                            onUserPostListArrived(current.body, responseText);
                            database.deletePendingCommand(Database.CMD_ID_SYNC_USERPOST, current.body);
                            handleNext();
                        }

                        @Override
                        public void onFail(String message) {
                            callErrorListeners("userpost sync fail: " + current.body + "," + message);
                            database.deletePendingCommand(Database.CMD_ID_SYNC_USERPOST, current.body);
                            handleNext();
                        }
                    });
                    break;

                }
                case Database.CMD_ID_POST_USERPOST: {
                    Map<String, String> postParams = new HashMap<String, String>();
                    postParams.put("json", current.body);
                    server.executePost("/post", postParams, new PendingUpdateListener(current.id));
                    break;
                }
                case Database.CMD_ID_SYNC_MYLIKE: {
                    server.executeGet("/likes/" + current.body + "/" + database.lastLikeTick(current.body), new Server.OnContentReadyListener() {
                        @Override
                        public void onReady(String responseText) {
                            onMyLikeListArrived(current.body, responseText);
                            database.deletePendingCommand(Database.CMD_ID_SYNC_MYLIKE, current.body);
                            handleNext();
                        }

                        @Override
                        public void onFail(String message) {
                            callErrorListeners("mylike sync fail: " + current.body + "," + message);
                            database.deletePendingCommand(Database.CMD_ID_SYNC_MYLIKE, current.body);
                            handleNext();
                        }
                    });
                    break;
                }
                case Database.CMD_ID_UPDATE_MYLIKE: {
                    Map<String, String> postParams = new HashMap<String, String>();
                    StringBuilder builder = new StringBuilder();
                    builder.append("{\"id\":");
                    builder.append(current.body);
                    builder.append(",\"val\":");
                    builder.append(current.arg);
                    builder.append("}");
                    postParams.put("json", builder.toString());
                    server.executePost("/like", postParams, new PendingUpdateListener(current.id));
                    break;
                }
                default:
                    throw new RuntimeException("Unknown pending cmd " + current.cmd);
            }
        }
    }


    // May be we should move this to other worker thread in the future.
    Handler handler = new Handler();

    boolean handlingPendingNow = false;
    public void handlePendingRequest() {
        if(handlingPendingNow)
            return;

        handlePendingInternal();
    }

    public void handlePendingInternal() {
        List<Database.PendingCommand> cmds = database.queryPendingCommand();
        if(cmds.size() == 0) {
            handlingPendingNow = false;
            return;
        }
        handler.post(new PendingTaskAction(cmds));
    }


    public Sync(Database db, Server sv) {
        database = db;
        server = sv;
    }

    static Sync s_instance;
    public static Sync getInstance(Context context) {
        if(s_instance == null) {
            s_instance = new Sync(Database.getInstance(context), Server.getInstance());
        }
        return s_instance;
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
                callNotifyStageListUpdateListeners();
            }

            @Override
            public void onFail(String message) {
                callErrorListeners(message);
            }
        });
    }


}
