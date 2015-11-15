package com.livejournal.karino2.openeibunpou;

import android.app.Dialog;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LatestUserPostActivity extends AppCompatActivity  implements LoaderManager.LoaderCallbacks<Cursor> {
    SimpleCursorAdapter adapter;
    Sync sync;

    final int DIALOG_ID_LOADING = 1;

    Sync getSync() {
        if(sync == null)
            sync = Sync.getInstance(this);
        return sync;
    }

    Database getDatabase() {
        return Database.getInstance(this);
    }

    void refresh() {
        Loader<Object> loader = getLoaderManager().getLoader(0);
        if(loader != null)
            loader.forceLoad();
    }

    @Override
    protected void onStart() {
        super.onStart();
        getSync().addLatestPostUpdateListener(R.layout.activity_latest_user_post, new Sync.NotifyUpdateListener() {
            @Override
            public void onUpdate() {
                ((TextView) findViewById(R.id.textViewStatus)).setText("");
                refresh();
            }
        });
    }

    @Override
    protected void onStop() {
        getSync().removeLatestPostUpdateListener(R.layout.activity_latest_user_post);

        getSync().removeOneStageLoadedListener(R.layout.activity_latest_user_post);
        if(progress != null) {
            progress.dismiss();
            progress = null;
        }
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_latest_user_post);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getSync().syncLatestUserPost();

        /*
        "_id", "stageName", "subName", "owner", "body", "date"
         */
        adapter = new SimpleCursorAdapter(this, R.layout.latest_post_item, null,
                new String[]{"body", "date", "owner"},
                new int[]{ R.id.textViewPost, R.id.textViewDate, R.id.textViewOwner });

        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {

                if(columnIndex == 5) {
                    view.setTag(R.layout.activity_latest_user_post, cursor.getString(1)); // stageName
                    view.setTag(R.layout.latest_post_item, cursor.getString(2)); // subName

                    // cursor.getLong(5) is the value from 2010, 1,1, seconds.
                    // may be we should convert when insert db, but I'm lazy to recreate DB, so I convert here.
                    Date baseDt = new Date(2010-1900, 0, 1, 0, 0, 0);
                    Date dt = new Date(baseDt.getTime() + cursor.getLong(5)*1000);

                    SimpleDateFormat ft = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                    ((TextView)view).setText(ft.format(dt));
                    return true;
                }
                return false;
            }
        });

        ListView lv = (ListView)findViewById(R.id.listViewPosts);
        lv.setAdapter(adapter);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView tvDate = (TextView) parent.findViewById(R.id.textViewDate);
                String stageName = (String) tvDate.getTag(R.layout.activity_latest_user_post);
                String subName = (String) tvDate.getTag(R.layout.latest_post_item);

                onPostClicked(stageName, subName);
            }
        });

        getLoaderManager().initLoader(0, null, this);
    }

    class WaitStageLoad implements Sync.OnStageUpdateListener {
        String stageName;
        String subName;

        WaitStageLoad(String stageN, String subN) {
            stageName = stageN;
            subName = subN;
        }

        @Override
        public void onStageUpdate(String stageName) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (progress != null) {
                        progress.dismiss();
                        getSync().removeOneStageLoadedListener(R.layout.activity_latest_user_post);
                        progress = null;
                    }
                    onPostClicked(WaitStageLoad.this.stageName, subName);
                }
            });
        }
    }

    private void onPostClicked(String stageName, String subName) {
        QuestionRecord rec = getDatabase().queryQuestion(stageName, subName);
        if(rec != null) {
            Intent intent = new Intent(this, AnswerActivity.class);
            intent.putExtra("stageName", stageName);
            rec.saveToIntent(intent);
            startActivity(intent);
            return;
        }
        getSync().loadStage(stageName);
        getSync().addOneStageLoadedListener(R.layout.activity_latest_user_post, new WaitStageLoad(stageName, subName));
        showDialog(DIALOG_ID_LOADING);
    }

    ProgressDialog progress;

    @Override
    protected Dialog onCreateDialog(int id) {
        switch(id) {
            case DIALOG_ID_LOADING:
                progress = new ProgressDialog(this);
                progress.setMessage("Loading stage...");
                progress.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                                progress = null;
                                getSync().removeOneStageLoadedListener(R.layout.activity_latest_user_post);
                            }
                        });
                return progress;
        }
        return super.onCreateDialog(id);
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch(id) {
            case DIALOG_ID_LOADING:
                progress = (ProgressDialog)dialog;
                return;
        }
        super.onPrepareDialog(id, dialog);
    }

    void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    Handler handler = new Handler();

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new SimpleCursorLoader(this) {
            @Override
            public Cursor loadCursor() {
                return getDatabase().queryLatestPosts();
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        adapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }
}
