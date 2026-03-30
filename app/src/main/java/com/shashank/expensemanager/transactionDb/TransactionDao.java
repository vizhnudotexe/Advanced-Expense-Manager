package com.shashank.expensemanager.transactionDb;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.shashank.expensemanager.utils.ExpenseList;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Dao
public interface TransactionDao {

    @Query("select * from transactionTable order by date DESC")
    List<TransactionEntry> loadAllTransactionBlocking();

    @Query("select coalesce(sum(case when transactionType='Income' then amount else -amount end),0) from transactionTable where walletType=:wallet")
    int getWalletBalance(String wallet);

    @Query("select * from transactionTable order by date DESC")
    LiveData<List<TransactionEntry>> loadAllTransactions();

    @Query("select * from transactionTable where id = :id")
    LiveData<TransactionEntry> loadExpenseById(int id);

    @Query("select sum(amount) from transactionTable where transactionType =:transactionType")
    int getAmountByTransactionType(String transactionType);

    @Query("select sum(amount) from transactionTable where transactionType =:transactionType and  date between :startDate and :endDate")
    int getAmountbyCustomDates(String transactionType,long startDate,long endDate);

    @Query("select sum(amount) from transactionTable where category=:category")
    int getSumExpenseByCategory(String category);

    @Query("select sum(amount) from transactionTable where category=:category and date between :startDate and :endDate")
    int getSumExpenseByCategoryCustomDate(String category,long startDate, long endDate);

    @Query("SELECT category, SUM(amount) as amount FROM transactionTable WHERE transactionType = 'Expense' AND date BETWEEN :startDate AND :endDate GROUP BY category")
    List<ExpenseList> getSumExpenseByCategoriesCustomDate(long startDate, long endDate);

    @Query("select min(date) from transactionTable ")
    long getFirstDate();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertExpense(TransactionEntry transactionEntry);

    @Delete
    void removeExpense(TransactionEntry transactionEntry);

    @Query("select * from transactionTable where date between :startDate and :endDate")
    List<TransactionEntry> getTransactionsByDateRange(long startDate, long endDate);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void updateExpenseDetails(TransactionEntry transactionEntry);
}
