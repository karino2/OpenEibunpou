package com.livejournal.karino2.openeibunpou;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class QuestionActivity extends AppCompatActivity {

    final int DUMMY_REQUEST_ANSWER_ID = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question);

        Intent intent = getIntent();
        if(intent != null)
        {
            stageName = intent.getStringExtra("stageName");
            stage = Database.getInstance(this).queryStage(stageName);
        }

        applyCurrentQuestion();

        findViewById(R.id.buttonEnter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gotoAnswer();
            }
        });

        OptionsView optionsView = (OptionsView)findViewById(R.id.optionView);
        optionsView.setEnableEnterListener(new OptionsView.EnableEnterListener() {
            @Override
            public void enableEnter() {
                findViewById(R.id.buttonEnter).setEnabled(true);
            }

            @Override
            public void disableEnter() {
                findViewById(R.id.buttonEnter).setEnabled(false);
            }
        });
    }

    private void applyCurrentQuestion() {
        QuestionRecord question = stage.getCurrentQuestion();
        TextView tv = (TextView)findViewById(R.id.textViewBody);
        tv.setText(question.getBody());

        String[] options = question.getOptions();
        OptionsView optionsView = (OptionsView)findViewById(R.id.optionView);
        switch(question.getQuestionType()) {
            case 3:
                optionsView.setOptionType(OptionsView.OPTION_TYPE_DOUBLE);
                break;
            default:
                optionsView.setOptionType(OptionsView.OPTION_TYPE_SINGLE);
                break;
        }
        optionsView.setOptions(options);
    }

    int[] getSelectedIds() {
        OptionsView optionsView = (OptionsView)findViewById(R.id.optionView);
        return optionsView.getSelectedIds();
    }

    void gotoAnswer() {
        QuestionRecord question = stage.getCurrentQuestion();

        Intent intent = new Intent(this, AnswerActivity.class);
        intent.putExtra("selectedNum", getSelectedIds());
        intent.putExtra("answerNum", question.getAnswers());
        intent.putExtra("options", question.getOptions());
        intent.putExtra("body", question.getBody());
        intent.putExtra("questionType", question.getQuestionType());

        startActivityForResult(intent, DUMMY_REQUEST_ANSWER_ID);
    }

    void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == DUMMY_REQUEST_ANSWER_ID)
        {
            if(stage.hasNext()) {
                stage.gotoNext();
                applyCurrentQuestion();
            } else {
                gotoFinishActivity();
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void gotoFinishActivity() {
        Intent intent = new Intent(this, FinishStageActivity.class);
        startActivity(intent);
        finish();
    }


    String stageName;
    Stage stage;


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("stageName", stageName);
        outState.putInt("stagePosition", stage.getCurrentPosition());
        super.onSaveInstanceState(outState);
    }


    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        stageName = savedInstanceState.getString("stageName");
        if(stage == null) {
            stage = Database.getInstance(this).queryStage(stageName);
            stage.gotoQuestion(savedInstanceState.getInt("stagePosition"));

            applyCurrentQuestion();
        }
    }
}