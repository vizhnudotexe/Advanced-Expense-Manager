package com.shashank.expensemanager.utils;

import android.content.Context;
import com.shashank.expensemanager.transactionDb.AppDatabase;
import com.shashank.expensemanager.transactionDb.AppExecutors;
import com.shashank.expensemanager.transactionDb.TransactionEntry;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class AutopayManager {

    public static void processRecurringTransactions(Context context) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            List<TransactionEntry> allTx = db.transactionDao().loadAllTransactionBlocking();
            if (allTx == null || allTx.isEmpty()) return;

            long now = System.currentTimeMillis();

            for (TransactionEntry t : allTx) {
                if (t.isRecurring()) {
                    long nextDueDate = calculateNextDueDate(t.getDate(), t.getRecurrenceType());
                    
                    if (now >= nextDueDate) {
                        // Mark the original transaction as no longer recurring
                        t.setRecurring(false);
                        db.transactionDao().updateExpenseDetails(t);
                        
                        long iterDate = nextDueDate;
                        while(now >= iterDate) {
                            long nextNext = calculateNextDueDate(new Date(iterDate), t.getRecurrenceType());
                            boolean isLatest = now < nextNext; // Make the last copied one the new recurring anchor
                            
                            String newDesc = t.getDescription();
                            if (!newDesc.contains("(Autopay)")) {
                                newDesc += " (Autopay)";
                            }
                            
                            TransactionEntry newTx = new TransactionEntry(
                                    t.getAmount(), t.getCategory(), newDesc,
                                    new Date(iterDate), t.getTransactionType(), 
                                    t.getWalletType(), isLatest, t.getRecurrenceType()
                            );
                            
                            db.transactionDao().insertExpense(newTx);
                            com.shashank.expensemanager.network.BackendSync.addTransaction(context, newTx, null);
                            
                            iterDate = nextNext;
                        }
                    }
                }
            }
        });
    }

    private static long calculateNextDueDate(Date originalDate, String recurrenceType) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(originalDate);
        if ("Daily".equalsIgnoreCase(recurrenceType)) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        } else if ("Weekly".equalsIgnoreCase(recurrenceType)) {
            cal.add(Calendar.WEEK_OF_YEAR, 1);
        } else if ("Monthly".equalsIgnoreCase(recurrenceType)) {
            cal.add(Calendar.MONTH, 1);
        } else {
            cal.add(Calendar.YEAR, 100);
        }
        return cal.getTimeInMillis();
    }
}
