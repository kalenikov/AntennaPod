package de.danoeh.antennapod.ui.screen.subscriptions;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import de.danoeh.antennapod.storage.preferences.UserPreferences;

/**
 * Fork feature: compact mode for the single-column subscription list — smaller covers and
 * denser rows (72dp -> 44dp). Sizes are applied programmatically on top of the stock
 * subscription_list_item layout, so no duplicated layout file has to be kept in sync with
 * upstream. Static and UI-free apart from the view parameter, so the sizing rules can be
 * tested directly.
 */
public class ForkCompactList {

    public static final int COVER_DP_NORMAL = 56;
    public static final int COVER_DP_COMPACT = 32;
    public static final int CORNER_DP_NORMAL = 12;
    public static final int CORNER_DP_COMPACT = 6;
    public static final int ROW_PADDING_DP_NORMAL = 8;
    public static final int ROW_PADDING_DP_COMPACT = 6;
    public static final int TITLE_MAX_LINES_NORMAL = 2;
    public static final int TITLE_MAX_LINES_COMPACT = 1;
    public static final int COUNT_SP_NORMAL = 14;
    public static final int COUNT_SP_COMPACT = 12;
    public static final int PIN_DP_NORMAL = 16;
    public static final int PIN_DP_COMPACT = 10;
    public static final int ERROR_DP_NORMAL = 24;
    public static final int ERROR_DP_COMPACT = 18;
    public static final int ERROR_MARGIN_DP_NORMAL = 8;
    public static final int ERROR_MARGIN_DP_COMPACT = 4;

    private ForkCompactList() {
    }

    public static boolean isEnabled() {
        return UserPreferences.getForkCompactSubscriptionList();
    }

    public static void setEnabled(boolean enabled) {
        UserPreferences.setForkCompactSubscriptionList(enabled);
    }

    /** Compact mode only makes sense for the single-column list, not for the grid. */
    public static boolean appliesTo(int columnCount) {
        return columnCount == 1;
    }

    public static int coverDp(boolean compact) {
        return compact ? COVER_DP_COMPACT : COVER_DP_NORMAL;
    }

    public static int cornerDp(boolean compact) {
        return compact ? CORNER_DP_COMPACT : CORNER_DP_NORMAL;
    }

    public static int rowPaddingDp(boolean compact) {
        return compact ? ROW_PADDING_DP_COMPACT : ROW_PADDING_DP_NORMAL;
    }

    public static int titleMaxLines(boolean compact) {
        return compact ? TITLE_MAX_LINES_COMPACT : TITLE_MAX_LINES_NORMAL;
    }

    public static int countSp(boolean compact) {
        return compact ? COUNT_SP_COMPACT : COUNT_SP_NORMAL;
    }

    public static int pinDp(boolean compact) {
        return compact ? PIN_DP_COMPACT : PIN_DP_NORMAL;
    }

    public static int errorDp(boolean compact) {
        return compact ? ERROR_DP_COMPACT : ERROR_DP_NORMAL;
    }

    public static int errorMarginDp(boolean compact) {
        return compact ? ERROR_MARGIN_DP_COMPACT : ERROR_MARGIN_DP_NORMAL;
    }

    /**
     * Applies or reverts compact sizing on an already bound subscription row. Both states are
     * always written explicitly, because the holders are recycled and would otherwise keep the
     * sizes of the mode they were bound in before.
     */
    public static void apply(View itemView, View coverImage, TextView title, TextView count,
                             @Nullable View pinIcon, @Nullable View errorIcon, int columnCount) {
        if (!appliesTo(columnCount)) {
            return;
        }
        boolean compact = isEnabled();

        View coverContainer = rowChildOf(itemView, coverImage);
        if (coverContainer != null) {
            setSize(coverContainer, dp(itemView, coverDp(compact)));
            if (coverContainer instanceof CardView) {
                ((CardView) coverContainer).setRadius(dp(itemView, cornerDp(compact)));
            }
        }

        int rowPadding = dp(itemView, rowPaddingDp(compact));
        itemView.setPadding(itemView.getPaddingLeft(), rowPadding, itemView.getPaddingRight(), rowPadding);

        title.setMaxLines(titleMaxLines(compact));
        int titlePadding = title.getPaddingLeft();
        title.setPadding(titlePadding, compact ? 0 : titlePadding, titlePadding, compact ? 0 : titlePadding);

        count.setTextSize(countSp(compact));
        setSize(pinIcon, dp(itemView, pinDp(compact)));
        setSize(errorIcon, dp(itemView, errorDp(compact)));
        setMargin(errorIcon, dp(itemView, errorMarginDp(compact)));
    }

    /**
     * Walks up from the cover to the direct child of the row root. The cover CardView has no id
     * in the layout, so this stays correct even if upstream adds or removes a wrapper around it.
     */
    @Nullable
    private static View rowChildOf(View itemView, View descendant) {
        View view = descendant;
        while (view != null && view.getParent() instanceof View && view.getParent() != itemView) {
            view = (View) view.getParent();
        }
        return view != null && view.getParent() == itemView ? view : null;
    }

    private static void setSize(@Nullable View view, int size) {
        if (view == null) {
            return;
        }
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params != null && (params.width != size || params.height != size)) {
            params.width = size;
            params.height = size;
            view.setLayoutParams(params);
        }
    }

    private static void setMargin(@Nullable View view, int margin) {
        if (view == null) {
            return;
        }
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) params;
            if (marginParams.leftMargin != margin) {
                marginParams.setMargins(margin, margin, margin, margin);
                view.setLayoutParams(marginParams);
            }
        }
    }

    private static int dp(View view, float value) {
        return (int) (value * view.getResources().getDisplayMetrics().density + 0.5f);
    }
}
