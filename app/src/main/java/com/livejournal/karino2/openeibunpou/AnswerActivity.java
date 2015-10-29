package com.livejournal.karino2.openeibunpou;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
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

    SimpleCursorAdapter adapter;
    Database database;

    String stageName;
    String subName;

    Sync sync;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_answer);

        database = Database.getInstance(this);
        sync = QuestionActivity.s_sync; // temp workaround.

        Intent intent = getIntent();
        if(intent != null) {
            // TODO: save instance state.
            stageName = intent.getStringExtra("stageName");
            subName = intent.getStringExtra("subName");



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

        /*
                                "userpost_table._id as _id",
                        "serverId",
                        "like",
                        "dislike",
                        "body",
                        "val as mylike"

         */
        adapter = new SimpleCursorAdapter(this, R.layout.post_item, null,
                new String[]{"body", "like", "dislike", "mylike"},
                new int[]{ R.id.textViewPost, R.id.textViewLikeTotal, R.id.textViewDislikeTotal, R.id.textViewLike });
        /*
        adapter = new SimpleCursorAdapter(this, R.layout.post_item, null,
                new String[]{"body", "like", "dislike"},
                new int[]{ R.id.textViewPost, R.id.textViewLikeTotal, R.id.textViewDislikeTotal});
                */

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

                    likeTv.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            View par = (View)v.getParent();
                            long id = (long)v.getTag();
                            TextView likeTv2 = (TextView)v;
                            TextView dislikeTv2 =  (TextView)par.findViewById(R.id.textViewDislike);
                            onLikeClicked(id, par, likeTv2, dislikeTv2);
                        }
                    });
                    dislikeTv.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            View par = (View)v.getParent();
                            TextView dislikeTv2 = (TextView)v;
                            TextView likeTv2 =  (TextView)par.findViewById(R.id.textViewLike);
                            long id = (long) likeTv2.getTag();
                            onDislikeClicked(id, par, likeTv2, dislikeTv2);
                        }
                    });



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
        ListView lv = (ListView)findViewById(R.id.listViewComment);
        /*
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                View parentView = (View)view.getParent();
                TextView likeTv = (TextView)parent.findViewById(R.id.textViewLike);
                TextView dislikeTv = (TextView)parent.findViewById(R.id.textViewDislike);

                switch(view.getId()) {
                    case R.id.textViewLike:
                    {
                        onLikeClicked(id, parentView, likeTv, dislikeTv);
                        break;
                    }
                    case R.id.textViewDislike:
                    {
                        onDislikeClicked(id, parentView, likeTv, dislikeTv);
                        return;
                    }
                }
            }
        });
        */
        lv.setAdapter(adapter);

        getLoaderManager().initLoader(0, null, new LoaderManager.LoaderCallbacks<Cursor>() {
            @Override
            public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                return new SimpleCursorLoader(AnswerActivity.this) {
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
        });

        findViewById(R.id.buttonSend).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText et = (EditText)findViewById(R.id.editTextComment);
                sync.postComment(stageName, subName, et.getText().toString());
                et.setText("");
            }
        });

    }

    public void onDislikeClicked(long id, View parentView, TextView likeTv, TextView dislikeTv) {
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

    public void onLikeClicked(long id, View parentView, TextView likeTv, TextView dislikeTv) {
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
