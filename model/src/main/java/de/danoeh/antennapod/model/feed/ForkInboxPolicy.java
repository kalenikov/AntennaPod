package de.danoeh.antennapod.model.feed;

/**
 * Fork feature: whether an episode is allowed to leave the inbox by itself.
 *
 * <p>Upstream clears the NEW flag from many places on the playback path, and most of them go
 * through {@link FeedMedia#setPosition(int)} — the playback service, the position observer, the
 * list row, the cast player and the Wear listener all call it. Guarding every caller would mean
 * touching all of them, so the guard sits in the single method they share.
 *
 * <p>The {@code model} module cannot read preferences (that dependency points the other way), so
 * {@code UserPreferences.init} installs a source here that reads the live preference value. Until
 * it does, the answer is {@code false} and upstream behavior is untouched. The value is read on
 * every call rather than mirrored, because the switch is written straight to SharedPreferences by
 * the settings screen and a cached copy would go stale.
 *
 * <p>The download path is not handled here: {@code FeedMedia.setDownloaded} has a single relevant
 * caller, {@code MediaDownloadedHandler}, which restores the flag itself.
 */
public final class ForkInboxPolicy {

    /** Reads the fork preference. Separate interface so that this module needs no Java 8 desugaring. */
    public interface Source {
        boolean keepInInbox();
    }

    private static volatile Source source = null;

    private ForkInboxPolicy() {
    }

    public static void setSource(Source source) {
        ForkInboxPolicy.source = source;
    }

    /**
     * @return whether episodes must stay in the inbox until they are played to the end or removed by hand
     */
    public static boolean keepInInbox() {
        Source current = source;
        return current != null && current.keepInInbox();
    }
}
