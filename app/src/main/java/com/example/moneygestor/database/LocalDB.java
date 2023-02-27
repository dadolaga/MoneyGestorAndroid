package com.example.moneygestor.database;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import org.apache.commons.lang3.NotImplementedException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.Arrays;
import java.util.List;

public class LocalDB implements Database{
    public static final String DB_NAME = "moneyGestor.db";
    public static final String TABLE_GENDER = "gender";
    public static final String TABLE_WALLET = "wallet";
    public static final String TABLE_USER = "user";
    public static final String TABLE_TRANSACTION = "transition";
    private SQLiteDatabase database;
    private Context context;

    public LocalDB(Context context) {
        this.context = context;
        createDbOrOpen();
    }

    @Override
    public JSONArray getJSON(String sql, Object... values) {
        String[] stringValues = new String[values.length];

        int i = 0;
        for(Object value : values)
            stringValues[i++] = value.toString();

        Cursor cursor;
        JSONArray array;

        try {
            cursor = database.rawQuery(sql, stringValues);

            array = new JSONArray();

            String[] columnNames = cursor.getColumnNames();

            while (cursor.moveToNext()) {
                JSONObject object = new JSONObject();
                for (int j = 0; j < columnNames.length; j++) {
                    object.put(columnNames[j], cursor.getString(j));
                }

                array.put(object);
            }

        } catch (JSONException e) {
            array = null;
            throw new RuntimeException(e);
        }

        if(cursor != null)
            cursor.close();

        return array;
    }

    @Override
    public boolean isConnected() {
        return database != null && database.isOpen();
    }

    @Override
    public void executeQuery(List<String> sqls) {
        database.beginTransaction();
        int i = 0;
        try {
            for (String sql : sqls) {
                executeSingleQuery(sql);
                i++;
            }

            database.setTransactionSuccessful();
        } catch (SQLException ex) {
            System.err.println("Query " + i + ": " + ex.getMessage());
            ex.printStackTrace();
        }
        database.endTransaction();
    }

    @Override
    public int insertQuery(String sql, Object... values) throws java.sql.SQLException {
        String[] stringValues = new String[values.length];

        int i = 0;
        for(Object value : values)
            stringValues[i++] = value.toString();

        database.execSQL(sql, stringValues);

        return NOT_GENERATED_KEY;
    }

    public void executeSingleQuery(String sql) {
        database.execSQL(sql);
    }

    public void dropDatabase() {
        context.deleteDatabase(DB_NAME);
        database = null;
    }

    public void createTable() {
        createGenderTable();
        createWalletTable();
        createUserTable();
        createTransactionTable();
    }

    private void createGenderTable() {
        database.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_GENDER + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "Name VARCHAR(63) NOT NULL)");
    }

    private void createWalletTable() {
        database.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_WALLET + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "Name VARCHAR(255) NOT NULL)");
    }

    private void createUserTable() {
        database.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_USER + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "Name VARCHAR(100) NOT NULL," +
                "Surname VARCHAR(100) NOT NULL)");
    }

    private void createTransactionTable() {
        database.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_TRANSACTION + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "Description VARCHAR(255) DEFAULT NULL," +
                "Value DECIMAL(65,2) NOT NULL," +
                "Date DATE NOT NULL, " +
                "Wallet INTEGR NOT NULL," +
                "UserTransaction INTEGR NOT NULL," +
                "UserAdded INTEGR NOT NULL," +
                "Gender INTEGR NOT NULL," +
                "ExchangeDestination INTEGR DEFAULT NULL," +
                "FOREIGN KEY (Wallet) REFERENCES wallet(id) ON DELETE CASCADE ON UPDATE CASCADE," +
                "FOREIGN KEY (UserTransaction) REFERENCES user(id) ON DELETE RESTRICT ON UPDATE RESTRICT," +
                "FOREIGN KEY (UserAdded) REFERENCES user(id) ON DELETE RESTRICT ON UPDATE RESTRICT," +
                "FOREIGN KEY (Gender) REFERENCES gender(id) ON DELETE RESTRICT ON UPDATE RESTRICT)");
    }

    public void createDbOrOpen() {
        boolean databaseIsCreated = !context.getDatabasePath(DB_NAME).exists();
        database = context.openOrCreateDatabase(DB_NAME, Context.MODE_PRIVATE, null);

        if(databaseIsCreated)
            createTable();
    }

    public boolean isOpen() {
        return database.isOpen();
    }
}
