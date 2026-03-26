package com.shashank.expensemanager.fragments;

import android.app.DatePickerDialog;
import androidx.lifecycle.ViewModelProvider;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import android.widget.ProgressBar;

import com.shashank.expensemanager.network.GroqApiClient;


import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.shashank.expensemanager.R;
import com.shashank.expensemanager.activities.MainActivity;
import com.shashank.expensemanager.transactionDb.AppDatabase;
import com.shashank.expensemanager.transactionDb.AppExecutors;
import com.shashank.expensemanager.transactionDb.TransactionViewModel;
import com.shashank.expensemanager.utils.Constants;
import com.shashank.expensemanager.utils.ExpenseList;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.shashank.expensemanager.transactionDb.BudgetEntry;
import com.shashank.expensemanager.transactionDb.TransactionEntry;
import java.util.Collections;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import static com.shashank.expensemanager.activities.MainActivity.fab;


public class BalanceFragment extends Fragment implements AdapterView.OnItemSelectedListener{


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
    private TextView tvPersonalityLoading;
    private GroqApiClient groqApiClient;

    private int balanceAmount, incomeAmount, expenseAmount;
    private int foodExpense, travelExpense, clothesExpense, moviesExpense, heathExpense, groceryExpense, otherExpense;

    long firstDate;

