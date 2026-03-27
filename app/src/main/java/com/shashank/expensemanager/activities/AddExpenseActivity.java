package com.shashank.expensemanager.activities;

import androidx.lifecycle.ViewModelProvider;
import android.content.Intent;
import android.os.Bundle;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.app.DatePickerDialog;

import com.shashank.expensemanager.R;
import com.shashank.expensemanager.transactionDb.AppDatabase;
import com.shashank.expensemanager.transactionDb.AppExecutors;
import com.shashank.expensemanager.transactionDb.TransactionEntry;
import com.shashank.expensemanager.transactionDb.TransactionViewModel;
import com.shashank.expensemanager.utils.Constants;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class AddExpenseActivity extends AppCompatActivity {

    TextInputEditText amountTextInputEditText, descriptionTextInputEditText;
    TextInputLayout amountTextInputLayout, descriptionTextInputLayout;
    TextView dateTextView;
    LinearLayout dateLinearLayout, categoryChipsLayout;
    LinearLayout chipWalletCash, chipWalletUpi, chipWalletCard;
    Switch recurringSwitch;
    LinearLayout recurrenceTypeLayout;
    TextView chipDaily, chipWeekly, chipMonthly;

    String selectedCategory = "";
    String selectedWallet = "Cash";
    String selectedRecurrence = "";
    View currentSelectedCategoryChip = null;
    LinearLayout currentSelectedWalletChip = null;
    Calendar myCalendar;

    String intentFrom;
    boolean isExpenseMode = false;

    TransactionViewModel transactionViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        amountTextInputEditText = findViewById(R.id.amountTextInputEditText);
        descriptionTextInputEditText = findViewById(R.id.descriptionTextInputEditText);
        amountTextInputLayout = findViewById(R.id.amountTextInputLayout);
        descriptionTextInputLayout = findViewById(R.id.descriptionTextInputLayout);
        dateTextView = findViewById(R.id.dateTextView);
        dateLinearLayout = findViewById(R.id.dateLinerLayout);
        categoryChipsLayout = findViewById(R.id.categoryChipsLayout);
        recurringSwitch = findViewById(R.id.recurringSwitch);
        recurrenceTypeLayout = findViewById(R.id.recurrenceTypeLayout);
        chipDaily = findViewById(R.id.chipDaily);
        chipWeekly = findViewById(R.id.chipWeekly);
        chipMonthly = findViewById(R.id.chipMonthly);

        chipWalletCash = findViewById(R.id.chipWalletCash);
        chipWalletUpi = findViewById(R.id.chipWalletUpi);
        chipWalletCard = findViewById(R.id.chipWalletCard);

        transactionViewModel = new ViewModelProvider(this).get(TransactionViewModel.class);

        myCalendar = Calendar.getInstance();
        setDateToTextView();

        Intent intent = getIntent();
        intentFrom = intent.getStringExtra("from");
        if (intentFrom == null) intentFrom = "";
        isExpenseMode = intentFrom.equals(Constants.addExpenseString) || intentFrom.equals(Constants.editExpenseString);

        if (intentFrom.equals(Constants.addIncomeString)) {
            setTitle("Add Income");
            selectedCategory = "Income";
            hideExpenseOnlyCards();
        } else if (intentFrom.equals(Constants.addExpenseString)) {
            setTitle("Add Expense");
            setupCategoryChips();
        }

        // Wallet selection
        selectWalletChip(chipWalletCash, "Cash");
        chipWalletCash.setOnClickListener(v -> selectWalletChip(chipWalletCash, "Cash"));
        chipWalletUpi.setOnClickListener(v -> selectWalletChip(chipWalletUpi, "UPI"));
        chipWalletCard.setOnClickListener(v -> selectWalletChip(chipWalletCard, "Card"));

        dateLinearLayout.setOnClickListener(v -> showDatePicker());
    }

    private void hideExpenseOnlyCards() {
        if (findViewById(R.id.categoryCard) != null)
            findViewById(R.id.categoryCard).setVisibility(View.GONE);
    }

    private void setupCategoryChips() {
        int[] chipIds = {R.id.chipFood, R.id.chipTravel, R.id.chipClothes,
                R.id.chipMovies, R.id.chipHealth, R.id.chipGrocery};
        String[] catNames = {"Food", "Travel", "Clothes", "Movies", "Health", "Grocery"};
        for (int i = 0; i < chipIds.length; i++) {
            final LinearLayout chip = findViewById(chipIds[i]);
            final String name = catNames[i];
            if (chip != null) chip.setOnClickListener(v -> selectCategoryChip(chip, name));
        }
    }

    private void selectCategoryChip(View chip, String category) {
        if (currentSelectedCategoryChip != null)
            currentSelectedCategoryChip.setBackgroundResource(R.drawable.chip_background);
        chip.setBackgroundResource(R.drawable.chip_background_selected);
        currentSelectedCategoryChip = chip;
        selectedCategory = category;
    }

    private void selectWalletChip(LinearLayout chip, String wallet) {
        if (currentSelectedWalletChip != null)
            currentSelectedWalletChip.setBackgroundResource(R.drawable.chip_background);
        chip.setBackgroundResource(R.drawable.chip_background_selected);
        currentSelectedWalletChip = chip;
        selectedWallet = wallet;
    }

    public void showDatePicker() {
        new DatePickerDialog(this, (view, year, month, day) -> {
            myCalendar.set(Calendar.YEAR, year);
            myCalendar.set(Calendar.MONTH, month);
            myCalendar.set(Calendar.DAY_OF_MONTH, day);
            setDateToTextView();
        }, myCalendar.get(Calendar.YEAR), myCalendar.get(Calendar.MONTH), myCalendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    public void setDateToTextView() {
        dateTextView.setText(new SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault()).format(myCalendar.getTime()));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_expense_activty_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        if (item.getItemId() == R.id.saveButton) {
            saveTransaction();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void saveTransaction() {
        if (amountTextInputEditText.getText() == null || amountTextInputEditText.getText().toString().isEmpty()) {
            amountTextInputLayout.setError("Amount is required");
            return;
        }
        AppExecutors.getInstance().diskIO().execute(() -> {
            TransactionEntry entry = new TransactionEntry(
                    Integer.parseInt(amountTextInputEditText.getText().toString()),
                    selectedCategory,
                    descriptionTextInputEditText.getText().toString(),
                    myCalendar.getTime(),
                    isExpenseMode ? Constants.expenseCategory : Constants.incomeCategory,
                    selectedWallet,
                    recurringSwitch.isChecked(),
                    selectedRecurrence
            );
            com.shashank.expensemanager.network.BackendSync.addTransaction(this, entry, this::finish);
        });
    }
}
