package com.abenoja.plan_and_eat; // ← Change to your actual package name

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Build;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * AddPlanActivity.java
 * ─────────────────────────────────────────────────────────────────────
 * Secondary screen for creating a new meal plan entry.
 *
 * Conflict fixes applied (vs previous version):
 *
 *   FIX 1 — Homemade checkbox: now disables/enables tilMealCost (the
 *            TextInputLayout wrapper) instead of etMealCost directly.
 *            Disabling the wrapper automatically:
 *              • Greys out the box stroke, hint, prefix, suffix
 *              • Disables the inner EditText
 *              • No manual setAlpha() hack needed
 *
 *   FIX 2 — Text clearing: uses tilMealCost.getEditText().setText()
 *            via the wrapper instead of etMealCost directly, matching
 *            how Material TextInputLayout expects text to be managed.
 *
 *   FIX 3 — Cost collection: reads from tilMealCost.getEditText()
 *            so it always gets the live inner EditText reference, even
 *            if the view hierarchy rebuilds after a rotation.
 *
 *   FIX 4 — Removed dependency on cb_homemade_selector drawable.
 *            The XML's app:buttonCompat="@drawable/cb_homemade_selector"
 *            will crash if that file doesn't exist. Java no longer
 *            expects or requires that selector — app:buttonTint alone
 *            tints the default Material checkbox amber correctly.
 * ─────────────────────────────────────────────────────────────────────
 */
public class AddPlanActivity extends AppCompatActivity {

    // ── Intent extra keys — used to pass the new Meal back to MainActivity ──
    public static final String EXTRA_MEAL_NAME     = "extra_meal_name";
    public static final String EXTRA_MEAL_CATEGORY = "extra_meal_category";
    public static final String EXTRA_MEAL_COST     = "extra_meal_cost";
    public static final String EXTRA_IS_HOMEMADE   = "extra_is_homemade";

    // ── View references ──────────────────────────────────────────────────────
    // FIX: We hold references to the TextInputLayout WRAPPERS as the source
    //      of truth for enable/disable state, not the inner EditTexts.
    private TextInputLayout      tilPlanTitle;    // wrapper — category dropdown
    private TextInputLayout      tilMealName;     // wrapper — meal name
    private TextInputLayout      tilMealCost;     // wrapper — cost (FIX 1 target)
    private AutoCompleteTextView etPlanTitle;     // inner — category selection
    private TextInputEditText    etMealName;      // inner — meal name text
    // NOTE: etMealCost is intentionally NOT stored as a field.
    //       All reads/writes go through tilMealCost.getEditText() (see FIX 2 & 3).
    private CheckBox             cbIsHomemade;    // "Homemade Meal" toggle
    private TextView             tvHomemadeBadge; // amber 🏠 badge
    private MaterialButton       btnSavePlan;     // primary action
    private MaterialButton       btnCancelPlan;   // secondary action

    // ── Meal category options (matches XML cheatsheet, extended for PH context) ──
    private static final String[] MEAL_CATEGORIES = {
            "Breakfast", "Brunch", "Lunch", "Merienda", "Dinner", "Midnight Snack"
    };

    // ────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_plan);
        hideSystemNavigation();

