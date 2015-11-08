package com.livejournal.karino2.openeibunpou;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.LoaderManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class StageActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private final int STATUS_NOTIFICAITON_ID = R.layout.activity_stage;


    void ensureCookie() {
        if(this.cookie == null) {
            // showMessage("setup...");
            Account account = getAccount();
            if(account == null)
            {
                pendingAuth = true;
                startActivity(new Intent(this, SetupActivity.class));
                return;
            }
            startRetrieveCookie(this, account);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stage);
        ensureCookie();



        ListView lv = (ListView)findViewById(R.id.listViewStages);

        adapter = new SimpleCursorAdapter(this, R.layout.stage_item, null,
                new String[]{"stageName", "loaded", "completion"}, new int[]{R.id.textViewStageName, R.id.textViewLoaded, R.id.textViewCompletion}, 0);
        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (columnIndex == 2) {
                    TextView tv = (TextView) view;
                    int loaded = cursor.getInt(2);
                    tv.setTag(loaded);
                    if (loaded == 0)
                        tv.setText("Not loaded.");
                    else
                        tv.setText("Loaded.");
                    return true;
                }

                if (columnIndex == 3) {
                    TextView tv = (TextView) view;
                    int loaded = cursor.getInt(2);
                    if (loaded == 0) {
                        tv.setText("- %");
                    } else {
                        int completion = cursor.getInt(3);
                        tv.setText(completion + " %");
                    }
                    return true;
                }

                return false;
            }
        });

        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView nameTv = (TextView) view.findViewById(R.id.textViewStageName);
                String stageName = nameTv.getText().toString();

                TextView loadedTv = (TextView) view.findViewById(R.id.textViewLoaded);
                int loaded = (int) loadedTv.getTag();
                if (loaded == 1) {
                    gotoGame(stageName);
                } else {
                    loadedTv.setText("Loading...");
                    sync.loadStage(stageName);
                }

            }
        });

        notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);;


        sync = Sync.getInstance(this);

        setStatusLabel("");

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        sync = Sync.getInstance(this);
        sync.addStageListUpdateListener(R.layout.activity_stage, new Sync.NotifyStageListListener() {
            @Override
            public void onUpdate() {
                setStatusLabel("");
                refresh();
            }
        });

        sync.addOneStageLoadedListener(R.layout.activity_stage, new Sync.OnStageUpdateListener() {
            @Override
            public void onStageUpdate(String stageName) {
                Stage stage = Database.getInstance(StageActivity.this).queryStage(stageName);
                sync.updateStageCompletion(stageName, stage.calcStageCompletion());

                showMessage("Stage " + stageName + " loaded.");
                refresh();
            }
        });

        sync.addErrorListener(R.layout.activity_stage, new Sync.ErrorListener() {
            @Override
            public void onError(String msg) {
                notifyStatusBarWithTicker("Error", msg, null);
            }
        });
    }

    @Override
    protected void onStop() {
        sync.removeStageListUpdateListener(R.layout.activity_stage);
        sync.removeOneStageLoadedListener(R.layout.activity_stage);
        sync.removeErrorListener(R.layout.activity_stage);
        super.onStop();
    }

    private void gotoGame(String stageName) {
        Intent intent = new Intent(this, QuestionActivity.class);
        intent.putExtra("stageName", stageName);
        startActivity(intent);
    }

    void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private void refresh() {
        Loader<Object> loader = getLoaderManager().getLoader(0);
        if(loader != null)
            loader.forceLoad();
    }

    NotificationManager notificationManager;

    private void notifyStatusBarWithTicker(String title, String message, String ticker) {
        if (title == null) {
            notificationManager.cancel(STATUS_NOTIFICAITON_ID);
            return;
        }

        notificationManager.notify(STATUS_NOTIFICAITON_ID, new NotificationCompat.Builder(this)
                .setTicker(ticker)
                .setContentTitle(title)
                .setContentText(message)
                .build());
    }


    SimpleCursorAdapter adapter;

    Database getDatabase(Context ctx) { return Database.getInstance(ctx); }

    Handler handler = new Handler();
    Sync sync;


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new SimpleCursorLoader(this) {
            @Override
            public Cursor loadCursor() {
                Cursor cursor = getDatabase(StageActivity.this).queryStageCursor();
                if(cursor == null || cursor.getCount() == 0)
                {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            setStatusLabel("Initializing...");
                            sync.syncStages();
                        }
                    });
                }
                return cursor;
            }
        };

    }

    private void setStatusLabel(String label) {
        TextView tv = (TextView)findViewById(R.id.textViewStatus);
        tv.setText(label);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        adapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_stage, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_delete:
                getDatabase(this).recreate();
                refresh();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }



    // cookie related.
    boolean pendingAuth = false;
	String authToken = null;
	String cookie = null;

	private Account getAccount() {
		String accountName = getAccountName();
		if("".equals(accountName))
			return null;
		return new Account(accountName, "com.google");
	}

	public String getAccountName() {
		SharedPreferences prefs = this.getSharedPreferences("account", Activity.MODE_PRIVATE);
		String accountName = prefs.getString("accountName", "");
		return accountName;
	}

    interface Retry {
        void run(String authToken);
    }

    private class GetAuthTokenCallback implements AccountManagerCallback<Bundle> {
        boolean isSecond;

        Retry onRetry;

        GetAuthTokenCallback(boolean isSecond, Retry onRetry) {
            this.isSecond = isSecond;
            this.onRetry = onRetry;
        }

        public void run(AccountManagerFuture<Bundle> result) {
            Bundle bundle;
            try {
                bundle = result.getResult();
                Intent intent = (Intent)bundle.get(AccountManager.KEY_INTENT);
                if(intent != null) {
                    // User input required
                    pendingAuth = true;
                    startActivity(intent);
                } else {
                    authToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                    sync.requestGetCookie(authToken, new Sync.OnCookieErrorListener() {
                        @Override
                        public void onError(String msg) {
                            if(!isSecond) {
                                showMessage("retry with invalidate authToken.");
                                onRetry.run(authToken);
                                return;
                            }
                            showMessage("get cookie fail.");
                            return;

                        }
                    });
                }
            } catch (OperationCanceledException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (AuthenticatorException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private void startRetrieveCookie(Context context, final Account account) {
        AccountManager accountManager = AccountManager.get(context);
        accountManager.getAuthToken (account, "ah", null, false, new GetAuthTokenCallback(false, new Retry(){
            public void run(String authToken) {
                invalidateAuthTokenAndRetry(account, authToken);
            }}), null);
    }

    private void invalidateAuthTokenAndRetry(Account account, String authToken) {
        AccountManager accountManager = AccountManager.get(this);
        accountManager.invalidateAuthToken(account.type, authToken);
        accountManager.getAuthToken (account, "ah", null, false, new GetAuthTokenCallback(true, null), null);
    }


    @Override
    protected void onResume() {
            super.onResume();
            if(pendingAuth)
	        {
	            Account account = getAccount();
	            // before start SetupActivity, onResume is called. This time account is still null.
	            if(account != null) {
		            pendingAuth = false;
	            	startRetrieveCookie(this, account);
	            }
	       } else {
                refresh();
            }
    }

    
    
}
