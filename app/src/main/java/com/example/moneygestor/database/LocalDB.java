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

    public void createDatabaseFromJSON(JSONObject json) {
        try {
            JSONArray tables = json.getJSONArray("tables");

            for(int i=0; i<tables.length(); i++) {
                database.execSQL(generateSQLTableFromJson(tables.getJSONObject(i)));
            }

            JSONArray trigger = json.getJSONArray("triggers");

            for(int i=0; i<trigger.length(); i++) {
                System.out.println(trigger);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String generateSQLTableFromJson(JSONObject table) throws JSONException {
        StringBuilder builder = new StringBuilder();
        boolean isAutoIncrement = false;

        builder.append("CREATE TABLE IF NOT EXISTS ")
                .append(table.getString("name"))
                .append("(");

        JSONArray attributes = table.getJSONArray("attributes");
        for(int i=0; i<attributes.length(); i++) {
            JSONObject attribute = attributes.getJSONObject(i);

            if(attribute.getBoolean("autoIncrement")) {
                builder.append(attribute.getString("name"))
                        .append(" INTEGER PRIMARY KEY AUTOINCREMENT");

                isAutoIncrement = true;
            } else {
                builder.append(attribute.getString("name"))
                        .append(" ")
                        .append(attribute.getString("type")).append(" ");

                if (!attribute.getBoolean("nullable"))
                    builder.append("NOT NULL").append(" ");

                try {
                    String defaultValue = attribute.getString("default");

                    builder.append("DEFAULT \"")
                            .append(defaultValue).append("\" ");
                } catch (JSONException ignored) {
                }
            }

            builder.append(i < attributes.length()-1? "," : "");
        }

        if(!isAutoIncrement) {
            builder.append(" PRIMARY KEY (");
            for (int i = 0; i < table.getJSONArray("primaryKey").length(); i++)
                builder.append(table.getJSONArray("primaryKey").getString(i)).append(i < table.getJSONArray("primaryKey").length() - 1 ? "," : "");
            builder.append(")");
        }

        for (int i = 0; i <table.getJSONArray("foreignKey").length(); i++) {
            JSONObject foreignKey = table.getJSONArray("foreignKey").getJSONObject(i);

            builder.append(", FOREIGN KEY (")
                    .append(foreignKey.getString("fromColumn")).append(") ")
                    .append("REFERENCES ").append(foreignKey.getString("toTable")).append("(").append(foreignKey.getString("toColumn")).append(")")
                    .append(" ON DELETE ").append(foreignKey.getString("onDelete"))
                    .append(" ON UPDATE ").append(foreignKey.getString("onUpdate"));
        }

        return builder.append(")").toString();
    }

    private String generateSQLForeignKeyFromJson(JSONObject json) throws JSONException {
        StringBuilder builder = new StringBuilder();

        builder.append("ALTER TABLE ")
                .append(json.getString("fromTable"))
                .append(" ADD ")
                .append(json.getString("fromColumn"))
                .append(" CONSTRAINT ")
                .append(json.getString("name"))
                .append(" REFERENCES ")
                .append(json.getString("toTable"))
                .append("(").append(json.getString("toColumn")).append(")")
                .append(" ON DELETE ").append(json.getString("onDelete"))
                .append(" ON UPDATE ").append(json.getString("onUpdate"));

        return builder.toString();
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
