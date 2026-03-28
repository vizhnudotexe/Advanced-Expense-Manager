package com.shashank.expensemanager.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.shashank.expensemanager.R;
import com.shashank.expensemanager.network.GroqApiClient;
import com.shashank.expensemanager.transactionDb.AppDatabase;
import com.shashank.expensemanager.transactionDb.AppExecutors;
import com.shashank.expensemanager.transactionDb.BudgetEntry;
import com.shashank.expensemanager.transactionDb.TransactionEntry;
import com.shashank.expensemanager.utils.Constants;
import com.shashank.expensemanager.utils.ExpenseList;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import static com.shashank.expensemanager.activities.MainActivity.fab;

public class BalanceFragment extends Fragment implements AdapterView.OnItemSelectedListener {

    private AppDatabase mAppDb;
    PieChart pieChart;
    BarChart barChart;
    Spinner spinner;
    Button btnLogout;

    private TextView balanceTv, incomeTv, expenseTv, dateTv;
    private TextView cashWalletTv, upiWalletTv, cardWalletTv;
    private TextView tvBudgetLeft, tvBudgetDetail;
    private ProgressBar pbDashboardBudget;
    private LineChart lineChart;

    private Button btnGenerateInsights;
    private TextView tvAiInsights;
    private ProgressBar progressAiInsights;
    private android.widget.LinearLayout llBadges;
    private GroqApiClient groqApiClient;

