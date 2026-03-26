package com.shashank.expensemanager.activities;

import com.shashank.expensemanager.utils.Constants;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import android.content.res.ColorStateList;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.Nullable;
import com.google.android.material.textfield.TextInputEditText;
import androidx.appcompat.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.shashank.expensemanager.R;
import com.shashank.expensemanager.network.BackendSync;
import com.shashank.expensemanager.transactionDb.AppDatabase;
import com.shashank.expensemanager.transactionDb.BudgetEntry;
import com.shashank.expensemanager.transactionDb.TransactionEntry;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BudgetActivity extends AppCompatActivity {

    TextView budgetAmountTextView, budgetMonthTextView, spentAmountTextView, remainingAmountTextView, spentPercentageTextView;
    ProgressBar budgetProgressBar;
    TextInputEditText budgetEditText;
    Button saveBudgetButton;
    Spinner categorySpinner;
    LinearLayout categoryBudgetsContainer;

    AppDatabase appDatabase;
    String currentMonthYear;
    Map<String, Integer> categorySpending = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Monthly Budget");
        }

        budgetAmountTextView = findViewById(R.id.budgetAmountTextView);
        budgetMonthTextView = findViewById(R.id.budgetMonthTextView);
        spentAmountTextView = findViewById(R.id.spentAmountTextView);
        remainingAmountTextView = findViewById(R.id.remainingAmountTextView);
        spentPercentageTextView = findViewById(R.id.spentPercentageTextView);
        budgetProgressBar = findViewById(R.id.budgetProgressBar);
        budgetEditText = findViewById(R.id.budgetEditText);
        saveBudgetButton = findViewById(R.id.saveBudgetButton);
        categorySpinner = findViewById(R.id.categorySpinner);
        categoryBudgetsContainer = findViewById(R.id.categoryBudgetsContainer);

        appDatabase = AppDatabase.getInstance(this);

        Calendar cal = Calendar.getInstance();
        currentMonthYear = new SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(cal.getTime());
        budgetMonthTextView.setText(new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.getTime()));

        setupCategorySpinner();
        
        // Sync budgets from cloud first
        BackendSync.fetchAllBudgets(this, currentMonthYear, () -> {
            // Local DB now has cloud budgets. LiveData will trigger UI update.
        });

        observeData();

        saveBudgetButton.setOnClickListener(v -> {
            String input = budgetEditText.getText().toString().trim();
            if (input.isEmpty()) return;

            int amount = Integer.parseInt(input);
            String category = categorySpinner.getSelectedItem().toString();
            
            new Thread(() -> {
                // Check if already exists to update instead of insert new
                BudgetEntry existing = appDatabase.budgetDao().getBudgetByCategory(category, currentMonthYear);
                BudgetEntry entry;
                if (existing != null) {
                    entry = new BudgetEntry(existing.getId(), category, amount, currentMonthYear);
                } else {
                    entry = new BudgetEntry(category, amount, currentMonthYear);
                }

                appDatabase.budgetDao().insertBudget(entry);
                
                // Then sync to cloud
                BackendSync.updateBudget(BudgetActivity.this, entry, () -> {
                    runOnUiThread(() -> {
                        Toast.makeText(BudgetActivity.this, "Budget updated for " + category, Toast.LENGTH_SHORT).show();
                        budgetEditText.setText("");
                        budgetEditText.clearFocus();
                    });
                });
            }).start();
        });
    }

    private void setupCategorySpinner() {
        String[] categories = {"Overall", "Food", "Travel", "Clothes", "Rent", "Utilities", "Subscription", "Med", "Coffee", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
    }

    private void observeData() {
        // Observe all transactions to calculate spending
        appDatabase.transactionDao().loadAllTransactions().observe(this, transactions -> {
            if (transactions != null) {
                calculateSpending(transactions);
                updateUI();
            }
        });

        // Observe budgets to update list
        appDatabase.budgetDao().loadAllBudgets(currentMonthYear).observe(this, budgets -> {
            if (budgets != null) {
                updateCategoryList(budgets);
                updateUI();
            }
        });
    }

    private void calculateSpending(List<TransactionEntry> transactions) {
        categorySpending.clear();
        int totalExpense = 0;
        Calendar now = Calendar.getInstance();
        
        for (TransactionEntry t : transactions) {
            if (Constants.expenseCategory.equals(t.getTransactionType())) {
                Calendar tc = Calendar.getInstance();
                tc.setTime(t.getDate());
                if (tc.get(Calendar.MONTH) == now.get(Calendar.MONTH) && tc.get(Calendar.YEAR) == now.get(Calendar.YEAR)) {
                    int amount = t.getAmount();
                    totalExpense += amount;
                    String cat = t.getCategory();
                    categorySpending.put(cat, categorySpending.getOrDefault(cat, 0) + amount);
                }
            }
        }
        categorySpending.put("Overall", totalExpense);
    }

    private void updateUI() {
        // Find overall budget
        new Thread(() -> {
            BudgetEntry overall = appDatabase.budgetDao().getBudgetByCategory("Overall", currentMonthYear);
            runOnUiThread(() -> {
                int budget = (overall != null) ? overall.getAmount() : 0;
                int spent = categorySpending.getOrDefault("Overall", 0);
                int remaining = Math.max(0, budget - spent);
                int percent = (budget > 0) ? Math.min(100, (spent * 100) / budget) : 0;

                budgetAmountTextView.setText("\u20B9 " + budget);
                spentAmountTextView.setText("\u20B9 " + spent);
                remainingAmountTextView.setText("\u20B9 " + remaining);
                spentPercentageTextView.setText(percent + "%");
                budgetProgressBar.setProgress(percent);

                if (percent >= 90) {
                    budgetProgressBar.setProgressTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorExpense)));
                } else if (percent >= 70) {
                    budgetProgressBar.setProgressTintList(ColorStateList.valueOf(0xFFFFAB40)); // Orange
                } else {
                    budgetProgressBar.setProgressTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorPrimary)));
                }
            });
        }).start();
    }

    private void updateCategoryList(List<BudgetEntry> budgets) {
        categoryBudgetsContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (BudgetEntry b : budgets) {
            if ("Overall".equals(b.getCategory())) {
                // Make the overall card clickable to edit
                findViewById(R.id.overallBudgetCard).setOnClickListener(v -> {
                    budgetEditText.setText(String.valueOf(b.getAmount()));
                    for (int i = 0; i < categorySpinner.getCount(); i++) {
                        if (categorySpinner.getItemAtPosition(i).toString().equals("Overall")) {
                            categorySpinner.setSelection(i);
                            break;
                        }
                    }
                });
                continue;
            }

            View view = inflater.inflate(R.layout.category_budget_item, categoryBudgetsContainer, false);
            TextView nameTv = view.findViewById(R.id.categoryNameTextView);
            TextView statusTv = view.findViewById(R.id.budgetStatusTextView);
            ProgressBar progress = view.findViewById(R.id.categoryProgressBar);
            TextView percentTv = view.findViewById(R.id.percentTextView);

            int spent = categorySpending.getOrDefault(b.getCategory(), 0);
            int budget = b.getAmount();
            int percent = (budget > 0) ? Math.min(100, (spent * 100) / budget) : 0;

            nameTv.setText(b.getCategory());
            statusTv.setText("\u20B9 " + spent + " / \u20B9 " + budget);
            progress.setProgress(percent);
            percentTv.setText(percent + "%");

            if (percent >= 90) {
                progress.setProgressTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorExpense)));
            } else if (percent >= 70) {
                progress.setProgressTintList(ColorStateList.valueOf(0xFFFFAB40));
            } else {
                progress.setProgressTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorPrimary)));
            }
            
            view.setOnClickListener(v -> {
                budgetEditText.setText(String.valueOf(budget));
                for (int i = 0; i < categorySpinner.getCount(); i++) {
                    if (categorySpinner.getItemAtPosition(i).toString().equals(b.getCategory())) {
                        categorySpinner.setSelection(i);
                        break;
                    }
                }
            });

            categoryBudgetsContainer.addView(view);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

