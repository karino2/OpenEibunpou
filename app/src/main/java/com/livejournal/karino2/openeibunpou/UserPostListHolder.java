package com.livejournal.karino2.openeibunpou;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

/**
 * Created by _ on 2015/11/15.
 */
public class UserPostListHolder {
    Activity owner;
    Sync sync;
    Database database;
    String stageName;
    String subName;


    SimpleCursorAdapter adapter;

    public UserPostListHolder(Activity owner, Sync sync, Database database, String stageName, String subName) {
        this.owner = owner;
        this.sync = sync;
        this.database = database;
        this.stageName = stageName;
        this.subName = subName;

        adapter = new SimpleCursorAdapter(owner, R.layout.post_item, null,
                new String[]{"body", "like", "dislike", "mylike", "nick"},
                new int[]{ R.id.textViewPost, R.id.textViewLikeTotal, R.id.textViewDislikeTotal, R.id.textViewLike, R.id.textViewNick });

        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if(columnIndex == 5) {
                    // view is testViewLike. set serverId here for a while.
                    view.setTag(cursor.getLong(1));

                    View parent = (View)view.getParent();
                    int myval =cursor.getInt(5);
                    parent.setTag(myval);
                    TextView likeTv = (TextView)parent.findViewById(R.id.textViewLike);
                    TextView dislikeTv = (TextView)parent.findViewById(R.id.textViewDislike);
                    likeTv.setTextColor(Color.LTGRAY);
                    dislikeTv.setTextColor(Color.LTGRAY);

                    likeTv.setOnClickListener(likeTvClickListener);
                    dislikeTv.setOnClickListener(dislikeTvClickListener);

                    if(myval == 1) {
                        likeTv.setTextColor(Color.RED);
                    } else if (myval == -1) {
                        dislikeTv.setTextColor(Color.RED);
                    }
                    return true;
                }
                return false;
            }
        });

    }


    public SimpleCursorAdapter getAdapter() { return adapter; }
    View.OnClickListener likeTvClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            View par = (View)v.getParent();
            long id = (long)v.getTag();
            TextView likeTv2 = (TextView)v;
            TextView dislikeTv2 =  (TextView)par.findViewById(R.id.textViewDislike);
            onLikeClicked(id, par, likeTv2, dislikeTv2);
        }
    };

    View.OnClickListener dislikeTvClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            View par = (View)v.getParent();
            TextView dislikeTv2 = (TextView)v;
            TextView likeTv2 =  (TextView)par.findViewById(R.id.textViewLike);
            long id = (long) likeTv2.getTag();
            onDislikeClicked(id, par, likeTv2, dislikeTv2);
        }
    };


    public LoaderManager.LoaderCallbacks<Cursor> createLoaderCallbacks() {
        return new LoaderManager.LoaderCallbacks<Cursor>() {
            @Override
            public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                return new SimpleCursorLoader(owner) {
                    @Override
                    public Cursor loadCursor() {
                        return database.queryPosts(stageName, subName);
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
        };
    }

    void onDislikeClicked(long id, View parentView, TextView likeTv, TextView dislikeTv) {
        int current =(Integer) parentView.getTag();
        if(current == -1) {
            parentView.setTag(0);
            likeTv.setTextColor(Color.LTGRAY);
            sync.postLike(id, 0);
            return;
        }
        parentView.setTag(-1);
        dislikeTv.setTextColor(Color.RED);
        likeTv.setTextColor(Color.LTGRAY);
        sync.postLike(id, -1);
        return;
    }

    void onLikeClicked(long id, View parentView, TextView likeTv, TextView dislikeTv) {
        int current =(Integer) parentView.getTag();
        if(current == 1) {
            parentView.setTag(0);
            likeTv.setTextColor(Color.LTGRAY);
            sync.postLike(id, 0);
            return;
        }
        parentView.setTag(1);
        likeTv.setTextColor(Color.RED);
        dislikeTv.setTextColor(Color.LTGRAY);
        sync.postLike(id, 1);
    }



}
