package com.example.moneygestor;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import com.example.moneygestor.data.Transaction;
import com.example.moneygestor.database.Database;
import com.example.moneygestor.database.SynchronizeServer;

import java.sql.SQLException;

public class SaveTransactionThread extends Thread {
    private Transaction transaction;
    private Context context;
    private LoadingInterface loading;

    public SaveTransactionThread(Transaction transaction, Context context, LoadingInterface loading) {
        this.transaction = transaction;
        this.context = context;
        this.loading = loading;
    }

    @Override
    public void run() {
        loading.show();

        Database database = SynchronizeServer.getDatabase();
        try {
            if(database != null)
                System.out.println(database.insertQuery("INSERT INTO transition(Description, Value, Date, Wallet, UserTransaction, UserAdded, Gender) VALUES (?,?,?,?,?,?,?)",
                        transaction.getDescription(),
                        transaction.getValue(),
                        Database.dateFormat.format(transaction.getDate().getTime()),
                        transaction.getWallet(),
                        transaction.getUser().getId(),
                        SynchronizeServer.getUserLogged(),
                        transaction.getGender()));
        } catch (SQLException e) {
            new AlertDialog.Builder(context)
                    .setMessage(R.string.error_general)
                    .show();

            e.printStackTrace();
        }

        loading.hide();
    }
}
