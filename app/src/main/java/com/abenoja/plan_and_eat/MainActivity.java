package com.abenoja.plan_and_eat; // ← Change to your actual package name

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
 * MainActivity.java  (updated — Step 2)
 * ─────────────────────────────────────────────────────────────────────
 * Changes from Step 1:
 *
 *   • fabAddMeal now launches AddPlanActivity via an Intent instead of
 *     showing an AlertDialog placeholder.
 *
 *   • Uses the modern ActivityResultLauncher API (replaces the deprecated
 *     startActivityForResult / onActivityResult pattern). This is the
 *     recommended, thread-safe approach from Jetpack Activity 1.2+.
 *
 *   • onAddPlanResult() reads the Meal extras from the result Intent and
 *     inserts the new Meal into the RecyclerView adapter.
 *
 *   • Budget is recalculated and displayed after every successful save.
 * ─────────────────────────────────────────────────────────────────────
 */
public class MainActivity extends AppCompatActivity {

    // ── View references ───────────────────────────────────────────────
    private RecyclerView         recyclerMeals;
    private FloatingActionButton fabAddMeal;
    private BottomNavigationView bottomNavigationView;
    private TextView             tvRemainingBudget;
    private TextView             tvDate;
    private LinearLayout         layoutEmptyState;

    // ── Data & Adapter ────────────────────────────────────────────────
    private MealAdapter mealAdapter;
    private List<Meal>  mealList;

    // ── Budget state ──────────────────────────────────────────────────
    // In a later step this will come from SharedPreferences / a database.
    // For now it is a static starting value for UI testing.
    private double totalBudget = 500.00; // user's set budget (placeholder)
    private double spentSoFar  = 0.00;   // sum of all non-homemade meal costs

