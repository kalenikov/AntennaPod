package de.danoeh.antennapod.ui.screen.preferences;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.ActionBar;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.ui.common.ToolbarActivity;

/**
 * Fork feature: shows the migration log and (via root) the recent app logcat, with one-tap
 * Copy/Share so the user can send diagnostics without a computer.
 */
public class LogViewerActivity extends ToolbarActivity {
    public static final String EXTRA_TAB = "tab"; // "migration" | "app"
    private static final int TAB_MIGRATION = 0;
    private static final int TAB_APP = 1;

    private TextView logView;
    private Button tabMigration;
    private Button tabApp;
    private int currentTab = TAB_MIGRATION;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setTitle(R.string.log_viewer_title);
            ab.setDisplayHomeAsUpEnabled(true);
        }
        setContentView(buildContent());

        if ("app".equals(getIntent().getStringExtra(EXTRA_TAB))) {
            currentTab = TAB_APP;
        }
        selectTab(currentTab);
    }

    private View buildContent() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabMigration = new Button(this);
        tabMigration.setText(R.string.log_tab_migration);
        tabMigration.setOnClickListener(v -> selectTab(TAB_MIGRATION));
        tabApp = new Button(this);
        tabApp.setText(R.string.log_tab_app);
        tabApp.setOnClickListener(v -> selectTab(TAB_APP));
        tabs.addView(tabMigration, equalWeight());
        tabs.addView(tabApp, equalWeight());
        root.addView(tabs);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button copy = new Button(this);
        copy.setText(R.string.log_copy);
        copy.setOnClickListener(v -> copyToClipboard());
        Button share = new Button(this);
        share.setText(R.string.log_share);
        share.setOnClickListener(v -> shareLog());
        Button refresh = new Button(this);
        refresh.setText(R.string.log_refresh);
        refresh.setOnClickListener(v -> selectTab(currentTab));
        actions.addView(copy, equalWeight());
        actions.addView(share, equalWeight());
        actions.addView(refresh, equalWeight());
        root.addView(actions);

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        logView = new TextView(this);
        logView.setTypeface(Typeface.MONOSPACE);
        logView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        logView.setTextIsSelectable(true);
        int pad = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 12, getResources().getDisplayMetrics());
        logView.setPadding(pad, pad, pad, pad);
        scroll.addView(logView);
        root.addView(scroll);
        return root;
    }

    private LinearLayout.LayoutParams equalWeight() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    }

    private void selectTab(int tab) {
        currentTab = tab;
        tabMigration.setTypeface(null, tab == TAB_MIGRATION ? Typeface.BOLD : Typeface.NORMAL);
        tabApp.setTypeface(null, tab == TAB_APP ? Typeface.BOLD : Typeface.NORMAL);
        logView.setText(R.string.log_loading);
        Context appContext = getApplicationContext();
        new Thread(() -> {
            final String text = tab == TAB_APP
                    ? RootMigrator.readAppLogcat()
                    : firstNonEmpty(RootMigrator.readMigrationLog(appContext),
                        getString(R.string.log_empty));
            runOnUiThread(() -> logView.setText(text));
        }).start();
    }

    private String firstNonEmpty(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private void copyToClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("KalenikovPod log", logView.getText().toString()));
        Toast.makeText(this, R.string.log_copied, Toast.LENGTH_SHORT).show();
    }

    private void shareLog() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, logView.getText().toString());
        startActivity(Intent.createChooser(intent, getString(R.string.log_share)));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
