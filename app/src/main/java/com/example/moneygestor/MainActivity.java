package com.example.moneygestor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.moneygestor.data.Genders;
import com.example.moneygestor.data.Users;
import com.example.moneygestor.data.Wallets;
import com.example.moneygestor.database.LocalDB;
import com.example.moneygestor.database.RemoteDB;
import com.example.moneygestor.database.SynchronizeServer;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private SharedPreferences preferences;
    private RemoteDB remoteDB;
    private LocalDB localDB;
    private ProgressBar progress;
    private MenuItem connectToServerIcon, userLoggedIcon;
    private final Genders genders = new Genders();
    private final Wallets wallets = new Wallets();
    private final Users users = new Users();

    private FloatingActionButton fabAddTransaction, fabAddMoney, fabRemoveMoney, fabSwitchMoney;
    private Animation rotateOpen, rotateClose, fromBottom, toBottom;
    boolean fabClicked = false;
    private int showCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LinearLayout scrollView = findViewById(R.id.listTransaction);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.main_activity_menu);
        setSupportActionBar(toolbar);

        progress = findViewById(R.id.main_progress);

        rotateOpen = AnimationUtils.loadAnimation(this, R.anim.rotate_open_anim);
        rotateClose = AnimationUtils.loadAnimation(this, R.anim.rotate_close_anim);
        fromBottom = AnimationUtils.loadAnimation(this, R.anim.from_bottom_anim);
        toBottom = AnimationUtils.loadAnimation(this, R.anim.to_bottom_anim);

        fabAddMoney = findViewById(R.id.fab_money_add);
        fabSwitchMoney = findViewById(R.id.fab_money_switch);
        fabRemoveMoney = findViewById(R.id.fab_money_remove);

        fabAddTransaction = findViewById(R.id.fab);

        preferences = getSharedPreferences("com.example.moneygestor_preferences", MODE_PRIVATE);
        System.out.println(preferences.getString("server_ip", null));

        fabAddTransaction.setOnClickListener(view -> {
            setVisibilityMiniFab(!fabClicked);
            startAnimationFab(!fabClicked);
            fabClicked = !fabClicked;

            //scrollView.addView(new ListTransactionView(this, 0));
        });

        fabAddMoney.setOnClickListener(view -> {
            new Thread(() -> {
                runOnUiThread(this::showLoadingIndeterminate);

                SynchronizeServer.synchronize(genders, wallets, users);

                runOnUiThread(this::hideLoadingIndeterminate);

                Intent intent = new Intent(this, AddTransactionActivity.class);
                intent.putExtra("genders", genders);
                intent.putExtra("wallets", wallets);
                intent.putExtra("users", users);
                startActivity(intent);
            }).start();
        });

        try {
            remoteDB = new RemoteDB(preferences.getString("server_ip", null), Integer.parseInt(preferences.getString("server_port", "-1")));
        } catch (NumberFormatException ignored) {
            remoteDB = new RemoteDB(preferences.getString("server_ip", null), RemoteDB.INVALID_PORT);
        }
        remoteDB.loadUserFromPreferences(preferences);

        localDB = new LocalDB(this);
        SynchronizeServer.setServer(remoteDB, localDB);

        new Thread(() -> {
            runOnUiThread(this::showLoadingIndeterminate);

            genders.loadFromLocalDatabase(localDB);
            wallets.loadFromLocalDatabase(localDB);
            users.loadFromLocalDatabase(localDB);

            tryToConnectToServer();

            SynchronizeServer.synchronize(genders, wallets, users);

            System.out.println("Users: " + users);

            runOnUiThread(this::hideLoadingIndeterminate);
        }).start();

        // Database test
        /*try (DbHelper dbHelper = new DbHelper(getApplicationContext())) {
            // insert
            SQLiteDatabase database = dbHelper.getWritableDatabase();

            ContentValues value = new ContentValues();
            value.put("title", "Prova");
            value.put("subtitle", "Prova");


            long id = database.insert("entry", null, value);
            System.out.println("Insert row: " + id);

            // read
            SQLiteDatabase database = dbHelper.getReadableDatabase();
            Cursor cursor = database.rawQuery("SELECT * FROM entry", null);
            while(cursor.moveToNext())
                System.out.println(cursor.getLong(0));

            cursor.close();
        }*/
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            remoteDB.changeIpAndPort(preferences.getString("server_ip", null), Integer.parseInt(preferences.getString("server_port", "-1")));
        } catch (NumberFormatException ignored) {
            remoteDB.changeIpAndPort(preferences.getString("server_ip", null), RemoteDB.INVALID_PORT);
        }

        if(connectToServerIcon != null) connectToServerIcon.setIcon(remoteDB.isConnected()? R.drawable.server : R.drawable.server_no_connect);
        if(userLoggedIcon != null) userLoggedIcon.setIcon(remoteDB.isUserLogged()? R.drawable.login : R.drawable.no_login);
        fabAddTransaction.setEnabled(remoteDB.isUserLogged());

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_menu, menu);
        System.out.println("createOptionMenu");

        connectToServerIcon = menu.findItem(R.id.server_connected);
        connectToServerIcon.setIcon(remoteDB.isConnected()? R.drawable.server : R.drawable.server_no_connect);

        userLoggedIcon = menu.findItem(R.id.login);
        userLoggedIcon.setIcon(remoteDB.isUserLogged()? R.drawable.login : R.drawable.no_login);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.change_ip:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.server_connected:
                if(remoteDB.isConnected())
                    break;

                new Thread(this::tryToConnectToServer).start();
                break;
            case R.id.login:
                new Thread(() -> {
                    runOnUiThread(this::showLoadingIndeterminate);

                    String message = getString(R.string.user_not_logged);

                    try {
                        if(remoteDB.isUserLogged()) {
                            remoteDB.checkConnection();
                            if (remoteDB.isConnected()) {
                                JSONArray userInfo = remoteDB.getJSON("SELECT * FROM User WHERE id = ?", remoteDB.getLoggedUserId());
                                if (userInfo.length() == 1) {
                                    JSONObject user = userInfo.getJSONObject(0);
                                    message = user.getString("Surname") + " " + user.getString("Name") + "\n<" + user.getString("Email") + ">";
                                } else
                                    message = getString(R.string.error_general);
                            } else
                                message = getString(R.string.user_logged_but_no_name);
                        }
                    } catch (JSONException e) {
                        message = getString(R.string.error_general);
                    }

                    final String finalMessage = message;
                    runOnUiThread(() -> {
                        hideLoadingIndeterminate();

                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle(R.string.user_info_title)
                                .setMessage(finalMessage)
                                // Add action buttons
                                .setPositiveButton("Reload", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int id) {
                                        Toast.makeText(MainActivity.this, "Coming soon", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.dismiss();
                                    }
                                });
                        builder.show();
                    });
                }).start();
        }
        return true;
    }

    private void tryToConnectToServer() {
        try {
            runOnUiThread(() -> {
                showLoadingIndeterminate();

                if(connectToServerIcon != null) connectToServerIcon.setIcon(R.drawable.server_no_connect);
            });

            remoteDB.connect(progress);
            remoteDB.loadUser(preferences);

        } catch (IllegalArgumentException ex) {
            runOnUiThread(() -> Toast.makeText(this, getText(R.string.server_info_not_valid), Toast.LENGTH_SHORT).show());
        }

        runOnUiThread(() -> {
            hideLoadingIndeterminate();

            if(remoteDB.isConnected())
                if(connectToServerIcon != null) connectToServerIcon.setIcon(R.drawable.server);

            if(userLoggedIcon != null) userLoggedIcon.setIcon(remoteDB.isUserLogged()? R.drawable.login : R.drawable.no_login);
        });
    }

    public void showLoadingIndeterminate() {
        progress.setVisibility(View.VISIBLE);
        progress.setIndeterminate(true);

        fabAddTransaction.setEnabled(false);

        showCount++;
    }

    public void hideLoadingIndeterminate() {
        if(--showCount != 0)
            return;

        progress.setVisibility(View.GONE);

        fabAddTransaction.setEnabled(true);
    }

    private void setVisibilityMiniFab(boolean visibility) {
        fabAddMoney.setVisibility(visibility? View.VISIBLE : View.GONE);
        fabSwitchMoney.setVisibility(visibility? View.VISIBLE : View.GONE);
        fabRemoveMoney.setVisibility(visibility? View.VISIBLE : View.GONE);
    }

    private void startAnimationFab(boolean visibility) {
        fabAddTransaction.startAnimation(visibility? rotateOpen : rotateClose);

        fabAddMoney.startAnimation(visibility? fromBottom : toBottom);
        fabSwitchMoney.startAnimation(visibility? fromBottom : toBottom);
        fabRemoveMoney.startAnimation(visibility? fromBottom : toBottom);
    }
}