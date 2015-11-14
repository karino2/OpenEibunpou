package com.livejournal.karino2.openeibunpou;

import android.content.Intent;
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
        completion = Math.max(0, completion - 50);
    }

    public QuestionRecord(String subName, String body, String[] options, int[] answers, int type, int completion)
    {
        this.sub = subName;
        this.body = body;
        this.options = options;
        this.answer = answers;
        this.type = type;
        this.completion = completion;
    }

    public static QuestionRecord createFromCursor(Cursor cursor, int completion) {
        Gson gson = new Gson();
        return new QuestionRecord(cursor.getString(2), cursor.getString(3), gson.fromJson(cursor.getString(4), String[].class),
                gson.fromJson(cursor.getString(5), int[].class),
                cursor.getInt(6), completion);

    }

    public static QuestionRecord createFromIntent(Intent intent)
    {
        return new QuestionRecord(intent.getStringExtra("subName"), intent.getStringExtra("body"), intent.getStringArrayExtra("options"),
                intent.getIntArrayExtra("answerNum"), intent.getIntExtra("questionType", 0), intent.getIntExtra("completion", 0));

    }

    public void saveToIntent(Intent intent)
    {
        intent.putExtra("subName", getSubName());
        intent.putExtra("body", getBody());
        intent.putExtra("options", getOptions());
        intent.putExtra("answerNum", getAnswers());
        intent.putExtra("questionType", getQuestionType());
        intent.putExtra("completion", getCompletion());
    }


    public void formatAnswers(StringBuffer buf) {
        formatIntArray(buf,getAnswers());
    }

    public String formatAnswers() {
        StringBuffer buf = new StringBuffer();
        formatAnswers(buf);
        return buf.toString();
    }

    public void formatOptions(StringBuffer buf) {
        formatStringArray(buf, getOptions());
    }



    // used by format selected, too.
    public void formatIntArray(StringBuffer buf, int[] arr) {
        for(int elm : arr) {
            buf.append(elm);
            buf.append(",");
        }
    }

    void formatStringArray(StringBuffer buf, String[] arr) {
        buf.append("\n");
        for(int i =0; i < arr.length; i++){
            buf.append("    ");
            buf.append(i+1);
            buf.append(": ");
            buf.append(arr[i]);
            buf.append("\n");
        }
    }


}