    ArrayList<ExpenseList> expenseList;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.fragment_balance,container,false);

        pieChart = view.findViewById(R.id.balancePieChart);
        barChart = view.findViewById(R.id.categoryBarChart);
        spinner = view.findViewById(R.id.spinner);
        spinner.setOnItemSelectedListener(this);

        mAppDb = AppDatabase.getInstance(getContext());

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
        tvPersonalityLoading = view.findViewById(R.id.tvPersonalityLoading);
        groqApiClient = new GroqApiClient(getContext());

        loadSavedPersonality();

        btnGenerateInsights.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateAiInsights();
            }
        });

        btnLogout = view.findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getContext() != null) {
                    getContext().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE).edit().clear().apply();
                    android.content.Intent intent = new android.content.Intent(getActivity(), com.shashank.expensemanager.activities.LoginActivity.class);
                    intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    if (getActivity() != null) getActivity().finish();
                }
            }
        });

        expenseList = new ArrayList<>();
        getAllBalanceAmount();
        setupPieChart();
        setupLineChart();
        updateBudgetVisibility();
        loadWalletBalances();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateBudgetVisibility();
        getAllBalanceAmount();
        loadWalletBalances();
    }
        //TODO 1.Change constraint to linear and change entire layout
        //TODO 2.Align piechart properly with label
        //TODO 3.See if can opytimize queries and spinner state and read about fragment lifecycle


    private void setupSpinner() {
        if (getContext() == null || spinner == null) return;
        ArrayAdapter<CharSequence> arrayAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.date_array,
                android.R.layout.simple_spinner_item);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(arrayAdapter);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        Log.i("fragment", String.valueOf(isVisibleToUser));
        if (isVisibleToUser){
            setupSpinner();
            if (fab != null) fab.setVisibility(View.GONE);
        } else{
            if (fab != null) fab.setVisibility(View.VISIBLE);
        }
    }

    private void setupPieChart() {

        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                if(spinner.getSelectedItemPosition()==0)
                    getAllPieValues();
                else if(spinner.getSelectedItemPosition()==1) {
                    try {
                        getWeekPieValues();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                else if(spinner.getSelectedItemPosition()==2){
                    try {
                        getMonthPieValues();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                expenseList.clear();
             if(foodExpense!=0)
                 expenseList.add(new ExpenseList("Food",foodExpense));
             if(travelExpense!=0)
                 expenseList.add(new ExpenseList("Travel",travelExpense));
             if(clothesExpense!=0)
                 expenseList.add(new ExpenseList("Clothes",clothesExpense));
             if(moviesExpense!=0)
                 expenseList.add(new ExpenseList("Movies",moviesExpense));
             if(heathExpense!=0)
                 expenseList.add(new ExpenseList("Health",heathExpense));
             if(groceryExpense!=0)
                 expenseList.add(new ExpenseList("Grocery",groceryExpense));
             if(otherExpense!=0)
                 expenseList.add(new ExpenseList("Other",otherExpense));

                 // Populate bar chart on background thread values
                 setupBarChart();
            }
        });


        AppExecutors.getInstance().mainThread().execute(new Runnable() {
            @Override
            public void run() {
                if (expenseList.isEmpty()) {
                    pieChart.setVisibility(View.GONE);
                    return;
                }
                
                List<PieEntry> pieEntries = new ArrayList<>();
                for(int i = 0 ; i <expenseList.size(); i++){
                    pieEntries.add(new PieEntry(expenseList.get(i).getAmount(),expenseList.get(i).getCategory()));
                }
                pieChart.setVisibility(View.VISIBLE);
                PieDataSet dataSet = new PieDataSet(pieEntries,null);
                dataSet.setColors(ColorTemplate.COLORFUL_COLORS);
                PieData pieData = new PieData(dataSet);

                pieData.setValueTextSize(14f);
                pieData.setValueTextColor(Color.WHITE);
                pieData.setValueFormatter(new PercentFormatter());
                pieChart.setUsePercentValues(true);
                pieChart.setData(pieData);
                pieChart.animateY(1000);
                
                // Add extra offsets so the legend/labels don't get cut off
                pieChart.setExtraOffsets(20f, 5f, 20f, 15f);
                pieChart.setEntryLabelColor(Color.WHITE);
                pieChart.setEntryLabelTextSize(12f);
                
                pieChart.invalidate();

                pieChart.getDescription().setText("");
                Legend l=pieChart.getLegend();
                l.setVerticalAlignment(Legend.LegendVerticalAlignment.CENTER);
                l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
                l.setOrientation(Legend.LegendOrientation.VERTICAL);
                l.setDrawInside(false);
                l.setXEntrySpace(7f);
                l.setYEntrySpace(0f);
                l.setYOffset(0f);
                l.setTextColor(Color.WHITE);
            }
        });

    }

    private void loadWalletBalances() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            int cash = mAppDb.transactionDao().getWalletBalance("Cash");
            int upi = mAppDb.transactionDao().getWalletBalance("UPI");
            int card = mAppDb.transactionDao().getWalletBalance("Card");
            AppExecutors.getInstance().mainThread().execute(() -> {
                if (cashWalletTv != null) cashWalletTv.setText("\u20B9 " + cash);
                if (upiWalletTv != null) upiWalletTv.setText("\u20B9 " + upi);
                if (cardWalletTv != null) cardWalletTv.setText("\u20B9 " + card);
            });
        });
    }

    private void setupBarChart() {
        if (barChart == null) return;
        String[] allLabels = {"Food", "Travel", "Clothes", "Movies", "Health", "Grocery", "Other"};
        float[] allValues = {foodExpense, travelExpense, clothesExpense, moviesExpense, heathExpense, groceryExpense, otherExpense};

        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> visibleLabels = new ArrayList<>();
        
        int xIndex = 0;
        for (int i = 0; i < allValues.length; i++) {
            if (allValues[i] > 0) {
                entries.add(new BarEntry(xIndex, allValues[i]));
                visibleLabels.add(allLabels[i]);
                xIndex++;
            }
        }

        if (entries.isEmpty()) {
            barChart.clear();
            return;
        }

        AppExecutors.getInstance().mainThread().execute(() -> {
            if (entries.isEmpty()) {
                barChart.setVisibility(View.GONE);
                return;
            }
            barChart.setVisibility(View.VISIBLE);
            BarDataSet dataSet = new BarDataSet(entries, "Categories");
            dataSet.setColors(ColorTemplate.COLORFUL_COLORS);
            dataSet.setValueTextColor(Color.WHITE);
            dataSet.setValueTextSize(11f);

            BarData barData = new BarData(dataSet);
            barData.setBarWidth(0.6f);

            barChart.setData(barData);
            barChart.setFitBars(true);
            barChart.getDescription().setEnabled(false);
            barChart.setBackgroundColor(Color.TRANSPARENT);
            barChart.getLegend().setEnabled(false);
            barChart.getAxisRight().setEnabled(false);
            barChart.getAxisLeft().setTextColor(Color.WHITE);
            barChart.getAxisLeft().setGridColor(0x33FFFFFF);

            XAxis xAxis = barChart.getXAxis();
            xAxis.setValueFormatter(new IndexAxisValueFormatter(visibleLabels));
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setGranularity(1f);
            xAxis.setLabelCount(visibleLabels.size());
            xAxis.setTextColor(Color.WHITE);
            xAxis.setDrawGridLines(false);
            
            // Add more offset to bottom for labels to prevent cutoff
            barChart.setExtraOffsets(10f, 10f, 10f, 40f);

            barChart.animateY(800);
            barChart.invalidate();
        });
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

        if(adapterView.getSelectedItemPosition()==0){
            getAllBalanceAmount();
            setupPieChart();
        }

        else if (adapterView.getSelectedItemPosition() == 1){
            //This week
            try {
                getWeekBalanceAmount();
                setupPieChart();
            }
            catch (ParseException e) {
                e.printStackTrace();
            }
        }
        else if(adapterView.getSelectedItemPosition()==2){
            //This month
            try {
                getMonthBalanceAmount();
                setupPieChart();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
    }


    private void getAllPieValues(){
        foodExpense =mAppDb.transactionDao().getSumExpenseByCategory("Food");
        travelExpense=mAppDb.transactionDao().getSumExpenseByCategory("Travel");
        clothesExpense=mAppDb.transactionDao().getSumExpenseByCategory("Clothes");
        moviesExpense=mAppDb.transactionDao().getSumExpenseByCategory("Movies");
        heathExpense=mAppDb.transactionDao().getSumExpenseByCategory("Health");
        groceryExpense=mAppDb.transactionDao().getSumExpenseByCategory("Grocery");
        otherExpense=mAppDb.transactionDao().getSumExpenseByCategory("Other");
    }

    private void getWeekPieValues() throws ParseException {
        Calendar calendar;
        calendar=Calendar.getInstance();

        DateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String startDate = "", endDate = "";
        // Set the calendar to sunday of the current week
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        startDate = df.format(calendar.getTime());
        Date sDate=df.parse(startDate);
        final long sdate=sDate.getTime();

        calendar.add(Calendar.DATE, 6);
        endDate = df.format(calendar.getTime());
        Date eDate=df.parse(endDate);
        final long edate=eDate.getTime();

        foodExpense =mAppDb.transactionDao().getSumExpenseByCategoryCustomDate("Food",sdate,edate);
        travelExpense=mAppDb.transactionDao().getSumExpenseByCategoryCustomDate("Travel",sdate,edate);
        clothesExpense=mAppDb.transactionDao().getSumExpenseByCategoryCustomDate("Clothes",sdate,edate);
        moviesExpense=mAppDb.transactionDao().getSumExpenseByCategoryCustomDate("Movies",sdate,edate);
        heathExpense=mAppDb.transactionDao().getSumExpenseByCategoryCustomDate("Health",sdate,edate);
        groceryExpense=mAppDb.transactionDao().getSumExpenseByCategoryCustomDate("Grocery",sdate,edate);
        otherExpense=mAppDb.transactionDao().getSumExpenseByCategoryCustomDate("Other",sdate,edate);
    }

    private void getMonthPieValues() throws ParseException{

        Calendar calendar;
        calendar=Calendar.getInstance();

        DateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String startDate = "", endDate = "";

        calendar.set(Calendar.DAY_OF_MONTH,1);
        startDate = df.format(calendar.getTime());
        Date sDate=df.parse(startDate);
        final long sdate=sDate.getTime();

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        endDate = df.format(calendar.getTime());
        Date eDate=df.parse(endDate);
        final long edate=eDate.getTime();

        foodExpense =mAppDb.transactionDao().getSumExpenseByCategoryCustomDate("Food",sdate,edate);
        travelExpense=mAppDb.transactionDao().getSumExpenseByCategoryCustomDate("Travel",sdate,edate);
        clothesExpense=mAppDb.transactionDao().getSumExpenseByCategoryCustomDate("Clothes",sdate,edate);
        moviesExpense=mAppDb.transactionDao().getSumExpenseByCategoryCustomDate("Movies",sdate,edate);
        heathExpense=mAppDb.transactionDao().getSumExpenseByCategoryCustomDate("Health",sdate,edate);
        groceryExpense=mAppDb.transactionDao().getSumExpenseByCategoryCustomDate("Grocery",sdate,edate);
        otherExpense=mAppDb.transactionDao().getSumExpenseByCategoryCustomDate("Other",sdate,edate);
    }

    private void getAllBalanceAmount(){

        //get date when first transaction date and todays date, then update balances
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                firstDate = mAppDb.transactionDao().getFirstDate();
                int income = mAppDb.transactionDao().getAmountByTransactionType(Constants.incomeCategory);
                incomeAmount = income;
                int expense = mAppDb.transactionDao().getAmountByTransactionType(Constants.expenseCategory);
                expenseAmount = expense;
                int balance = income - expense;
                balanceAmount = balance;

                AppExecutors.getInstance().mainThread().execute(new Runnable() {
                    @Override
                    public void run() {
                        SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
                        String first = df.format(new Date(firstDate));
                        Date today = Calendar.getInstance().getTime();
                        String todaysDate = df.format(today);
                        String dateStr = first + " - " + todaysDate;
                        dateTv.setText(dateStr);
                        balanceTv.setText(String.valueOf(balanceAmount) + " \u20B9");
                        incomeTv.setText(String.valueOf(incomeAmount) + " \u20B9");
                        expenseTv.setText(String.valueOf(expenseAmount) + " \u20B9");
                    }
                });
            }
        });
    }

    private void getWeekBalanceAmount() throws ParseException {
        Calendar calendar;
        calendar=Calendar.getInstance();

        DateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String startDate = "", endDate = "";
        // Set the calendar to sunday of the current week
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        startDate = df.format(calendar.getTime());
        Date sDate=df.parse(startDate);
        final long sdate=sDate.getTime();

        calendar.add(Calendar.DATE, 6);
        endDate = df.format(calendar.getTime());
        Date eDate=df.parse(endDate);
        final long edate=eDate.getTime();

        String dateString = startDate + " - " + endDate;
        dateTv.setText(dateString);

        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                int income = mAppDb.transactionDao().getAmountbyCustomDates(Constants.incomeCategory,sdate,edate);
                incomeAmount = income;
                int expense = mAppDb.transactionDao().getAmountbyCustomDates(Constants.expenseCategory,sdate,edate);
                expenseAmount = expense;
                int balance = income - expense;
                balanceAmount = balance;

            }
        });
        AppExecutors.getInstance().mainThread().execute(new Runnable() {
            @Override
            public void run() {
                balanceTv.setText(String.valueOf(balanceAmount)+" \u20B9");
                incomeTv.setText(String.valueOf(incomeAmount)+" \u20B9");
                expenseTv.setText(String.valueOf(expenseAmount)+" \u20B9");
            }
        });
    }


    private void getMonthBalanceAmount() throws ParseException {
        Calendar calendar;
        calendar=Calendar.getInstance();

        DateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String startDate = "", endDate = "";

        calendar.set(Calendar.DAY_OF_MONTH,1);
        startDate = df.format(calendar.getTime());
        Date sDate=df.parse(startDate);
        final long sdate=sDate.getTime();

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        endDate = df.format(calendar.getTime());
        Date eDate=df.parse(endDate);
        final long edate=eDate.getTime();

        String dateString = startDate + " - " + endDate;
        dateTv.setText(dateString);

        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                int income = mAppDb.transactionDao().getAmountbyCustomDates(Constants.incomeCategory,sdate,edate);
                incomeAmount = income;
                int expense = mAppDb.transactionDao().getAmountbyCustomDates(Constants.expenseCategory,sdate,edate);
                expenseAmount = expense;
                int balance = income - expense;
                balanceAmount = balance;

            }
        });
        AppExecutors.getInstance().mainThread().execute(new Runnable() {
            @Override
            public void run() {
                balanceTv.setText(String.valueOf(balanceAmount)+" \u20B9");
                incomeTv.setText(String.valueOf(incomeAmount)+" \u20B9");
                expenseTv.setText(String.valueOf(expenseAmount)+" \u20B9");
            }
        });
    }

    private void generateAiInsights() {
        btnGenerateInsights.setEnabled(false);
        progressAiInsights.setVisibility(View.VISIBLE);
        tvAiInsights.setText("Analyzing patterns & predicting expenses...");

        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.DAY_OF_MONTH, 1);
                    long startOfMonth = cal.getTimeInMillis();
                    
                    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                    cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59);
                    long endOfMonth = cal.getTimeInMillis();

                    int currentDayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
                    int totalDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
                    
                    int currentMonthExpense = mAppDb.transactionDao().getAmountbyCustomDates(Constants.expenseCategory, startOfMonth, endOfMonth);
                    
                    // Fetch Budgets to know limits
                    String monthYear = new SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(new Date());
                    List<BudgetEntry> budgets = mAppDb.budgetDao().loadAllBudgetsSync(monthYear);
                    int totalBudget = 0;
                    for (BudgetEntry b : budgets) {
                        if ("Overall".equals(b.getCategory())) {
                            totalBudget = b.getAmount(); break;
                        } else {
                            totalBudget += b.getAmount();
                        }
                    }

                    // Top Categories this month
                    String[] cats = {"Food", "Travel", "Clothes", "Movies", "Health", "Grocery", "Other"};
                    StringBuilder categoryBreakdown = new StringBuilder();
                    for (String cat : cats) {
                        int exp = mAppDb.transactionDao().getSumExpenseByCategoryCustomDate(cat, startOfMonth, endOfMonth);
                        if (exp > 0) {
                            categoryBreakdown.append(cat).append(": ₹").append(exp).append(", ");
                        }
                    }

                    // Calculate Run-rate
                    int projectedEndMonthExpense = (currentDayOfMonth > 0) ? (currentMonthExpense / currentDayOfMonth) * totalDaysInMonth : 0;

                    // Construct highly specific predictive prompt
                    String promptText = String.format(Locale.getDefault(), 
                        "You are an expert financial predictor. Act directly, no greetings.\n" +
                        "Current Day: %d of %d in the month.\n" +
                        "Total Budget Set: ₹%d\n" +
                        "Spent So Far: ₹%d\n" +
                        "Projected End of Month Spend (at current rate): ₹%d\n" +
                        "Category Breakdown: %s\n" +
                        "Task: Provide 2 short, punchy sentences. 1. An 'Overspending Alert' predicting their end-of-month situation based on current trends. 2. A specific behavioral insight (e.g., 'You've spent X% of your budget in just Y days, primarily on Food').", 
                        currentDayOfMonth, totalDaysInMonth, totalBudget, currentMonthExpense, projectedEndMonthExpense, categoryBreakdown.toString());

                    final String promptData = promptText;

                    AppExecutors.getInstance().mainThread().execute(new Runnable() {
                        @Override
                        public void run() {
                            groqApiClient.generateInsights(promptData, new GroqApiClient.GroqCallback() {
                                @Override
                                public void onSuccess(String insight) {
                                    progressAiInsights.setVisibility(View.GONE);
                                    tvAiInsights.setText(insight);
                                    btnGenerateInsights.setEnabled(true);
                                    btnGenerateInsights.setText("Refresh Insight");
                                    generateNewPersonality();
                                }

                                @Override
                                public void onError(String error) {
                                    progressAiInsights.setVisibility(View.GONE);
                                    tvAiInsights.setText("Oops: " + error);
                                    btnGenerateInsights.setEnabled(true);
                                    btnGenerateInsights.setText("Retry");
                                }
                            });
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void setupLineChart() {
        if (lineChart == null) return;

        AppExecutors.getInstance().diskIO().execute(() -> {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0);
            cal.set(Calendar.DAY_OF_MONTH, 1);
            long startOfMonth = cal.getTimeInMillis();
            
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            long endOfMonth = cal.getTimeInMillis();

            // Fetch all transactions for this month to group by day
            List<TransactionEntry> transactions = mAppDb.transactionDao().getTransactionsByDateRange(startOfMonth, endOfMonth);
            TreeMap<Integer, Integer> dailyTotals = new TreeMap<>();
            
            // Initialize with first 15 days or current day
            int currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
            for(int i=1; i<=currentDay; i++) dailyTotals.put(i, 0);

            if (transactions != null) {
                for (TransactionEntry t : transactions) {
                    if (Constants.expenseCategory.equals(t.getTransactionType())) {
                        Calendar tc = Calendar.getInstance();
                        tc.setTime(t.getDate());
                        int day = tc.get(Calendar.DAY_OF_MONTH);
                        dailyTotals.put(day, dailyTotals.getOrDefault(day, 0) + t.getAmount());
                    }
                }
            }

            List<Entry> entries = new ArrayList<>();
            for (Map.Entry<Integer, Integer> entry : dailyTotals.entrySet()) {
                entries.add(new Entry(entry.getKey(), entry.getValue()));
            }

            AppExecutors.getInstance().mainThread().execute(() -> {
                if (entries.isEmpty() || (entries.size() == 1 && entries.get(0).getY() == 0)) {
                    lineChart.setVisibility(View.GONE);
                    return;
                }
                lineChart.setVisibility(View.VISIBLE);
                LineDataSet dataSet = new LineDataSet(entries, "Daily Spending");
                dataSet.setColor(getResources().getColor(R.color.colorPrimary));
                dataSet.setLineWidth(2f);
                dataSet.setCircleColor(getResources().getColor(R.color.colorPrimary));
                dataSet.setCircleRadius(4f);
                dataSet.setDrawValues(false);
                dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                dataSet.setDrawFilled(true);
                dataSet.setFillAlpha(50);
                dataSet.setFillColor(getResources().getColor(R.color.colorPrimary));

                LineData lineData = new LineData(dataSet);
                lineChart.setData(lineData);
                lineChart.getDescription().setEnabled(false);
                lineChart.getLegend().setEnabled(false);
                lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
                lineChart.getXAxis().setTextColor(Color.WHITE);
                lineChart.getAxisLeft().setTextColor(Color.WHITE);
                lineChart.getAxisRight().setEnabled(false);
                
                // Add offset to bottom for labels
                lineChart.setExtraOffsets(0f, 0f, 0f, 15f);

                lineChart.animateX(1000);
                lineChart.invalidate();
            });
        });
    }

    private void updateBudgetVisibility() {
        if (pbDashboardBudget == null || tvBudgetDetail == null) return;
        
        AppExecutors.getInstance().diskIO().execute(() -> {
            String monthYear = new SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(new Date());
            
            // Get all budgets for the current month
            List<BudgetEntry> budgets = mAppDb.budgetDao().loadAllBudgetsSync(monthYear);
            
            int budgetLimit = 0;
            BudgetEntry overallBudget = null;
            
            // Look for "Overall" budget
            for (BudgetEntry b : budgets) {
                if ("Overall".equals(b.getCategory())) {
                    overallBudget = b;
                    break;
                }
            }
            
            if (overallBudget != null) {
                // If "Overall" exists, use it as the limit
                budgetLimit = overallBudget.getAmount();
            } else {
                // If no "Overall" budget, sum up all other category budgets
                for (BudgetEntry b : budgets) {
                    budgetLimit += b.getAmount();
                }
            }
            
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            cal.set(Calendar.DAY_OF_MONTH, 1);
            long start = cal.getTimeInMillis();
            
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            long end = cal.getTimeInMillis();
            
            int spent = mAppDb.transactionDao().getAmountbyCustomDates(Constants.expenseCategory, start, end);
            
            final int finalBudgetLimit = budgetLimit;
            final int finalSpent = spent;
            
            AppExecutors.getInstance().mainThread().execute(() -> {
                int limit = finalBudgetLimit;
                int spentVal = finalSpent;
                int remaining = Math.max(0, limit - spentVal);
                int percent = (limit > 0) ? Math.min(100, (spentVal * 100) / limit) : 0;

                tvBudgetLeft.setText("\u20B9 " + remaining + " Left");
                tvBudgetDetail.setText("Spent \u20B9 " + spentVal + " of \u20B9 " + limit);
                pbDashboardBudget.setProgress(percent);
                
                if (percent >= 90) {
                    pbDashboardBudget.setProgressTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.colorExpense)));
                } else if (percent >= 70) {
                    pbDashboardBudget.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFFFFAB40));
                } else {
                    pbDashboardBudget.setProgressTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.colorPrimary)));
                }
            });
        });
    }

    private void loadSavedPersonality() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            com.shashank.expensemanager.transactionDb.PersonalityEntry entry = mAppDb.aiDao().getPersonality();
            if (entry != null && entry.getBadges() != null && !entry.getBadges().isEmpty()) {
                AppExecutors.getInstance().mainThread().execute(() -> {
                    displayPersonalityBadges(entry.getBadges());
                });
            } else {
                com.shashank.expensemanager.network.BackendSync.fetchPersonality(getContext(), () -> {
                    AppExecutors.getInstance().diskIO().execute(() -> {
                        com.shashank.expensemanager.transactionDb.PersonalityEntry fetched = mAppDb.aiDao().getPersonality();
                        if (fetched != null && fetched.getBadges() != null && !fetched.getBadges().isEmpty()) {
                            AppExecutors.getInstance().mainThread().execute(() -> {
                                displayPersonalityBadges(fetched.getBadges());
                            });
                        }
                    });
                });
            }
        });
    }

    private void displayPersonalityBadges(String insight) {
        if (getContext() == null || llBadges == null) return;
        llBadges.removeAllViews();
        String[] badges = insight.split(",");
        for (String badge : badges) {
            String trimmedBadge = badge.trim();
            if (trimmedBadge.isEmpty()) continue;
            
            TextView tvBadge = new TextView(getContext());
            tvBadge.setText(trimmedBadge);
            tvBadge.setTextColor(Color.WHITE);
            tvBadge.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
            tvBadge.setPadding(32, 16, 32, 16);
            tvBadge.setBackgroundResource(R.drawable.chip_background_selected);
            
            android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 16, 0);
            tvBadge.setLayoutParams(params);
            
            llBadges.addView(tvBadge);
        }
    }

    private void generateNewPersonality() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, -1);
            long startOfLastMonth = cal.getTimeInMillis();
            long now = System.currentTimeMillis();

            List<TransactionEntry> transactions = mAppDb.transactionDao().getTransactionsByDateRange(startOfLastMonth, now);
            
            if (transactions == null || transactions.isEmpty() || transactions.size() < 3) {
                return;
            }

            StringBuilder contextBuilder = new StringBuilder();
            SimpleDateFormat sdf = new SimpleDateFormat("E, dd/MM", Locale.getDefault());
            for (int i = 0; i < Math.min(transactions.size(), 30); i++) {
                TransactionEntry t = transactions.get(i);
                if (t.getTransactionType().equals("Expense")) {
                    contextBuilder.append(t.getCategory()).append(" (₹").append(t.getAmount()).append(") on ").append(sdf.format(t.getDate())).append(", ");
                }
            }

            String prompt = "You are a behavioral financial analyst. Here are the user's recent expenses: " + contextBuilder.toString() + 
                            "\nBased on their spending habits (amount, category, day of week), give me EXACTLY 3 short 'Financial Personality' badges for this user, separated by completely plain commas (no quotes, no bullet points). " +
                            "Examples of badges: Weekend Spender, Die-hard Foodie, Cautious Saver, Impulsive Shopper, Travel Enthusiast. " +
                            "Return ONLY the comma-separated string, nothing else.";

            AppExecutors.getInstance().mainThread().execute(() -> {
                groqApiClient.generateInsights(prompt, new GroqApiClient.GroqCallback() {
                    @Override
                    public void onSuccess(String insight) {
                        displayPersonalityBadges(insight);
                        com.shashank.expensemanager.network.BackendSync.updatePersonality(getContext(), insight, null);
                    }

                    @Override
                    public void onError(String error) {
                    }
                });
            });
        });
    }
}

