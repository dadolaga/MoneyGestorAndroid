package com.example.moneygestor.database;

import com.example.moneygestor.data.Genders;
import com.example.moneygestor.data.Users;
import com.example.moneygestor.data.Wallets;

public class SynchronizeServer {
    private static RemoteDB remote;
    private static LocalDB local;

    public static void setServer(RemoteDB remote, LocalDB local) {
        SynchronizeServer.remote = remote;
        SynchronizeServer.local = local;
    }

    public static Database getDatabase() {
        remote.checkConnection();

        if(remote.isConnected())
            return remote;

        if(local.isConnected())
            return local;

        return null;
    }

    public static boolean isRemote() {
        return getDatabase() instanceof RemoteDB;
    }

    public static void synchronize(Genders genders, Wallets wallets, Users users) {
        if(!isRemote())
            return;

        if(genders.checkDifferenceWithRemote(remote))
            genders.saveGenderInLocalDB(local);

        if(wallets.checkDifferenceWithRemote(remote))
            wallets.saveGenderInLocalDB(local);

        if(users.checkDifferenceWithRemote(remote))
            users.saveGenderInLocalDB(local);
    }
}
