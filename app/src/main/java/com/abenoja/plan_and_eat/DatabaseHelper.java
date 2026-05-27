package com.abenoja.plan_and_eat;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * DatabaseHelper.java  (v2 — budget carry-over + archive)
 * ─────────────────────────────────────────────────────────────────────
 * New in this version:
 *
 *   • Budget carry-over  — if no budget row exists for today, the
 *     remaining balance from the most recent previous day is automatically
 *     carried over and seeded as today's starting budget.
 *
 *   • setDailyBudget()   — sets (or overwrites) today's budget.
 *
 *   • getDailyArchive()  — returns one DailySummary per day, containing
 *     the meal list, original budget, total cost, and closing balance.
 * ─────────────────────────────────────────────────────────────────────
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    // ── Database metadata ─────────────────────────────────────────────
    private static final String DB_NAME    = "plan_and_eat.db";
    private static final int    DB_VERSION = 2;     // bumped from v1

    // ══════════════════════════════════════════════════════════════════
    // TABLE: meals
    // ══════════════════════════════════════════════════════════════════
    public static final String TABLE_MEALS          = "meals";
    public static final String COL_MEAL_ID          = "id";
    public static final String COL_MEAL_CATEGORY    = "category";
    public static final String COL_MEAL_NAME        = "meal_name";
    public static final String COL_MEAL_COST        = "cost";
    public static final String COL_MEAL_IS_HOMEMADE = "is_homemade";
    public static final String COL_MEAL_DATE        = "meal_date";
    public static final String COL_MEAL_CREATED_AT  = "created_at";

    private static final String CREATE_TABLE_MEALS =
            "CREATE TABLE " + TABLE_MEALS + " ("
                    + COL_MEAL_ID          + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COL_MEAL_CATEGORY    + " TEXT    NOT NULL, "
                    + COL_MEAL_NAME        + " TEXT    NOT NULL, "
                    + COL_MEAL_COST        + " REAL    NOT NULL DEFAULT 0.0, "
                    + COL_MEAL_IS_HOMEMADE + " INTEGER NOT NULL DEFAULT 0, "
                    + COL_MEAL_DATE        + " TEXT    NOT NULL, "
                    + COL_MEAL_CREATED_AT  + " TEXT    NOT NULL"
                    + ");";

    // ══════════════════════════════════════════════════════════════════
    // TABLE: budget
    // ══════════════════════════════════════════════════════════════════
    public static final String TABLE_BUDGET          = "budget";
    public static final String COL_BUDGET_ID         = "id";
    public static final String COL_BUDGET_DATE       = "budget_date";
    public static final String COL_BUDGET_TOTAL      = "total_budget";
    public static final String COL_BUDGET_CREATED_AT = "created_at";

    private static final String CREATE_TABLE_BUDGET =
            "CREATE TABLE " + TABLE_BUDGET + " ("
                    + COL_BUDGET_ID         + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COL_BUDGET_DATE       + " TEXT    NOT NULL UNIQUE, "
                    + COL_BUDGET_TOTAL      + " REAL    NOT NULL DEFAULT 500.0, "
                    + COL_BUDGET_CREATED_AT + " TEXT    NOT NULL"
                    + ");";

    // ─────────────────────────────────────────────────────────────────
    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_MEALS);
        db.execSQL(CREATE_TABLE_BUDGET);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // v1 → v2: schema is the same, nothing to migrate structurally.
        // If upgrading from v1, tables already exist — just ensure the
        // carry-over logic runs on the next app open.
        if (oldVersion < 2) {
            // No structural change needed; carry-over is purely runtime logic.
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════════

    public static String getToday() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date());
    }

    private static String nowTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());
    }

    // ══════════════════════════════════════════════════════════════════
    // BUDGET — CARRY-OVER + SET
    // ══════════════════════════════════════════════════════════════════

    /**
     * Call this once in MainActivity.onCreate().
     *
     * Logic:
     *   1. If a budget row already exists for today → do nothing (user
     *      may have already set it manually).
     *   2. If no row exists → look up the remaining balance from the
     *      most recent previous day and seed it as today's budget.
     *   3. If there are no previous days at all → seed 500.00.
     *
     * This means:
     *   • Unspent budget rolls forward automatically every day.
     *   • If you spent everything, tomorrow starts at ₱0 — not negative.
     *   • The user can override at any time with setDailyBudget().
     */
    public void initTodayBudget() {
        String today = getToday();
        if (budgetExistsForDate(today)) return;   // already set today

        double carryOver = getMostRecentRemainingBalance(today);
        setDailyBudgetForDate(Math.max(carryOver, 0.0), today);
    }

    /** Returns true if a budget row already exists for the given date. */
    private boolean budgetExistsForDate(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT 1 FROM " + TABLE_BUDGET
                        + " WHERE " + COL_BUDGET_DATE + " = ? LIMIT 1",
                new String[]{ date });
        boolean exists = cursor.moveToFirst();
        cursor.close();
        db.close();
        return exists;
    }

    /**
     * Finds the most recent budget row strictly before {@code beforeDate}
     * and returns (total_budget − spent) for that day.
     * Returns 500.00 if no previous day exists.
     */
    private double getMostRecentRemainingBalance(String beforeDate) {
        SQLiteDatabase db = this.getReadableDatabase();

        // Join budget with meals to compute remaining in one query
        String query =
                "SELECT b." + COL_BUDGET_TOTAL
                        + ", COALESCE(SUM(m." + COL_MEAL_COST + "), 0.0) AS spent"
                        + " FROM " + TABLE_BUDGET + " b"
                        + " LEFT JOIN " + TABLE_MEALS + " m"
                        + "   ON m." + COL_MEAL_DATE        + " = b." + COL_BUDGET_DATE
                        + "  AND m." + COL_MEAL_IS_HOMEMADE + " = 0"
                        + " WHERE b." + COL_BUDGET_DATE + " < ?"
                        + " GROUP BY b." + COL_BUDGET_DATE
                        + " ORDER BY b." + COL_BUDGET_DATE + " DESC"
                        + " LIMIT 1";

        Cursor cursor = db.rawQuery(query, new String[]{ beforeDate });

        double remaining = 500.00;
        if (cursor.moveToFirst()) {
            double total = cursor.getDouble(0);
            double spent = cursor.getDouble(1);
            remaining = total - spent;
        }
        cursor.close();
        db.close();
        return remaining;
    }

    /**
     * Sets (or overwrites) today's budget.
     * Call this when the user manually enters a new budget amount.
     *
     * @param totalBudget New budget in Peso.
     */
    public long setDailyBudget(double totalBudget) {
        return setDailyBudgetForDate(totalBudget, getToday());
    }

    /** Sets the budget for a specific date (used internally). */
    public long setDailyBudgetForDate(double totalBudget, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_BUDGET_DATE,       date);
        values.put(COL_BUDGET_TOTAL,      totalBudget);
        values.put(COL_BUDGET_CREATED_AT, nowTimestamp());

        long rowId = db.replace(TABLE_BUDGET, null, values);
        db.close();
        return rowId;
    }

    // ══════════════════════════════════════════════════════════════════
    // BUDGET — READ
    // ══════════════════════════════════════════════════════════════════

    public double getTodayTotalBudget() {
        return getTotalBudgetForDate(getToday());
    }

    public double getTotalBudgetForDate(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_BUDGET,
                new String[]{ COL_BUDGET_TOTAL },
                COL_BUDGET_DATE + " = ?",
                new String[]{ date },
                null, null, null);

        double budget = 500.00;
        if (cursor.moveToFirst()) {
            budget = cursor.getDouble(0);
        }
        cursor.close();
        db.close();
        return budget;
    }

    public double getTodaySpent() {
        return getSpentForDate(getToday());
    }

    public double getSpentForDate(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query =
                "SELECT COALESCE(SUM(" + COL_MEAL_COST + "), 0.0)"
                        + " FROM " + TABLE_MEALS
                        + " WHERE " + COL_MEAL_DATE        + " = ?"
                        + "   AND " + COL_MEAL_IS_HOMEMADE + " = 0";

        Cursor cursor = db.rawQuery(query, new String[]{ date });
        double spent = 0.0;
        if (cursor.moveToFirst()) spent = cursor.getDouble(0);
        cursor.close();
        db.close();
        return spent;
    }

    public double getRemainingBudget() {
        return getTodayTotalBudget() - getTodaySpent();
    }

    // ══════════════════════════════════════════════════════════════════
    // MEALS — INSERT
    // ══════════════════════════════════════════════════════════════════

    public long insertMeal(String category, String mealName,
                           double cost, boolean isHomemade) {
        return insertMealOnDate(category, mealName, cost, isHomemade, getToday());
    }

    public long insertMealOnDate(String category, String mealName,
                                 double cost, boolean isHomemade, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_MEAL_CATEGORY,    category);
        values.put(COL_MEAL_NAME,        mealName);
        values.put(COL_MEAL_COST,        cost);
        values.put(COL_MEAL_IS_HOMEMADE, isHomemade ? 1 : 0);
        values.put(COL_MEAL_DATE,        date);
        values.put(COL_MEAL_CREATED_AT,  nowTimestamp());

        long rowId = db.insert(TABLE_MEALS, null, values);
        db.close();
        return rowId;
    }

    // ══════════════════════════════════════════════════════════════════
    // MEALS — READ
    // ══════════════════════════════════════════════════════════════════

    public List<MealRecord> getMealsByDate(String date) {
        List<MealRecord> records = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(
                TABLE_MEALS, null,
                COL_MEAL_DATE + " = ?", new String[]{ date },
                null, null, COL_MEAL_CREATED_AT + " ASC");

        if (cursor.moveToFirst()) {
            do { records.add(cursorToMealRecord(cursor)); }
            while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return records;
    }

    public List<MealRecord> getAllMeals() {
        List<MealRecord> records = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(
                TABLE_MEALS, null, null, null, null, null,
                COL_MEAL_DATE + " DESC, " + COL_MEAL_CREATED_AT + " DESC");

        if (cursor.moveToFirst()) {
            do { records.add(cursorToMealRecord(cursor)); }
            while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return records;
    }

    public List<Meal> getTodayMealsAsMeal() {
        List<Meal> meals = new ArrayList<>();
        for (MealRecord r : getMealsByDate(getToday())) {
            meals.add(new Meal(r.mealName, r.cost, r.isHomemade));
        }
        return meals;
    }

    // ══════════════════════════════════════════════════════════════════
    // MEALS — UPDATE / DELETE
    // ══════════════════════════════════════════════════════════════════

    public int updateMeal(long id, String category, String mealName,
                          double cost, boolean isHomemade) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_MEAL_CATEGORY,    category);
        values.put(COL_MEAL_NAME,        mealName);
        values.put(COL_MEAL_COST,        cost);
        values.put(COL_MEAL_IS_HOMEMADE, isHomemade ? 1 : 0);

        int rows = db.update(TABLE_MEALS, values,
                COL_MEAL_ID + " = ?", new String[]{ String.valueOf(id) });
        db.close();
        return rows;
    }

    public int deleteMeal(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_MEALS,
                COL_MEAL_ID + " = ?", new String[]{ String.valueOf(id) });
        db.close();
        return rows;
    }

    // ══════════════════════════════════════════════════════════════════
    // ARCHIVE — getDailyArchive()
    // ══════════════════════════════════════════════════════════════════

    /**
     * Returns a list of DailySummary — one entry per calendar day that
     * has at least one meal OR a budget row, ordered newest-first.
     *
     * Each DailySummary contains:
     *   • date            — "YYYY-MM-DD"
     *   • meals           — all MealRecords for that day
     *   • originalBudget  — the total_budget set for that day
     *   • totalCost       — sum of non-homemade meal costs
     *   • budgetAfterCost — originalBudget − totalCost
     */
    public List<DailySummary> getDailyArchive() {
        List<DailySummary> archive = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Collect all distinct dates that have meals OR a budget row
        String datesQuery =
                "SELECT DISTINCT date_col FROM ("
                        + "  SELECT " + COL_MEAL_DATE   + " AS date_col FROM " + TABLE_MEALS
                        + "  UNION"
                        + "  SELECT " + COL_BUDGET_DATE + " AS date_col FROM " + TABLE_BUDGET
                        + ") ORDER BY date_col DESC";

        Cursor dateCursor = db.rawQuery(datesQuery, null);

        if (dateCursor.moveToFirst()) {
            do {
                String date = dateCursor.getString(0);
                DailySummary summary = buildDailySummary(db, date);
                archive.add(summary);
            } while (dateCursor.moveToNext());
        }

        dateCursor.close();
        db.close();
        return archive;
    }

    /** Builds a single DailySummary for the given date using an open DB. */
    private DailySummary buildDailySummary(SQLiteDatabase db, String date) {
        DailySummary summary = new DailySummary();
        summary.date = date;

        // ── Original budget for that day ──────────────────────────────
        Cursor budgetCursor = db.query(
                TABLE_BUDGET,
                new String[]{ COL_BUDGET_TOTAL },
                COL_BUDGET_DATE + " = ?",
                new String[]{ date },
                null, null, null);

        summary.originalBudget = 500.00; // fallback
        if (budgetCursor.moveToFirst()) {
            summary.originalBudget = budgetCursor.getDouble(0);
        }
        budgetCursor.close();

        // ── Meals for that day ────────────────────────────────────────
        Cursor mealCursor = db.query(
                TABLE_MEALS, null,
                COL_MEAL_DATE + " = ?", new String[]{ date },
                null, null, COL_MEAL_CREATED_AT + " ASC");

        summary.meals = new ArrayList<>();
        double totalCost = 0.0;

        if (mealCursor.moveToFirst()) {
            do {
                MealRecord r = cursorToMealRecord(mealCursor);
                summary.meals.add(r);
                if (!r.isHomemade) totalCost += r.cost;
            } while (mealCursor.moveToNext());
        }
        mealCursor.close();

        summary.totalCost       = totalCost;
        summary.budgetAfterCost = summary.originalBudget - totalCost;
        return summary;
    }

    // ══════════════════════════════════════════════════════════════════
    // PRIVATE — Cursor mapper
    // ══════════════════════════════════════════════════════════════════

    private MealRecord cursorToMealRecord(Cursor cursor) {
        MealRecord r = new MealRecord();
        r.id         = cursor.getLong  (cursor.getColumnIndexOrThrow(COL_MEAL_ID));
        r.category   = cursor.getString(cursor.getColumnIndexOrThrow(COL_MEAL_CATEGORY));
        r.mealName   = cursor.getString(cursor.getColumnIndexOrThrow(COL_MEAL_NAME));
        r.cost       = cursor.getDouble (cursor.getColumnIndexOrThrow(COL_MEAL_COST));
        r.isHomemade = cursor.getInt   (cursor.getColumnIndexOrThrow(COL_MEAL_IS_HOMEMADE)) == 1;
        r.date       = cursor.getString(cursor.getColumnIndexOrThrow(COL_MEAL_DATE));
        r.createdAt  = cursor.getString(cursor.getColumnIndexOrThrow(COL_MEAL_CREATED_AT));
        return r;
    }

    // ══════════════════════════════════════════════════════════════════
    // INNER CLASSES
    // ══════════════════════════════════════════════════════════════════

    /** Full row from the meals table — includes DB id, category, date. */
    public static class MealRecord {
        public long    id;
        public String  category;
        public String  mealName;
        public double  cost;
        public boolean isHomemade;
        public String  date;
        public String  createdAt;

        public Meal toMeal() { return new Meal(mealName, cost, isHomemade); }
    }

    /**
     * One entry in the archive — summarises a single calendar day.
     *
     * Fields:
     *   date            — "YYYY-MM-DD"
     *   meals           — every meal logged that day
     *   originalBudget  — budget set for that day (₱)
     *   totalCost       — sum of non-homemade meal costs (₱)
     *   budgetAfterCost — originalBudget − totalCost (₱)
     */
    public static class DailySummary {
        public String           date;
        public List<MealRecord> meals;
        public double           originalBudget;
        public double           totalCost;
        public double           budgetAfterCost;
    }
}