package com.abenoja.plan_and_eat;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
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
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // ── Views ─────────────────────────────────────────────────────────
    private RecyclerView         recyclerMeals;
    private FloatingActionButton fabAddMeal;
    private BottomNavigationView bottomNavigationView;
    private TextView             tvRemainingBudget;
    private TextView             tvDate;
    private LinearLayout         layoutEmptyState;
    private Chip                 chipBudgetStatus;

    // ── Data ──────────────────────────────────────────────────────────
    private MealAdapter                          mealAdapter;
    private List<DatabaseHelper.MealRecord>      mealList;

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
        db.initTodayBudget();

        hideSystemNavigation();
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
        bottomNavigationView.post(() ->
                bottomNavigationView.setSelectedItemId(R.id.nav_plan));

        refreshTodayMeals();
        updateBudgetDisplay();
    }

    @Override
    protected void onDestroy() {
        if (db != null) db.close();
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemNavigation();
        }
    }

    /**
     * Hides the three-button navigation bar in immersive sticky mode.
     * Swiping up from the bottom temporarily reveals it, then it auto-hides.
     */
    private void hideSystemNavigation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            //noinspection deprecation
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
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
        chipBudgetStatus     = findViewById(R.id.chipBudgetStatus);

        chipBudgetStatus.setOnClickListener(v -> showSetBudgetDialog());
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
        tvRemainingBudget.setText(
                String.format(Locale.getDefault(), "P%.2f", remaining));
    }

    private void showSetBudgetDialog() {
        double currentBudget = db.getTodayTotalBudget();

        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(String.format(Locale.getDefault(), "%.2f", currentBudget));
        input.setSelectAllOnFocus(true);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, 0, pad, 0);
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
                        Toast.makeText(this, "Invalid amount.", Toast.LENGTH_SHORT).show();
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
            int id = item.getItemId();

            if (id == R.id.nav_plan) {
                return true;

            } else if (id == R.id.nav_past_records) {
                Intent intent = new Intent(MainActivity.this, ArchiveActivity.class);
                startActivity(intent);
                bottomNavigationView.post(() ->
                        bottomNavigationView.setSelectedItemId(R.id.nav_plan));
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
    // ACTIVITY RESULT (add new meal)
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

        DatabaseHelper.MealRecord record = new DatabaseHelper.MealRecord();
        record.id         = rowId;
        record.category   = category;
        record.mealName   = mealName;
        record.cost       = cost;
        record.isHomemade = isHomemade;

        mealAdapter.addRecord(record);
        mealList.add(record);
        recyclerMeals.smoothScrollToPosition(mealList.size() - 1);
        updateBudgetDisplay();
        updateEmptyState();

        Toast.makeText(this, category + " added: " + mealName, Toast.LENGTH_SHORT).show();
    }

    // ════════════════════════════════════════════════════════════════
    // RECYCLERVIEW
    // ════════════════════════════════════════════════════════════════

    private void setupRecyclerView() {
        mealList = db.getTodayMealRecords();
        if (mealList == null) mealList = new ArrayList<>();

        recyclerMeals.setLayoutManager(new LinearLayoutManager(this));
        recyclerMeals.setItemAnimator(null);

        mealAdapter = new MealAdapter(this, mealList);
        recyclerMeals.setAdapter(mealAdapter);

        mealAdapter.setOnMealEditListener((record, position) ->
                showEditMealDialog(record, position));

        mealAdapter.setOnMealDeleteListener((record, position) ->
                showDeleteConfirmDialog(record, position));

        updateEmptyState();
    }

    private void refreshTodayMeals() {
        List<DatabaseHelper.MealRecord> fresh = db.getTodayMealRecords();
        if (fresh == null) fresh = new ArrayList<>();
        mealList = fresh;
        mealAdapter.updateList(fresh);
        updateEmptyState();
    }

    private void updateEmptyState() {
        boolean isEmpty = (mealList == null || mealList.isEmpty());
        recyclerMeals.setVisibility(isEmpty ? View.GONE  : View.VISIBLE);
        layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    // ════════════════════════════════════════════════════════════════
    // EDIT MEAL DIALOG
    // ════════════════════════════════════════════════════════════════

    private void showEditMealDialog(DatabaseHelper.MealRecord record, int position) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad / 2, pad, 0);

        TextInputLayout tilName = new TextInputLayout(this);
        tilName.setHint("Meal name");
        TextInputEditText etName = new TextInputEditText(this);
        etName.setText(record.mealName);
        tilName.addView(etName);
        layout.addView(tilName);

        TextInputLayout tilCost = new TextInputLayout(this);
        tilCost.setHint("Cost (₱)");
        TextInputEditText etCost = new TextInputEditText(this);
        etCost.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etCost.setText(String.format(Locale.getDefault(), "%.2f", record.cost));
        tilCost.addView(etCost);
        layout.addView(tilCost);

        CheckBox cbHomemade = new CheckBox(this);
        cbHomemade.setText("Homemade");
        cbHomemade.setChecked(record.isHomemade);
        int cbPad = (int) (4 * getResources().getDisplayMetrics().density);
        cbHomemade.setPadding(0, cbPad * 3, 0, 0);
        layout.addView(cbHomemade);

        new AlertDialog.Builder(this)
                .setTitle("Edit Meal")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = etName.getText() != null
                            ? etName.getText().toString().trim() : "";
                    String rawCost = etCost.getText() != null
                            ? etCost.getText().toString().trim() : "0";
                    boolean newHomemade = cbHomemade.isChecked();

                    if (newName.isEmpty()) {
                        Toast.makeText(this, "Meal name cannot be empty.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double newCost;
                    try {
                        newCost = Double.parseDouble(rawCost);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Invalid cost amount.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    db.updateMeal(record.id, record.category, newName, newCost, newHomemade);

                    record.mealName   = newName;
                    record.cost       = newCost;
                    record.isHomemade = newHomemade;
                    mealAdapter.updateAt(position, record);

                    updateBudgetDisplay();
                    Toast.makeText(this, "Meal updated.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ════════════════════════════════════════════════════════════════
    // DELETE CONFIRMATION
    // ════════════════════════════════════════════════════════════════

    private void showDeleteConfirmDialog(DatabaseHelper.MealRecord record, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Meal")
                .setMessage("Remove \"" + record.mealName + "\" from today's plan?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.deleteMeal(record.id);
                    // removeAt handles removal from the shared list + RecyclerView animation
                    mealAdapter.removeAt(position);
                    updateBudgetDisplay();
                    updateEmptyState();
                    Toast.makeText(this, "Meal deleted.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}