package de.danoeh.antennapod;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.FeedDatabaseWriter;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Instrumented test for the fork feature: every episode that is new to AntennaPod is marked NEW (inbox),
 * ignoring the upstream pubDate cutoff — so a fresh subscription's whole catalog and older episodes
 * back-filled by a widened Pinchflat cutoff both land in the inbox. Exercises FeedDatabaseWriter.updateFeed.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ForkInboxNewEpisodesTest {

    private static final long DAY = 24L * 60 * 60 * 1000;

    private Feed subscribedFeed(String id) {
        return new Feed(0, null, "KP Inbox " + id, "http://kp.test/", "desc",
                null, "author", "en", Feed.TYPE_RSS2, "kp-inbox-" + id, null, null,
                "http://kp.test/inbox-" + id + ".xml", System.currentTimeMillis());
    }

    private FeedItem item(Feed feed, String guid, Date pubDate, int state) {
        FeedItem it = new FeedItem(0, "Item " + guid, guid, "http://kp.test/" + guid,
                pubDate, state, feed);
        it.setMedia(new FeedMedia(0, it, 0, 0, 12345, "audio/mp3",
                null, "http://kp.test/" + guid + ".mp3", 0, null, 0, 0));
        return it;
    }

    @Test
    public void firstSubscription_marksWholeCatalogNew() {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Feed feed = subscribedFeed("first");
        List<FeedItem> items = new ArrayList<>();
        items.add(item(feed, "first-a", new Date(System.currentTimeMillis() - 40 * DAY), FeedItem.UNPLAYED));
        items.add(item(feed, "first-b", new Date(System.currentTimeMillis() - 30 * DAY), FeedItem.UNPLAYED));
        feed.setItems(items);

        FeedDatabaseWriter.updateFeed(ctx, feed, false);

        FeedItem a = DBReader.getFeedItemByGuidOrEpisodeUrl("first-a", "http://kp.test/first-a.mp3");
        FeedItem b = DBReader.getFeedItemByGuidOrEpisodeUrl("first-b", "http://kp.test/first-b.mp3");
        assertNotNull(a);
        assertNotNull(b);
        assertTrue("whole back-catalog of a new subscription should be NEW (in inbox)", a.isNew());
        assertTrue("whole back-catalog of a new subscription should be NEW (in inbox)", b.isNew());
    }

    @Test
    public void widenedCutoff_olderEpisodeBecomesNew() {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // Existing subscription with one recent, already-played episode.
        Feed feed1 = subscribedFeed("cutoff");
        Date recentDate = new Date(System.currentTimeMillis() - DAY);
        List<FeedItem> initial = new ArrayList<>();
        initial.add(item(feed1, "cutoff-recent", recentDate, FeedItem.PLAYED));
        feed1.setItems(initial);
        FeedDatabaseWriter.updateFeed(ctx, feed1, false);

        // Feed now also exposes an older episode (widened Pinchflat cutoff).
        Feed feed2 = subscribedFeed("cutoff");
        List<FeedItem> updated = new ArrayList<>();
        updated.add(item(feed2, "cutoff-recent", recentDate, FeedItem.PLAYED));
        updated.add(item(feed2, "cutoff-older", new Date(System.currentTimeMillis() - 60 * DAY), FeedItem.UNPLAYED));
        feed2.setItems(updated);
        FeedDatabaseWriter.updateFeed(ctx, feed2, false);

        FeedItem older = DBReader.getFeedItemByGuidOrEpisodeUrl("cutoff-older", "http://kp.test/cutoff-older.mp3");
        FeedItem recent = DBReader.getFeedItemByGuidOrEpisodeUrl("cutoff-recent", "http://kp.test/cutoff-recent.mp3");
        assertNotNull(older);
        assertTrue("older back-filled episode should become NEW despite its earlier date", older.isNew());
        // Control: an existing, already-played episode must not be flipped back to NEW.
        assertFalse("existing played episode must stay played, not NEW", recent.isNew());
    }
}
