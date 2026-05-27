package com.abenoja.plan_and_eat;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Random;

/**
 * CoinFlipDialogFragment.java
 * ─────────────────────────────────────────────────────────────────
 * A full-featured coin flip dialog that:
 *  1. Lets the user set two custom choice labels (Heads / Tails).
 *  2. Animates the coin flipping by rapidly toggling between the
 *     heads and tails ImageViews with a scaleX squeeze animation
 *     that simulates a real 3D coin spin.
 *  3. Shows the winning result label after the animation ends.
 *
 * Usage (from MainActivity or any FragmentActivity):
 *   CoinFlipDialogFragment.show(getSupportFragmentManager());
 *
 * Placement suggestion:
 *   Wire up a "Coin Flip" button/icon in MainActivity — e.g., a
 *   secondary mini-FAB, a chip inside the dashboard card, or a
 *   long-press on the existing FAB.
 * ─────────────────────────────────────────────────────────────────
 */
public class CoinFlipDialogFragment extends DialogFragment {

    // ── Constants ────────────────────────────────────────────────
    private static final String TAG              = "CoinFlipDialog";

    /** Total number of face-switches during the animation */
    private static final int    FLIP_CYCLES      = 12;

    /** Duration (ms) of each half-flip (scaleX 1→0 or 0→1) */
    private static final int    HALF_FLIP_MS     = 80;

    /** Pause between each full-face switch (ms) — ramps up toward the end */
    private static final int    BASE_INTERVAL_MS = 160;

    // ── Views ─────────────────────────────────────────────────────
    private TextInputEditText etHeadsLabel;
    private TextInputEditText etTailsLabel;
    private ImageView         ivCoinHeads;
    private ImageView         ivCoinTails;
    private TextView          tvCoinResult;
    private MaterialButton    btnFlipCoin;
    private MaterialButton    btnClose;

    // ── State ─────────────────────────────────────────────────────
    private boolean isFlipping       = false;
    private boolean isShowingHeads   = true;   // which face is visible
    private int     flipCount        = 0;
    private boolean resultIsHeads    = false;

    private final Handler  handler  = new Handler(Looper.getMainLooper());
    private final Random   random   = new Random();

    // ═════════════════════════════════════════════════════════════
    // Factory / Show helpers
    // ═════════════════════════════════════════════════════════════

    /** Convenience method: create and display the dialog. */
    public static void show(@NonNull androidx.fragment.app.FragmentManager fm) {
        CoinFlipDialogFragment dialog = new CoinFlipDialogFragment();
        dialog.show(fm, TAG);
    }

    // ═════════════════════════════════════════════════════════════
    // Lifecycle
    // ═════════════════════════════════════════════════════════════

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_coin_flip, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        setupListeners();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Make the dialog full-width with rounded background
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            int width  = (int) (getResources().getDisplayMetrics().widthPixels * 0.92);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    @Override
    public void onDestroyView() {
        // Cancel any pending animation callbacks to avoid leaks
        handler.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }

    // ═════════════════════════════════════════════════════════════
    // Setup
    // ═════════════════════════════════════════════════════════════

    private void bindViews(View root) {
        etHeadsLabel = root.findViewById(R.id.etHeadsLabel);
        etTailsLabel = root.findViewById(R.id.etTailsLabel);
        ivCoinHeads  = root.findViewById(R.id.ivCoinHeads);
        ivCoinTails  = root.findViewById(R.id.ivCoinTails);
        tvCoinResult = root.findViewById(R.id.tvCoinResult);
        btnFlipCoin  = root.findViewById(R.id.btnFlipCoin);
        btnClose     = root.findViewById(R.id.btnCoinFlipClose);
    }

    private void setupListeners() {
        btnFlipCoin.setOnClickListener(v -> {
            if (!isFlipping) startFlip();
        });

        btnClose.setOnClickListener(v -> dismiss());

        // Tapping the coin image also triggers a flip
        ivCoinHeads.setOnClickListener(v -> { if (!isFlipping) startFlip(); });
        ivCoinTails.setOnClickListener(v -> { if (!isFlipping) startFlip(); });
    }

    // ═════════════════════════════════════════════════════════════
    // Coin Flip Logic & Animation
    // ═════════════════════════════════════════════════════════════

    /**
     * Kicks off the coin flip sequence.
     * Randomly decides the final outcome, then runs FLIP_CYCLES
     * face-switch animations with accelerating intervals to feel
     * like a real coin slowing down before landing.
     */
    private void startFlip() {
        isFlipping   = true;
        flipCount    = 0;
        resultIsHeads = random.nextBoolean();

        // Reset result text while flipping
        tvCoinResult.setText("…");
        tvCoinResult.setTextColor(0xFF808080);

        // Disable the flip button during animation
        btnFlipCoin.setEnabled(false);
        btnFlipCoin.setAlpha(0.5f);

        scheduleNextFaceSwitch();
    }

