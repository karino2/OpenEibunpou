package com.livejournal.karino2.openeibunpou;

import java.util.ArrayList;


import android.os.Bundle;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class SetupActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        String accounts[] = getGoogleAccounts();
        ListView listView = (ListView)findViewById(R.id.lvText);
        listView.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, accounts));
        listView.setOnItemClickListener(new OnItemClickListener(){

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int pos,
                                    long id) {
                TextView tv = (TextView)arg1;
                register((String)tv.getText());
                finish();
            }});
    }

    protected void register(String account) {
        SharedPreferences prefs = this.getSharedPreferences("account", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("accountName", account);
        editor.commit();
    }


    private String[] getGoogleAccounts() {
        ArrayList<String> accountNames = new ArrayList<String>();
        Account[] accounts = AccountManager.get(this).getAccounts();
        for (Account account : accounts) {
            if (account.type.equals("com.google")) {
                accountNames.add(account.name);
            }
        }

        String[] result = new String[accountNames.size()];
        accountNames.toArray(result);
        return result;
    }
}
