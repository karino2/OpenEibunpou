package com.livejournal.karino2.openeibunpou;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.gson.Gson;

public class Database {



    private class OpenHelper extends SQLiteOpenHelper {
		
		OpenHelper(Context context) {
			super(context, "openeibunpou.db", null, 1);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			createTables(db);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int arg1, int arg2) {
			recreate(db);
		}
		
	}

	private void createTables(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE question_table (_id integer primary key autoincrement"
	+",stageName text not null"
	+",subName text not null"
	+",body text"
	+",options text" // "[\"abc\", \"def\"]"
	+",answer text" // "[1, 2]"
	+",type integer not null"
				+");");
		
		db.execSQL("CREATE TABLE stage_table (_id integer primary key autoincrement"
	+",stageName text not null" // 13-1, 14-1, etc.
	+",loaded integer not null"  // 0: not loaded 1: loaded
	+",completion integer not null" // 0-100. percentage.
				+");");
		
		
		// cmd 1: sync one stage, body equal stageName
		db.execSQL("CREATE TABLE pendingrequest_table (_id integer primary key autoincrement"
	+",cmd integer"
	+",body text not null"
				+");");
				
	}

	private void dropTables(SQLiteDatabase db) {
		db.execSQL("DROP TABLE IF EXISTS question_table;");
		db.execSQL("DROP TABLE IF EXISTS stage_table;");
		db.execSQL("DROP TABLE IF EXISTS pendingrequest_table;");
	}
	
	private void recreate(SQLiteDatabase db) {
		dropTables(db);
		createTables(db);
	}

	private Database(){}
	
		
	public static void finalizeInstance() {
		if(s_instance != null) {
			s_instance.close();
			s_instance = null;
		}
	}
	
	public static synchronized Database getInstance(Context ctx) {
		if(s_instance == null) {
			s_instance = new Database();
			s_instance.open(ctx);
		}
		return s_instance;
	}
	
	private static Database s_instance;
	/*
	public static Database getInstance() {
		if(s_instance == null) {
			s_instance = new Database();
			s_instance.open(s_context);
		}
		return s_instance;
	}
	*/
	
	OpenHelper helper;
	SQLiteDatabase database;
	public void open(Context context) {
		helper = new OpenHelper(context);
		database = helper.getWritableDatabase();
	}
	
	public void close() {
		helper.close();
	}
	
	public void recreate() {
		recreate(database);
	}
	

		
	public Cursor queryStageCursor() {
		return database.query("stage_table", new String[]{"_id", "stageName", "loaded", "completion"}, null, null,null,null, "stageName DESC");
	}

    public void updateStageLoadState(String stageName, int newLoadedVal) {
        ContentValues values = new ContentValues();
        values.put("loaded", newLoadedVal);
        database.update("stage_table", values, "stageName = ?", new String[]{stageName});
    }

    public void insertOneQuestionRecord(String stageName, QuestionRecord record, Gson gson) {
        ContentValues values = new ContentValues();
        values.put("stageName", stageName);
        values.put("subName", record.sub);
        values.put("body", record.body);
        values.put("options", gson.toJson(record.options));
        values.put("answer", gson.toJson(record.answer));
        values.put("type", record.type);
        database.insert("question_table", null, values);
    }

	
	public void insertStage(String stageName) {
		ContentValues values = new ContentValues();
		values.put("stageName", stageName);
		values.put("loaded", 0);
		values.put("completion", 0);
		database.insert("stage_table", null, values);
	}

	public void insertOneStageSyncRequest(String stageName) {
        ContentValues values = new ContentValues();
        values.put("cmd", 1);
        values.put("body", stageName);
        database.insert("pendingrequest_table", null, values);
    }
	public void deletePendingOneStageSyncRequest(String stageName) {
		database.delete("pendingrequest_table", "body = ?", new String[]{stageName});
	}

    public static class PendingCommand {
        public int cmd;
        public String body;
        public PendingCommand(int cmd, String body) {
            this.cmd = cmd;
            this.body = body;
        }
    }

    Cursor queryPendingCursor() {
        return database.query("pendingrequest_table", new String[]{"_id", "cmd", "body"}, null, null,null,null, "_id ASC");
    }

    public List<PendingCommand> queryPendingCommand() {
        ArrayList<PendingCommand> res = new ArrayList<>();
        Cursor cursor = queryPendingCursor();
        try {
            if(cursor.getCount() == 0)
                return res;
            cursor.moveToFirst();
            do {
                PendingCommand cmd = new PendingCommand(cursor.getInt(1), cursor.getString(2));
                res.add(cmd);
            }while(cursor.moveToNext());
        }finally {
            if(cursor != null)
                cursor.close();
        }
        return res;
    }


}
