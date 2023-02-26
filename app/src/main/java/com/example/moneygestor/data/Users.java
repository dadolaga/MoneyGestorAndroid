package com.example.moneygestor.data;

import androidx.annotation.NonNull;

import com.example.moneygestor.database.LocalDB;
import com.example.moneygestor.database.RemoteDB;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

public class Users extends LinkedList<User> {

    public Users() {
        super();
    }

    public void loadFromLocalDatabase(LocalDB database) {
        if(!database.isConnected())
            return;

        JSONArray users = database.getJSON("SELECT * FROM user");

        try {
            for (int i = 0; i < users.length(); i++) {
                JSONObject gender = users.getJSONObject(i);

                add(new User(gender.getInt("id"), gender.getString("Name"), gender.getString("Surname")));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void saveGenderInLocalDB(LocalDB local) {
        local.executeSingleQuery("DELETE FROM user");

        LinkedList<String> sqlList = new LinkedList<>();

        for(User user : this) {
            sqlList.add("INSERT INTO user VALUES (" + user.getId() + ", '" + user.getName() + "', '" + user.getSurname() + "')");
        }

        local.executeQuery(sqlList);
    }

    public boolean checkDifferenceWithRemote(RemoteDB database) {
        JSONArray users = database.getJSON("SELECT * FROM user");

        List<Integer> listKey = getListOfAllId();
        boolean edit = false;
        try {
            for (int i = 0; i < users.length(); i++) {
                JSONObject user = users.getJSONObject(i);

                User serverUser = new User(user.getInt("id"), user.getString("Name"), user.getString("Surname"));
                User localUser = searchById(serverUser.getId());

                if(localUser != null) {
                    if(!serverUser.getFullName().equals(localUser.getFullName())) {
                        remove(localUser);
                        add(serverUser);
                        edit = true;
                    }

                    listKey.remove((Object) serverUser.getId());
                } else {
                    add(serverUser);
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

    private List<Integer> getListOfAllId() {
        List<Integer> list = new LinkedList<>();

        for(User user : this) {
            list.add(user.getId());
        }

        return list;
    }

    private User searchById(int id) {
        for(User user : this)
            if(user.getId() == id)
                return user;

        return null;
    }

    private boolean containId(int id) {
        return searchById(id) != null;
    }

    @NonNull
    @Override
    public String toString() {
        return super.toString();
    }
}
