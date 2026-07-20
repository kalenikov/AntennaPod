package de.danoeh.antennapod.ui.screen.subscriptions;

import android.app.Activity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedCounter;
import de.danoeh.antennapod.storage.preferences.ForkFeedCustomization;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.CoverLoader;
import de.danoeh.antennapod.ui.common.ThemeUtils;

import java.lang.ref.WeakReference;
import java.text.NumberFormat;

public class SubscriptionViewHolder extends RecyclerView.ViewHolder {
    public final TextView title;
    public final ImageView coverImage;
    public final TextView count;
    public final TextView fallbackTitle;
    public final ImageView gradient;
    public final ImageView pinIcon;
    public final ImageView selectIcon;
    public final CardView card;
    public final View errorIcon;
    public final WeakReference<Activity> mainActivityRef;

    public SubscriptionViewHolder(@NonNull View itemView, Activity mainActivity) {
        super(itemView);
        title = itemView.findViewById(R.id.titleLabel);
        coverImage = itemView.findViewById(R.id.coverImage);
        count = itemView.findViewById(R.id.countViewPill);
        fallbackTitle = itemView.findViewById(R.id.fallbackTitleLabel);
        gradient = itemView.findViewById(R.id.gradientOverlay);
        pinIcon = itemView.findViewById(R.id.pinIcon);
        selectIcon = itemView.findViewById(R.id.selectedIcon);
        card = itemView.findViewById(R.id.outerContainer);
        errorIcon = itemView.findViewById(R.id.errorIcon);
        this.mainActivityRef = new WeakReference<>(mainActivity);
    }

    public void bind(Feed feed, int columnCount, int counter, int counter2) {
        title.setText(feed.getTitle());
        fallbackTitle.setText(feed.getTitle());
        coverImage.setContentDescription(feed.getTitle());

        // Fork: two independent counters shown in one pill as "counter1/counter2". A disabled
        // (SHOW_NONE) counter is omitted; a single active counter behaves as before.
        boolean showFirst = UserPreferences.getFeedCounterSetting() != FeedCounter.SHOW_NONE;
        boolean showSecond = UserPreferences.getFeedCounterSetting2() != FeedCounter.SHOW_NONE;
        String counterText = null;
        NumberFormat nf = NumberFormat.getInstance();
        if (showFirst && showSecond) {
            if (counter > 0 || counter2 > 0) {
                counterText = nf.format(counter) + "/" + nf.format(counter2);
            }
        } else if (showFirst) {
            if (counter > 0) {
                counterText = nf.format(counter);
            }
        } else if (showSecond) {
            if (counter2 > 0) {
                counterText = nf.format(counter2);
            }
        }
        if (counterText != null) {
            count.setText(counterText);
            count.setVisibility(View.VISIBLE);
        } else {
            count.setVisibility(View.GONE);
        }

        CoverLoader coverLoader = new CoverLoader();
        boolean textAndImageCombined = feed.isLocalFeed() && feed.getImageUrl() != null
                && feed.getImageUrl().startsWith(Feed.PREFIX_GENERATIVE_COVER);
        String tileText = ForkCoverOverride.tileText(feed);
        if (tileText != null) {
            fallbackTitle.setText(tileText);
            textAndImageCombined = true;
        }
        coverLoader.withUri(ForkCoverOverride.effectiveImageUrl(feed));
        errorIcon.setVisibility(feed.hasLastUpdateFailed() ? View.VISIBLE : View.GONE);
        if (pinIcon != null) {
            pinIcon.setVisibility(
                    ForkFeedCustomization.isPinned(feed.getId()) ? View.VISIBLE : View.GONE);
        }

        if ((UserPreferences.shouldShowSubscriptionTitle() || columnCount == 1) && tileText == null) {
            // No need for fallback title when already showing title
            fallbackTitle.setVisibility(View.GONE);
        } else {
            coverLoader.withPlaceholderView(fallbackTitle, textAndImageCombined);
        }
        coverLoader.withCoverView(coverImage);
        coverLoader.load();

        if (card != null) {
            card.setCardBackgroundColor(ThemeUtils.getColorFromAttr(
                    mainActivityRef.get(), R.attr.colorSurfaceContainer));
        }

        int textPadding = columnCount <= 3 ? 16 : 8;
        title.setPadding(textPadding, textPadding, textPadding, textPadding);
        fallbackTitle.setPadding(textPadding, textPadding, textPadding, textPadding);

        int textSize = 14;
        if (columnCount == 3) {
            textSize = 15;
        } else if (columnCount == 2) {
            textSize = 16;
        }
        title.setTextSize(textSize);
        fallbackTitle.setTextSize(textSize);
    }
}