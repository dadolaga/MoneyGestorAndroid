package com.example.moneygestor.database;

import android.content.SharedPreferences;
import android.view.MenuItem;
import android.widget.ProgressBar;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;

public class RemoteDB implements Database {
    public enum State {
        NOT_CONNECTED, CONNECTED, CONNECTING
    }

    public static final int INVALID_PORT = -1;
    public static final int USER_NOT_LOGGED = 0;
    public final String DB_NAME = "moneygestor";
    private Connection connection;
    private String ipAddress;
    private int port;
    private final String username, password;
    private State state;
    private MenuItem icon;
    private int loggedUserId;

    public RemoteDB(String ipAddress) {
        this(ipAddress, 3306, "remote", "N36O4j!A");
    }
    public RemoteDB(String ipAddress, int port) {
        this(ipAddress, port, "remote", "N36O4j!A");
    }
    public RemoteDB(String ipAddress, int port, String user, String password) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.username = user;
        this.password = password;

        icon = null;
        state = State.NOT_CONNECTED;
        loggedUserId = USER_NOT_LOGGED;
    }

    @Override
    public void executeQuery(List<String> sqls) {
        throw new NotImplementedException();
    }

    @Override
    public int insertQuery(String sql, Object... values) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

        int i = 1;
        for(Object value : values)
            statement.setObject(i++, value);

        statement.executeUpdate();

        ResultSet rsKey = statement.getGeneratedKeys();
        if(rsKey.next())
            return rsKey.getInt(1);

        return NOT_GENERATED_KEY;
    }

    public void setIcon(MenuItem icon) {
        this.icon = icon;
    }

    public void setLoggedUserId(int loggedUserId) {
        this.loggedUserId = loggedUserId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public State getState() {
        return state;
    }

    public void connect() {
        connect(null);
    }

    public void disconnect() {
        Thread disconnect = new Thread(() -> {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } catch (NullPointerException ignored) { }

            connection = null;
            state = State.NOT_CONNECTED;
        });
        disconnect.start();

        try {
            disconnect.join();
        } catch (InterruptedException ignored) { }
    }

    public int getLoggedUserId() {
        return loggedUserId;
    }

    public boolean isUserLogged() {
        return loggedUserId != USER_NOT_LOGGED;
    }

    public void connect(ProgressBar loading) {
        if(ipAddress == null || port == INVALID_PORT)
            throw new IllegalArgumentException("ip and/or port aren't valid");

        try {
            state = State.CONNECTING;
            DriverManager.setLoginTimeout(5);
            connection = DriverManager.getConnection("jdbc:mariadb://" + ipAddress + ":" + port + "/" + DB_NAME + "?user=" + username + "&password=" + password);
            state = State.CONNECTED;
        } catch (SQLException e) {
            connection = null;
            state = State.NOT_CONNECTED;

            e.printStackTrace();
        }
    }

    public void changeIp(String newIp) {
        if(Objects.equals(ipAddress, newIp))
            return;

        ipAddress = newIp;
        disconnect();
    }

    public void changePort(int newPort) {
        if(Objects.equals(port, newPort))
            return;

        port = newPort;
        disconnect();
    }

    public void changeIpAndPort(String newIp, int newPort) {
        changeIp(newIp);
        changePort(newPort);
    }

    public boolean isConnected() {
        return state == State.CONNECTED;
    }

    public void loadUserFromPreferences(SharedPreferences preferences) {
        int userId = preferences.getInt("user", USER_NOT_LOGGED);
        if (userId != USER_NOT_LOGGED)
            loggedUserId = userId;
    }

    public void loadUser(SharedPreferences preferences) {
        loadUser(preferences, false);
    }
    public void loadUser(SharedPreferences preferences, boolean reload) {
        if(!reload) {
            loadUserFromPreferences(preferences);
        }

        if(!isConnected())
            return;

        String token = preferences.getString("token", null);
        if(token == null)
            return;

        try {
            JSONArray user = getJSON("SELECT * FROM user WHERE Token = ?", preferences.getString("token", null));

            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt("user", user.getJSONObject(0).getInt("id"));
            editor.apply();

            loggedUserId = user.getJSONObject(0).getInt("id");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void checkConnection() {
        if(connection == null)
            return;

        try {
            if(!connection.isValid(5))
                disconnect();

        } catch (SQLException ignored) {
            disconnect();
        }
    }

    public JSONArray getJSON(String sql, Object... values) {
        checkConnection();

        if(!isConnected())
            return null;

        if(StringUtils.countMatches(sql, "?") != values.length)
            throw new IllegalArgumentException(String.format("Expected %d statement but was received %d", StringUtils.countMatches(sql, "?"), values.length));

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int i=1;
            for(Object obj : values)
                statement.setObject(i++, obj);

            JSONArray array = new JSONArray();

            ResultSet rs = statement.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();

            String[] columnNames = new String[metaData.getColumnCount()];
            for(i=0; i<metaData.getColumnCount(); i++) {
                columnNames[i] = metaData.getColumnName(i+1);
            }

            while (rs.next()) {
                JSONObject object = new JSONObject();
                for(String name : columnNames)
                    object.put(name, rs.getObject(name));

                array.put(object);
            }

            return array;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
