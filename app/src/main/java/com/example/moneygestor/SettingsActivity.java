package com.example.moneygestor;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        setSupportActionBar(findViewById(R.id.settings_toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.settings);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            getPreferenceScreen().findPreference("server_ip").setOnPreferenceChangeListener((preference, newValue) -> {
                if(newValue.equals(""))
                    return true;

                if(newValue.toString().matches("^(?:\\d{1,3}\\.){3}\\d{1,3}$"))
                    return true;

                showAlertDialog(R.string.server_ip_not_valid_message);

                return false;
            });

            getPreferenceScreen().findPreference("server_port").setOnPreferenceChangeListener((preference, newValue) -> {
                if(newValue.equals(""))
                    return true;

                try {
                    int value = Integer.parseInt(newValue.toString());

                    if(value <= 0 || value >= 65535)
                        throw new IllegalArgumentException("value is too high or low");
                } catch (IllegalArgumentException ignored) {
                    showAlertDialog(R.string.server_port_not_valid_message);
                    return false;
                }

                return true;
            });
        }

        private void showAlertDialog(int message) {
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.attention)
                    .setMessage(message)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        finish();
        return true;
    }
}