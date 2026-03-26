package com.shashank.expensemanager.transactionDb;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "personalityTable")
public class PersonalityEntry {

    @PrimaryKey
    private int id = 1; // Singleton
    
    private String badges;

    public PersonalityEntry(String badges) {
        this.badges = badges;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getBadges() { return badges; }
    public void setBadges(String badges) { this.badges = badges; }
}
