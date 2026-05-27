package com.abenoja.plan_and_eat;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * MainActivity.java  (v3 — set budget dialog + archive navigation)
 */
public class MainActivity extends AppCompatActivity {

    // ── Views ─────────────────────────────────────────────────────────
    private RecyclerView         recyclerMeals;
    private FloatingActionButton fabAddMeal;
    private BottomNavigationView bottomNavigationView;
    private TextView             tvRemainingBudget;
    private TextView             tvDate;
    private LinearLayout         layoutEmptyState;

    // ── Data ──────────────────────────────────────────────────────────
    private MealAdapter mealAdapter;
    private List<Meal>  mealList;

    // ── Database ──────────────────────────────────────────────────────
    private DatabaseHelper db;

    // ── Launcher ──────────────────────────────────────────────────────
    private final ActivityResultLauncher<Intent> addPlanLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> onAddPlanResult(result)
            );

    // ─────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new DatabaseHelper(this);
        db.initTodayBudget();       // carry-over or seed default

        initViews();
        displayTodayDate();
        setupRecyclerView();
        updateBudgetDisplay();
        setupBottomNavigation();
        setupFab();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshTodayMeals();
        updateBudgetDisplay();
    }

    @Override
    protected void onDestroy() {
        if (db != null) db.close();
        super.onDestroy();
    }

    // ════════════════════════════════════════════════════════════════
    // VIEWS
    // ════════════════════════════════════════════════════════════════

    private void initViews() {
        recyclerMeals        = findViewById(R.id.recyclerMeals);
        fabAddMeal           = findViewById(R.id.fabAddMeal);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        tvRemainingBudget    = findViewById(R.id.tvRemainingBudget);
        tvDate               = findViewById(R.id.tvDate);
        layoutEmptyState     = findViewById(R.id.layoutEmptyState);

        // Tap the budget amount to edit it
        tvRemainingBudget.setOnClickListener(v -> showSetBudgetDialog());
    }

    private void displayTodayDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM dd yyyy", Locale.getDefault());
        tvDate.setText(sdf.format(new Date()));
    }

    // ════════════════════════════════════════════════════════════════
    // BUDGET
    // ════════════════════════════════════════════════════════════════

    private void updateBudgetDisplay() {
        double remaining = db.getRemainingBudget();
        String formatted = String.format(Locale.getDefault(), "P%.2f", remaining);
        tvRemainingBudget.setText(formatted);
    }

    /**
     * Shows an AlertDialog with a numeric input so the user can set
     * today's budget manually.
     *
     * Pre-fills the current total budget so the user knows the starting value.
     */
    private void showSetBudgetDialog() {
        double currentBudget = db.getTodayTotalBudget();

        // Build a simple EditText for the number input
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(String.format(Locale.getDefault(), "%.2f", currentBudget));
        input.setSelectAllOnFocus(true);

        // Add padding so the EditText doesn't touch the dialog edges
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, 0, padding, 0);
        container.addView(input);

        new AlertDialog.Builder(this)
                .setTitle("Set Today's Budget")
                .setMessage("Enter your budget for today (₱):")
                .setView(container)
                .setPositiveButton("Save", (dialog, which) -> {
                    String raw = input.getText().toString().trim();
                    if (raw.isEmpty()) {
                        Toast.makeText(this, "Please enter a budget amount.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        double newBudget = Double.parseDouble(raw);
                        if (newBudget < 0) {
                            Toast.makeText(this, "Budget cannot be negative.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        db.setDailyBudget(newBudget);
                        updateBudgetDisplay();
                        Toast.makeText(this,
                                String.format(Locale.getDefault(), "Budget set to ₱%.2f", newBudget),
                                Toast.LENGTH_SHORT).show();
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Invalid amount. Enter a number like 500 or 350.50.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ════════════════════════════════════════════════════════════════
    // BOTTOM NAVIGATION
    // ════════════════════════════════════════════════════════════════

    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_plan);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_plan) {
                return true;

            } else if (itemId == R.id.nav_past_records) {
                // Open the archive screen
                Intent intent = new Intent(MainActivity.this, ArchiveActivity.class);
                startActivity(intent);
                return true;
            }

            return false;
        });
    }

    // ════════════════════════════════════════════════════════════════
    // FAB
    // ════════════════════════════════════════════════════════════════

    private void setupFab() {
        fabAddMeal.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddPlanActivity.class);
            addPlanLauncher.launch(intent);
        });
    }

    // ════════════════════════════════════════════════════════════════
    // ACTIVITY RESULT
    // ════════════════════════════════════════════════════════════════

    private void onAddPlanResult(ActivityResult result) {
        if (result.getResultCode() != RESULT_OK || result.getData() == null) return;

        Intent data = result.getData();

        String  mealName   = data.getStringExtra(AddPlanActivity.EXTRA_MEAL_NAME);
        String  category   = data.getStringExtra(AddPlanActivity.EXTRA_MEAL_CATEGORY);
        double  cost       = data.getDoubleExtra(AddPlanActivity.EXTRA_MEAL_COST, 0.0);
        boolean isHomemade = data.getBooleanExtra(AddPlanActivity.EXTRA_IS_HOMEMADE, false);

        if (mealName == null || mealName.isEmpty()) {
            Toast.makeText(this, "Could not read meal data.", Toast.LENGTH_SHORT).show();
            return;
        }

        long rowId = db.insertMeal(category, mealName, cost, isHomemade);
        if (rowId == -1) {
            Toast.makeText(this, "Failed to save meal.", Toast.LENGTH_SHORT).show();
            return;
        }

        addMealToList(new Meal(mealName, cost, isHomemade));
        updateBudgetDisplay();

        Toast.makeText(this, category + " added: " + mealName, Toast.LENGTH_SHORT).show();
    }

    // ════════════════════════════════════════════════════════════════
    // RECYCLERVIEW
    // ════════════════════════════════════════════════════════════════

    private void setupRecyclerView() {
        mealList = db.getTodayMealsAsMeal();
        if (mealList == null) mealList = new ArrayList<>();

        recyclerMeals.setLayoutManager(new LinearLayoutManager(this));
        recyclerMeals.setItemAnimator(null);

        mealAdapter = new MealAdapter(this, mealList);
        recyclerMeals.setAdapter(mealAdapter);

        mealAdapter.setOnMealClickListener((meal, position) ->
                Toast.makeText(this, "Tapped: " + meal.getMealName(), Toast.LENGTH_SHORT).show());

        updateEmptyState();
    }

    private void refreshTodayMeals() {
        List<Meal> fresh = db.getTodayMealsAsMeal();
        if (fresh == null) fresh = new ArrayList<>();
        mealList = fresh;
        mealAdapter.updateList(fresh);
        updateEmptyState();
    }

    private void addMealToList(Meal meal) {
        mealAdapter.addMeal(meal);
        recyclerMeals.smoothScrollToPosition(mealList.size() - 1);
        updateEmptyState();
    }

    private void updateEmptyState() {
        boolean isEmpty = (mealList == null || mealList.isEmpty());
        recyclerMeals.setVisibility(isEmpty ? View.GONE  : View.VISIBLE);
        layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }
}