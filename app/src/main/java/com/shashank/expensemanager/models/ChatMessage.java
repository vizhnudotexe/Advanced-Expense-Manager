package com.shashank.expensemanager.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "chatMessageTable")
public class ChatMessage {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private String text;
    private boolean isUser;

    public ChatMessage(String text, boolean isUser) {
        this.text = text;
        this.isUser = isUser;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public boolean isUser() { return isUser; }
    public void setUser(boolean user) { isUser = user; }
}
