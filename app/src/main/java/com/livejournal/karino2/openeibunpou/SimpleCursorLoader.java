package com.livejournal.karino2.openeibunpou;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;

/**
 * Created by karino on 10/24/15.
 */
public abstract class SimpleCursorLoader extends CursorLoader {
    private final ForceLoadContentObserver observer = new ForceLoadContentObserver();

    public SimpleCursorLoader(Context context) {
        super(context);
    }

    public abstract Cursor loadCursor();

    @Override
    public Cursor loadInBackground() {
        Cursor cursor = loadCursor();

        if (cursor != null) {
            cursor.getCount();
            cursor.registerContentObserver(observer);
        }

        return cursor;
    }
}
