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
import de.danoeh.antennapod.storage.preferences.ForkInboxRetention;
import de.danoeh.antennapod.storage.preferences.UserPreferences;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.Date;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Instrumented test for the fork feature: with the "downloading does not remove from inbox" switch
 * on, neither enqueueing an episode for download nor finishing the download clears its NEW flag.
 * Covers both places where upstream drops it: DBWriter.addQueueItem and FeedMedia.setDownloaded.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ForkInboxKeepOnDownloadTest {

    private final boolean originalSetting = UserPreferences.getForkKeepNewOnDownload();

    @After
    public void restoreSetting() {
        UserPreferences.setForkKeepNewOnDownload(originalSetting);
    }

    private FeedItem seedNewItem(String suffix) {
        String unique = suffix + "-" + System.currentTimeMillis();
        String feedUrl = "http://kp.test/inbox-keep-" + unique + ".xml";
        String mediaUrl = "http://kp.test/inbox-keep-" + unique + ".mp3";
        String guid = "kp-inbox-keep-" + unique;

        Feed feed = new Feed(0, null, "KP Inbox Keep", "http://kp.test/", "desc",
                null, "author", "en", Feed.TYPE_RSS2, "kp-inbox-keep-feed-" + unique, null, null,
                feedUrl, System.currentTimeMillis());
        FeedItem item = new FeedItem(0, "KP Inbox Keep Item", guid, "http://kp.test/item",
                new Date(), FeedItem.NEW, feed);
        item.setMedia(new FeedMedia(0, item, 0, 0, 12345, "audio/mp3",
                null, mediaUrl, 0, null, 0, 0));
        feed.setItems(Collections.singletonList(item));

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();

        FeedItem persisted = DBReader.getFeedItemByGuidOrEpisodeUrl(guid, mediaUrl);
        assertNotNull("seeding failed", persisted);
        // reload by id: only this reader attaches the Feed, which queueing needs
        FeedItem withFeed = DBReader.getFeedItem(persisted.getId());
        assertNotNull("seeding failed", withFeed);
        assertTrue("precondition: seeded episode should be in the inbox", withFeed.isNew());
        return withFeed;
    }

    @Test
    public void enqueueingForDownload_keepsEpisodeInInbox() throws Exception {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        FeedItem item = seedNewItem("queue-keep");

        DBWriter.addQueueItem(ctx, true, item).get();

        assertTrue("episode queued by a download must stay in the inbox",
                DBReader.getFeedItem(item.getId()).isNew());
    }

    @Test
    public void manualEnqueueing_stillRemovesFromInbox() throws Exception {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        FeedItem item = seedNewItem("queue-drop");

        DBWriter.addQueueItem(ctx, item).get();

        assertFalse("adding to the queue by hand must keep the upstream behavior",
                DBReader.getFeedItem(item.getId()).isNew());
    }

    @Test
    public void finishedDownload_keepsNewFlagWhenEnabled() {
        UserPreferences.setForkKeepNewOnDownload(true);
        FeedItem item = seedNewItem("download-keep");
        boolean wasNew = item.isNew();

        item.getMedia().setDownloaded(true, System.currentTimeMillis());
        ForkInboxRetention.restoreNewAfterDownload(item, wasNew);

        assertTrue("a finished download must not push the episode out of the inbox", item.isNew());
    }

    @Test
    public void finishedDownload_dropsNewFlagWhenDisabled() {
        UserPreferences.setForkKeepNewOnDownload(false);
        FeedItem item = seedNewItem("download-drop");
        boolean wasNew = item.isNew();

        item.getMedia().setDownloaded(true, System.currentTimeMillis());
        ForkInboxRetention.restoreNewAfterDownload(item, wasNew);

        assertFalse("with the switch off, upstream behavior must be untouched", item.isNew());
    }
}
