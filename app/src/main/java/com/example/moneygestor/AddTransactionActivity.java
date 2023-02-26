package com.example.moneygestor;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.example.moneygestor.data.Genders;
import com.example.moneygestor.data.KeyValue;
import com.example.moneygestor.data.User;
import com.example.moneygestor.data.Wallets;
import com.example.moneygestor.database.RemoteDB;
import com.example.moneygestor.database.SynchronizeServer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AddTransactionActivity extends AppCompatActivity {

    EditText editName, editValue;
    Spinner spiWallet, spiUser, spiGender;

    Button btnSave, btnCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transactio);

        editName = findViewById(R.id.transaction_editText_name);
        editValue = findViewById(R.id.transaction_editText_value);
        spiWallet = findViewById(R.id.transaction_spinner_wallet);
        spiUser = findViewById(R.id.transaction_spinner_user);
        spiGender = findViewById(R.id.transaction_spinner_gender);

        btnSave = findViewById(R.id.transaction_button_save);
        btnCancel = findViewById(R.id.transaction_button_cancel);

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

            btnSave.setOnClickListener(view -> {
                System.out.println(((KeyValue<Integer, String>) spiWallet.getSelectedItem()).getKey());
            });

        } catch (ClassCastException ex) {
            new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.error_general))
                    .setOnDismissListener(dialogInterface -> {
                        finish();
                    })
                    .show();

            ex.printStackTrace();
        }

        btnCancel.setOnClickListener(view -> finish());
    }
}