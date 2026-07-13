package de.danoeh.antennapod;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.net.sync.service.SyncService;
import de.danoeh.antennapod.net.sync.serviceinterface.EpisodeAction;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.PodDBAdapter;

import org.awaitility.Awaitility;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Instrumented test for the receiving half of cross-device watched sync: when a completed gpodder
 * PLAY action arrives from another device, the episode must be marked played locally (so it hides
 * from the user's filters on this phone too). Seeds an UNPLAYED feed+item with a known duration,
 * feeds a completed PLAY action (position == total == duration, as the fork's manual mark-as-played
 * enqueues), and asserts the item becomes PLAYED. Real code path — no UI, no coordinates.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class RemotePlayedSyncTest {

    private static final String FEED_URL = "http://kp.test/rp/feed.xml";
    private static final String MEDIA_URL = "http://kp.test/rp/media.mp3";
    private static final String GUID = "rp-guid-1";
    private static final int DURATION_MS = 60000;

    @Test
    public void remoteCompletedPlayAction_marksEpisodePlayed() {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();

        Feed feed = new Feed(0, null, "RP Feed", "http://kp.test/rp/", "desc",
                null, "author", "en", Feed.TYPE_RSS2, "rp-feed-id", null, null,
                FEED_URL, System.currentTimeMillis());
        FeedItem item = new FeedItem(0, "RP Item", GUID, "http://kp.test/rp/item",
                new Date(), FeedItem.UNPLAYED, feed);
        item.setMedia(new FeedMedia(0, item, DURATION_MS, 0, 12345, "audio/mp3",
                null, MEDIA_URL, 0, null, 0, 0));
        feed.setItems(Collections.singletonList(item));

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();

        FeedItem persisted = DBReader.getFeedItemByGuidOrEpisodeUrl(GUID, MEDIA_URL);
        assertNotNull("seeding failed: item not persisted", persisted);
        long id = persisted.getId();
        assertFalse("precondition: item must start unplayed", persisted.isPlayed());

        int durationSec = DURATION_MS / 1000;
        EpisodeAction played = new EpisodeAction.Builder(FEED_URL, MEDIA_URL, EpisodeAction.PLAY)
                .guid(GUID)
                .currentTimestamp()
                .started(0)
                .position(durationSec)
                .total(durationSec)
                .build();
        SyncService.applyRemotePlayActions(ctx, Collections.singletonList(played));

        // applyRemotePlayActions persists via DBWriter's async executor — poll instead of
        // reading back immediately (immediate read races with writes queued by other tests).
        Awaitility.await("episode marked played")
                .atMost(15, TimeUnit.SECONDS)
                .until(() -> {
                    FeedItem after = DBReader.getFeedItem(id);
                    return after != null && after.isPlayed();
                });
        assertTrue("remote completed PLAY action should have marked the episode played",
                DBReader.getFeedItem(id).isPlayed());
    }
}
