package com.livejournal.karino2.openeibunpou;

import android.app.LoaderManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class StageActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private final int STATUS_NOTIFICAITON_ID = R.layout.activity_stage;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stage);

        ListView lv = (ListView)findViewById(R.id.listViewStages);

        adapter = new SimpleCursorAdapter(this, R.layout.stage_item, null,
                new String[]{"stageName", "loaded", "completion"}, new int[]{R.id.textViewStageName, R.id.textViewLoaded, R.id.textViewCompletion}, 0);
        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (columnIndex == 2) {
                    TextView tv = (TextView) view;
                    int loaded = cursor.getInt(2);
                    tv.setTag(loaded);
                    if (loaded == 0)
                        tv.setText("Not loaded.");
                    else
                        tv.setText("Loaded.");
                    return true;
                }

                if (columnIndex == 3) {
                    TextView tv = (TextView) view;
                    int completion = cursor.getInt(3);
                    tv.setText(completion + " %");
                    return true;
                }

                return false;
            }
        });

        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView nameTv = (TextView)view.findViewById(R.id.textViewStageName);
                String stageName = nameTv.getText().toString();

                TextView loadedTv = (TextView)view.findViewById(R.id.textViewLoaded);
                int loaded = (int)loadedTv.getTag();
                if(loaded == 1) {
                    gotoGame(stageName);
                } else {
                    loadedTv.setText("Loading...");
                    sync.loadStage(stageName);
                }

            }
        });

        notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);;


        sync = new Sync(getDatabase(this), new Server(), new Sync.StateListener() {
            @Override
            public void notifyStagesUpdate() {
                setStatusLabel("");
                refresh();
            }

            @Override
            public void notifyOneStageUpdate(String stageName) {
                refresh();
            }

            @Override
            public void notifyOneStageSyncError(String stageName, String msg) {
                notifyStatusBarWithTicker("Error", stageName + "load fail: " + msg, null);
            }

            @Override
            public void notifyError(String msg) {
                notifyStatusBarWithTicker("Error", msg, null);
            }
        });

        setStatusLabel("");

        getLoaderManager().initLoader(0, null, this);
    }

    private void gotoGame(String stageName) {
        Intent intent = new Intent(this, QuestionActivity.class);
        intent.putExtra("stageName", stageName);
        startActivity(intent);
    }

    void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private void refresh() {
        Loader<Object> loader = getLoaderManager().getLoader(0);
        if(loader != null)
            loader.forceLoad();
    }

    NotificationManager notificationManager;

    private void notifyStatusBarWithTicker(String title, String message, String ticker) {
        if (title == null) {
            notificationManager.cancel(STATUS_NOTIFICAITON_ID);
            return;
        }

        notificationManager.notify(STATUS_NOTIFICAITON_ID, new NotificationCompat.Builder(this)
                .setTicker(ticker)
                .setContentTitle(title)
                .setContentText(message)
                .build());
    }


    SimpleCursorAdapter adapter;

    Database getDatabase(Context ctx) { return Database.getInstance(ctx); }

    Handler handler = new Handler();
    Sync sync;


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new SimpleCursorLoader(this) {
            @Override
            public Cursor loadCursor() {
                Cursor cursor = getDatabase(StageActivity.this).queryStageCursor();
                if(cursor == null || cursor.getCount() == 0)
                {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            setStatusLabel("Initializing...");
                            sync.syncStages();
                        }
                    });
                }
                return cursor;
            }
        };

    }

    private void setStatusLabel(String label) {
        TextView tv = (TextView)findViewById(R.id.textViewStatus);
        tv.setText(label);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        adapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_stage, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_delete:
                getDatabase(this).recreate();
                refresh();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
