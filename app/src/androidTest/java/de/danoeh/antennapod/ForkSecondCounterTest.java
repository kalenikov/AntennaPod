package de.danoeh.antennapod;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedCounter;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.PodDBAdapter;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Instrumented test for the fork's second subscription counter. Verifies the new SHOW_TOTAL mode
 * (counts every episode) and that different counter modes produce independent counts on the same
 * feed — which is what makes two independent counters possible.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ForkSecondCounterTest {

    private long count(FeedCounter setting, long feedId) {
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try {
            Map<Long, Integer> map = adapter.getFeedCounters(setting, feedId);
            Integer v = map.get(feedId);
            return v == null ? 0 : v;
        } finally {
            adapter.close();
        }
    }

    @Test
    public void totalCountsAll_whileModesStayIndependent() {
        Feed feed = new Feed(0, null, "KP Counter Feed", "http://kp.test/", "desc",
                null, "author", "en", Feed.TYPE_RSS2, "kp-counter-feed", null, null,
                "http://kp.test/counter.xml", System.currentTimeMillis());
        int[] states = {FeedItem.NEW, FeedItem.PLAYED, FeedItem.PLAYED, FeedItem.UNPLAYED};
        List<FeedItem> items = new ArrayList<>();
        for (int i = 0; i < states.length; i++) {
            FeedItem it = new FeedItem(0, "Item " + i, "kp-counter-" + i, "http://kp.test/" + i,
                    new Date(System.currentTimeMillis() - i * 1000L), states[i], feed);
            it.setMedia(new FeedMedia(0, it, 0, 0, 12345, "audio/mp3",
                    null, "http://kp.test/counter-" + i + ".mp3", 0, null, 0, 0));
            items.add(it);
        }
        feed.setItems(items);

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();

        FeedItem persisted = DBReader.getFeedItemByGuidOrEpisodeUrl("kp-counter-0", "http://kp.test/counter-0.mp3");
        assertNotNull("seeding failed", persisted);
        long feedId = persisted.getFeedId();

        assertEquals("SHOW_TOTAL should count every episode", 4, count(FeedCounter.SHOW_TOTAL, feedId));
        assertEquals("SHOW_NEW should count only NEW episodes", 1, count(FeedCounter.SHOW_NEW, feedId));
        // NEW + UNPLAYED = 2 (NEW is treated as unplayed too)
        assertEquals("SHOW_UNPLAYED should count NEW+UNPLAYED", 2, count(FeedCounter.SHOW_UNPLAYED, feedId));
        assertEquals("SHOW_NONE should count nothing", 0, count(FeedCounter.SHOW_NONE, feedId));
    }
}
