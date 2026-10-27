package de.christina.atmen;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    // Farben: ruhig, dunkel, ohne grelles Weiß
    private static final int BG = 0xFF0E1224;
    private static final int TEXT = 0xFFE4E6F4;
    private static final int TEXT_SOFT = 0xFF9097BC;
    private static final int ACCENT = 0xFF7B8CDE;
    private static final int CHIP_BORDER = 0xFF343B5E;
    private static final int CHIP_SELECTED = 0xFF262D52;
    private static final int BUTTON = 0xFF333C78;

    private static final String PREFS = "atmen";
    private static final String KEY_TECHNIQUE = "technique";

    private final Button[] chips = new Button[Technique.ALL.length];
    private TextView summaryView;
    private BreathView breathView;
    private TextView phaseView;
    private Button startButton;

    private int selected = 0;
    private boolean running = false;
    private long startedAt;
    private final Technique.State state = new Technique.State();
    private Technique.Kind lastKind = null;

    private final Runnable frame = new Runnable() {
        @Override public void run() {
            if (!running) return;
            tick();
            breathView.postOnAnimation(this);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        selected = prefs.getInt(KEY_TECHNIQUE, 0);
        if (selected < 0 || selected >= Technique.ALL.length) selected = 0;

        setContentView(buildLayout());
        showIdle();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Wer die App verlässt, beendet die Übung.
        if (running) stop();
    }

    // ---------- Aufbau ----------

    private View buildLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(20), dp(28), dp(20), dp(28));
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = text("Atmen", 15, TEXT_SOFT);
        title.setLetterSpacing(0.15f);
        if (android.os.Build.VERSION.SDK_INT >= 28) title.setAccessibilityHeading(true);
        root.addView(title, wrap());

        LinearLayout chipRow = new LinearLayout(this);
        chipRow.setOrientation(LinearLayout.HORIZONTAL);
        chipRow.setGravity(Gravity.CENTER);
        for (int i = 0; i < Technique.ALL.length; i++) {
            final int index = i;
            Button chip = new Button(this);
            chip.setText(Technique.ALL[i].name);
            chip.setAllCaps(false);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            chip.setStateListAnimator(null);
            chip.setMinHeight(dp(48));
            chip.setMinimumHeight(dp(48));
            chip.setPadding(dp(18), 0, dp(18), 0);
            chip.setOnClickListener(v -> select(index));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(48));
            lp.setMargins(dp(5), 0, dp(5), 0);
            chipRow.addView(chip, lp);
            chips[i] = chip;
        }
        LinearLayout.LayoutParams rowLp = wrap();
        rowLp.topMargin = dp(20);
        root.addView(chipRow, rowLp);

        summaryView = text("", 14, TEXT_SOFT);
        LinearLayout.LayoutParams sumLp = wrap();
        sumLp.topMargin = dp(12);
        root.addView(summaryView, sumLp);

        // Kreis mit der aktuellen Phase in der Mitte
        FrameLayout stage = new FrameLayout(this);
        breathView = new BreathView(this);
        stage.addView(breathView, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout center = new LinearLayout(this);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER);
        phaseView = text("", 26, TEXT);
        phaseView.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        // Sprachausgabe (TalkBack) liest jede neue Phase vor
        phaseView.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        center.addView(phaseView, wrap());
        stage.addView(center, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        LinearLayout.LayoutParams stageLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        stageLp.topMargin = dp(8);
        root.addView(stage, stageLp);

        startButton = new Button(this);
        startButton.setAllCaps(false);
        startButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        startButton.setTextColor(TEXT);
        startButton.setStateListAnimator(null);
        startButton.setBackground(rounded(BUTTON, 0, 0));
        startButton.setOnClickListener(v -> { if (running) stop(); else start(); });
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(dp(200), dp(56));
        btnLp.topMargin = dp(16);
        root.addView(startButton, btnLp);

        refreshChips();
        return root;
    }

    // ---------- Ablauf ----------

    private void select(int index) {
        if (running) return;
        selected = index;
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putInt(KEY_TECHNIQUE, index).apply();
        refreshChips();
    }

    private void start() {
        running = true;
        startedAt = SystemClock.uptimeMillis();
        lastKind = null;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        startButton.setText("Beenden");
        refreshChips();
        tick();
        breathView.postOnAnimation(frame);
    }

    private void stop() {
        running = false;
        breathView.removeCallbacks(frame);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        refreshChips();
        showIdle();
    }

    private void showIdle() {
        breathView.setLevel(0f);
        phaseView.setText("Bereit");
        startButton.setText("Start");
    }

    private void tick() {
        long elapsed = SystemClock.uptimeMillis() - startedAt;
        Technique.ALL[selected].stateAt(elapsed, state);
        breathView.setLevel(state.level);
        if (state.kind != lastKind) {
            lastKind = state.kind;
            phaseView.setText(Technique.label(state.kind));
        }
    }

    private void refreshChips() {
        for (int i = 0; i < chips.length; i++) {
            boolean on = i == selected;
            chips[i].setSelected(on);
            chips[i].setTextColor(on ? TEXT : TEXT_SOFT);
            chips[i].setBackground(on ? rounded(CHIP_SELECTED, ACCENT, dp(1))
                                      : rounded(Color.TRANSPARENT, CHIP_BORDER, dp(1)));
            chips[i].setEnabled(!running || on);
            chips[i].setAlpha(running && !on ? 0.35f : 1f);
        }
        summaryView.setText(Technique.ALL[selected].summary);
    }

    // ---------- Hilfen ----------

    private TextView text(String s, int sp, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private GradientDrawable rounded(int fill, int stroke, int strokeWidth) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(28));
        if (strokeWidth > 0) d.setStroke(strokeWidth, stroke);
        return d;
    }

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
