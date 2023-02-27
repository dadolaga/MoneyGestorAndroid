package com.example.moneygestor.data;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class Transaction implements Serializable {
    String description;
    double value;
    Calendar date;
    User user;
    int wallet, gender;

    public Transaction() {
        this(null, 0.0, new GregorianCalendar(), null, 0, 0);
    }
    private Transaction(String description, double value, Calendar date, User user, int wallet, int gender) {
        this.description = description;
        this.value = value;
        this.date = date;
        this.user = user;
        this.wallet = wallet;
        this.gender = gender;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public Calendar getDate() {
        return date;
    }

    public void setDate(Calendar date) {
        this.date = date;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public int getWallet() {
        return wallet;
    }

    public void setWallet(int wallet) {
        this.wallet = wallet;
    }

    public int getGender() {
        return gender;
    }

    public void setGender(int gender) {
        this.gender = gender;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "description='" + description + '\'' +
                ", value=" + value +
                ", date=" + date +
                ", user=" + user +
                ", wallet=" + wallet +
                ", gender=" + gender +
                '}';
    }
}
