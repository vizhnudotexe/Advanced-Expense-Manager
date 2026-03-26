package com.shashank.expensemanager.transactionDb;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

import java.util.Date;

@Entity(tableName = "transactionTable")
public class TransactionEntry {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private int amount;
    private String category;
    private String description;
    private Date date;
    private String transactionType;

    @NonNull
    @ColumnInfo(name = "walletType")
    private String walletType;

    @ColumnInfo(name = "isRecurring")
    private boolean isRecurring;

    @NonNull
    @ColumnInfo(name = "recurrenceType")
    private String recurrenceType;

    // Full constructor (Room uses this)
    public TransactionEntry(int id, int amount, String category, String description,
                             Date date, String transactionType, @NonNull String walletType,
                             boolean isRecurring, @NonNull String recurrenceType) {
        this.id = id;
        this.amount = amount;
        this.category = category;
        this.description = description;
        this.date = date;
        this.transactionType = transactionType;
        this.walletType = walletType;
        this.isRecurring = isRecurring;
        this.recurrenceType = recurrenceType;
    }

    // Short constructor for add (no recurring) - @Ignore so Room uses full one
    @Ignore
    public TransactionEntry(int amount, String category, String description,
                             Date date, String transactionType) {
        this.amount = amount;
        this.category = category;
        this.description = description;
        this.date = date;
        this.transactionType = transactionType;
        this.walletType = "Cash";
        this.isRecurring = false;
        this.recurrenceType = "";
    }

    // Convenience constructor with wallet
    @Ignore
    public TransactionEntry(int amount, String category, String description,
                             Date date, String transactionType, @NonNull String walletType,
                             boolean isRecurring, @NonNull String recurrenceType) {
        this.amount = amount;
        this.category = category;
        this.description = description;
        this.date = date;
        this.transactionType = transactionType;
        this.walletType = walletType;
        this.isRecurring = isRecurring;
        this.recurrenceType = recurrenceType;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    @NonNull
    public String getWalletType() { return walletType; }
    public void setWalletType(@NonNull String walletType) { this.walletType = walletType; }

    public boolean isRecurring() { return isRecurring; }
    public void setRecurring(boolean recurring) { isRecurring = recurring; }

    @NonNull
    public String getRecurrenceType() { return recurrenceType; }
    public void setRecurrenceType(@NonNull String recurrenceType) { this.recurrenceType = recurrenceType; }
}
