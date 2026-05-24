package com.abenoja.plan_and_eat; // ← Change to your actual package name

/**
 * Meal.java
 * ─────────────────────────────────────────────────────────────
 * Plain data model representing a single meal entry.
 *
 * Fields:
 *   mealName   – Display name of the meal (e.g., "Sinangag at Itlog")
 *   cost       – Cost of the meal in Philippine Peso (e.g., 75.00)
 *   isHomemade – true = cooked at home, false = bought/ordered
 * ─────────────────────────────────────────────────────────────
 */
public class Meal {

    // ── Fields ────────────────────────────────────────────────
    private String  mealName;
    private double  cost;
    private boolean isHomemade;

    // ── Constructor ───────────────────────────────────────────
    /**
     * @param mealName   Name of the meal.
     * @param cost       Cost in Peso.
     * @param isHomemade Whether the meal is home-cooked.
     */
    public Meal(String mealName, double cost, boolean isHomemade) {
        this.mealName   = mealName;
        this.cost       = cost;
        this.isHomemade = isHomemade;
    }

    // ── Getters ───────────────────────────────────────────────
    public String  getMealName()   { return mealName;   }
    public double  getCost()       { return cost;        }
    public boolean isHomemade()    { return isHomemade;  }

    // ── Setters ───────────────────────────────────────────────
    public void setMealName(String mealName)     { this.mealName   = mealName;   }
    public void setCost(double cost)             { this.cost       = cost;        }
    public void setHomemade(boolean isHomemade)  { this.isHomemade = isHomemade;  }
}