package de.danoeh.antennapod.ui.screen.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import de.danoeh.antennapod.R;

/**
 * Fork changelog: one entry per release, newest first. Shown automatically once after each
 * update (see {@link #showIfUpdated}) and on demand from the KalenikovPod settings screen.
 * Hand-maintained — add an entry (and its strings) with every fork release.
 */
public class ForkChangelog {

    /** {version, date, changes} — NEWEST FIRST. */
    private static final int[][] ENTRIES = {
            {R.string.fork_cl_v35, R.string.fork_cl_v35_date, R.string.fork_cl_v35_text},
            {R.string.fork_cl_v34, R.string.fork_cl_v34_date, R.string.fork_cl_v34_text},
            {R.string.fork_cl_v33, R.string.fork_cl_v33_date, R.string.fork_cl_v33_text},
            {R.string.fork_cl_v32, R.string.fork_cl_v32_date, R.string.fork_cl_v32_text},
            {R.string.fork_cl_v31, R.string.fork_cl_v31_date, R.string.fork_cl_v31_text},
            {R.string.fork_cl_v30, R.string.fork_cl_v30_date, R.string.fork_cl_v30_text},
            {R.string.fork_cl_v29, R.string.fork_cl_v29_date, R.string.fork_cl_v29_text},
            {R.string.fork_cl_v28, R.string.fork_cl_v28_date, R.string.fork_cl_v28_text},
            {R.string.fork_cl_v27, R.string.fork_cl_v27_date, R.string.fork_cl_v27_text},
            {R.string.fork_cl_v26, R.string.fork_cl_v26_date, R.string.fork_cl_v26_text},
            {R.string.fork_cl_v25, R.string.fork_cl_v25_date, R.string.fork_cl_v25_text},
            {R.string.fork_cl_v24, R.string.fork_cl_v24_date, R.string.fork_cl_v24_text},
            {R.string.fork_cl_v23, R.string.fork_cl_v23_date, R.string.fork_cl_v23_text},
            {R.string.fork_cl_v22, R.string.fork_cl_v22_date, R.string.fork_cl_v22_text},
            {R.string.fork_cl_v21, R.string.fork_cl_v21_date, R.string.fork_cl_v21_text},
            {R.string.fork_cl_v20, R.string.fork_cl_v20_date, R.string.fork_cl_v20_text},
            {R.string.fork_cl_v19, R.string.fork_cl_v19_date, R.string.fork_cl_v19_text},
            {R.string.fork_cl_v18, R.string.fork_cl_v18_date, R.string.fork_cl_v18_text},
            {R.string.fork_cl_v17, R.string.fork_cl_v17_date, R.string.fork_cl_v17_text},
            {R.string.fork_cl_v16, R.string.fork_cl_v16_date, R.string.fork_cl_v16_text},
            {R.string.fork_cl_v15, R.string.fork_cl_v15_date, R.string.fork_cl_v15_text},
            {R.string.fork_cl_v14, R.string.fork_cl_v14_date, R.string.fork_cl_v14_text},
            {R.string.fork_cl_v13, R.string.fork_cl_v13_date, R.string.fork_cl_v13_text},
            {R.string.fork_cl_v12, R.string.fork_cl_v12_date, R.string.fork_cl_v12_text},
            {R.string.fork_cl_v11, R.string.fork_cl_v11_date, R.string.fork_cl_v11_text},
            {R.string.fork_cl_v10, R.string.fork_cl_v10_date, R.string.fork_cl_v10_text},
            {R.string.fork_cl_v9, R.string.fork_cl_v9_date, R.string.fork_cl_v9_text},
            {R.string.fork_cl_v8, R.string.fork_cl_v8_date, R.string.fork_cl_v8_text},
            {R.string.fork_cl_v7, R.string.fork_cl_v7_date, R.string.fork_cl_v7_text},
            {R.string.fork_cl_v6, R.string.fork_cl_v6_date, R.string.fork_cl_v6_text},
            {R.string.fork_cl_v5, R.string.fork_cl_v5_date, R.string.fork_cl_v5_text},
            {R.string.fork_cl_v4, R.string.fork_cl_v4_date, R.string.fork_cl_v4_text},
            {R.string.fork_cl_v3, R.string.fork_cl_v3_date, R.string.fork_cl_v3_text},
            {R.string.fork_cl_v2, R.string.fork_cl_v2_date, R.string.fork_cl_v2_text},
            {R.string.fork_cl_v1, R.string.fork_cl_v1_date, R.string.fork_cl_v1_text},
    };

    private static final String PREF_LAST_SEEN_FORK_VERSION = "prefLastSeenForkVersion";

    private ForkChangelog() {
    }

    /** Shows the changelog dialog (manual entry point from settings). */
    public static void show(Context context) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.fork_changelog_title)
                .setView(buildDialogView(context))
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    /**
     * Shows the changelog once after an update: compares the fork version stored in prefs with
     * the running build. On a fresh install nothing is shown — the current version is just
     * recorded, so the dialog only ever appears for versions the user skipped over.
     */
    public static void showIfUpdated(Context context, SharedPreferences prefs, boolean firstLaunch) {
        if (shouldShowAndMark(prefs, firstLaunch)) {
            show(context);
        }
    }

    /**
     * Decides whether the "What's new" dialog is due and records the current version in prefs.
     * Public static so the decision logic can be tested without launching MainActivity (which
     * never reaches RESUMED under instrumentation because of system permission dialogs).
     */
    public static boolean shouldShowAndMark(SharedPreferences prefs, boolean firstLaunch) {
        String lastSeen = prefs.getString(PREF_LAST_SEEN_FORK_VERSION, null);
        boolean updated = ForkUpdateChecker.parseForkNumber(lastSeen)
                < ForkUpdateChecker.parseForkNumber(ForkUpdateChecker.FORK_VERSION);
        if (lastSeen == null && firstLaunch) {
            updated = false;
        }
        if (updated || lastSeen == null) {
            prefs.edit().putString(PREF_LAST_SEEN_FORK_VERSION, ForkUpdateChecker.FORK_VERSION).apply();
        }
        return updated;
    }

    public static View buildDialogView(Context context) {
        ScrollView scroll = new ScrollView(context);
        LinearLayout list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(context, 20);
        list.setPadding(pad, dp(context, 8), pad, 0);
        scroll.addView(list);

        for (int[] entry : ENTRIES) {
            LinearLayout header = new LinearLayout(context);
            header.setOrientation(LinearLayout.HORIZONTAL);

            TextView version = new TextView(context);
            version.setText(entry[0]);
            version.setTypeface(null, Typeface.BOLD);
            version.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            header.addView(version, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView date = new TextView(context);
            date.setText(entry[1]);
            date.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            date.setGravity(Gravity.END);
            date.setAlpha(0.6f);
            header.addView(date, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            list.addView(header);

            TextView text = new TextView(context);
            text.setText(entry[2]);
            text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            textParams.topMargin = dp(context, 2);
            textParams.bottomMargin = dp(context, 16);
            text.setLayoutParams(textParams);
            list.addView(text);
        }
        return scroll;
    }

    private static int dp(Context context, int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics());
    }
}
