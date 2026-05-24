package com.abenoja.plan_and_eat; // ← Change to your actual package name

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

/**
 * MealAdapter.java
 * ─────────────────────────────────────────────────────────────
 * RecyclerView Adapter for displaying a list of {@link Meal} objects
 * on the main dashboard.
 *
 * Binds each Meal to a row card defined in res/layout/item_meal.xml.
 *
 * Usage (in MainActivity):
 *   MealAdapter adapter = new MealAdapter(this, mealList);
 *   recyclerMeals.setAdapter(adapter);
 *
 * To refresh the list later:
 *   adapter.updateList(newMealList);
 * ─────────────────────────────────────────────────────────────
 */
public class MealAdapter extends RecyclerView.Adapter<MealAdapter.MealViewHolder> {

    // ── Dependencies ──────────────────────────────────────────
    private final Context    context;
    private       List<Meal> mealList;

    // ── Listener interface for row-level click events ──────────
    public interface OnMealClickListener {
        void onMealClick(Meal meal, int position);
    }

    private OnMealClickListener clickListener;

    /** Optional: set a click listener to handle row taps. */
    public void setOnMealClickListener(OnMealClickListener listener) {
        this.clickListener = listener;
    }

    // ── Constructor ───────────────────────────────────────────
    /**
     * @param context  The host Activity or Fragment context.
     * @param mealList The list of Meal objects to display.
     */
    public MealAdapter(Context context, List<Meal> mealList) {
        this.context  = context;
        this.mealList = mealList;
    }

    // ── RecyclerView.Adapter overrides ────────────────────────

    /**
     * Inflates item_meal.xml and wraps it in a MealViewHolder.
     */
    @NonNull
    @Override
    public MealViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater
                .from(context)
                .inflate(R.layout.item_meal, parent, false); // ← requires item_meal.xml
        return new MealViewHolder(itemView);
    }

    /**
     * Binds a single Meal's data to the ViewHolder at the given position.
     */
    @Override
    public void onBindViewHolder(@NonNull MealViewHolder holder, int position) {
        Meal meal = mealList.get(position);

        // Bind meal name
        holder.tvMealName.setText(meal.getMealName());

        // Format cost as "P 120.00"
        String formattedCost = String.format(Locale.getDefault(), "P %.2f", meal.getCost());
        holder.tvMealCost.setText(formattedCost);

        // Show "Homemade" or "Takeout" tag
        holder.tvMealType.setText(meal.isHomemade() ? "🏠 Homemade" : "🛍️ Takeout");

        // Handle row click (optional, used for future edit functionality)
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onMealClick(meal, holder.getAdapterPosition());
            }
        });
    }

    /**
     * Returns the total number of meal items in the list.
     */
    @Override
    public int getItemCount() {
        return (mealList != null) ? mealList.size() : 0;
    }

    // ── Public helper methods ─────────────────────────────────

    /**
     * Replaces the current meal list and refreshes the RecyclerView.
     *
     * @param newList The updated list of meals.
     */
    public void updateList(List<Meal> newList) {
        this.mealList = newList;
        notifyDataSetChanged();
    }

    /**
     * Appends a single meal to the list and smoothly inserts it at the bottom.
     *
     * @param meal The new Meal to add.
     */
    public void addMeal(Meal meal) {
        mealList.add(meal);
        notifyItemInserted(mealList.size() - 1);
    }

    /**
     * Removes the meal at the given position.
     *
     * @param position Index of the meal to remove.
     */
    public void removeMeal(int position) {
        if (position >= 0 && position < mealList.size()) {
            mealList.remove(position);
            notifyItemRemoved(position);
        }
    }

    // ── ViewHolder ────────────────────────────────────────────

    /**
     * Holds references to the Views inside each item_meal.xml row.
     *
     * ⚠️ The IDs below (tvMealName, tvMealCost, tvMealType) must match
     *    the IDs you define in res/layout/item_meal.xml.
     */
    public static class MealViewHolder extends RecyclerView.ViewHolder {

        TextView tvMealName; // e.g., "Arroz Caldo"
        TextView tvMealCost; // e.g., "P 85.00"
        TextView tvMealType; // e.g., "🏠 Homemade" or "🛍️ Takeout"

        public MealViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMealName = itemView.findViewById(R.id.tvMealName);
            tvMealCost = itemView.findViewById(R.id.tvMealCost);
            tvMealType = itemView.findViewById(R.id.tvMealType);
        }
    }
}