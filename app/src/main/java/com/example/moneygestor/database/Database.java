package com.example.moneygestor.database;

import org.json.JSONArray;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.SimpleTimeZone;

public interface Database {
    int NOT_GENERATED_KEY = -1;
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    JSONArray getJSON(String sql, Object... values);
    boolean isConnected();
    void executeQuery(List<String> sqls);
    int insertQuery(String sql, Object... values) throws SQLException;
}
