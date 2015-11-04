package com.livejournal.karino2.openeibunpou;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

public class UserPostActivity extends AppCompatActivity {


    String stageName;
    String subName;
    String draftText;
    Sync sync;

    Intent resultIntent = new Intent();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_post);

        Intent intent = getIntent();
        if(intent != null) {
            stageName = intent.getStringExtra("stageName");
            subName = intent.getStringExtra("subName");
            draftText = intent.getStringExtra("draftText");

            resultIntent.putExtra("draftText", draftText);
            setResult(RESULT_CANCELED, resultIntent);

            setTVText(R.id.textViewBody, intent.getStringExtra("body"));
            setTVText(R.id.textViewAnswer, intent.getStringExtra("answer"));
        }

    }

    void setTVText(int id, String text) {
        ((TextView)findViewById(id)).setText(text);
    }

    @Override
    protected void onStart() {
        super.onStart();
        sync = Sync.getInstance(this);
        sync.addOnUserPostArrivedListener(R.layout.activity_user_post, new Sync.OnStageUpdateListener() {
            @Override
            public void onStageUpdate(String stageName) {
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    @Override
    protected void onStop() {
        sync.removeOnUserPostArrivedListener(R.layout.activity_user_post);
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        draftText = ((EditText)findViewById(R.id.editTextUserPost)).getText().toString();
        resultIntent.putExtra("draftText", draftText);
        setResult(RESULT_CANCELED, resultIntent);
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_user_post, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_post:
                CheckBox cb = (CheckBox)findViewById(R.id.checkBoxAnonymous);
                int anonymous = cb.isChecked() ? 1 : 0 ;

                EditText et = (EditText)findViewById(R.id.editTextUserPost);
                Sync.getInstance(this).postComment(stageName, subName, anonymous, et.getText().toString());
                et.setText("");

                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
