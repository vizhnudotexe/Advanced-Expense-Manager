package com.shashank.expensemanager.transactionDb;

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import android.content.Context;
import android.content.Context;
import androidx.annotation.NonNull;
import android.util.Log;

import com.shashank.expensemanager.models.ChatMessage;

@Database(entities = {TransactionEntry.class, BudgetEntry.class, ChatMessage.class, PersonalityEntry.class}, version = 5, exportSchema = false)
@TypeConverters(DateConverter.class)
public abstract class AppDatabase extends RoomDatabase {

    private static final String LOG_TAG = AppDatabase.class.getSimpleName();
    private static final Object LOCK = new Object();
    private static final String DATABASE_NAME = "TransactionDb";
    private static AppDatabase sInstance;

    // Migration from version 1 to 2: add walletType, isRecurring, recurrenceType columns
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE transactionTable ADD COLUMN walletType TEXT NOT NULL DEFAULT 'Cash'");
            database.execSQL("ALTER TABLE transactionTable ADD COLUMN isRecurring INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE transactionTable ADD COLUMN recurrenceType TEXT NOT NULL DEFAULT ''");
        }
    };

    // Migration from version 2 to 3: fix NOT NULL constraints on walletType and recurrenceType
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Recreate the table with proper NOT NULL constraints
            database.execSQL("CREATE TABLE transactionTable_new ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "amount INTEGER NOT NULL, "
                    + "category TEXT, "
                    + "description TEXT, "
                    + "date INTEGER, "
                    + "transactionType TEXT, "
                    + "walletType TEXT NOT NULL DEFAULT 'Cash', "
                    + "isRecurring INTEGER NOT NULL DEFAULT 0, "
                    + "recurrenceType TEXT NOT NULL DEFAULT '')");
            database.execSQL("INSERT INTO transactionTable_new (id, amount, category, description, date, transactionType, walletType, isRecurring, recurrenceType) "
                    + "SELECT id, amount, category, description, date, transactionType, "
                    + "COALESCE(walletType, 'Cash'), isRecurring, COALESCE(recurrenceType, '') "
                    + "FROM transactionTable");
            database.execSQL("DROP TABLE transactionTable");
            database.execSQL("ALTER TABLE transactionTable_new RENAME TO transactionTable");
        }
    };

    // Migration from version 3 to 4: add budgetTable
    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS budgetTable ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "category TEXT, "
                    + "amount INTEGER NOT NULL, "
                    + "monthYear TEXT)");
        }
    };

    // Migration from version 4 to 5: add chatMessageTable and personalityTable
    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS chatMessageTable ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "text TEXT, "
                    + "isUser INTEGER NOT NULL)");
            database.execSQL("CREATE TABLE IF NOT EXISTS personalityTable ("
                    + "id INTEGER PRIMARY KEY NOT NULL, "
                    + "badges TEXT)");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (sInstance == null) {
            synchronized (LOCK) {
                Log.d(LOG_TAG, "Creating new database instance");
                sInstance = Room.databaseBuilder(context.getApplicationContext(),
                        AppDatabase.class, AppDatabase.DATABASE_NAME)
                        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                        .fallbackToDestructiveMigration()
                        .build();
            }
        }
        Log.d(LOG_TAG, "Getting the database instance");
        return sInstance;
    }

    public abstract TransactionDao transactionDao();
    public abstract BudgetDao budgetDao();
    public abstract AiDao aiDao();
}
