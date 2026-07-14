package de.danoeh.antennapod.ui.screen.preferences;

import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.ActionBar;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.ui.common.ToolbarActivity;

/**
 * Fork feature: a plain, scrollable list of the extras KalenikovPod adds on top of stock
 * AntennaPod. Hand-maintained — add a row to {@link #FEATURES} whenever a new fork feature ships.
 * Lives next to the log viewer in Settings and touches none of AntennaPod's own navigation.
 */
public class ForkFeaturesActivity extends ToolbarActivity {

    /** {title, description, "added in" version} — newest last. */
    private static final int[][] FEATURES = {
            {R.string.fork_feat_delete_title, R.string.fork_feat_delete_desc, R.string.fork_feat_v1},
            {R.string.fork_feat_migrate_title, R.string.fork_feat_migrate_desc, R.string.fork_feat_v2},
            {R.string.fork_feat_logs_title, R.string.fork_feat_logs_desc, R.string.fork_feat_v2},
            {R.string.fork_feat_ytcull_title, R.string.fork_feat_ytcull_desc, R.string.fork_feat_v5},
            {R.string.fork_feat_watchedsync_title, R.string.fork_feat_watchedsync_desc, R.string.fork_feat_v5},
            {R.string.fork_feat_update_title, R.string.fork_feat_update_desc, R.string.fork_feat_v7},
            {R.string.fork_feat_changelog_title, R.string.fork_feat_changelog_desc, R.string.fork_feat_v8},
            {R.string.fork_feat_settings_title, R.string.fork_feat_settings_desc, R.string.fork_feat_v9},
            {R.string.fork_feat_pincover_title, R.string.fork_feat_pincover_desc, R.string.fork_feat_v10},
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setTitle(R.string.fork_features_title);
            ab.setDisplayHomeAsUpEnabled(true);
        }
        setContentView(buildContent());
    }

    private View buildContent() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        list.setPadding(pad, pad, pad, pad);
        scroll.addView(list);

        for (int[] feature : FEATURES) {
            list.addView(buildCard(feature[0], feature[1], feature[2]));
        }
        return scroll;
    }

    private View buildCard(int titleRes, int descRes, int versionRes) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(20);
        card.setLayoutParams(cardParams);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);

        TextView title = new TextView(this);
        title.setText(titleRes);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView version = new TextView(this);
        version.setText(versionRes);
        version.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        version.setGravity(Gravity.END);
        version.setAlpha(0.6f);
        header.addView(version, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        card.addView(header);

        TextView desc = new TextView(this);
        desc.setText(descRes);
        desc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        descParams.topMargin = dp(4);
        desc.setLayoutParams(descParams);
        card.addView(desc);
        return card;
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
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
