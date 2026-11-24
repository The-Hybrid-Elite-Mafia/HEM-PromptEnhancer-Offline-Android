package com.hem.promptenhancer;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private static final String PREFS_NAME = "hem_prompt_enhancer_offline_v1";

    private static final int MODE_STRUCTURED = 0;
    private static final int MODE_INLINE = 1;
    private static final int MODE_SCRUB_ONLY = 2;

    // Obsidian Glass & Titanium Palette
    private static final int COLOR_BG = Color.parseColor("#0A0D12");
    private static final int COLOR_SURFACE = Color.parseColor("#121821");
    private static final int COLOR_BORDER = Color.parseColor("#253244");
    private static final int COLOR_CYAN = Color.parseColor("#00D8F5");
    private static final int COLOR_AMBER = Color.parseColor("#FF9900");
    private static final int COLOR_TEXT = Color.parseColor("#F2F6FA");
    private static final int COLOR_MUTED = Color.parseColor("#8A99AA");
    private static final int COLOR_DARK_BTN = Color.parseColor("#1A2330");

    private SharedPreferences prefs;
    private LinearLayout root;

    private EditText promptInput;
    private EditText rulesInput;
    private EditText bannedInput;
    private EditText outputBox;
    private TextView statusText;

    private final Button[] slotButtons = new Button[3];
    private final Button[] modeButtons = new Button[3];

    private int activeSlot = 0;
    private int activeMode = MODE_STRUCTURED;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(COLOR_BG);
        window.setNavigationBarColor(COLOR_BG);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        activeSlot = prefs.getInt("active_slot", 0);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(COLOR_BG);
        scrollView.setFillViewport(true);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(20), dp(16), dp(36));
        root.setBackgroundColor(COLOR_BG);
        scrollView.addView(root);

        buildUI();
        loadSlot(activeSlot);

        setContentView(scrollView);
    }

    private void buildUI() {
        // Header
        TextView title = new TextView(this);
        title.setText("HEM PROMPT ENHANCER");
        title.setTextColor(COLOR_CYAN);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        title.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("100% OFFLINE ENGINE  •  ZERO API KEYS  •  ZERO TRACKING");
        subtitle.setTextColor(COLOR_MUTED);
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        subtitle.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
        subtitle.setPadding(0, dp(2), 0, dp(14));
        root.addView(subtitle);

        // Profile Slot Selector
        addSectionLabel("RULE PROFILE SLOTS");
        LinearLayout slotRow = makeHorizontalRow();
        for (int i = 0; i < 3; i++) {
            final int slotIndex = i;
            Button btn = makeToggleButton("SLOT " + (i + 1));
            btn.setOnClickListener(v -> {
                saveCurrentSlot(false);
                loadSlot(slotIndex);
            });
            slotButtons[i] = btn;
            addWeighted(slotRow, btn, i < 2 ? dp(8) : 0);
        }
        root.addView(slotRow);

        // Original Prompt
        addSectionLabel("ORIGINAL PROMPT");
        promptInput = makeInputBox("Paste or type your raw prompt here...", 5);
        root.addView(promptInput);

        LinearLayout promptActions = makeHorizontalRow();
        promptActions.setPadding(0, dp(8), 0, 0);
        Button pasteBtn = makeActionButton("PASTE", COLOR_DARK_BTN, COLOR_TEXT, COLOR_BORDER);
        pasteBtn.setOnClickListener(v -> pasteToPrompt());
        Button clearPromptBtn = makeActionButton("CLEAR INPUT", COLOR_DARK_BTN, COLOR_MUTED, COLOR_BORDER);
        clearPromptBtn.setOnClickListener(v -> {
            promptInput.setText("");
            setStatus("Input cleared");
        });
        addWeighted(promptActions, pasteBtn, dp(8));
        addWeighted(promptActions, clearPromptBtn, 0);
        root.addView(promptActions);

        // Master Rules
        addSectionLabel("MASTER RULES (SAVED PER SLOT — STARTS AT ZERO)");
        rulesInput = makeInputBox("Enter your own rules/directives here (blank by default)...", 5);
        root.addView(rulesInput);

        // Banned Words
        addSectionLabel("BANNED WORDS / CLICHÉS TO STRIP (COMMA-SEPARATED)");
        bannedInput = makeInputBox("e.g. neon lights, tapestry, echoes (blank by default)...", 2);
        root.addView(bannedInput);

        // Output Mode Selector
        addSectionLabel("OFFLINE OUTPUT MODE");
        LinearLayout modeRow = makeHorizontalRow();
        String[] modeNames = {"STRUCTURED", "INLINE", "SCRUB ONLY"};
        for (int i = 0; i < 3; i++) {
            final int modeIndex = i;
            Button btn = makeToggleButton(modeNames[i]);
            btn.setOnClickListener(v -> {
                activeMode = modeIndex;
                updateModeStyles();
                saveCurrentSlot(false);
            });
            modeButtons[i] = btn;
            addWeighted(modeRow, btn, i < 2 ? dp(8) : 0);
        }
        root.addView(modeRow);

        // Primary Actions
        LinearLayout mainActions = makeHorizontalRow();
        mainActions.setPadding(0, dp(16), 0, 0);
        Button saveBtn = makeActionButton("SAVE SLOT", COLOR_DARK_BTN, COLOR_CYAN, COLOR_CYAN);
        saveBtn.setOnClickListener(v -> saveCurrentSlot(true));

        Button enhanceBtn = makeActionButton("ENHANCE PROMPT", COLOR_AMBER, COLOR_BG, COLOR_AMBER);
        enhanceBtn.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        enhanceBtn.setOnClickListener(v -> runOfflineEnhance());

        addWeighted(mainActions, saveBtn, dp(8));
        addWeighted(mainActions, enhanceBtn, 0);
        root.addView(mainActions);

        // Status Line
        statusText = new TextView(this);
        statusText.setText("Ready (offline)");
        statusText.setTextColor(COLOR_MUTED);
        statusText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        statusText.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
        statusText.setPadding(0, dp(10), 0, dp(4));
        root.addView(statusText);

        // Output
        addSectionLabel("ENHANCED OUTPUT");
        outputBox = makeInputBox("Enhanced result will appear here...", 7);
        root.addView(outputBox);

        // Copy Button
        LinearLayout copyRow = makeHorizontalRow();
        copyRow.setPadding(0, dp(10), 0, 0);
        Button copyBtn = makeActionButton("COPY RESULT", COLOR_CYAN, COLOR_BG, COLOR_CYAN);
        copyBtn.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        copyBtn.setOnClickListener(v -> copyResult());
        addWeighted(copyRow, copyBtn, 0);
        root.addView(copyRow);
    }

    private void loadSlot(int slotIndex) {
        activeSlot = slotIndex;
        prefs.edit().putInt("active_slot", activeSlot).apply();

        String prefix = "slot_" + activeSlot + "_";
        promptInput.setText(prefs.getString(prefix + "prompt", ""));
        rulesInput.setText(prefs.getString(prefix + "rules", ""));
        bannedInput.setText(prefs.getString(prefix + "banned", ""));
        activeMode = prefs.getInt(prefix + "mode", MODE_STRUCTURED);

        updateSlotStyles();
        updateModeStyles();
        setStatus("Loaded Slot " + (activeSlot + 1) + " (offline)");
    }

    private void saveCurrentSlot(boolean showMessage) {
        String prefix = "slot_" + activeSlot + "_";
        prefs.edit()
                .putInt("active_slot", activeSlot)
                .putString(prefix + "prompt", promptInput.getText().toString())
                .putString(prefix + "rules", rulesInput.getText().toString())
                .putString(prefix + "banned", bannedInput.getText().toString())
                .putInt(prefix + "mode", activeMode)
                .apply();
        if (showMessage) {
            setStatus("Saved Slot " + (activeSlot + 1) + " locally");
            Toast.makeText(this, "Slot " + (activeSlot + 1) + " saved", Toast.LENGTH_SHORT).show();
        }
    }

    private void runOfflineEnhance() {
        saveCurrentSlot(false);

        String rawPrompt = promptInput.getText().toString().trim();
        String rawRules = rulesInput.getText().toString().trim();
        String rawBanned = bannedInput.getText().toString().trim();

        if (rawPrompt.isEmpty()) {
            setStatus("Enter an original prompt first.");
            return;
        }

        // Parse banned terms
        List<String> bannedTerms = new ArrayList<>();
        if (!rawBanned.isEmpty()) {
            String[] parts = rawBanned.split("[,\\n]+");
            for (String part : parts) {
                String term = part.trim();
                if (!term.isEmpty()) {
                    bannedTerms.add(term);
                }
            }
        }

        // Scrub banned terms from prompt
        String cleanedPrompt = rawPrompt;
        int strippedCount = 0;
        for (String term : bannedTerms) {
            Pattern p = Pattern.compile("(?i)(?<!\\w)" + Pattern.quote(term) + "(?!\\w)");
            Matcher m = p.matcher(cleanedPrompt);
            while (m.find()) {
                strippedCount++;
            }
            cleanedPrompt = p.matcher(cleanedPrompt).replaceAll("");
        }

        cleanedPrompt = normalizeText(cleanedPrompt);

        // Assemble final output according to selected mode
        StringBuilder out = new StringBuilder();
        if (activeMode == MODE_SCRUB_ONLY) {
            out.append(cleanedPrompt);
        } else if (activeMode == MODE_INLINE) {
            out.append(cleanedPrompt);
            if (!rawRules.isEmpty()) {
                out.append("\n\n").append(normalizeText(rawRules));
            }
            if (!bannedTerms.isEmpty()) {
                out.append("\n\nAvoid: ").append(joinTerms(bannedTerms));
            }
        } else {
            // MODE_STRUCTURED
            if (!rawRules.isEmpty()) {
                out.append("[MASTER RULES]\n").append(normalizeText(rawRules)).append("\n\n");
            }
            out.append("[PROMPT]\n").append(cleanedPrompt);
            if (!bannedTerms.isEmpty()) {
                out.append("\n\n[EXCLUDE]\n").append(joinTerms(bannedTerms));
            }
        }

        String finalOutput = out.toString().trim();
        outputBox.setText(finalOutput);
        setStatus("Enhanced offline • " + strippedCount + " banned hit(s) stripped • " + finalOutput.length() + " chars");
    }

    private String normalizeText(String input) {
        String text = input.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = text.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i]
                    .replaceAll("[ \\t]{2,}", " ")
                    .replaceAll(" +([,.;:!?])", "$1")
                    .replaceAll(",\\s*,+", ",")
                    .trim();
            sb.append(line);
            if (i < lines.length - 1) {
                sb.append("\n");
            }
        }
        return sb.toString().replaceAll("\\n{3,}", "\n\n").trim();
    }

    private String joinTerms(List<String> terms) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < terms.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(terms.get(i));
        }
        return sb.toString();
    }

    private void pasteToPrompt() {
        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip() != null && cm.getPrimaryClip().getItemCount() > 0) {
            CharSequence clip = cm.getPrimaryClip().getItemAt(0).coerceToText(this);
            if (clip != null) {
                promptInput.setText(clip.toString());
                setStatus("Pasted from clipboard");
                return;
            }
        }
        setStatus("Clipboard is empty");
    }

    private void copyResult() {
        String text = outputBox.getText().toString();
        if (text.trim().isEmpty()) {
            Toast.makeText(this, "Nothing to copy", Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("HEM Enhanced Prompt", text));
            setStatus("Copied to clipboard (" + text.length() + " chars)");
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateSlotStyles() {
        for (int i = 0; i < slotButtons.length; i++) {
            boolean selected = (i == activeSlot);
            styleButton(
                    slotButtons[i],
                    selected ? COLOR_CYAN : COLOR_DARK_BTN,
                    selected ? COLOR_BG : COLOR_MUTED,
                    selected ? COLOR_CYAN : COLOR_BORDER
            );
        }
    }

    private void updateModeStyles() {
        for (int i = 0; i < modeButtons.length; i++) {
            boolean selected = (i == activeMode);
            styleButton(
                    modeButtons[i],
                    selected ? COLOR_AMBER : COLOR_DARK_BTN,
                    selected ? COLOR_BG : COLOR_MUTED,
                    selected ? COLOR_AMBER : COLOR_BORDER
            );
        }
    }

    private void setStatus(String msg) {
        statusText.setText(msg);
    }

    private void addSectionLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(COLOR_CYAN);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tv.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        tv.setPadding(0, dp(14), 0, dp(6));
        root.addView(tv);
    }

    private EditText makeInputBox(String hint, int minLines) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setTextColor(COLOR_TEXT);
        et.setHintTextColor(COLOR_MUTED);
        et.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        et.setMinLines(minLines);
        et.setGravity(Gravity.TOP | Gravity.START);
        et.setPadding(dp(12), dp(10), dp(12), dp(10));
        et.setBackground(makeRoundedBox(COLOR_SURFACE, COLOR_BORDER, dp(8)));
        et.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        return et;
    }

    private Button makeToggleButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(true);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        b.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        b.setPadding(dp(8), dp(8), dp(8), dp(8));
        b.setMinHeight(dp(40));
        b.setMinimumHeight(dp(40));
        return b;
    }

    private Button makeActionButton(String label, int bgColor, int textColor, int borderColor) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(true);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        b.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        b.setPadding(dp(10), dp(10), dp(10), dp(10));
        b.setMinHeight(dp(46));
        b.setMinimumHeight(dp(46));
        styleButton(b, bgColor, textColor, borderColor);
        return b;
    }

    private void styleButton(Button b, int bgColor, int textColor, int borderColor) {
        b.setTextColor(textColor);
        b.setBackground(makeRoundedBox(bgColor, borderColor, dp(8)));
    }

    private GradientDrawable makeRoundedBox(int fillColor, int strokeColor, int radiusPx) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(fillColor);
        gd.setCornerRadius(radiusPx);
        gd.setStroke(dp(1), strokeColor);
        return gd;
    }

    private LinearLayout makeHorizontalRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        return row;
    }

    private void addWeighted(LinearLayout row, View child, int rightMarginPx) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        lp.setMargins(0, 0, rightMarginPx, 0);
        child.setLayoutParams(lp);
        row.addView(child);
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        ));
    }
}