        initViews();
        setupCategoryDropdown();
        setupCheckboxLogic();
        setupSaveButton();
        setupCancelButton();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemNavigation();
    }

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

    // ════════════════════════════════════════════════════════════════════════
    // 1. VIEW INITIALISATION
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Binds all Views in activity_add_plan.xml to Java fields.
     *
     * IMPORTANT: Only the TextInputLayout WRAPPERS are stored for cost/name.
     * The inner EditText for cost is always accessed via getEditText() to
     * stay in sync with Material's internal state management.
     */
    private void initViews() {
        // Wrappers
        tilPlanTitle  = findViewById(R.id.tilPlanTitle);
        tilMealName   = findViewById(R.id.tilMealName);
        tilMealCost   = findViewById(R.id.tilMealCost);   // ← FIX 1: own the wrapper

        // Inner inputs
        etPlanTitle   = findViewById(R.id.etPlanTitle);
        etMealName    = findViewById(R.id.etMealName);
        // etMealCost is NOT stored — use tilMealCost.getEditText() below

        // Checkbox + badge
        cbIsHomemade    = findViewById(R.id.cbIsHomemade);
        tvHomemadeBadge = findViewById(R.id.tvHomemadeBadge);

        // Buttons
        btnSavePlan   = findViewById(R.id.btnSavePlan);
        btnCancelPlan = findViewById(R.id.btnCancelPlan);

        // Back arrow
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 2. CATEGORY DROPDOWN
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Populates the ExposedDropdownMenu with MEAL_CATEGORIES.
     * Requires res/layout/list_item_dropdown.xml (dark-themed TextView row).
     */
    private void setupCategoryDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.list_item_dropdown,
                MEAL_CATEGORIES
        );
        etPlanTitle.setAdapter(adapter);

        // Clear error the moment the user picks a valid category
        etPlanTitle.setOnItemClickListener((parent, view, position, id) ->
                tilPlanTitle.setError(null));
    }

    // ════════════════════════════════════════════════════════════════════════
    // 3. SMART CHECKBOX LOGIC
    //    FIX 1 + FIX 2: disable/enable the TIL wrapper, not the inner EditText
    // ════════════════════════════════════════════════════════════════════════

    private void setupCheckboxLogic() {
        cbIsHomemade.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                onHomemadeSelected();
            } else {
                onTakeoutSelected();
            }
        });
    }

    /**
     * User checked "Homemade Meal".
     *
     * FIX 1: tilMealCost.setEnabled(false) is the correct Material call.
     *   → It disables the inner EditText automatically
     *   → It applies the built-in disabled tint to stroke, hint, prefix & suffix
     *   → No manual setAlpha() hack needed
     *
     * FIX 2: tilMealCost.getEditText().setText("0") is the safe way to
     *   write text through the wrapper, avoiding stale EditText references.
     */
    private void onHomemadeSelected() {
        // Set cost to 0 through the wrapper (FIX 2)
        if (tilMealCost.getEditText() != null) {
            tilMealCost.getEditText().setText("0");
        }

        // Disable the whole TIL wrapper — greys out box, hint, prefix, suffix (FIX 1)
        tilMealCost.setEnabled(false);

        // Clear any stale validation error
        tilMealCost.setError(null);

        // Show the amber "🏠 Home" badge
        tvHomemadeBadge.setVisibility(View.VISIBLE);
    }

    /**
     * User unchecked "Homemade Meal".
     *
     * FIX 1: tilMealCost.setEnabled(true) restores the full Material
     *   visual state without any manual alpha manipulation.
     *
     * FIX 2: Clear text through the wrapper's getEditText().
     */
    private void onTakeoutSelected() {
        // Re-enable the wrapper — restores all visual states (FIX 1)
        tilMealCost.setEnabled(true);

        // Clear the "0" placeholder through the wrapper (FIX 2)
        if (tilMealCost.getEditText() != null) {
            tilMealCost.getEditText().setText("");
            tilMealCost.getEditText().requestFocus();
        }

        // Hide the badge
        tvHomemadeBadge.setVisibility(View.GONE);
    }

    // ════════════════════════════════════════════════════════════════════════
    // 4. SAVE BUTTON
    //    FIX 3: cost is read via tilMealCost.getEditText() throughout
    // ════════════════════════════════════════════════════════════════════════

    private void setupSaveButton() {
        btnSavePlan.setOnClickListener(v -> {

            // ── Collect raw values ─────────────────────────────────────────
            String category = etPlanTitle.getText() != null
                    ? etPlanTitle.getText().toString().trim() : "";

            String mealName = etMealName.getText() != null
                    ? etMealName.getText().toString().trim() : "";

            // FIX 3: always read cost through the wrapper reference
            String costInput = (tilMealCost.getEditText() != null
                    && tilMealCost.getEditText().getText() != null)
                    ? tilMealCost.getEditText().getText().toString().trim() : "";

            boolean isHomemade = cbIsHomemade.isChecked();

            // ── Validate ───────────────────────────────────────────────────
            if (!validateForm(category, mealName, costInput, isHomemade)) {
                return; // errors already set on the TILs by validateForm()
            }

            // ── Parse & deliver ────────────────────────────────────────────
            double mealCost = parseCostSafely(costInput, isHomemade);
            deliverResultAndFinish(category, mealName, mealCost, isHomemade);
        });
    }

    /**
     * Validates all fields and sets TIL error messages on failure.
     *
     * Rules:
     *   • category  — required, must not be empty
     *   • mealName  — required, must not be empty
     *   • costInput — required and numeric only when NOT homemade;
     *                 skipped entirely when homemade (value is always "0")
     *
     * @return true if the form is fully valid; false if any field fails.
     */
    private boolean validateForm(String category,
                                 String mealName,
                                 String costInput,
                                 boolean isHomemade) {
        clearAllErrors();
        boolean isValid = true;

        // 1. Category
        if (TextUtils.isEmpty(category)) {
            tilPlanTitle.setError("Please select a meal category.");
            etPlanTitle.requestFocus();
            isValid = false;
        }

        // 2. Meal name
        if (TextUtils.isEmpty(mealName)) {
            tilMealName.setError("Meal name cannot be empty.");
            if (isValid) etMealName.requestFocus();
            isValid = false;
        }

        // 3. Cost — only validated when the user is logging a purchased meal
        if (!isHomemade) {
            if (TextUtils.isEmpty(costInput)) {
                tilMealCost.setError("Please enter the meal cost.");
                if (isValid && tilMealCost.getEditText() != null) {
                    tilMealCost.getEditText().requestFocus();
                }
                isValid = false;
            } else {
                try {
                    double cost = Double.parseDouble(costInput);
                    if (cost < 0) {
                        tilMealCost.setError("Cost cannot be negative.");
                        isValid = false;
                    }
                } catch (NumberFormatException e) {
                    tilMealCost.setError("Enter a valid number (e.g. 85.00).");
                    isValid = false;
                }
            }
        }

        return isValid;
    }

    /** Clears all TIL error messages before each validation pass. */
    private void clearAllErrors() {
        tilPlanTitle.setError(null);
        tilMealName.setError(null);
        tilMealCost.setError(null);
    }

    /**
     * Safely parses the cost string to a double.
     * Returns 0.0 for homemade meals or if parsing unexpectedly fails.
     */
    private double parseCostSafely(String rawCost, boolean isHomemade) {
        if (isHomemade) return 0.0;
        try {
            return Double.parseDouble(rawCost);
        } catch (NumberFormatException e) {
            return 0.0; // validateForm() guards against this, but safety first
        }
    }

    /**
     * Bundles validated meal data into a result Intent and closes this Activity.
     * MainActivity's ActivityResultLauncher receives the data via onAddPlanResult().
     */
    private void deliverResultAndFinish(String category,
                                        String mealName,
                                        double cost,
                                        boolean isHomemade) {
        Intent result = new Intent();
        result.putExtra(EXTRA_MEAL_CATEGORY, category);
        result.putExtra(EXTRA_MEAL_NAME,     mealName);
        result.putExtra(EXTRA_MEAL_COST,     cost);
        result.putExtra(EXTRA_IS_HOMEMADE,   isHomemade);

        setResult(RESULT_OK, result);
        Toast.makeText(this, "✅ Meal plan saved!", Toast.LENGTH_SHORT).show();
        finish();
    }

    // ════════════════════════════════════════════════════════════════════════
    // 5. CANCEL BUTTON
    // ════════════════════════════════════════════════════════════════════════

    private void setupCancelButton() {
        btnCancelPlan.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
    }
}