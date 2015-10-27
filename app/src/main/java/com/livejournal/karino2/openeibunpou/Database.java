package com.livejournal.karino2.openeibunpou;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.gson.Gson;

public class Database {
	// cmd 1: sync one stage, body equal stageName
	// cmd 2: sync one stage completion, body equal stageName
	public static final int CMD_ID_SYNC_QUESTIONS = 1;
	public static final int CMD_ID_SYNC_COMPLETION = 2;
	public static final int CMD_ID_UPDATE_COMPLETION = 3;

	public void insertUpdateCompletionRequest(String json) {
		insertPendingCmd(CMD_ID_UPDATE_COMPLETION, json);
	}


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

		db.execSQL("CREATE TABLE completion_table (_id integer primary key autoincrement"
		+",stageName text not null"
		+",subName text not null"
		+",completion integer not null"
		+",date integer not null"
		+");");
		
		
		// cmd 1: sync one stage, body equal stageName
		// cmd 2: sync one stage completion, body equal stageName
		db.execSQL("CREATE TABLE pendingrequest_table (_id integer primary key autoincrement"
	+",cmd integer"
	+",body text not null"
				+");");
				
	}

	private void dropTables(SQLiteDatabase db) {
		db.execSQL("DROP TABLE IF EXISTS question_table;");
		db.execSQL("DROP TABLE IF EXISTS stage_table;");
		db.execSQL("DROP TABLE IF EXISTS completion_table;");
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

	public void updateStageCompletion(String stageName, int newCompletionVal) {
		ContentValues values = new ContentValues();
		values.put("completion", newCompletionVal);
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

	public void insertQuestionCompletion(String stageName, String subName, int completion, long tick) {
		ContentValues values = new ContentValues();
		values.put("stageName", stageName);
		values.put("subName", subName);
		values.put("completion", completion);
		values.put("date", tick);
		database.insert("completion_table", null, values);
	}




	public void insertPendingCmd(int cmd, String body) {
		ContentValues values = new ContentValues();
		values.put("cmd", cmd);
		values.put("body", body);
		database.insert("pendingrequest_table", null, values);
	}

	public void insertOneStageSyncRequest(String stageName) {
		insertPendingCmd(CMD_ID_SYNC_QUESTIONS, stageName);
	}

	public void insertCompletionRequest(String stageName) {
		insertPendingCmd(CMD_ID_SYNC_COMPLETION, stageName);
	}

	public void deletePendingOneStageSyncRequest(String stageName) {
		deletePendingCommand(CMD_ID_SYNC_QUESTIONS, stageName);
	}

	public void deletePendingCompletionRequest(String stageName) {
		deletePendingCommand(CMD_ID_SYNC_COMPLETION, stageName);
	}

	public void deletePendingCommandById(long id) {
		database.delete("pendingrequest_table", "_id = ?", new String[]{String.valueOf(id)});

	}

	public void deletePendingCommand(int cmdId, String body) {
		database.delete("pendingrequest_table", "cmd = ? AND body = ?", new String[]{String.valueOf(cmdId), body});
	}


	public static class PendingCommand {
		public long id;
        public int cmd;
        public String body;
        public PendingCommand(long id, int cmd, String body) {
			this.id = id;
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
                PendingCommand cmd = new PendingCommand(cursor.getLong(0), cursor.getInt(1), cursor.getString(2));
                res.add(cmd);
            }while(cursor.moveToNext());
        }finally {
            if(cursor != null)
                cursor.close();
        }
        return res;
    }

    public Stage queryStage(String stageName) {
        Stage stage = new Stage(stageName);
		Map<String, Integer> compMap = queryCompletionMap(stageName);
		Cursor cursor = database.query("question_table", new String[]{"_id", "stageName", "subName", "body", "options", "answer", "type"}, "stageName = ?", new String[]{stageName}, null, null, "subName ASC");
        try {
            if(0 == cursor.getCount())
                return stage;
            cursor.moveToFirst();
			do {
				int completion = getCompletion(cursor.getString(2), compMap);
				stage.addQuestion(new QuestionRecord(cursor, completion));
            }while(cursor.moveToNext());
            
            return stage;
        }finally {
            cursor.close();
        }
    }

	public int getCompletion(String sub, Map<String, Integer> compMap) {
		int completion = -1;
		if(compMap.containsKey(sub))
            completion = compMap.get(sub);
		return completion;
	}

	Map<String, Integer> queryCompletionMap(String stageName) {
		Map<String, Integer> map = new HashMap<>();
		Cursor cursor = database.query("completion_table", new String[]{"subName", "completion"}, "stageName = ?", new String[]{stageName}, null, null, null);
		try {
			if(cursor.getCount() == 0)
				return map;
			cursor.moveToFirst();
			do {
				map.put(cursor.getString(0), cursor.getInt(1));
			}while(cursor.moveToNext());
		} finally
		{
			cursor.close();
		}
		return map;
	}

	public long lastCompletionTick(String stageName) {
		Cursor cursor = database.query("completion_table", new String[]{"max(date)"}, "stageName = ?", new String[]{stageName}, null, null, null);
		try {
			if(cursor.getCount() == 0)
				return 0;
			cursor.moveToFirst();
			return cursor.getLong(0);
		}finally {
			cursor.close();
		}

	}


}
