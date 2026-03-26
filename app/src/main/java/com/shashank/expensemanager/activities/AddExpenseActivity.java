package com.shashank.expensemanager.activities;

import android.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.app.DatePickerDialog;

import com.shashank.expensemanager.R;
import com.shashank.expensemanager.transactionDb.AppDatabase;
import com.shashank.expensemanager.transactionDb.AppExecutors;
import com.shashank.expensemanager.transactionDb.TransactionEntry;
import com.shashank.expensemanager.transactionDb.TransactionViewModel;
import com.shashank.expensemanager.utils.Constants;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class AddExpenseActivity extends AppCompatActivity {

    TextInputEditText amountTextInputEditText, descriptionTextInputEditText;
    TextInputLayout amountTextInputLayout, descriptionTextInputLayout;
    TextView dateTextView;
    LinearLayout dateLinearLayout, categoryChipsLayout, walletChipsLayout;
    LinearLayout chipWalletCash, chipWalletUpi, chipWalletCard;
    Switch recurringSwitch;
    LinearLayout recurrenceTypeLayout;
    TextView chipDaily, chipWeekly, chipMonthly;

    String selectedCategory = "";
    String selectedWallet = "Cash";
    String selectedRecurrence = "";
    View currentSelectedCategoryChip = null;
    LinearLayout currentSelectedWalletChip = null;
    TextView currentSelectedRecurrenceChip = null;
    Calendar myCalendar;

    String intentFrom;
    int transactionid;
    boolean isExpenseMode = false;

    private static AppDatabase appDatabase;
    TransactionViewModel transactionViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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

        appDatabase = AppDatabase.getInstance(getApplicationContext());
        transactionViewModel = new ViewModelProvider(this).get(TransactionViewModel.class);

        myCalendar = Calendar.getInstance();
        setDateToTextView();

        Intent intent = getIntent();
        intentFrom = intent.getStringExtra("from");
        isExpenseMode = intentFrom.equals(Constants.addExpenseString) || intentFrom.equals(Constants.editExpenseString);

        if (intentFrom.equals(Constants.addIncomeString)) {
            setTitle("Add Income");
            selectedCategory = "Income";
            hideExpenseOnlyCards();
        } else if (intentFrom.equals(Constants.addExpenseString)) {
            setTitle("Add Expense");
            setupCategoryChips();
        } else if (intentFrom.equals(Constants.editIncomeString)) {
            setTitle("Edit Income");
            selectedCategory = "Income";
            hideExpenseOnlyCards();
            loadIntentData(intent);
        } else if (intentFrom.equals(Constants.editExpenseString)) {
            setTitle("Edit Expense");
            setupCategoryChips();
            loadIntentData(intent);
            String existingCat = intent.getStringExtra("category");
            if (existingCat != null) preselectCategoryChip(existingCat);
            String existingWallet = intent.getStringExtra("wallet");
            if (existingWallet != null) {
                selectedWallet = existingWallet;
                highlightWalletChip(existingWallet);
            }
            boolean isRecurring = intent.getBooleanExtra("recurring", false);
            if (isRecurring) {
                recurringSwitch.setChecked(true);
                recurrenceTypeLayout.setVisibility(View.VISIBLE);
                String recType = intent.getStringExtra("recurrenceType");
                if ("Daily".equals(recType)) selectRecurrenceChip(chipDaily, "Daily");
                else if ("Weekly".equals(recType)) selectRecurrenceChip(chipWeekly, "Weekly");
                else if ("Monthly".equals(recType)) selectRecurrenceChip(chipMonthly, "Monthly");
            }
        }

        // Wallet selection
        selectWalletChip(chipWalletCash, "Cash"); // default
        chipWalletCash.setOnClickListener(v -> selectWalletChip(chipWalletCash, "Cash"));
        chipWalletUpi.setOnClickListener(v -> selectWalletChip(chipWalletUpi, "UPI"));
        chipWalletCard.setOnClickListener(v -> selectWalletChip(chipWalletCard, "Card"));

        // Recurring switch
        recurringSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            recurrenceTypeLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) {
                deselectAllRecurrence();
                selectedRecurrence = "";
            }
        });

        chipDaily.setOnClickListener(v -> selectRecurrenceChip(chipDaily, "Daily"));
        chipWeekly.setOnClickListener(v -> selectRecurrenceChip(chipWeekly, "Weekly"));
        chipMonthly.setOnClickListener(v -> selectRecurrenceChip(chipMonthly, "Monthly"));

        dateLinearLayout.setOnClickListener(v -> showDatePicker());

        // Smart Categorization
        descriptionTextInputEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!isExpenseMode) return;
                String note = s.toString().toLowerCase(java.util.Locale.getDefault());
                if (note.contains("swiggy") || note.contains("zomato") || note.contains("macd") || note.contains("food") || note.contains("restaurant") || note.contains("pizza") || note.contains("burger")) {
                    preselectCategoryChip("Food");
                } else if (note.contains("uber") || note.contains("ola") || note.contains("petrol") || note.contains("bus") || note.contains("train") || note.contains("flight") || note.contains("cab")) {
                    preselectCategoryChip("Travel");
                } else if (note.contains("myntra") || note.contains("zara") || note.contains("shirt") || note.contains("shoes") || note.contains("pant") || note.contains("clothing")) {
                    preselectCategoryChip("Clothes");
                } else if (note.contains("netflix") || note.contains("movie") || note.contains("cinema") || note.contains("prime") || note.contains("spotify") || note.contains("gaming")) {
                    preselectCategoryChip("Movies");
                } else if (note.contains("doctor") || note.contains("medicine") || note.contains("gym") || note.contains("health") || note.contains("pharmacy") || note.contains("clinic")) {
                    preselectCategoryChip("Health");
                } else if (note.contains("grocery") || note.contains("milk") || note.contains("vegetables") || note.contains("supermarket") || note.contains("mart") || note.contains("fruits")) {
                    preselectCategoryChip("Grocery");
                }
            }
            
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
    }
    private void hideExpenseOnlyCards() {
        if (findViewById(R.id.categoryCard) != null)
            findViewById(R.id.categoryCard).setVisibility(View.GONE);
        if (findViewById(R.id.recurringCard) != null)
            findViewById(R.id.recurringCard).setVisibility(View.GONE);
    }

    private void loadIntentData(Intent intent) {
        amountTextInputEditText.setText(String.valueOf(intent.getIntExtra("amount", 0)));
        amountTextInputEditText.setSelection(amountTextInputEditText.getText().length());
        descriptionTextInputEditText.setText(intent.getStringExtra("description"));
        transactionid = intent.getIntExtra("id", -1);
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        try {
            Date date = sdf.parse(intent.getStringExtra("date"));
            myCalendar.setTime(date);
        } catch (ParseException e) { e.printStackTrace(); }
        dateTextView.setText(intent.getStringExtra("date"));
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
        LinearLayout chipAddCustom = findViewById(R.id.chipAddCustom);
        if (chipAddCustom != null) chipAddCustom.setOnClickListener(v -> showAddCustomCategoryDialog());
    }

    private void selectCategoryChip(View chip, String category) {
        if (currentSelectedCategoryChip != null)
            currentSelectedCategoryChip.setBackground(getResources().getDrawable(R.drawable.chip_background));
        chip.setBackground(getResources().getDrawable(R.drawable.chip_background_selected));
        currentSelectedCategoryChip = chip;
        selectedCategory = category;
    }

    private void preselectCategoryChip(String name) {
        int[] chipIds = {R.id.chipFood, R.id.chipTravel, R.id.chipClothes,
                R.id.chipMovies, R.id.chipHealth, R.id.chipGrocery};
        String[] catNames = {"Food", "Travel", "Clothes", "Movies", "Health", "Grocery"};
        for (int i = 0; i < chipIds.length; i++) {
            if (catNames[i].equalsIgnoreCase(name)) {
                LinearLayout chip = findViewById(chipIds[i]);
                if (chip != null) selectCategoryChip(chip, name);
                return;
            }
        }
        addDynamicCategoryChip(name, true);
    }

    private void selectWalletChip(LinearLayout chip, String wallet) {
        if (currentSelectedWalletChip != null)
            currentSelectedWalletChip.setBackground(getResources().getDrawable(R.drawable.chip_background));
        chip.setBackground(getResources().getDrawable(R.drawable.chip_background_selected));
        // Tint child icons/text white
        for (int i = 0; i < chip.getChildCount(); i++) {
            View child = chip.getChildAt(i);
            if (child instanceof ImageView) ((ImageView) child).setColorFilter(0xFFFFFFFF);
            if (child instanceof TextView) ((TextView) child).setTextColor(0xFFFFFFFF);
        }
        if (currentSelectedWalletChip != null) {
            for (int i = 0; i < currentSelectedWalletChip.getChildCount(); i++) {
                View child = currentSelectedWalletChip.getChildAt(i);
                if (child instanceof ImageView) ((ImageView) child).setColorFilter(getResources().getColor(R.color.colorTextPrimary));
                if (child instanceof TextView) ((TextView) child).setTextColor(getResources().getColor(R.color.colorTextPrimary));
            }
        }
        currentSelectedWalletChip = chip;
        selectedWallet = wallet;
    }

    private void highlightWalletChip(String wallet) {
        switch (wallet) {
            case "UPI": selectWalletChip(chipWalletUpi, "UPI"); break;
            case "Card": selectWalletChip(chipWalletCard, "Card"); break;
            default:     selectWalletChip(chipWalletCash, "Cash"); break;
        }
    }

    private void selectRecurrenceChip(TextView chip, String type) {
        deselectAllRecurrence();
        chip.setBackground(getResources().getDrawable(R.drawable.chip_background_selected));
        chip.setTextColor(0xFFFFFFFF);
        currentSelectedRecurrenceChip = chip;
        selectedRecurrence = type;
    }

    private void deselectAllRecurrence() {
        for (TextView c : new TextView[]{chipDaily, chipWeekly, chipMonthly}) {
            c.setBackground(getResources().getDrawable(R.drawable.chip_background));
            c.setTextColor(getResources().getColor(R.color.colorTextPrimary));
        }
        currentSelectedRecurrenceChip = null;
    }

    private void showAddCustomCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Custom Category");
        final EditText input = new EditText(this);
        input.setHint("e.g. Rent, Gym, Coffee...");
        input.setPadding(48, 24, 48, 24);
        builder.setView(input);
        builder.setPositiveButton("Add", (dialog, which) -> {
            String custom = input.getText().toString().trim();
            if (!custom.isEmpty()) addDynamicCategoryChip(custom, true);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void addDynamicCategoryChip(String categoryName, boolean select) {
        LinearLayout chip = new LinearLayout(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                (int)(44 * getResources().getDisplayMetrics().density));
        int margin = (int)(8 * getResources().getDisplayMetrics().density);
        int padding = (int)(14 * getResources().getDisplayMetrics().density);
        params.setMarginEnd(margin);
        chip.setLayoutParams(params);
        chip.setPadding(padding, 0, padding, 0);
        chip.setOrientation(LinearLayout.HORIZONTAL);
        chip.setGravity(android.view.Gravity.CENTER);
        chip.setBackground(getResources().getDrawable(R.drawable.chip_background));

        ImageView icon = new ImageView(this);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                (int)(16 * getResources().getDisplayMetrics().density),
                (int)(16 * getResources().getDisplayMetrics().density));
        iconParams.setMarginEnd((int)(6 * getResources().getDisplayMetrics().density));
        icon.setLayoutParams(iconParams);
        icon.setImageResource(R.drawable.ic_label);
        icon.setColorFilter(getResources().getColor(R.color.colorTextPrimary));

        TextView label = new TextView(this);
        label.setText(categoryName);
        label.setTextSize(13f);
        label.setTextColor(getResources().getColor(R.color.colorTextPrimary));

        chip.addView(icon);
        chip.addView(label);

        chip.setOnClickListener(v -> selectCategoryChip(chip, categoryName));

        int insertPos = categoryChipsLayout.getChildCount() - 1;
        categoryChipsLayout.addView(chip, insertPos);
        if (select) selectCategoryChip(chip, categoryName);
    }

    public void showDatePicker() {
        DatePickerDialog dialog = new DatePickerDialog(this, R.style.CustomDatePickerDialogTheme,
                (view, year, month, day) -> {
                    myCalendar.set(Calendar.YEAR, year);
                    myCalendar.set(Calendar.MONTH, month);
                    myCalendar.set(Calendar.DAY_OF_MONTH, day);
                    setDateToTextView();
                },
                myCalendar.get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                myCalendar.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialog.show();
    }

    public void setDateToTextView() {
        dateTextView.setText(new SimpleDateFormat("dd-MM-yyyy").format(myCalendar.getTime()));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_expense_activty_menu, menu);
        MenuItem deleteItem = menu.findItem(R.id.deleteButton);
        boolean isEdit = intentFrom.equals(Constants.editIncomeString) || intentFrom.equals(Constants.editExpenseString);
        if (deleteItem != null) {
            deleteItem.setVisible(isEdit);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }

        if (item.getItemId() == R.id.deleteButton) {
            boolean isEdit = intentFrom.equals(Constants.editIncomeString) || intentFrom.equals(Constants.editExpenseString);
            if (!isEdit) return true;

            String amountStr = amountTextInputEditText.getText().toString().trim();
            if (amountStr.isEmpty()) amountStr = "0";

            boolean recurring = recurringSwitch.isChecked();
            String categoryOfTransaction = (intentFrom.equals(Constants.addIncomeString) || intentFrom.equals(Constants.editIncomeString))
                    ? Constants.incomeCategory : Constants.expenseCategory;

            final TransactionEntry entryToDelete = new TransactionEntry(
                    Integer.parseInt(amountStr), selectedCategory, descriptionTextInputEditText.getText().toString().trim(),
                    myCalendar.getTime(), categoryOfTransaction,
                    selectedWallet, recurring, selectedRecurrence);
            entryToDelete.setId(transactionid);

            com.shashank.expensemanager.network.BackendSync.deleteTransaction(AddExpenseActivity.this, entryToDelete);

            Toast.makeText(this, "Transaction Deleted", Toast.LENGTH_SHORT).show();
            finish();
            return true;
        }

        if (item.getItemId() == R.id.saveButton) {
            String amountStr = amountTextInputEditText.getText().toString().trim();
            String desc = descriptionTextInputEditText.getText().toString().trim();

            if (amountStr.isEmpty()) { amountTextInputEditText.setError("Amount required"); return true; }
            if (isExpenseMode && selectedCategory.isEmpty()) {
                selectedCategory = "Other";
            }
            if (recurringSwitch.isChecked() && selectedRecurrence.isEmpty()) {
                Snackbar.make(findViewById(android.R.id.content), "Select recurrence type (Daily/Weekly/Monthly)", Snackbar.LENGTH_SHORT).show();
                return true;
            }

            boolean recurring = recurringSwitch.isChecked();
            String categoryOfTransaction = (intentFrom.equals(Constants.addIncomeString) || intentFrom.equals(Constants.editIncomeString))
                    ? Constants.incomeCategory : Constants.expenseCategory;

            final TransactionEntry entry = new TransactionEntry(
                    Integer.parseInt(amountStr), selectedCategory, desc,
                    myCalendar.getTime(), categoryOfTransaction,
                    selectedWallet, recurring, selectedRecurrence);

            boolean isEdit = intentFrom.equals(Constants.editIncomeString) || intentFrom.equals(Constants.editExpenseString);
            if (isEdit) entry.setId(transactionid);

            AppExecutors.getInstance().diskIO().execute(() -> {
                if (isEdit) {
                    appDatabase.transactionDao().updateExpenseDetails(entry);
                } else {
                    // Sync to cloud (which also saves locally)
                    com.shashank.expensemanager.network.BackendSync.addTransaction(AddExpenseActivity.this, entry, null);
                }
            });

            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (getCurrentFocus() != null)
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

            Snackbar.make(findViewById(android.R.id.content),
                    isEdit ? "Transaction Updated" : "Transaction Saved", Snackbar.LENGTH_LONG).show();

            new Handler().postDelayed(this::finish, 900);
        }
        return true;
    }
}

