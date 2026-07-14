package de.danoeh.antennapod.ui.screen.subscriptions;

import androidx.annotation.Nullable;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.storage.preferences.ForkFeedCustomization;

/**
 * Fork feature: resolves the effective cover of a feed with the user's per-feed override
 * applied — a generative tile (with the title's first letter or custom text drawn on top by
 * the caller) or a custom local image instead of the feed's own logo.
 */
public class ForkCoverOverride {

    private ForkCoverOverride() {
    }

    /** Text to draw over the tile (letter or custom text), or null to show the image only. */
    @Nullable
    public static String tileText(Feed feed) {
        return ForkFeedCustomization.getCoverTileText(feed.getId(), feed.getTitle());
    }

    /** Image URL to load for this feed with overrides applied. */
    public static String effectiveImageUrl(Feed feed) {
        if (tileText(feed) != null) {
            return Feed.PREFIX_GENERATIVE_COVER + "fork-cover-" + feed.getId();
        }
        String customImage = ForkFeedCustomization.getCoverImagePath(feed.getId());
        if (customImage != null) {
            return "file://" + customImage;
        }
        return feed.getImageUrl();
    }
}
