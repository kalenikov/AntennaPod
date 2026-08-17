package de.danoeh.antennapod.storage.preferences;

import de.danoeh.antennapod.model.feed.FeedItem;

/**
 * Fork feature: keep a downloaded episode in the inbox.
 *
 * <p>Upstream drops the NEW flag twice on the download path: once when the episode is enqueued
 * ({@code DBWriter.addQueueItem}) and again when the file has arrived
 * ({@code FeedMedia.setDownloaded} flips NEW to UNPLAYED). With this switch on, both are
 * neutralized, so the episode only leaves the inbox when it is played or removed by hand.
 * Manually adding an episode to the queue keeps the upstream behavior.
 */
public final class ForkInboxRetention {

    private ForkInboxRetention() {
    }

    /**
     * @return whether downloads should leave the NEW (inbox) flag alone
     */
    public static boolean isEnabled() {
        return UserPreferences.getForkKeepNewOnDownload();
    }

    /**
     * Restores the NEW flag that {@code FeedMedia.setDownloaded} has just cleared.
     *
     * @param item   the downloaded episode, may be {@code null}
     * @param wasNew whether the episode was in the inbox before the download finished
     */
    public static void restoreNewAfterDownload(FeedItem item, boolean wasNew) {
        if (item != null && wasNew && isEnabled()) {
            item.setNew();
        }
    }
}
