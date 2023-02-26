package com.example.moneygestor.database;

import org.json.JSONArray;

import java.util.List;

public interface Database {
    JSONArray getJSON(String sql, Object... values);
    boolean isConnected();
    void executeQuery(List<String> sqls);
}
