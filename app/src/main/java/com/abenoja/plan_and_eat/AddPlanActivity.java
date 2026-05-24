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

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * AddPlanActivity.java
 * ─────────────────────────────────────────────────────────────────────
 * Secondary screen for creating a new meal plan entry.
 *
 * Responsibilities:
 *   1. Initialize all form Views via findViewById
 *   2. Populate the category dropdown with meal-type options
 *   3. Smart checkbox logic — disables cost field for homemade meals
 *   4. Validate and collect form data on "Save Plan"
 *   5. Return a result Intent to MainActivity with the new Meal data
 *
 * Launched from: MainActivity → fabAddMeal click
 * Returns to   : MainActivity via setResult() + finish()
 * ─────────────────────────────────────────────────────────────────────
 */
public class AddPlanActivity extends AppCompatActivity {

    // ── Request code (used by MainActivity.startActivityForResult) ──
    // Keep in sync with the constant in MainActivity.java
    public static final int REQUEST_CODE_ADD_PLAN = 1001;

    // ── Intent extra keys — used to pass the new Meal back ──────────
    public static final String EXTRA_MEAL_NAME      = "extra_meal_name";
    public static final String EXTRA_MEAL_CATEGORY  = "extra_meal_category";
    public static final String EXTRA_MEAL_COST      = "extra_meal_cost";
    public static final String EXTRA_IS_HOMEMADE    = "extra_is_homemade";

    // ── View references ──────────────────────────────────────────────
    private TextInputLayout       tilPlanTitle;   // wrapper for the category dropdown
    private TextInputLayout       tilMealName;    // wrapper for meal name field
    private TextInputLayout       tilMealCost;    // wrapper for cost field
    private AutoCompleteTextView  etPlanTitle;    // dropdown: Breakfast / Lunch / etc.
    private TextInputEditText     etMealName;     // free-text meal name
    private TextInputEditText     etMealCost;     // numeric cost input
    private CheckBox              cbIsHomemade;   // "Homemade Meal" toggle
    private TextView              tvHomemadeBadge;// amber badge shown when homemade
    private MaterialButton        btnSavePlan;    // primary save action
    private MaterialButton        btnCancelPlan;  // secondary cancel action

    // ── Dropdown options ─────────────────────────────────────────────
    private static final String[] MEAL_CATEGORIES = {
            "Breakfast", "Brunch", "Lunch", "Merienda", "Dinner", "Midnight Snack"
    };

    // ────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_plan);

