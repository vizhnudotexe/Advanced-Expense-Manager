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
import java.util.Locale;

public class AddExpenseActivity extends AppCompatActivity {

    TextInputEditText amountTextInputEditText, descriptionTextInputEditText;
    TextInputLayout amountTextInputLayout, descriptionTextInputLayout;
    TextView dateTextView;
    LinearLayout dateLinearLayout, categoryChipsLayout;
    LinearLayout chipWalletCash, chipWalletUpi, chipWalletCard;
    Switch recurringSwitch;
    LinearLayout recurrenceTypeLayout;
    TextView chipDaily, chipWeekly, chipMonthly;

    String selectedWallet = "Cash";
    String selectedRecurrence = "";
    LinearLayout currentSelectedWalletChip = null;
    Calendar myCalendar;

    String intentFrom;
    boolean isExpenseMode = false;
    boolean isEditMode = false;
    int editingTransactionId = -1;

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
        isEditMode = intentFrom.equals(Constants.editExpenseString) || intentFrom.equals(Constants.editIncomeString);

        if (isEditMode) {
            editingTransactionId = intent.getIntExtra("id", -1);
        }

        if (intentFrom.equals(Constants.addIncomeString)) {
            setTitle("Add Income");
            hideExpenseOnlyCards();
        } else if (intentFrom.equals(Constants.editIncomeString)) {
            setTitle("Edit Income");
            hideExpenseOnlyCards();
        } else if (intentFrom.equals(Constants.addExpenseString)) {
            setTitle("Add Expense");
        } else if (intentFrom.equals(Constants.editExpenseString)) {
            setTitle("Edit Expense");
        }

        // Hide category UI (removed)
        if (categoryChipsLayout != null) categoryChipsLayout.setVisibility(View.GONE);

        // Populate edit fields
        if (isEditMode) {
            if (intent.hasExtra("amount")) {
                amountTextInputEditText.setText(String.valueOf(intent.getIntExtra("amount", 0)));
            }
            if (intent.hasExtra("description")) {
                descriptionTextInputEditText.setText(intent.getStringExtra("description"));
            }
            if (intent.hasExtra("date")) {
                dateTextView.setText(intent.getStringExtra("date"));
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
                    myCalendar.setTime(sdf.parse(intent.getStringExtra("date")));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (intent.hasExtra("wallet")) {
                String wallet = intent.getStringExtra("wallet");
                selectWalletByName(wallet);
            }
            if (intent.hasExtra("recurring")) {
                recurringSwitch.setChecked(intent.getBooleanExtra("recurring", false));
            }
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
        if (item.getItemId() == R.id.deleteButton && isEditMode) {
            deleteTransaction();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteTransaction() {
        if (editingTransactionId == -1) return;

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Delete Transaction");
        // Use a custom TextView for the message so it is always visible across themes
        android.widget.TextView msgView = new android.widget.TextView(this);
        msgView.setText("Are you sure you want to delete this transaction?");
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        msgView.setPadding(pad, pad, pad, pad);
        msgView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16);
        int nightModeFlags = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            msgView.setTextColor(android.graphics.Color.WHITE);
        } else {
            msgView.setTextColor(android.graphics.Color.DKGRAY);
        }
        builder.setView(msgView);
        builder.setPositiveButton("Delete", (dialog, which) -> {
            AppExecutors.getInstance().diskIO().execute(() -> {
                TransactionEntry entry = new TransactionEntry(
                        0, "", "", new java.util.Date(), "", "", false, ""
                );
                entry.setId(editingTransactionId);
                com.shashank.expensemanager.network.BackendSync.deleteTransaction(this, entry);
                AppDatabase.getInstance(this).transactionDao().removeExpense(entry);
                runOnUiThread(this::finish);
            });
        });
        builder.setNegativeButton("Cancel", null);
        android.app.AlertDialog dialog = builder.show();
        try {
            android.widget.Button positive = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE);
            android.widget.Button negative = dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE);
            if (positive != null) positive.setTextColor(android.graphics.Color.parseColor("#D32F2F"));
            if (negative != null) negative.setTextColor(android.graphics.Color.parseColor("#616161"));

            if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                android.view.Window window = dialog.getWindow();
                if (window != null) {
                    window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.parseColor("#222222")));
                }
                int messageId = getResources().getIdentifier("message", "id", "android");
                android.widget.TextView messageView = dialog.findViewById(messageId);
                if (messageView != null) messageView.setTextColor(android.graphics.Color.WHITE);
                int titleId = getResources().getIdentifier("alertTitle", "id", "android");
                android.widget.TextView titleView = dialog.findViewById(titleId);
                if (titleView != null) titleView.setTextColor(android.graphics.Color.WHITE);
            }
        } catch (Exception e) {
            // ignore styling errors
        }
    }

    public void saveTransaction() {
        if (amountTextInputEditText.getText() == null || amountTextInputEditText.getText().toString().isEmpty()) {
            amountTextInputLayout.setError("Amount is required");
            return;
        }
        if (descriptionTextInputEditText.getText() == null || descriptionTextInputEditText.getText().toString().trim().isEmpty()) {
            descriptionTextInputLayout.setError("Note is required");
            return;
        }

        AppExecutors.getInstance().diskIO().execute(() -> {
            TransactionEntry entry = new TransactionEntry(
                    Integer.parseInt(amountTextInputEditText.getText().toString()),
                    descriptionTextInputEditText.getText().toString(), // Replace 'General' with the note/description
                    descriptionTextInputEditText.getText().toString(),
                    myCalendar.getTime(),
                    isExpenseMode ? Constants.expenseCategory : Constants.incomeCategory,
                    selectedWallet,
                    recurringSwitch.isChecked(),
                    selectedRecurrence
            );

            if (isEditMode) {
                entry.setId(editingTransactionId);
                AppDatabase.getInstance(this).transactionDao().updateExpenseDetails(entry);
                com.shashank.expensemanager.network.BackendSync.addTransaction(this, entry, this::finish);
            } else {
                AppDatabase.getInstance(this).transactionDao().insertExpense(entry);
                com.shashank.expensemanager.network.BackendSync.addTransaction(this, entry, this::finish);
            }
        });
        // Close activity immediately to avoid duplicate saves when user taps Save multiple times.
        // Backend sync and local insert run in background.
        finish();
    }

    private void selectWalletChip(LinearLayout chip, String wallet) {
        if (currentSelectedWalletChip != null)
            currentSelectedWalletChip.setBackgroundResource(R.drawable.chip_background);
        chip.setBackgroundResource(R.drawable.chip_background_selected);
        currentSelectedWalletChip = chip;
        selectedWallet = wallet;
    }

    private void selectWalletByName(String walletName) {
        if (walletName == null) return;
        if (walletName.equalsIgnoreCase("UPI")) {
            selectWalletChip(chipWalletUpi, "UPI");
        } else if (walletName.equalsIgnoreCase("Card")) {
            selectWalletChip(chipWalletCard, "Card");
        } else {
            selectWalletChip(chipWalletCash, "Cash");
        }
    }

    public void showDatePicker() {
        new DatePickerDialog(this, R.style.CustomDatePickerDialogTheme, (view, year, month, day) -> {
            myCalendar.set(Calendar.YEAR, year);
            myCalendar.set(Calendar.MONTH, month);
            myCalendar.set(Calendar.DAY_OF_MONTH, day);
            setDateToTextView();
        }, myCalendar.get(Calendar.YEAR), myCalendar.get(Calendar.MONTH), myCalendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    public void setDateToTextView() {
        dateTextView.setText(new SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault()).format(myCalendar.getTime()));
    }
}
