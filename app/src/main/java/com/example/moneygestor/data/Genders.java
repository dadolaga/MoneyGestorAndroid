package com.example.moneygestor.data;

import androidx.annotation.NonNull;

import com.example.moneygestor.database.Database;
import com.example.moneygestor.database.LocalDB;
import com.example.moneygestor.database.RemoteDB;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Genders extends LinkedHashMap<Integer, String> {
    public int EXCHANGE_GENDER = 100;
    public int TIE_GENDER = 101;

    public Genders() {
        super();
    }

    public void loadFromLocalDatabase(LocalDB database) {
        if(!database.isConnected())
            return;

        JSONArray genders = database.getJSON("SELECT * FROM gender");

        try {
            for (int i = 0; i < genders.length(); i++) {
                JSONObject gender = genders.getJSONObject(i);

                put(gender.getInt("id"), gender.getString("Name"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void saveGenderInLocalDB(LocalDB local) {
        local.executeSingleQuery("DELETE FROM gender");

        LinkedList<String> sqlList = new LinkedList<>();

        for(int key : keySet()) {
            sqlList.add("INSERT INTO gender VALUES (" + key + ", '" + get(key) + "')");
        }

        local.executeQuery(sqlList);
    }

    public boolean checkDifferenceWithRemote(RemoteDB database) {
        JSONArray genders = database.getJSON("SELECT * FROM gender WHERE Display = true");

        List<Integer> listKey = new ArrayList<>(keySet());
        boolean edit = false;
        try {
            for (int i = 0; i < genders.length(); i++) {
                JSONObject gender = genders.getJSONObject(i);

                int keyServer = gender.getInt("id");
                String nameServer = gender.getString("Name");

                if(containsKey(keyServer)) {
                    String nameLocal = get(keyServer);
                    if(!nameServer.equals(nameLocal)) {
                        put(keyServer, nameServer);
                        edit = true;
                    }

                    listKey.remove((Object) keyServer);
                } else {
                    put(keyServer, nameServer);
                    edit = true;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        for(int key : listKey) {
            remove(key);
            edit = true;
        }

        return edit;
    }

    @NonNull
    @Override
    public String toString() {
        return super.toString();
    }
}
