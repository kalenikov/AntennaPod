package de.danoeh.antennapod.storage.preferences;

import de.danoeh.antennapod.model.feed.FeedItem;

/**
 * Fork feature: keep an episode in the inbox while it is being downloaded and listened to.
 *
 * <p>Upstream drops the NEW flag from five places. On the download path: when the episode is
 * enqueued ({@code DBWriter.addQueueItem}) and when the file has arrived
 * ({@code FeedMedia.setDownloaded} flips NEW to UNPLAYED). On the playback path: the automatic
 * enqueueing done by both playback services when playback starts, and the position update
 * ({@code PlayableUtils.saveCurrentPosition} writes UNPLAYED to the database, while
 * {@code FeedMedia.setPosition} flips the in-memory copy).
 *
 * <p>With this switch on, all of them are neutralized, so the episode leaves the inbox only when
 * it has been played to the end (upstream then marks it PLAYED), when it is marked as played by
 * hand, or when it is removed from the inbox by swipe or menu. Because the state is a single
 * field with three values, a half-listened episode stays flagged NEW and keeps counting towards
 * the inbox badge — that is inherent, not a defect.
 *
 * <p>Manually adding an episode to the queue keeps the upstream behavior.
 *
 * <p>The in-memory flip in {@code FeedMedia.setPosition} is guarded by
 * {@code ForkInboxPolicy} instead, because the {@code model} module cannot read preferences.
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
