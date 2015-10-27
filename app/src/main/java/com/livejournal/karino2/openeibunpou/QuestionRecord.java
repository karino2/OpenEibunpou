package com.livejournal.karino2.openeibunpou;

import android.database.Cursor;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

/**
 * Created by karino on 10/24/15.
 */
public class QuestionRecord {
    String body;
    String sub;
    int[] answer;
    String[] options;
    int type;
    int completion = -1;

    public QuestionRecord(){}

    public String getBody() { return body; }
    public String getSubName() { return sub; }
    public int[] getAnswers() { return answer; }
    public String[] getOptions() { return options; }
    public int getQuestionType() { return type; }
    public int getCompletion() { return completion; }

    public void answerCorrectUpdateCompletion(){
        if(completion == -1) {
            completion = 100;
            return;
        }
        completion = Math.min(100, completion+34);
    }
    public void answerWrongUpdateCompletion() {
        if(completion == -1) {
            completion = 0;
            return;
        }
        completion = Math.max(0, completion-50);
    }

    public QuestionRecord(Cursor cursor, int completion) {
        Gson gson = new Gson();
        sub = cursor.getString(2);
        body = cursor.getString(3);
        options = gson.fromJson(cursor.getString(4), String[].class);
        answer = gson.fromJson(cursor.getString(5), int[].class);
        type = cursor.getInt(6);
        this.completion = completion;
    }
}
