package de.danoeh.antennapod;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.storage.database.PodDBAdapter;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.Date;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Instrumented test for the fork feature: manually deleting a download also removes the episode from
 * the playback history (playback_completion_date is reset), while automatic deletion keeps it.
 * Exercises the real DBWriter.deleteFeedMediaOfItem code path — no UI.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ForkDeleteClearsHistoryTest {

    private long seedItemInHistory(String suffix) throws Exception {
        String feedUrl = "http://kp.test/hist-" + suffix + ".xml";
        String mediaUrl = "http://kp.test/hist-" + suffix + ".mp3";
        String guid = "kp-hist-" + suffix;

        Feed feed = new Feed(0, null, "KP Hist Feed", "http://kp.test/", "desc",
                null, "author", "en", Feed.TYPE_RSS2, "kp-hist-feed-" + suffix, null, null,
                feedUrl, System.currentTimeMillis());
        FeedItem item = new FeedItem(0, "KP Hist Item", guid, "http://kp.test/item",
                new Date(), FeedItem.PLAYED, feed);
        item.setMedia(new FeedMedia(0, item, 0, 0, 12345, "audio/mp3",
                null, mediaUrl, 0, null, 0, 0));
        feed.setItems(Collections.singletonList(item));

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();

        FeedItem persisted = DBReader.getFeedItemByGuidOrEpisodeUrl(guid, mediaUrl);
        assertNotNull("seeding failed", persisted);
        DBWriter.addItemToPlaybackHistory(persisted.getMedia(), new Date()).get();
        return persisted.getId();
    }

    private long historyValue(long itemId) {
        Date d = DBReader.getFeedItem(itemId).getMedia().getLastPlayedTimeHistory();
        return d == null ? 0 : d.getTime();
    }

    @Test
    public void manualDelete_clearsHistory() throws Exception {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        long id = seedItemInHistory("manual");
        assertTrue("precondition: item should be in history", historyValue(id) > 0);

        DBWriter.deleteFeedMediaOfItem(ctx, DBReader.getFeedItem(id).getMedia(), true).get();

        assertTrue("manual deletion should remove the episode from playback history",
                historyValue(id) == 0);
    }

    @Test
    public void autoDelete_keepsHistory() throws Exception {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        long id = seedItemInHistory("auto");
        assertTrue("precondition: item should be in history", historyValue(id) > 0);

        DBWriter.deleteFeedMediaOfItem(ctx, DBReader.getFeedItem(id).getMedia(), false).get();

        assertTrue("automatic deletion should keep the episode in playback history",
                historyValue(id) > 0);
    }
}
