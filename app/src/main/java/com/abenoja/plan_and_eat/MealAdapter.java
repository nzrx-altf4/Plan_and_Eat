package com.abenoja.plan_and_eat;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

/**
 * MealAdapter.java  (updated — edit/delete support via MealRecord)
 * ─────────────────────────────────────────────────────────────────
 * Uses DatabaseHelper.MealRecord instead of Meal so each row has
 * access to the DB row id needed for edit and delete operations.
 *
 * Replace the existing MealAdapter.java entirely with this file.
 * ─────────────────────────────────────────────────────────────────
 */
public class MealAdapter extends RecyclerView.Adapter<MealAdapter.MealViewHolder> {

    // ── Listener interfaces ───────────────────────────────────────────

    /** Called when the user taps a meal row. */
    public interface OnMealClickListener {
        void onMealClick(DatabaseHelper.MealRecord record, int position);
    }

    /** Called when the user picks Edit from the overflow menu. */
    public interface OnMealEditListener {
        void onMealEdit(DatabaseHelper.MealRecord record, int position);
    }

    /** Called when the user picks Delete from the overflow menu. */
    public interface OnMealDeleteListener {
        void onMealDelete(DatabaseHelper.MealRecord record, int position);
    }

    // ── Fields ────────────────────────────────────────────────────────
    private final Context                        context;
    private       List<DatabaseHelper.MealRecord> mealList;

    /** Tracks which DB row ids are currently checked (done). */
    private final java.util.Set<Long> checkedIds = new java.util.HashSet<>();

    private OnMealClickListener  clickListener;
    private OnMealEditListener   editListener;
    private OnMealDeleteListener deleteListener;

    // ── Constructor ───────────────────────────────────────────────────
    public MealAdapter(Context context, List<DatabaseHelper.MealRecord> mealList) {
        this.context  = context;
        this.mealList = mealList;
    }

    // ── Listener setters ──────────────────────────────────────────────
    public void setOnMealClickListener(OnMealClickListener l)   { this.clickListener  = l; }
    public void setOnMealEditListener(OnMealEditListener l)     { this.editListener   = l; }
    public void setOnMealDeleteListener(OnMealDeleteListener l) { this.deleteListener = l; }

    // ── RecyclerView overrides ────────────────────────────────────────

    @NonNull
    @Override
    public MealViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_meal, parent, false);
        return new MealViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MealViewHolder holder, int position) {
        DatabaseHelper.MealRecord record = mealList.get(position);
        boolean isDone = checkedIds.contains(record.id);

        holder.tvMealName.setText(record.mealName);
        holder.tvMealCost.setText(
                String.format(Locale.getDefault(), "P %.2f", record.cost));
        holder.tvMealType.setText(record.isHomemade ? "🏠 Homemade" : "🛍️ Takeout");

        // ── Checkbox state ────────────────────────────────────────────
        // Temporarily remove the listener before setting state to avoid
        // triggering a callback during recycling/rebind.
        holder.cbMealDone.setOnCheckedChangeListener(null);
        holder.cbMealDone.setChecked(isDone);
        holder.cbMealDone.setOnCheckedChangeListener((btn, checked) -> {
            if (checked) {
                checkedIds.add(record.id);
            } else {
                checkedIds.remove(record.id);
            }
            // Redraw this item to update strikethrough + menu state
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_ID) notifyItemChanged(pos);
        });

        // ── Visual: strikethrough name when done ──────────────────────
        if (isDone) {
            holder.tvMealName.setPaintFlags(
                    holder.tvMealName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvMealName.setTextColor(0xFF505050);
            holder.tvMealCost.setTextColor(0xFF505050);
        } else {
            holder.tvMealName.setPaintFlags(
                    holder.tvMealName.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvMealName.setTextColor(0xFFFFFFFF);
            holder.tvMealCost.setTextColor(0xFFFFC107);
        }

        // ── Overflow menu — disabled when checked ─────────────────────
        holder.btnMealMenu.setEnabled(!isDone);
        holder.btnMealMenu.setAlpha(isDone ? 0.25f : 1.0f);
        holder.btnMealMenu.setOnClickListener(isDone ? null :
                v -> showPopupMenu(v, record, holder));

        // Row tap
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null)
                clickListener.onMealClick(record, holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return mealList != null ? mealList.size() : 0;
    }

    // ── Popup menu ────────────────────────────────────────────────────

    private void showPopupMenu(View anchor,
                               DatabaseHelper.MealRecord record,
                               MealViewHolder holder) {
        PopupMenu popup = new PopupMenu(context, anchor);
        popup.getMenu().add(0, 1, 0, "✏️  Edit");
        popup.getMenu().add(0, 2, 1, "🗑️  Delete");

        popup.setOnMenuItemClickListener(item -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_ID) return false;

            if (item.getItemId() == 1 && editListener != null) {
                editListener.onMealEdit(record, pos);
                return true;
            }
            if (item.getItemId() == 2 && deleteListener != null) {
                deleteListener.onMealDelete(record, pos);
                return true;
            }
            return false;
        });

        popup.show();
    }

    // ── Public helpers ────────────────────────────────────────────────

    /** Replace the entire list and refresh. */
    public void updateList(List<DatabaseHelper.MealRecord> newList) {
        this.mealList = newList;
        notifyDataSetChanged();
    }

    /** Append one record and animate the insertion. */
    public void addRecord(DatabaseHelper.MealRecord record) {
        mealList.add(record);
        notifyItemInserted(mealList.size() - 1);
    }

    /** Remove the item at position and animate removal. */
    public void removeAt(int position) {
        if (position >= 0 && position < mealList.size()) {
            mealList.remove(position);
            notifyItemRemoved(position);
        }
    }

    /** Update a single record in-place and animate the change. */
    public void updateAt(int position, DatabaseHelper.MealRecord record) {
        if (position >= 0 && position < mealList.size()) {
            mealList.set(position, record);
            notifyItemChanged(position);
        }
    }

    // ── ViewHolder ────────────────────────────────────────────────────

    public static class MealViewHolder extends RecyclerView.ViewHolder {
        CheckBox    cbMealDone;
        TextView    tvMealName;
        TextView    tvMealCost;
        TextView    tvMealType;
        ImageButton btnMealMenu;

        public MealViewHolder(@NonNull View itemView) {
            super(itemView);
            cbMealDone  = itemView.findViewById(R.id.cbMealDone);
            tvMealName  = itemView.findViewById(R.id.tvMealName);
            tvMealCost  = itemView.findViewById(R.id.tvMealCost);
            tvMealType  = itemView.findViewById(R.id.tvMealType);
            btnMealMenu = itemView.findViewById(R.id.btnMealMenu);
        }
    }
}