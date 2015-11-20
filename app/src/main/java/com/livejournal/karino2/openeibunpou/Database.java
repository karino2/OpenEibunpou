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
import android.database.sqlite.SQLiteQueryBuilder;

import com.google.gson.Gson;

public class Database {
	// cmd 1: sync one stage, body equal stageName
	// cmd 2: sync one stage completion, body equal stageName
	public static final int CMD_ID_SYNC_QUESTIONS = 1;
	public static final int CMD_ID_SYNC_COMPLETION = 2;
	public static final int CMD_ID_UPDATE_COMPLETION = 3;
	public static final int CMD_ID_SYNC_USERPOST = 4;
    public static final int CMD_ID_POST_USERPOST = 5;
	public static final int CMD_ID_SYNC_MYLIKE = 6;
    public static final int CMD_ID_UPDATE_MYLIKE = 7;
    public static final int CMD_ID_SYNC_LATEST_POST = 8;

    static final int DATABASE_VERSION = 3;

    private class OpenHelper extends SQLiteOpenHelper {
		
		OpenHelper(Context context) {
			super(context, "openeibunpou.db", null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			createTables(db);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int arg1, int arg2)
        {
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

        createUserPostTable(db, "userpost_table");
        createUserPostTable(db, "userpost_latest_table");


        db.execSQL("CREATE TABLE like_table (_id integer primary key autoincrement"
                +",postId integer not null"
				+",val integer not null"
				+",date integer not null"
				+");");

		
		// cmd 1: sync one stage, body equal stageName
		// cmd 2: sync one stage completion, body equal stageName
		db.execSQL("CREATE TABLE pendingrequest_table (_id integer primary key autoincrement"
                +",cmd integer"
                +",body text not null"
                +",arg text"
				+");");

	}

    private void createUserPostTable(SQLiteDatabase db, String tableName) {
        db.execSQL("CREATE TABLE " + tableName + " (_id integer primary key autoincrement"
                + ",serverId integer not null"
                + ",owner text not null"
                + ",nick text"
                + ",stageName text not null"
                + ",subName text not null"
                + ",parent integer not null"
                + ",childrenNum integer not null"
                + ",like integer not null"
                + ",dislike integer not null"
                + ",totalScore integer not null"
                + ",report integer not null"
                + ",body text not null"
                + ",date integer not null"
                + ");");
    }

    private void dropTables(SQLiteDatabase db) {
		db.execSQL("DROP TABLE IF EXISTS question_table;");
		db.execSQL("DROP TABLE IF EXISTS stage_table;");
		db.execSQL("DROP TABLE IF EXISTS completion_table;");
		db.execSQL("DROP TABLE IF EXISTS userpost_table;");
		db.execSQL("DROP TABLE IF EXISTS like_table;");
		db.execSQL("DROP TABLE IF EXISTS pendingrequest_table;");
        db.execSQL("DROP TABLE IF EXISTS userpost_latest_table;");
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

    public void insertMyLike(long postId, int val, long tick) {
        ContentValues values = new ContentValues();
        values.put("val", val);
        values.put("date", tick);
		int resNum = database.update("like_table", values, "postId = ?", new String[]{String.valueOf(postId)});

		if(resNum == 0) {
			values.put("postId", postId);
			database.insert("like_table", null, values);
		}
    }

    public static class UserPostRecord {
        public long id;
        public String owner;
        public String nick;
        public String year;
        public String sub;
        public long parent;
        public String body;
        public int like;
        public int dislike;
        public long date;
    }

    public void insertUserPost(String tableName, UserPostRecord rec) {
        ContentValues values = new ContentValues();
        values.put("serverId", rec.id);
        values.put("owner", rec.owner);
        values.put("nick", rec.nick);
        values.put("stageName", rec.year);
        values.put("subName", rec.sub);
        values.put("parent", rec.parent);
        values.put("childrenNum", 0);
        values.put("like", rec.like);
        values.put("dislike", rec.dislike);
        values.put("totalScore", rec.like - rec.dislike);
        values.put("report", 0);
        values.put("body", rec.body);
        values.put("date", rec.date);
		int res = database.update(tableName, values, "serverId = ?" , new String[]{String.valueOf(rec.id)});
		if(res == 0)
	        database.insert(tableName, null, values);
    }


    public void insertPendingCmdWithArg(int cmd, String body, String arg) {
        ContentValues values = new ContentValues();
        values.put("cmd", cmd);
        values.put("body", body);
        values.put("arg", arg);
        database.insert("pendingrequest_table", null, values);

    }


	public void insertPendingCmd(int cmd, String body) {
        insertPendingCmdWithArg(cmd, body, "");
	}

	public void insertOneStageSyncRequest(String stageName) {
		insertPendingCmd(CMD_ID_SYNC_QUESTIONS, stageName);
	}

	public void insertCompletionRequest(String stageName) {
		insertPendingCmd(CMD_ID_SYNC_COMPLETION, stageName);
	}

    public void insertUpdateCompletionRequest(String json) {
        insertPendingCmd(CMD_ID_UPDATE_COMPLETION, json);
    }

    public void insertUserPostSyncRequest(String stageName) {
        insertPendingCmd(CMD_ID_SYNC_USERPOST, stageName);
    }

    public void insertPostUserPostRequest(String json) {
        insertPendingCmd(CMD_ID_POST_USERPOST, json);
    }

    public void insertMyLikeSyncRequest(String stageName) {
        insertPendingCmd(CMD_ID_SYNC_MYLIKE, stageName);
    }

    public void insertSyncLatestPostRequest() {
        insertPendingCmd(CMD_ID_SYNC_LATEST_POST, "");
    }

    public void deleteSyncLatestPostRequest() {
        deletePendingCommandByCmdId(CMD_ID_SYNC_LATEST_POST);
    }

    public void insertMyLikeUpdateRequest(long id, int newVal) {
        insertPendingCmdWithArg(CMD_ID_UPDATE_MYLIKE, String.valueOf(id), String.valueOf(newVal));
    }

    public void deletePendingMyLikeUpdate(long id) {
        deletePendingCommand(CMD_ID_UPDATE_MYLIKE, String.valueOf(id));
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

    public void deletePendingCommandByCmdId(int cmdId) {
        database.delete("pendingrequest_table", "cmd = ?", new String[]{String.valueOf(cmdId)});
    }


	public static class PendingCommand {
		public long id;
        public int cmd;
        public String body;
        public String arg;
        public PendingCommand(long id, int cmd, String body, String arg) {
			this.id = id;
            this.cmd = cmd;
            this.body = body;
            this.arg = arg;
        }
    }

    Cursor queryPendingCursor() {
        return database.query("pendingrequest_table", new String[]{"_id", "cmd", "body", "arg"}, null, null,null,null, "_id DESC");
    }

    public List<PendingCommand> queryPendingCommand() {
        ArrayList<PendingCommand> res = new ArrayList<>();
        Cursor cursor = queryPendingCursor();
        try {
            if(cursor.getCount() == 0)
                return res;
            cursor.moveToFirst();
            do {
                PendingCommand cmd = new PendingCommand(cursor.getLong(0), cursor.getInt(1), cursor.getString(2), cursor.getString(3));
                res.add(cmd);
            }while(cursor.moveToNext());
        }finally {
            if(cursor != null)
                cursor.close();
        }
        return res;
    }

    public QuestionRecord queryQuestion(String stageName, String subName) {
        Map<String, Integer> compMap = queryCompletionMap(stageName);
        Cursor cursor = database.query("question_table", new String[]{"_id", "stageName", "subName", "body", "options", "answer", "type"}, "stageName = ? AND subName = ?", new String[]{stageName, subName}, null, null, null);
        try {
            if(0 == cursor.getCount())
                return null;
            cursor.moveToFirst();
            int completion = getCompletion(cursor.getString(2), compMap);
            return QuestionRecord.createFromCursor(cursor, completion);
        }finally {
            cursor.close();
        }

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
				stage.addQuestion(QuestionRecord.createFromCursor(cursor, completion));
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
        return lastTick(stageName, "completion_table");
	}

    public long lastLikeTick(String stageName) {
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables("userpost_table LEFT OUTER JOIN like_table ON (userpost_table.serverId = like_table.postId)");
        Cursor cursor = builder.query(database, new String[]{"max(like_table.date)"}, "stageName =?", new String[]{stageName}, null, null, null);
        return getMaxDate(cursor);
    }

    public long lastTick(String stageName, String tableName) {
        Cursor cursor = database.query(tableName, new String[]{"max(date)"}, "stageName = ?", new String[]{stageName}, null, null, null);
        return getMaxDate(cursor);
    }

    public long lastLatestPostTick() {
        Cursor cursor = database.query("userpost_latest_table", new String[]{"max(date)"},null, null, null, null, null);
        return getMaxDate(cursor);
    }

    public long getMaxDate(Cursor cursor) {
        try {
            if(cursor.getCount() == 0)
                return 0;
            cursor.moveToFirst();
            return cursor.getLong(0);
        }finally {
            cursor.close();
        }
    }

    public long lastPostTick(String stageName) {
        return lastTick(stageName, "userpost_table");
    }

    public Cursor queryLatestPosts() {
        return database.query("userpost_latest_table", new String[]{"_id", "stageName", "subName", "owner", "body", "date", "nick"}, null, null, null, null, "date DESC");
    }

    public Cursor queryPosts(String stageName, String subName) {
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables("userpost_table LEFT OUTER JOIN like_table ON (userpost_table.serverId = like_table.postId)");
        return builder.query(database, new String[]{
                        "userpost_table._id as _id",
                        "serverId",
                        "like",
                        "dislike",
                        "body",
                        "val as mylike",
						"owner",
                        "nick"
                },
                "stageName = ? AND subName = ?",
                new String[] {
                        stageName,
                        subName
                },
                null,
                null,
                "totalScore DESC"
                );

    }


}