    // ── ActivityResultLauncher ────────────────────────────────────────
    /**
     * Modern replacement for startActivityForResult().
     *
     * Register the launcher BEFORE onCreate() runs (field-level or in
     * onCreate before setContentView) — registering it after the Activity
     * starts will throw an IllegalStateException.
     *
     * The lambda receives an ActivityResult which carries:
     *   • getResultCode() — RESULT_OK or RESULT_CANCELED
     *   • getData()       — the result Intent from AddPlanActivity
     */
    private final ActivityResultLauncher<Intent> addPlanLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> onAddPlanResult(result)   // called on the main thread ✓
            );

    // ─────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        displayTodayDate();
        updateBudgetDisplay();
        setupRecyclerView();
        setupBottomNavigation();
        setupFab();             // ← now launches AddPlanActivity
    }

    // ════════════════════════════════════════════════════════════════
    // 1. UI INITIALISATION
    // ════════════════════════════════════════════════════════════════

    /** Binds every View in activity_main.xml to a Java field. */
    private void initViews() {
        recyclerMeals        = findViewById(R.id.recyclerMeals);
        fabAddMeal           = findViewById(R.id.fabAddMeal);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        tvRemainingBudget    = findViewById(R.id.tvRemainingBudget);
        tvDate               = findViewById(R.id.tvDate);
        layoutEmptyState     = findViewById(R.id.layoutEmptyState);
    }

    /** Formats and displays today's date in the subtitle TextView. */
    private void displayTodayDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM dd yyyy", Locale.getDefault());
        tvDate.setText(sdf.format(new Date()));
    }

    /**
     * Recalculates the remaining budget and refreshes the display.
     *
     * Formula:  remaining = totalBudget − spentSoFar
     * Homemade meals contribute 0 to spentSoFar, so they never
     * reduce the remaining budget.
     */
    private void updateBudgetDisplay() {
        double remaining = totalBudget - spentSoFar;
        String formatted = String.format(Locale.getDefault(), "P%.2f", remaining);
        tvRemainingBudget.setText(formatted);
    }

    // ════════════════════════════════════════════════════════════════
    // 2. BOTTOM NAVIGATION LOGIC
    // ════════════════════════════════════════════════════════════════

    /**
     * Sets up the BottomNavigationView item-selection listener.
     * Toast placeholders — replace with Fragment transactions in Step 3.
     */
    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_plan);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_plan) {
                // TODO (Step 3): Replace with PlanFragment transaction
                Toast.makeText(this, "Viewing your Meal Plan", Toast.LENGTH_SHORT).show();
                return true;

            } else if (itemId == R.id.nav_past_records) {
                // TODO (Step 3): Replace with PastRecordsFragment transaction
                Toast.makeText(this, "Navigating to Archive", Toast.LENGTH_SHORT).show();
                return true;
            }

            return false;
        });
    }

    // ════════════════════════════════════════════════════════════════
    // 3. FAB — Launch AddPlanActivity
    // ════════════════════════════════════════════════════════════════

    /**
     * Replaces the Step 1 AlertDialog placeholder.
     *
     * Clicking the FAB creates an explicit Intent pointing at
     * AddPlanActivity and launches it through the ActivityResultLauncher
     * so we can receive the new Meal data when the user presses "Save Plan".
     */
    private void setupFab() {
        fabAddMeal.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddPlanActivity.class);
            // Optional: pass contextual data to AddPlanActivity in future steps
            // intent.putExtra("current_budget", totalBudget - spentSoFar);
            addPlanLauncher.launch(intent);
        });
    }

    // ════════════════════════════════════════════════════════════════
    // 4. ACTIVITY RESULT HANDLER
    // ════════════════════════════════════════════════════════════════

    /**
     * Invoked by {@code addPlanLauncher} on the main thread after
     * AddPlanActivity finishes.
     *
     * RESULT_OK     → extract Meal extras, add to list, update budget
     * RESULT_CANCELED → user pressed Cancel; do nothing
     *
     * This method is always called on the main (UI) thread by the
     * ActivityResultLauncher, so direct View manipulation is safe here.
     *
     * @param result The ActivityResult containing resultCode and data Intent.
     */
    private void onAddPlanResult(ActivityResult result) {
        if (result.getResultCode() != RESULT_OK || result.getData() == null) {
            // User cancelled or something went wrong — silently ignore
            return;
        }

        Intent data = result.getData();

        // ── Extract extras using AddPlanActivity's public keys ─────────
        String  mealName   = data.getStringExtra(AddPlanActivity.EXTRA_MEAL_NAME);
        String  category   = data.getStringExtra(AddPlanActivity.EXTRA_MEAL_CATEGORY);
        double  cost       = data.getDoubleExtra(AddPlanActivity.EXTRA_MEAL_COST, 0.0);
        boolean isHomemade = data.getBooleanExtra(AddPlanActivity.EXTRA_IS_HOMEMADE, false);

        // ── Guard against malformed data ───────────────────────────────
        if (mealName == null || mealName.isEmpty()) {
            Toast.makeText(this, "Could not read meal data.", Toast.LENGTH_SHORT).show();
            return;
        }

        // ── Build the Meal object and add it to the list ───────────────
        Meal newMeal = new Meal(mealName, cost, isHomemade);
        addMealToList(newMeal);

        // ── Update the budget tracker ──────────────────────────────────
        // Homemade meals have cost = 0.0, so they never reduce the budget
        spentSoFar += cost;
        updateBudgetDisplay();

        // ── Confirm to the user ────────────────────────────────────────
        String confirmation = category + " added: " + mealName;
        Toast.makeText(this, confirmation, Toast.LENGTH_SHORT).show();
    }

    // ════════════════════════════════════════════════════════════════
    // 5. RECYCLERVIEW SETUP
    // ════════════════════════════════════════════════════════════════

    /** Initialises the RecyclerView with a LinearLayoutManager and dummy data. */
    private void setupRecyclerView() {
        mealList = buildDummyMeals();

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerMeals.setLayoutManager(layoutManager);
        recyclerMeals.setItemAnimator(null); // avoids flicker on updates

        mealAdapter = new MealAdapter(this, mealList);
        recyclerMeals.setAdapter(mealAdapter);

        mealAdapter.setOnMealClickListener((meal, position) ->
                        Toast.makeText(this, "Tapped: " + meal.getMealName(), Toast.LENGTH_SHORT).show()
                // TODO (Step 3): Open EditMealBottomSheet
        );

        // Sync the dummy data cost into spentSoFar so the budget is accurate
        for (Meal meal : mealList) {
            if (!meal.isHomemade()) {
                spentSoFar += meal.getCost();
            }
        }
        updateBudgetDisplay();
        updateEmptyState();
    }

    // ════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ════════════════════════════════════════════════════════════════

    /**
     * Adds a new Meal to the adapter, scrolls to it, and refreshes
     * the empty-state visibility.
     *
     * @param meal The Meal to insert at the end of the list.
     */
    private void addMealToList(Meal meal) {
        mealAdapter.addMeal(meal);
        recyclerMeals.smoothScrollToPosition(mealList.size() - 1);
        updateEmptyState();
    }

    /**
     * Shows the RecyclerView or the empty-state layout depending on
     * whether the meal list has any items.
     */
    private void updateEmptyState() {
        boolean isEmpty = mealList.isEmpty();
        recyclerMeals.setVisibility(isEmpty ? View.GONE  : View.VISIBLE);
        layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    /**
     * Returns a small set of hardcoded meals used during UI development.
     * Remove once real persistence (Room / SharedPreferences) is wired up.
     */
    private List<Meal> buildDummyMeals() {
        List<Meal> dummies = new ArrayList<>();
        dummies.add(new Meal("Tapsilog",             85.00,  true));
        dummies.add(new Meal("Chicken Adobo & Rice", 110.00, true));
        dummies.add(new Meal("Jollibee Chickenjoy",  149.00, false));
        return dummies;
    }
}