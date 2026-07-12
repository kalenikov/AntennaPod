package de.danoeh.antennapod;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.net.sync.serviceinterface.EpisodeAction;
import de.danoeh.antennapod.net.sync.service.SynchronizationQueueStorage;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.storage.database.PodDBAdapter;
import de.danoeh.antennapod.storage.preferences.SynchronizationSettings;

import org.awaitility.Awaitility;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

/**
 * Instrumented test for the fork patch that syncs a manual "mark as played" to gpodder.
 * Marks an episode played and asserts a completed PLAY action is enqueued to the sync queue.
 * Real code path (DBWriter + SynchronizationQueue) — no UI, no coordinates.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MarkPlayedSyncTest {

    private static final String FEED_URL = "http://kp.test/mp/feed.xml";
    private static final String MEDIA_URL = "http://kp.test/mp/media.mp3";
    private static final int DURATION_MS = 60000;

    @Test
    public void markPlayed_enqueuesCompletedPlayAction() {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // A sync provider must be connected for episode actions to be enqueued.
        SynchronizationSettings.setSelectedSyncProvider("GPODDER_NET");

        Feed feed = new Feed(0, null, "MP Feed", "http://kp.test/mp/", "desc",
                null, "author", "en", Feed.TYPE_RSS2, "mp-feed-id", null, null,
                FEED_URL, System.currentTimeMillis());
        FeedItem item = new FeedItem(0, "MP Item", "mp-guid", "http://kp.test/mp/item",
                new Date(), FeedItem.UNPLAYED, feed);
        item.setMedia(new FeedMedia(0, item, DURATION_MS, 0, 12345, "audio/mp3",
                null, MEDIA_URL, 0, null, 0, 0));
        feed.setItems(Collections.singletonList(item));

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();
        long id = feed.getItems().get(0).getId();

        DBWriter.markItemPlayed(FeedItem.PLAYED, id);

        SynchronizationQueueStorage storage = new SynchronizationQueueStorage(ctx);
        Awaitility.await("played action enqueued")
                .atMost(15, TimeUnit.SECONDS)
                .until(() -> hasCompletedPlayAction(storage));
        assertTrue(hasCompletedPlayAction(storage));
    }

    private boolean hasCompletedPlayAction(SynchronizationQueueStorage storage) {
        for (EpisodeAction a : storage.getQueuedEpisodeActions()) {
            if (a.getAction() == EpisodeAction.Action.PLAY
                    && MEDIA_URL.equals(a.getEpisode())
                    && a.getPosition() == DURATION_MS / 1000
                    && a.getTotal() == DURATION_MS / 1000) {
                return true;
            }
        }
        return false;
    }
}