        initViews();
        setupCategoryDropdown();
        setupCheckboxLogic();
        setupSaveButton();
        setupCancelButton();
    }

    // ════════════════════════════════════════════════════════════════
    // 1. VIEW INITIALISATION
    // ════════════════════════════════════════════════════════════════

    /**
     * Binds every View in activity_add_plan.xml to a Java field.
     * All IDs must match what is declared in the XML layout.
     */
    private void initViews() {
        // TextInputLayout wrappers (used to show/hide error messages)
        tilPlanTitle  = findViewById(R.id.tilPlanTitle);
        tilMealName   = findViewById(R.id.tilMealName);
        tilMealCost   = findViewById(R.id.tilMealCost);

        // Actual input controls inside the wrappers
        etPlanTitle   = findViewById(R.id.etPlanTitle);
        etMealName    = findViewById(R.id.etMealName);
        etMealCost    = findViewById(R.id.etMealCost);

        // Checkbox and its companion badge
        cbIsHomemade    = findViewById(R.id.cbIsHomemade);
        tvHomemadeBadge = findViewById(R.id.tvHomemadeBadge);

        // Action buttons
        btnSavePlan   = findViewById(R.id.btnSavePlan);
        btnCancelPlan = findViewById(R.id.btnCancelPlan);

        // Optional: back arrow in the header closes this activity
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 2. CATEGORY DROPDOWN
    // ════════════════════════════════════════════════════════════════

    /**
     * Wires the AutoCompleteTextView to a static list of meal categories.
     *
     * Uses res/layout/list_item_dropdown.xml as the dropdown row layout
     * (a dark-themed single TextView — see activity_add_plan.xml comments).
     */
    private void setupCategoryDropdown() {
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this,
                R.layout.list_item_dropdown, // dark-themed row layout
                MEAL_CATEGORIES
        );
        etPlanTitle.setAdapter(categoryAdapter);

        // Clear the validation error as soon as the user picks a value
        etPlanTitle.setOnItemClickListener((parent, view, position, id) ->
                tilPlanTitle.setError(null));
    }

    // ════════════════════════════════════════════════════════════════
    // 3. SMART CHECKBOX LOGIC (Core requirement)
    // ════════════════════════════════════════════════════════════════

    /**
     * Attaches an OnCheckedChangeListener to {@code cbIsHomemade}.
     *
     * Checked (homemade = true):
     *   • Clears the cost field and sets its text to "0"
     *   • Disables the cost TextInputLayout so it appears greyed-out
     *   • Shows the amber "🏠 Home" badge
     *   • Clears any lingering cost validation error
     *
     * Unchecked (bought/takeout):
     *   • Re-enables the cost TextInputLayout
     *   • Clears the "0" placeholder so the user can type freely
     *   • Hides the badge
     */
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
     * Called when the user checks "Homemade Meal".
     * Zeroes out cost and locks the field — homemade meals don't count
     * against the tracked budget.
     */
    private void onHomemadeSelected() {
        // Zero out and lock the cost field
        etMealCost.setText("0");
        etMealCost.setEnabled(false);

        // Dim the wrapper to give a clear disabled visual signal
        tilMealCost.setAlpha(0.45f);

        // Clear any previous cost validation error
        tilMealCost.setError(null);

        // Show the amber "🏠 Home" badge beside the checkbox
        tvHomemadeBadge.setVisibility(View.VISIBLE);
    }

    /**
     * Called when the user unchecks "Homemade Meal".
     * Re-enables cost entry so the user can type a price.
     */
    private void onTakeoutSelected() {
        // Re-enable and restore the cost field to its normal appearance
        etMealCost.setEnabled(true);
        tilMealCost.setAlpha(1.0f);

        // Clear the "0" placeholder so the hint reappears naturally
        etMealCost.setText("");

        // Request focus to invite the user to type immediately
        etMealCost.requestFocus();

        // Hide the homemade badge
        tvHomemadeBadge.setVisibility(View.GONE);
    }

    // ════════════════════════════════════════════════════════════════
    // 4. SAVE BUTTON — Validation + Result
    // ════════════════════════════════════════════════════════════════

    /**
     * Attaches an OnClickListener to {@code btnSavePlan}.
     *
     * Flow:
     *   1. Collect raw text from each field
     *   2. Run field-by-field validation (stops at first failure)
     *   3. Parse numeric cost safely
     *   4. Build a result Intent and finish the Activity
     */
    private void setupSaveButton() {
        btnSavePlan.setOnClickListener(v -> {
            // ── Collect raw input ──────────────────────────────────
            String category  = etPlanTitle.getText() != null
                    ? etPlanTitle.getText().toString().trim() : "";
            String mealName  = etMealName.getText() != null
                    ? etMealName.getText().toString().trim()  : "";
            String costInput = etMealCost.getText() != null
                    ? etMealCost.getText().toString().trim()  : "";
            boolean isHomemade = cbIsHomemade.isChecked();

            // ── Validate ───────────────────────────────────────────
            if (!validateForm(category, mealName, costInput, isHomemade)) {
                // validateForm() sets the appropriate error on each TIL;
                // return early and let the user correct the problem.
                return;
            }

            // ── Parse cost safely ──────────────────────────────────
            // isHomemade meals always have cost = 0.0 (set by checkbox logic),
            // but we parse defensively in case the field was edited via paste.
            double mealCost = parseCostSafely(costInput, isHomemade);

            // ── Build and send result back to MainActivity ─────────
            deliverResultAndFinish(category, mealName, mealCost, isHomemade);
        });
    }

    /**
     * Validates all form fields in order and sets error messages on the
     * wrapping TextInputLayouts. Returns true only if every field is valid.
     *
     * Validation rules:
     *   • category  — must not be empty
     *   • mealName  — must not be empty
     *   • cost      — must not be empty (unless homemade); must be a
     *                 valid non-negative number if provided
     *
     * @return {@code true} if all fields pass; {@code false} otherwise.
     */
    private boolean validateForm(String category,
                                 String mealName,
                                 String costInput,
                                 boolean isHomemade) {
        // Reset all errors before re-validating
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
            if (isValid) etMealName.requestFocus(); // focus only the first error
            isValid = false;
        }

        // 3. Cost — only required and validated when NOT homemade
        if (!isHomemade) {
            if (TextUtils.isEmpty(costInput)) {
                tilMealCost.setError("Please enter the meal cost.");
                if (isValid) etMealCost.requestFocus();
                isValid = false;
            } else {
                // Check it's a valid positive number
                try {
                    double cost = Double.parseDouble(costInput);
                    if (cost < 0) {
                        tilMealCost.setError("Cost cannot be negative.");
                        if (isValid) etMealCost.requestFocus();
                        isValid = false;
                    }
                } catch (NumberFormatException e) {
                    tilMealCost.setError("Enter a valid number (e.g. 85.00).");
                    if (isValid) etMealCost.requestFocus();
                    isValid = false;
                }
            }
        }

        return isValid;
    }

    /**
     * Resets the error state on all TextInputLayouts so stale error
     * messages don't persist between validation attempts.
     */
    private void clearAllErrors() {
        tilPlanTitle.setError(null);
        tilMealName.setError(null);
        tilMealCost.setError(null);
    }

    /**
     * Safely converts the raw cost string to a double.
     *
     * Falls back to 0.0 if:
     *   • The meal is homemade (cost is intentionally free)
     *   • The string cannot be parsed (guarded by validateForm, but
     *     defensive coding prevents any crash here)
     *
     * @param rawCost    The string from etMealCost.
     * @param isHomemade Whether the homemade checkbox is checked.
     * @return Parsed cost as a double, or 0.0 as a safe fallback.
     */
    private double parseCostSafely(String rawCost, boolean isHomemade) {
        if (isHomemade) return 0.0;
        try {
            return Double.parseDouble(rawCost);
        } catch (NumberFormatException e) {
            // Should never reach here if validateForm() passed, but
            // returning 0.0 is a safe, non-crashing fallback.
            return 0.0;
        }
    }

    /**
     * Packages the validated meal data into a result Intent and calls
     * finish() to close this Activity and return to MainActivity.
     *
     * MainActivity receives the data in onActivityResult() by reading
     * the Intent extras using the public EXTRA_* keys defined above.
     *
     * @param category   Selected category string (e.g. "Breakfast").
     * @param mealName   User-entered meal name.
     * @param cost       Parsed cost value (0.0 if homemade).
     * @param isHomemade True if the meal is home-cooked.
     */
    private void deliverResultAndFinish(String category,
                                        String mealName,
                                        double cost,
                                        boolean isHomemade) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_MEAL_CATEGORY, category);
        resultIntent.putExtra(EXTRA_MEAL_NAME,     mealName);
        resultIntent.putExtra(EXTRA_MEAL_COST,     cost);
        resultIntent.putExtra(EXTRA_IS_HOMEMADE,   isHomemade);

        setResult(RESULT_OK, resultIntent);

        Toast.makeText(this, "✅ Meal plan saved!", Toast.LENGTH_SHORT).show();

        finish(); // Returns to MainActivity
    }

    // ════════════════════════════════════════════════════════════════
    // 5. CANCEL BUTTON
    // ════════════════════════════════════════════════════════════════

    /**
     * Discards the form and returns to MainActivity with RESULT_CANCELED.
     * MainActivity's onActivityResult() will receive this and do nothing.
     */
    private void setupCancelButton() {
        btnCancelPlan.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
    }
}