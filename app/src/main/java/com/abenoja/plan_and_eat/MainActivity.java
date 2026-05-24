package com.abenoja.plan_and_eat;

import android.os.Bundle;
import android.view.View; //for View.GONE and View.VISIBLE
import android.widget.LinearLayout; //for Empty State layout
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView; //  this import for RecyclerView

public class MainActivity extends AppCompatActivity {


    private RecyclerView recyclerMeals;
    private LinearLayout layoutEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        recyclerMeals = findViewById(R.id.recyclerMeals);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);


        updateEmptyState(true);
    }

    public void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            recyclerMeals.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerMeals.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
        }
    }
}