package com.shashank.expensemanager.transactionDb;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface BudgetDao {

    @Query("SELECT * FROM budgetTable WHERE monthYear = :monthYear")
    LiveData<List<BudgetEntry>> loadAllBudgets(String monthYear);

    @Query("SELECT * FROM budgetTable WHERE monthYear = :monthYear")
    List<BudgetEntry> loadAllBudgetsSync(String monthYear);

    @Query("SELECT * FROM budgetTable WHERE category = :category AND monthYear = :monthYear")
    BudgetEntry getBudgetByCategory(String category, String monthYear);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertBudget(BudgetEntry budgetEntry);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void updateBudget(BudgetEntry budgetEntry);

    @Delete
    void deleteBudget(BudgetEntry budgetEntry);

    @Query("DELETE FROM budgetTable")
    void deleteAll();
}

