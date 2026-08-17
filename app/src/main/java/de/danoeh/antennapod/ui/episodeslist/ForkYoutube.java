package de.danoeh.antennapod.ui.episodeslist;

import java.util.regex.Pattern;

import de.danoeh.antennapod.model.feed.FeedItem;

/**
 * Fork helper: recognizes episodes that came from YouTube so the episode context menu can offer
 * to open the original video.
 *
 * <p>Pinchflat puts the video page into each item's {@code <link>}, e.g.
 * {@code https://www.youtube.com/watch?v=UxnSVXq8Rwk}, and AntennaPod stores it as the item link.
 * Only the item's own link is used — {@link FeedItem#getLinkWithFallback()} would fall back to the
 * feed link, which points at the whole channel or playlist instead of this episode.
 */
public final class ForkYoutube {

    private static final Pattern YOUTUBE_LINK = Pattern.compile(
            "^https?://(?:(?:www\\.|m\\.|music\\.)?(?:youtube\\.com|youtube-nocookie\\.com)"
                    + "/(?:watch\\?|shorts/|live/|embed/|v/)|youtu\\.be/)\\S+$",
            Pattern.CASE_INSENSITIVE);

    private ForkYoutube() {
    }

    /**
     * @param item the episode
     * @return the YouTube video URL of this episode, or {@code null} if it does not have one
     */
    public static String videoUrl(FeedItem item) {
        if (item == null || item.getLink() == null) {
            return null;
        }
        String link = item.getLink().trim();
        return YOUTUBE_LINK.matcher(link).matches() ? link : null;
    }

    /**
     * @param item the episode
     * @return whether the "open on YouTube" menu item should be shown for this episode
     */
    public static boolean hasVideo(FeedItem item) {
        return videoUrl(item) != null;
    }
}
