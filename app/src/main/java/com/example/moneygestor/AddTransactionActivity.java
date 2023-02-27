package com.example.moneygestor;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.moneygestor.data.Genders;
import com.example.moneygestor.data.KeyValue;
import com.example.moneygestor.data.Transaction;
import com.example.moneygestor.data.User;
import com.example.moneygestor.data.Wallets;
import com.example.moneygestor.database.RemoteDB;
import com.example.moneygestor.database.SynchronizeServer;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

public class AddTransactionActivity extends AppCompatActivity implements LoadingInterface {

    EditText editName, editValue;
    TextView viewDate;
    Spinner spiWallet, spiUser, spiGender;
    Button btnSave, btnCancel;

    private Transaction transaction;
    private DateFormat dateFormat;
    private Calendar newDate;

    ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transactio);

        editName = findViewById(R.id.transaction_editText_name);
        editValue = findViewById(R.id.transaction_editText_value);
        viewDate = findViewById(R.id.transaction_textView_date);
        spiWallet = findViewById(R.id.transaction_spinner_wallet);
        spiUser = findViewById(R.id.transaction_spinner_user);
        spiGender = findViewById(R.id.transaction_spinner_gender);

        btnSave = findViewById(R.id.transaction_button_save);
        btnCancel = findViewById(R.id.transaction_button_cancel);

        progress = findViewById(R.id.transaction_progress);

        transaction = new Transaction();

        dateFormat = new SimpleDateFormat(getString(R.string.date_format));

        viewDate.setText(dateFormat.format(transaction.getDate().getTime()));

        try {
            ArrayAdapter<KeyValue<Integer, String>> genderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, KeyValue.toList((Map<Integer, String>) getIntent().getSerializableExtra("genders")));
            genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spiGender.setAdapter(genderAdapter);

            ArrayAdapter<KeyValue<Integer, String>> walletAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, KeyValue.toList((Map<Integer, String>) getIntent().getSerializableExtra("wallets")));
            walletAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spiWallet.setAdapter(walletAdapter);

            ArrayAdapter<User> userAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, (List<User>) getIntent().getSerializableExtra("users"));
            userAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spiUser.setAdapter(userAdapter);

        } catch (ClassCastException ex) {
            new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.error_general))
                    .setOnDismissListener(dialogInterface -> {
                        finish();
                    })
                    .show();

            ex.printStackTrace();
        }

        btnSave.setOnClickListener(view -> {
            if(editName.getText().toString().isEmpty() || editValue.getText().toString().isEmpty()) {
                new AlertDialog.Builder(this)
                        .setMessage(R.string.transaction_insert_all_data)
                        .show();

                return;
            }

            try {
                transaction.setDescription(editName.getText().toString().trim());
                transaction.setValue(Double.parseDouble(editValue.getText().toString()));
                transaction.setWallet(((KeyValue<Integer, String>) spiWallet.getSelectedItem()).getKey());
                transaction.setUser(((User) spiUser.getSelectedItem()));
                transaction.setGender(((KeyValue<Integer, String>) spiGender.getSelectedItem()).getKey());
            } catch (ClassCastException | NumberFormatException ignored) {
                new AlertDialog.Builder(this)
                        .setMessage(R.string.error_general)
                        .show();
            }

            new SaveTransactionThread(transaction, this, this).start();

            System.out.println(transaction);
        });

        viewDate.setOnClickListener(view -> {
            if(getCurrentFocus() != null) getCurrentFocus().clearFocus();

            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

            Calendar date = transaction.getDate();
            DatePickerDialog dateDialog = new DatePickerDialog(this);
            dateDialog.updateDate(date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH));
            dateDialog.setOnDateSetListener((datePicker, year, month, day) -> newDate = new GregorianCalendar(year, month, day));
            dateDialog.setOnCancelListener(dialogInterface -> newDate = null);
            dateDialog.setOnDismissListener(dialogInterface -> {
                if(newDate != null) {
                    transaction.setDate(newDate);
                    viewDate.setText(dateFormat.format(newDate.getTime()));
                }

                });
            dateDialog.show();
        });

        btnCancel.setOnClickListener(view -> finish());
    }

    @Override
    public void show(boolean indeterminate) {
        runOnUiThread(() -> {
            progress.setIndeterminate(indeterminate);
            progress.setVisibility(View.VISIBLE);

            btnSave.setEnabled(false);
        });
    }

    @Override
    public void hide() {
        runOnUiThread(() -> {
            progress.setVisibility(View.GONE);

            btnSave.setEnabled(true);
        });
    }

    @Override
    public void setMaximus(int maximus) {
        runOnUiThread(() -> {
            progress.setIndeterminate(false);
            progress.setMax(maximus);

            reset();
        });
    }

    @Override
    public boolean increment(int value) {
        int actualValue = progress.getProgress();
        if(actualValue + value > progress.getMax())
            return false;

        progress.setProgress(actualValue + value);
        return true;
    }

    @Override
    public void reset() {
        progress.setProgress(0);
    }
}