package de.danoeh.antennapod;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.ui.episodeslist.ForkYoutube;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Fork feature test: {@link ForkYoutube#videoUrl} recognizes the YouTube video link that Pinchflat
 * puts into every feed item, and stays silent for everything else — the "open on YouTube" context
 * menu entry is shown based on it.
 */
@RunWith(AndroidJUnit4.class)
public class ForkYoutubeLinkTest {

    private FeedItem itemWithLink(String link) {
        Feed feed = new Feed(0, null, "KP YouTube", "https://youtube.com/playlist?list=PLTest", "desc",
                null, "author", "en", Feed.TYPE_RSS2, "kp-yt", null, null,
                "http://192.168.1.87:8945/sources/2da1e161-e065-4fd3-8984-e01c4083f1c6/feed.xml",
                System.currentTimeMillis());
        return new FeedItem(0, "Item", "kp-yt-guid", link, new Date(), FeedItem.UNPLAYED, feed);
    }

    @Test
    public void recognizesPinchflatEpisodeLink() {
        String link = "https://www.youtube.com/watch?v=UxnSVXq8Rwk";
        assertEquals(link, ForkYoutube.videoUrl(itemWithLink(link)));
        assertTrue(ForkYoutube.hasVideo(itemWithLink(link)));
    }

    @Test
    public void recognizesOtherYoutubeUrlShapes() {
        assertEquals("https://youtu.be/UxnSVXq8Rwk",
                ForkYoutube.videoUrl(itemWithLink("https://youtu.be/UxnSVXq8Rwk")));
        assertEquals("https://m.youtube.com/watch?v=UxnSVXq8Rwk&t=42s",
                ForkYoutube.videoUrl(itemWithLink("https://m.youtube.com/watch?v=UxnSVXq8Rwk&t=42s")));
        assertEquals("https://www.youtube.com/shorts/abc123",
                ForkYoutube.videoUrl(itemWithLink("https://www.youtube.com/shorts/abc123")));
        assertEquals("https://www.youtube.com/live/abc123",
                ForkYoutube.videoUrl(itemWithLink("https://www.youtube.com/live/abc123")));
        assertEquals("http://youtube.com/watch?v=UxnSVXq8Rwk",
                ForkYoutube.videoUrl(itemWithLink("http://youtube.com/watch?v=UxnSVXq8Rwk")));
        // surrounding whitespace from the feed must not hide the link
        assertEquals("https://www.youtube.com/watch?v=UxnSVXq8Rwk",
                ForkYoutube.videoUrl(itemWithLink("  https://www.youtube.com/watch?v=UxnSVXq8Rwk  ")));
    }

    @Test
    public void ignoresNonEpisodeLinks() {
        // channel and playlist pages are not a single video
        assertNull(ForkYoutube.videoUrl(itemWithLink("https://www.youtube.com/@PolinaPars")));
        assertNull(ForkYoutube.videoUrl(itemWithLink("https://youtube.com/playlist?list=PLTest")));
        assertNull(ForkYoutube.videoUrl(itemWithLink("https://example.com/episode/1")));
        assertNull(ForkYoutube.videoUrl(itemWithLink("http://192.168.1.87:8945/media/1/stream")));
        // a look-alike host must not pass
        assertNull(ForkYoutube.videoUrl(itemWithLink("https://notyoutube.com/watch?v=UxnSVXq8Rwk")));
    }

    @Test
    public void ignoresMissingLinkAndItem() {
        assertNull(ForkYoutube.videoUrl(itemWithLink(null)));
        assertNull(ForkYoutube.videoUrl(null));
        assertFalse(ForkYoutube.hasVideo(itemWithLink(null)));
        // no fallback to the feed link: the feed points at the whole playlist, not this episode
        assertFalse(ForkYoutube.hasVideo(itemWithLink("")));
    }
}
