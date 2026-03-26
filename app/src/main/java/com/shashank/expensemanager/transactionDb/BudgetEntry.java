package com.shashank.expensemanager.transactionDb;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "budgetTable")
public class BudgetEntry {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private String category;
    private int amount;
    private String monthYear; // Format: "MM-yyyy"

    @Ignore
    public BudgetEntry(String category, int amount, String monthYear) {
        this.category = category;
        this.amount = amount;
        this.monthYear = monthYear;
    }

    public BudgetEntry(int id, String category, int amount, String monthYear) {
        this.id = id;
        this.category = category;
        this.amount = amount;
        this.monthYear = monthYear;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getMonthYear() {
        return monthYear;
    }

    public void setMonthYear(String monthYear) {
        this.monthYear = monthYear;
    }
}