    private int foodExpense, travelExpense, clothesExpense, moviesExpense, heathExpense, groceryExpense, otherExpense;
    private int incomeAmount, expenseAmount, balanceAmount;
    private long firstDate;
    private ArrayList<ExpenseList> expenseList;
    private SharedPreferences aiCache;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_balance, container, false);

        aiCache = getContext().getSharedPreferences("AiCache", Context.MODE_PRIVATE);
        mAppDb = AppDatabase.getInstance(getContext());
        groqApiClient = new GroqApiClient(getContext());

        pieChart = view.findViewById(R.id.balancePieChart);
        barChart = view.findViewById(R.id.categoryBarChart);
        spinner = view.findViewById(R.id.spinner);
        spinner.setOnItemSelectedListener(this);

        balanceTv = view.findViewById(R.id.totalAmountTextView);
        expenseTv = view.findViewById(R.id.amountForExpenseTextView);
        incomeTv = view.findViewById(R.id.amountForIncomeTextView);
        dateTv = view.findViewById(R.id.dateTextView);
        cashWalletTv = view.findViewById(R.id.cashWalletTextView);
        upiWalletTv = view.findViewById(R.id.upiWalletTextView);
        cardWalletTv = view.findViewById(R.id.cardWalletTextView);
        
        tvBudgetLeft = view.findViewById(R.id.tvBudgetLeft);
        tvBudgetDetail = view.findViewById(R.id.tvBudgetDetail);
        pbDashboardBudget = view.findViewById(R.id.pbDashboardBudget);
        lineChart = view.findViewById(R.id.dailyTrendLineChart);

        btnGenerateInsights = view.findViewById(R.id.btnGenerateInsights);
        tvAiInsights = view.findViewById(R.id.tvAiInsights);
        progressAiInsights = view.findViewById(R.id.progressAiInsights);
        llBadges = view.findViewById(R.id.llBadges);

        // INSTANT LOAD CACHE
        String savedInsight = aiCache.getString("last_insight", null);
        if (savedInsight != null) {
            tvAiInsights.setText(savedInsight);
        }

        btnGenerateInsights.setOnClickListener(v -> generateAiInsights());

        btnLogout = view.findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            getContext().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE).edit().clear().apply();
            android.content.Intent intent = new android.content.Intent(getActivity(), com.shashank.expensemanager.activities.LoginActivity.class);
            intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) getActivity().finish();
        });

        expenseList = new ArrayList<>();
        
        // Load all data initially - this will display any existing transactions
        getAllBalanceAmount();
        setupPieChart();
        setupLineChart();
        updateBudgetVisibility();
        loadWalletBalances();
        
        return view;
    }

    private void generateAiInsights() {
        btnGenerateInsights.setEnabled(false);
        progressAiInsights.setVisibility(View.VISIBLE);
        
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.DAY_OF_MONTH, 1);
                long startOfMonth = cal.getTimeInMillis();
                int currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
                int totalDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
                
                int spent = mAppDb.transactionDao().getAmountbyCustomDates(Constants.expenseCategory, startOfMonth, System.currentTimeMillis());
                List<ExpenseList> categories = mAppDb.transactionDao().getSumExpenseByCategoriesCustomDate(startOfMonth, System.currentTimeMillis());
                
                // Check if there's any data
                if (spent == 0 && (categories == null || categories.isEmpty())) {
                    AppExecutors.getInstance().mainThread().execute(() -> {
                        tvAiInsights.setText("Add some transactions to get personalized financial insights!");
                        progressAiInsights.setVisibility(View.GONE);
                        btnGenerateInsights.setEnabled(true);
                    });
                    return;
                }
                
                StringBuilder cats = new StringBuilder();
                for (ExpenseList e : categories) cats.append(e.getCategory()).append(": ").append(e.getAmount()).append(", ");

                String prompt = String.format(Locale.getDefault(), "Spent so far: %d. Categories: %s. Day %d of %d. Give me a 2-sentence financial prediction.", spent, cats, currentDay, totalDays);

                AppExecutors.getInstance().mainThread().execute(() -> {
                    groqApiClient.generateInsights(prompt, new GroqApiClient.GroqCallback() {
                        @Override
                        public void onSuccess(String insight) {
                            tvAiInsights.setText(insight);
                            aiCache.edit().putString("last_insight", insight).apply();
                            progressAiInsights.setVisibility(View.GONE);
                            btnGenerateInsights.setEnabled(true);
                        }

                        @Override
                        public void onError(String error) {
                            progressAiInsights.setVisibility(View.GONE);
                            btnGenerateInsights.setEnabled(true);
                        }
                    });
                });
            } catch (Exception e) {
                AppExecutors.getInstance().mainThread().execute(() -> {
                    progressAiInsights.setVisibility(View.GONE);
                    btnGenerateInsights.setEnabled(true);
                });
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        getAllBalanceAmount();
        loadWalletBalances();
        updateBudgetVisibility();
    }

    public void refreshData() {
        try {
            if (balanceTv != null && mAppDb != null && getViewLifecycleOwner() != null) {
                getAllBalanceAmount();
                loadWalletBalances();
                setupPieChart();
                setupLineChart();
                updateBudgetVisibility();
            }
        } catch (Exception e) {
            android.util.Log.e("BalanceFragment", "Error refreshing data", e);
        }
    }

    private void setupSpinner() {
        if (getContext() == null || spinner == null) return;
        ArrayAdapter<CharSequence> arrayAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.date_array, android.R.layout.simple_spinner_item);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(arrayAdapter);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            setupSpinner();
            if (fab != null) fab.setVisibility(View.GONE);
        } else {
            if (fab != null) fab.setVisibility(View.VISIBLE);
        }
    }

    private void setupPieChart() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            List<ExpenseList> fetched = null;
            if (spinner.getSelectedItemPosition() == 0) fetched = mAppDb.transactionDao().getSumExpenseByCategoriesCustomDate(0, System.currentTimeMillis());
            else if (spinner.getSelectedItemPosition() == 1) fetched = getWeekPieValuesBatch();
            else fetched = getMonthPieValuesBatch();

            if (fetched != null) {
                expenseList.clear();
                expenseList.addAll(fetched);
                foodExpense=0; travelExpense=0; clothesExpense=0; moviesExpense=0; heathExpense=0; groceryExpense=0; otherExpense=0;
                for (ExpenseList e : fetched) {
                    if ("Food".equals(e.getCategory())) foodExpense = e.getAmount();
                    else if ("Travel".equals(e.getCategory())) travelExpense = e.getAmount();
                    else if ("Clothes".equals(e.getCategory())) clothesExpense = e.getAmount();
                    else if ("Movies".equals(e.getCategory())) moviesExpense = e.getAmount();
                    else if ("Health".equals(e.getCategory())) heathExpense = e.getAmount();
                    else if ("Grocery".equals(e.getCategory())) groceryExpense = e.getAmount();
                    else if ("Other".equals(e.getCategory())) otherExpense = e.getAmount();
                }
            }
            AppExecutors.getInstance().mainThread().execute(this::updatePieUI);
            setupBarChart();
        });
    }

    private void updatePieUI() {
        if (expenseList.isEmpty()) { pieChart.setVisibility(View.GONE); return; }
        pieChart.setVisibility(View.VISIBLE);
        List<PieEntry> entries = new ArrayList<>();
        for (ExpenseList e : expenseList) entries.add(new PieEntry(e.getAmount(), e.getCategory()));
        PieDataSet set = new PieDataSet(entries, "");
        set.setColors(ColorTemplate.COLORFUL_COLORS);
        PieData data = new PieData(set);
        data.setValueFormatter(new PercentFormatter());
        pieChart.setData(data);
        pieChart.setUsePercentValues(true);
        pieChart.setExtraOffsets(20, 5, 20, 15);
        pieChart.getLegend().setEnabled(false);
        pieChart.invalidate();
    }

    private void setupBarChart() {
        String[] labels = {"Food", "Travel", "Clothes", "Movies", "Health", "Grocery", "Other"};
        int[] vals = {foodExpense, travelExpense, clothesExpense, moviesExpense, heathExpense, groceryExpense, otherExpense};
        List<BarEntry> entries = new ArrayList<>();
        List<String> visibleLabels = new ArrayList<>();
        int x = 0;
        for (int i=0; i<vals.length; i++) {
            if (vals[i] > 0) {
                entries.add(new BarEntry(x++, vals[i]));
                visibleLabels.add(labels[i]);
            }
        }
        AppExecutors.getInstance().mainThread().execute(() -> {
            if (entries.isEmpty()) { barChart.setVisibility(View.GONE); return; }
            barChart.setVisibility(View.VISIBLE);
            BarDataSet set = new BarDataSet(entries, "");
            set.setColors(ColorTemplate.COLORFUL_COLORS);
            barChart.setData(new BarData(set));
            barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(visibleLabels));
            barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
            barChart.getXAxis().setTextColor(Color.WHITE);
            barChart.getAxisLeft().setTextColor(Color.WHITE);
            barChart.getAxisRight().setEnabled(false);
            barChart.getLegend().setEnabled(false);
            barChart.invalidate();
        });
    }

    private List<ExpenseList> getWeekPieValuesBatch() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        long start = c.getTimeInMillis();
        c.add(Calendar.DATE, 6);
        return mAppDb.transactionDao().getSumExpenseByCategoriesCustomDate(start, c.getTimeInMillis());
    }

    private List<ExpenseList> getMonthPieValuesBatch() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1);
        long start = c.getTimeInMillis();
        c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
        return mAppDb.transactionDao().getSumExpenseByCategoriesCustomDate(start, c.getTimeInMillis());
    }

    private void getAllBalanceAmount() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            firstDate = mAppDb.transactionDao().getFirstDate();
            incomeAmount = mAppDb.transactionDao().getAmountByTransactionType(Constants.incomeCategory);
            expenseAmount = mAppDb.transactionDao().getAmountByTransactionType(Constants.expenseCategory);
            balanceAmount = incomeAmount - expenseAmount;
            AppExecutors.getInstance().mainThread().execute(() -> {
                balanceTv.setText(balanceAmount + " \u20B9");
                incomeTv.setText(incomeAmount + " \u20B9");
                expenseTv.setText(expenseAmount + " \u20B9");
                dateTv.setText("All Time Summary");
            });
        });
    }

    private void loadWalletBalances() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            int cash = mAppDb.transactionDao().getWalletBalance("Cash");
            int upi = mAppDb.transactionDao().getWalletBalance("UPI");
            int card = mAppDb.transactionDao().getWalletBalance("Card");
            AppExecutors.getInstance().mainThread().execute(() -> {
                cashWalletTv.setText("\u20B9 " + cash);
                upiWalletTv.setText("\u20B9 " + upi);
                cardWalletTv.setText("\u20B9 " + card);
            });
        });
    }

    private void setupLineChart() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.DAY_OF_MONTH, 1);
            List<TransactionEntry> list = mAppDb.transactionDao().getTransactionsByDateRange(c.getTimeInMillis(), System.currentTimeMillis());
            TreeMap<Integer, Integer> map = new TreeMap<>();
            for (TransactionEntry t : list) {
                if (Constants.expenseCategory.equals(t.getTransactionType())) {
                    Calendar tc = Calendar.getInstance(); tc.setTime(t.getDate());
                    int d = tc.get(Calendar.DAY_OF_MONTH);
                    map.put(d, map.getOrDefault(d, 0) + t.getAmount());
                }
            }
            List<Entry> entries = new ArrayList<>();
            for (Map.Entry<Integer, Integer> e : map.entrySet()) entries.add(new Entry(e.getKey(), e.getValue()));
            AppExecutors.getInstance().mainThread().execute(() -> {
                if (entries.isEmpty()) { lineChart.setVisibility(View.GONE); return; }
                lineChart.setVisibility(View.VISIBLE);
                LineDataSet set = new LineDataSet(entries, "");
                set.setColor(Color.CYAN);
                lineChart.setData(new LineData(set));
                lineChart.getXAxis().setTextColor(Color.WHITE);
                lineChart.getAxisLeft().setTextColor(Color.WHITE);
                lineChart.invalidate();
            });
        });
    }

    private void updateBudgetVisibility() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            String my = new SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(new Date());
            List<BudgetEntry> budgets = mAppDb.budgetDao().loadAllBudgetsSync(my);
            int limit = 0;
            for (BudgetEntry b : budgets) limit += b.getAmount();
            Calendar c = Calendar.getInstance(); c.set(Calendar.DAY_OF_MONTH, 1);
            int spent = mAppDb.transactionDao().getAmountbyCustomDates(Constants.expenseCategory, c.getTimeInMillis(), System.currentTimeMillis());
            int finalLimit = limit;
            AppExecutors.getInstance().mainThread().execute(() -> {
                tvBudgetLeft.setText("\u20B9 " + (finalLimit - spent) + " Left");
                tvBudgetDetail.setText("Spent \u20B9 " + spent + " of \u20B9 " + finalLimit);
                pbDashboardBudget.setProgress(finalLimit > 0 ? (spent * 100) / finalLimit : 0);
            });
        });
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        setupPieChart();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}
}
