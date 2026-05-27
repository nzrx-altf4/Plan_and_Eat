package com.abenoja.plan_and_eat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

/**
 * ArchiveAdapter.java
 * ─────────────────────────────────────────────────────────────────────
 * RecyclerView adapter for the archive screen.
 * Each item represents one calendar day and is bound to item_archive_day.xml.
 *
 * Card layout (item_archive_day.xml) needs these View IDs:
 *   tvArchiveDate         — date header         e.g. "2025-05-24"
 *   tvArchiveMeals        — meal list            e.g. "• Breakfast  Tapsilog  🏠 Homemade\n• Lunch  ..."
 *   tvArchiveOrigBudget   — original budget      e.g. "Original budget:  ₱500.00"
 *   tvArchiveTotalCost    — total cost           e.g. "Total spent:       ₱149.00"
 *   tvArchiveBudgetAfter  — remaining balance    e.g. "Remaining budget:  ₱351.00"
 * ─────────────────────────────────────────────────────────────────────
 */
public class ArchiveAdapter
        extends RecyclerView.Adapter<ArchiveAdapter.ArchiveViewHolder> {

    private final List<DatabaseHelper.DailySummary> summaries;

    public ArchiveAdapter(List<DatabaseHelper.DailySummary> summaries) {
        this.summaries = summaries;
    }

    @NonNull
    @Override
    public ArchiveViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_archive_day, parent, false);
        return new ArchiveViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArchiveViewHolder holder, int position) {
        DatabaseHelper.DailySummary summary = summaries.get(position);

        // ── Date ───────────────────────────────────────────────────────
        holder.tvDate.setText(formatDate(summary.date));

        // ── Meal list ──────────────────────────────────────────────────
        if (summary.meals == null || summary.meals.isEmpty()) {
            holder.tvMeals.setText("No meals logged.");
        } else {
            StringBuilder sb = new StringBuilder();
            for (DatabaseHelper.MealRecord meal : summary.meals) {
                sb.append("• ")
                  .append(meal.category)
                  .append("  ")
                  .append(meal.mealName);

                if (meal.isHomemade) {
                    sb.append("  🏠 Homemade");
                } else {
                    sb.append(String.format(Locale.getDefault(),
                            "  ₱%.2f", meal.cost));
                }
                sb.append("\n");
            }
            // Trim the trailing newline
            if (sb.length() > 0) sb.deleteCharAt(sb.length() - 1);
            holder.tvMeals.setText(sb.toString());
        }

        // ── Budget breakdown ───────────────────────────────────────────
        holder.tvOrigBudget.setText(
                String.format(Locale.getDefault(),
                        "Original budget:  ₱%.2f", summary.originalBudget));

        holder.tvTotalCost.setText(
                String.format(Locale.getDefault(),
                        "Total spent:        ₱%.2f", summary.totalCost));

        // Colour the remaining budget red if it went negative
        double remaining = summary.budgetAfterCost;
        holder.tvBudgetAfter.setText(
                String.format(Locale.getDefault(),
                        "Remaining budget: ₱%.2f", remaining));

        int colour = remaining < 0
                ? android.graphics.Color.parseColor("#D32F2F")   // red
                : android.graphics.Color.parseColor("#388E3C");  // green
        holder.tvBudgetAfter.setTextColor(colour);
    }

    @Override
    public int getItemCount() {
        return summaries != null ? summaries.size() : 0;
    }

    // ── ViewHolder ────────────────────────────────────────────────────
    static class ArchiveViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate;
        TextView tvMeals;
        TextView tvOrigBudget;
        TextView tvTotalCost;
        TextView tvBudgetAfter;

        ArchiveViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate        = itemView.findViewById(R.id.tvArchiveDate);
            tvMeals       = itemView.findViewById(R.id.tvArchiveMeals);
            tvOrigBudget  = itemView.findViewById(R.id.tvArchiveOrigBudget);
            tvTotalCost   = itemView.findViewById(R.id.tvArchiveTotalCost);
            tvBudgetAfter = itemView.findViewById(R.id.tvArchiveBudgetAfter);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────

    /**
     * Converts "2025-05-24" to "May 24, 2025" for display.
     * Falls back to the raw string if parsing fails.
     */
    private String formatDate(String rawDate) {
        try {
            java.text.SimpleDateFormat in  =
                    new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            java.text.SimpleDateFormat out =
                    new java.text.SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
            return out.format(in.parse(rawDate));
        } catch (Exception e) {
            return rawDate;
        }
    }
}
