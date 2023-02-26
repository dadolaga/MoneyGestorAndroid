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

    /*public JSONObject getDatabaseStructure() {
        final String DATABASE_NAME = "moneygestor";
        JSONObject json = new JSONObject();

        try {
            // table
            int columRemaining = 0;
            JSONArray tables = new JSONArray();

            try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ?")) {
                statement.setString(1, DATABASE_NAME);
                ResultSet rs = statement.executeQuery();

                rs.next();
                columRemaining = rs.getInt(1);
            }

            List<String> tablesInsert = new ArrayList<>();

            while(columRemaining > 0) {
                try (PreparedStatement statement = connection.prepareStatement("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES T WHERE TABLE_SCHEMA = ? AND NOT EXISTS (SELECT DISTINCT REFERENCED_TABLE_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE WHERE REFERENCED_TABLE_SCHEMA IS NOT NULL AND TABLE_NAME = T.TABLE_NAME " + getSQLTableInsert(tablesInsert) + ") ORDER BY TABLE_NAME")) {
                    statement.setString(1, DATABASE_NAME);

                    ResultSet resultSet = statement.executeQuery();

                    boolean insert = false;

                    while (resultSet.next()) {
                        String tableName = resultSet.getString(1);
                        if (tablesInsert.contains(tableName))
                            continue;

                        insert = true;
                        columRemaining--;
            
                        JSONObject objectTable = new JSONObject();
                        objectTable.put("name", tableName);
                        objectTable.put("attributes", getJsonColumns(DATABASE_NAME, tableName));
                        objectTable.put("primaryKey", getJsonPrimaryKey(DATABASE_NAME, tableName));
                        objectTable.put("foreignKey", getJsonForeignKey(DATABASE_NAME, tableName));
            
                        tables.put(objectTable);

                        tablesInsert.add(tableName);
                    }

                    if (!insert)
                        throw new SQLWarning("Database not insertable, check relation between table");
                }

                json.put("tables", tables);
            }

            json.put("triggers", getJsonTriggers(DATABASE_NAME));
        } catch (SQLException | JSONException exception) {
            exception.printStackTrace();
        }

        return json;
    }*/

    private String getSQLTableInsert(List<String> tables) {
        if(tables.size() == 0)
            return "";

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < tables.size(); i++) {
            builder.append(" EXCEPT ")
                    .append("SELECT ")
                    .append('"').append(tables.get(i)).append('"');
        }

        return builder.toString();
    }

    private JSONArray getJsonColumns(String databaseName, String tableName) throws SQLException, JSONException {
        JSONArray arrayAttributes = null;
        try (PreparedStatement rowStatement = connection.prepareStatement("SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION")) {
            rowStatement.setString(1, databaseName);
            rowStatement.setString(2, tableName);

            arrayAttributes = new JSONArray();

            ResultSet resultSet = rowStatement.executeQuery();
            while (resultSet.next()) {
                JSONObject objectAttribute = new JSONObject();

                objectAttribute.put("name", resultSet.getString("COLUMN_NAME"));
                objectAttribute.put("nullable", resultSet.getString("IS_NULLABLE").equals("YES"));
                objectAttribute.put("type", resultSet.getString("COLUMN_TYPE"));
                objectAttribute.put("autoIncrement", resultSet.getString("EXTRA").equals("auto_increment"));
                objectAttribute.put("default", resultSet.getString("COLUMN_DEFAULT"));

                arrayAttributes.put(objectAttribute);
            }
        }

        return arrayAttributes;
    }

    private JSONArray getJsonPrimaryKey(String databaseName, String tableName) throws SQLException, JSONException {
        JSONArray primaryKeys;
        try (PreparedStatement rowStatement = connection.prepareStatement("SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_KEY = 'PRI' ORDER BY ORDINAL_POSITION")) {
            rowStatement.setString(1, databaseName);
            rowStatement.setString(2, tableName);

            primaryKeys = new JSONArray();

            ResultSet resultSet = rowStatement.executeQuery();
            while (resultSet.next()) {
                primaryKeys.put(resultSet.getString("COLUMN_NAME"));
            }
        }

        return primaryKeys;
    }

    private JSONArray getJsonForeignKey(String databaseName, String tableName) throws SQLException, JSONException {
        JSONArray foreignKeys;
        try (PreparedStatement rowStatement = connection.prepareStatement("SELECT * FROM information_schema.KEY_COLUMN_USAGE KCU INNER JOIN information_schema.REFERENTIAL_CONSTRAINTS RC ON KCU.CONSTRAINT_NAME = RC.CONSTRAINT_NAME WHERE KCU.CONSTRAINT_SCHEMA = ? AND KCU.TABLE_NAME = ?")) {
            rowStatement.setString(1, databaseName);
            rowStatement.setString(2, tableName);

            foreignKeys = new JSONArray();

            ResultSet resultSet = rowStatement.executeQuery();
            while (resultSet.next()) {
                JSONObject foreignKeyObject = new JSONObject();

                foreignKeyObject.put("name", resultSet.getString("CONSTRAINT_NAME"));
                foreignKeyObject.put("fromTable", resultSet.getString("KCU.TABLE_NAME"));
                foreignKeyObject.put("fromColumn", resultSet.getString("COLUMN_NAME"));
                foreignKeyObject.put("toTable", resultSet.getString("REFERENCED_TABLE_NAME"));
                foreignKeyObject.put("toColumn", resultSet.getString("REFERENCED_COLUMN_NAME"));
                foreignKeyObject.put("onDelete", resultSet.getString("DELETE_RULE"));
                foreignKeyObject.put("onUpdate", resultSet.getString("UPDATE_RULE"));

                foreignKeys.put(foreignKeyObject);
            }
        }

        return foreignKeys;
    }

    private JSONArray getJsonTriggers(String databaseName) throws SQLException, JSONException {
        JSONArray triggers;
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA = ? ORDER BY ACTION_ORDER")) {
            preparedStatement.setString(1, databaseName);

            ResultSet resultSet = preparedStatement.executeQuery();

            triggers = new JSONArray();

            while (resultSet.next()) {
                JSONObject trigger = new JSONObject();

                trigger.put("name", resultSet.getString("TRIGGER_NAME"));
                trigger.put("event", resultSet.getString("EVENT_MANIPULATION"));
                trigger.put("timing", resultSet.getString("ACTION_TIMING"));
                trigger.put("orientation", resultSet.getString("ACTION_ORIENTATION"));
                trigger.put("statement", resultSet.getString("ACTION_STATEMENT"));

                triggers.put(trigger);
            }
        }

        return triggers;
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
