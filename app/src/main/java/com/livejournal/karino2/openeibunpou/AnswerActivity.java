package com.livejournal.karino2.openeibunpou;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.LoaderManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class AnswerActivity extends AppCompatActivity {
    final int DIALOG_ID_NEWPOST = 1;

    String stageName;

    Sync getSync() {
        return  Sync.getInstance(this);
    }

    QuestionRecord question;

    UserPostListHolder postListHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_answer);

        Intent intent = getIntent();
        if(intent != null) {
            // TODO: save instance state.
            question = QuestionRecord.createFromIntent(intent);

            stageName = intent.getStringExtra("stageName");

            setTitle(String.valueOf(question.getCompletion() + " %"));


            TextView tv = (TextView)findViewById(R.id.textViewBody);
            tv.setText(question.getBody());

            int[] selected = intent.getIntArrayExtra("selectedNum");
            boolean userAnswerExist = selected != null;

            if(userAnswerExist) {
                tv = (TextView) findViewById(R.id.textViewResult);
                if (isCorrect(selected, question.getAnswers())) {
                    tv.setText("Correct");
                } else {
                    tv.setText("Wrong");
                }
            }

            StringBuffer buf = new StringBuffer();
            if(userAnswerExist) {
                buf.append("selected: ");
                question.formatIntArray(buf, selected);
                buf.append("\n");
            }

            buf.append("answer: ");
            question.formatAnswers(buf);
            buf.append("\n");

            buf.append("options: ");
            question.formatOptions(buf);
            buf.append("\n");

            buf.append("questionType: ");
            buf.append(question.getQuestionType());

            tv = (TextView)findViewById(R.id.textViewAnswer);
            tv.setText(buf.toString());

            if(!userAnswerExist) {
                ((Button)findViewById(R.id.buttonNext)).setText(R.string.label_back);
            }

        }
        postListHolder = new UserPostListHolder(this, getSync(), Database.getInstance(this), stageName, question.getSubName());


        findViewById(R.id.buttonNext).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(Activity.RESULT_OK);
                finish();
            }
        });

        ListView lv = (ListView)findViewById(R.id.listViewComment);
        lv.setAdapter(postListHolder.getAdapter());

        getLoaderManager().initLoader(0, null, postListHolder.createLoaderCallbacks());

        findViewById(R.id.buttonNewPost).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(DIALOG_ID_NEWPOST);
            }
        });
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch(id) {
            case DIALOG_ID_NEWPOST:
                View view = getLayoutInflater().inflate(R.layout.dialog_user_post, null);
                final Dialog dialog =  new AlertDialog.Builder(this)
                        .setView(view)
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                draftText = ((EditText)((AlertDialog)dialog).findViewById(R.id.editTextUserPost)).getText().toString();
                            }
                        })
                        .setCancelable(true)
                        .create();
                dialog.setCanceledOnTouchOutside(true);
                (view.findViewById(R.id.buttonPost)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        View holder = (View) v.getParent();
                        CheckBox cb = (CheckBox) holder.findViewById(R.id.checkBoxAnonymous);
                        int anonymous = cb.isChecked() ? 1 : 0;

                        EditText et = (EditText) holder.findViewById(R.id.editTextUserPost);
                        getSync().postComment(stageName, question.getSubName(), anonymous, et.getText().toString());
                        draftText = "";
                        // TODO: should show notification.
                        dialog.dismiss();
                    }
                });
                return dialog;
        }
        return super.onCreateDialog(id);
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch(id) {
            case DIALOG_ID_NEWPOST:
                ((EditText)dialog.findViewById(R.id.editTextUserPost)).setText(draftText);
                return;
        }
        super.onPrepareDialog(id, dialog);
    }

    String getTVText(int id) {
        return ((TextView)findViewById(id)).getText().toString();
    }

    String draftText = "";


    @Override
    protected void onStart() {
        super.onStart();
        getSync().addOnUserPostArrivedListener(R.layout.activity_answer, new Sync.OnStageUpdateListener() {
            @Override
            public void onStageUpdate(String stageName) {
                refresh();
            }
        });
    }

    void refresh() {
        Loader<Object> loader = getLoaderManager().getLoader(0);
        if(loader != null)
            loader.forceLoad();
    }

    @Override
    protected void onStop() {
        getSync().removeOnUserPostArrivedListener(R.layout.activity_answer);
        super.onStop();
    }

    public static boolean isCorrect(int[] selected, int[] answer) {
        if(selected.length != answer.length)
            return false;
        for(int i = 0; i < selected.length; i++) {
            if(selected[i] != answer[i])
                return false;
        }
        return true;
    }
}
