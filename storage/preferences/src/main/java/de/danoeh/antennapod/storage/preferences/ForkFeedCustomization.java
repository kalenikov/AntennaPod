package de.danoeh.antennapod.storage.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Fork feature: per-feed customization of the subscriptions list — pinning feeds to the top
 * and overriding the cover (first letter of the title, custom short text, or a custom image).
 * Stored in its own SharedPreferences file keyed by feed id, so no database migration is
 * needed and feed refreshes can't clobber the override. Lives in this module so that
 * DBReader (storage:database) can apply pinning when sorting.
 */
public class ForkFeedCustomization {
    private static final String PREF_NAME = "ForkFeedCustomization";
    private static final String PREF_PINNED_FEEDS = "pinnedFeeds";
    private static final String PREF_COVER_PREFIX = "cover.";

    public static final String COVER_LETTER = "letter";
    public static final String COVER_TEXT_PREFIX = "text:";
    public static final String COVER_IMAGE_PREFIX = "image:";

    private static SharedPreferences prefs;

    private ForkFeedCustomization() {
    }

    public static void init(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isPinned(long feedId) {
        return prefs != null && getPinned().contains(String.valueOf(feedId));
    }

    public static void setPinned(long feedId, boolean pinned) {
        Set<String> ids = new HashSet<>(getPinned());
        if (pinned) {
            ids.add(String.valueOf(feedId));
        } else {
            ids.remove(String.valueOf(feedId));
        }
        prefs.edit().putStringSet(PREF_PINNED_FEEDS, ids).apply();
    }

    private static Set<String> getPinned() {
        return prefs.getStringSet(PREF_PINNED_FEEDS, Collections.emptySet());
    }

    /** Pinned feeds first; 0 when both or neither are pinned (fall back to the base order). */
    public static int comparePinned(long lhsFeedId, long rhsFeedId) {
        return Boolean.compare(isPinned(rhsFeedId), isPinned(lhsFeedId));
    }

    /**
     * Raw cover override: null (none), {@link #COVER_LETTER}, "text:&lt;value&gt;" or
     * "image:&lt;absolute path&gt;".
     */
    @Nullable
    public static String getCoverOverride(long feedId) {
        return prefs == null ? null : prefs.getString(PREF_COVER_PREFIX + feedId, null);
    }

    public static void setCoverOverride(long feedId, @Nullable String value) {
        SharedPreferences.Editor editor = prefs.edit();
        if (value == null) {
            editor.remove(PREF_COVER_PREFIX + feedId);
        } else {
            editor.putString(PREF_COVER_PREFIX + feedId, value);
        }
        editor.apply();
    }

    /** Text to show on the cover tile, or null when the real cover image should be shown. */
    @Nullable
    public static String getCoverTileText(long feedId, @Nullable String feedTitle) {
        String override = getCoverOverride(feedId);
        if (COVER_LETTER.equals(override)) {
            return feedTitle == null || feedTitle.isEmpty() ? "?"
                    : feedTitle.substring(0, feedTitle.offsetByCodePoints(0, 1)).toUpperCase();
        } else if (override != null && override.startsWith(COVER_TEXT_PREFIX)) {
            return override.substring(COVER_TEXT_PREFIX.length());
        }
        return null;
    }

    /** Path of the custom cover image, or null. */
    @Nullable
    public static String getCoverImagePath(long feedId) {
        String override = getCoverOverride(feedId);
        if (override != null && override.startsWith(COVER_IMAGE_PREFIX)) {
            return override.substring(COVER_IMAGE_PREFIX.length());
        }
        return null;
    }
}
