package com.livejournal.karino2.openeibunpou;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class AnswerActivity extends AppCompatActivity {

    void formatIntArray(StringBuffer buf, int[] arr) {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_answer);

        Intent intent = getIntent();
        if(intent != null) {
            TextView tv = (TextView)findViewById(R.id.textViewBody);
            tv.setText(intent.getStringExtra("body"));

            int[] selected = intent.getIntArrayExtra("selectedNum");
            int[] answer = intent.getIntArrayExtra("answerNum");

            tv = (TextView)findViewById(R.id.textViewResult);
            if(isCorrect(selected, answer))
            {
                tv.setText("Correct");
            } else {
                tv.setText("Wrong");
            }

            StringBuffer buf = new StringBuffer();
            buf.append("selected: ");
            formatIntArray(buf, selected);
            buf.append("\n");

            buf.append("answer: ");
            formatIntArray(buf, answer);
            buf.append("\n");

            buf.append("options: ");
            formatStringArray(buf, intent.getStringArrayExtra("options"));
            buf.append("\n");

            buf.append("questionType: ");
            buf.append(intent.getIntExtra("questionType", 0));

            tv = (TextView)findViewById(R.id.textViewAnswer);
            tv.setText(buf.toString());
        /*
                intent.putExtra("selectedNum", new int[]{selected});
        intent.putExtra("answerNum", question.getAnswers());
        intent.putExtra("options", question.getOptions());
        intent.putExtra("body", question.getBody());
        intent.putExtra("questionType", question.getQuestionType());

         */

        }

        findViewById(R.id.buttonNext).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(Activity.RESULT_OK);
                finish();
            }
        });
    }

    private boolean isCorrect(int[] selected, int[] answer) {
        if(selected.length != answer.length)
            return false;
        for(int i = 0; i < selected.length; i++) {
            if(selected[i] != answer[i])
                return false;
        }
        return true;
    }
}