    /**
     * Recursively schedules each face-switch with an increasing
     * delay to simulate the coin slowing down.
     */
    private void scheduleNextFaceSwitch() {
        if (flipCount >= FLIP_CYCLES) {
            // ── Final landing ──────────────────────────────────
            // Ensure the final face matches the result
            boolean finalFaceIsHeads = resultIsHeads;
            animateFaceSwitch(finalFaceIsHeads, () -> {
                isFlipping = false;
                showResult();
                btnFlipCoin.setEnabled(true);
                btnFlipCoin.setAlpha(1.0f);
            });
            return;
        }

        // Accelerating delay: starts fast, slows toward the end
        float progress    = (float) flipCount / FLIP_CYCLES;           // 0.0 → 1.0
        long  interval    = (long) (BASE_INTERVAL_MS * (0.5f + progress * 1.5f));

        handler.postDelayed(() -> {
            // Toggle which face to show
            boolean showHeads = (flipCount % 2 == 0) != isShowingHeads;
            animateFaceSwitch(showHeads, () -> {
                flipCount++;
                scheduleNextFaceSwitch();
            });
        }, interval);
    }

    /**
     * Animates a single face switch using a scaleX squeeze:
     *   current face: scaleX 1.0 → 0.0  (squeeze away)
     *   new face:     scaleX 0.0 → 1.0  (expand in)
     *
     * @param showHeads  true = land on heads face, false = tails
     * @param onComplete runnable invoked after both halves finish
     */
    private void animateFaceSwitch(boolean showHeads, @Nullable Runnable onComplete) {
        ImageView currentFace = isShowingHeads ? ivCoinHeads : ivCoinTails;
        ImageView nextFace    = showHeads      ? ivCoinHeads : ivCoinTails;

        if (currentFace == nextFace) {
            // Same face — just do a quick scale pulse
            ObjectAnimator pulse = ObjectAnimator.ofFloat(currentFace, "scaleX", 1f, 0.85f, 1f);
            pulse.setDuration(HALF_FLIP_MS * 2L);
            pulse.setInterpolator(new AccelerateDecelerateInterpolator());
            pulse.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator animation) {
                    if (onComplete != null) onComplete.run();
                }
            });
            pulse.start();
            return;
        }

        // Half 1: squeeze current face out
        ObjectAnimator squeezeOut = ObjectAnimator.ofFloat(currentFace, "scaleX", 1f, 0f);
        squeezeOut.setDuration(HALF_FLIP_MS);
        squeezeOut.setInterpolator(new AccelerateDecelerateInterpolator());

        squeezeOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Swap visibility
                currentFace.setVisibility(View.GONE);
                nextFace.setScaleX(0f);
                nextFace.setVisibility(View.VISIBLE);
                isShowingHeads = showHeads;

                // Half 2: expand next face in
                ObjectAnimator expandIn = ObjectAnimator.ofFloat(nextFace, "scaleX", 0f, 1f);
                expandIn.setDuration(HALF_FLIP_MS);
                expandIn.setInterpolator(new AccelerateDecelerateInterpolator());
                expandIn.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (onComplete != null) onComplete.run();
                    }
                });
                expandIn.start();
            }
        });

        squeezeOut.start();
    }

    /**
     * Reads the user's custom labels and shows the result text
     * after the animation completes.
     */
    private void showResult() {
        String headsLabel = getLabel(etHeadsLabel, "Heads");
        String tailsLabel = getLabel(etTailsLabel, "Tails");

        String winner = resultIsHeads ? headsLabel : tailsLabel;
        String side   = resultIsHeads ? "Heads" : "Tails";

        // Display result
        tvCoinResult.setText(side + " — " + winner + "!");
        tvCoinResult.setTextColor(0xFFFFC107);  // amber accent

        // Small bounce animation on result text
        ObjectAnimator bounce = ObjectAnimator.ofFloat(tvCoinResult, "scaleX", 0.8f, 1.1f, 1.0f);
        ObjectAnimator bounceY = ObjectAnimator.ofFloat(tvCoinResult, "scaleY", 0.8f, 1.1f, 1.0f);
        bounce.setDuration(300);
        bounceY.setDuration(300);
        bounce.start();
        bounceY.start();
    }

    /** Safely reads a TextInputEditText, falling back to a default value. */
    private String getLabel(@NonNull TextInputEditText et, @NonNull String fallback) {
        String text = et.getText() != null ? et.getText().toString().trim() : "";
        return text.isEmpty() ? fallback : text;
    }
}