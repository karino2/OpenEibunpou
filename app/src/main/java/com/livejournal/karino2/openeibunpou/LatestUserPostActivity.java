package com.livejournal.karino2.openeibunpou;

import android.app.LoaderManager;
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
                ((TextView)findViewById(R.id.textViewStatus)).setText("");
                refresh();
            }
        });
    }

    @Override
    protected void onStop() {
        getSync().removeLatestPostUpdateListener(R.layout.activity_latest_user_post);
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

                    Date dt = new Date(cursor.getLong(5));
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

                showMessage("stage, sub=" + stageName + ", " + subName);
            }
        });

        getLoaderManager().initLoader(0, null, this);
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
                // return getDatabase().queryLatestPosts();
                Cursor cursor = getDatabase().queryLatestPosts();

                /*
                final String count = String.valueOf(cursor.getCount());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        showMessage(count);
                    }
                });
                */
                return cursor;
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
