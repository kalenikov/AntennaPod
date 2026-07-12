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

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.Date;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Instrumented test for the fork patch that deletes an episode when the sync server reports a
 * gpodder DELETE action for it. Seeds a feed+item in the DB, feeds a remote DELETE action,
 * and asserts the item is gone. Tests the real code path — no UI, no coordinates.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class RemoteDeleteSyncTest {

    private static final String FEED_URL = "http://kp.test/feed.xml";
    private static final String MEDIA_URL = "http://kp.test/media.mp3";
    private static final String GUID = "kp-guid-1";

    @Test
    public void remoteDeleteAction_removesFeedItem() {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();

        Feed feed = new Feed(0, null, "KP Test Feed", "http://kp.test/", "desc",
                null, "author", "en", Feed.TYPE_RSS2, "kp-feed-id", null, null,
                FEED_URL, System.currentTimeMillis());
        FeedItem item = new FeedItem(0, "KP Item", GUID, "http://kp.test/item",
                new Date(), FeedItem.PLAYED, feed);
        item.setMedia(new FeedMedia(0, item, 0, 0, 12345, "audio/mp3",
                null, MEDIA_URL, 0, null, 0, 0));
        feed.setItems(Collections.singletonList(item));

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();

        FeedItem persisted = DBReader.getFeedItemByGuidOrEpisodeUrl(GUID, MEDIA_URL);
        assertNotNull("seeding failed: item not persisted", persisted);
        long id = persisted.getId();

        EpisodeAction delete = new EpisodeAction.Builder(FEED_URL, MEDIA_URL, EpisodeAction.DELETE)
                .guid(GUID)
                .currentTimestamp()
                .build();
        SyncService.processRemoteDeleteActions(ctx, Collections.singletonList(delete));

        assertNull("feed item should have been deleted by the remote DELETE action",
                DBReader.getFeedItem(id));
    }
}
