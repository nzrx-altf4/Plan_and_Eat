package com.abenoja.plan_and_eat;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * ArchiveActivity.java
 * ─────────────────────────────────────────────────────────────────────
 * Displays the complete history of daily meal plans.
 *
 * Each card in the list shows:
 *   • Date
 *   • Every meal logged that day (name, category, cost)
 *   • Total daily cost
 *   • Original budget for that day
 *   • Remaining budget after cost
 *
 * Layout required: res/layout/activity_archive.xml
 * ─────────────────────────────────────────────────────────────────────
 */
public class ArchiveActivity extends AppCompatActivity {

    private RecyclerView    recyclerArchive;
    private TextView        tvEmpty;
    private DatabaseHelper  db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_archive);

        db = new DatabaseHelper(this);

        recyclerArchive = findViewById(R.id.recyclerArchive);
        tvEmpty         = findViewById(R.id.tvArchiveEmpty);

        // Back button (if you added one in the layout)
        View btnBack = findViewById(R.id.btnArchiveBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        loadArchive();
    }

    @Override
    protected void onDestroy() {
        if (db != null) db.close();
        super.onDestroy();
    }

    private void loadArchive() {
        List<DatabaseHelper.DailySummary> archive = db.getDailyArchive();

        if (archive == null || archive.isEmpty()) {
            recyclerArchive.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }

        recyclerArchive.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        recyclerArchive.setLayoutManager(new LinearLayoutManager(this));
        recyclerArchive.setAdapter(new ArchiveAdapter(archive));
    }
}
