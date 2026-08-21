package de.danoeh.antennapod;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.playback.service.internal.PlayableUtils;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.storage.database.PodDBAdapter;
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
 * Instrumented test for the fork feature: with the "keep episode in the inbox" switch on, starting
 * playback does not clear the NEW flag. Covers both places upstream drops it while playing:
 * FeedMedia.setPosition (in memory) and PlayableUtils.saveCurrentPosition (in the database).
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ForkInboxKeepOnPlaybackTest {

    private final boolean originalSetting = UserPreferences.getForkKeepNewOnDownload();

    @After
    public void restoreSetting() {
        UserPreferences.setForkKeepNewOnDownload(originalSetting);
    }

    private FeedItem seedNewItem(String suffix) {
        String unique = suffix + "-" + System.currentTimeMillis();
        String feedUrl = "http://kp.test/inbox-play-" + unique + ".xml";
        String mediaUrl = "http://kp.test/inbox-play-" + unique + ".mp3";
        String guid = "kp-inbox-play-" + unique;

        Feed feed = new Feed(0, null, "KP Inbox Play", "http://kp.test/", "desc",
                null, "author", "en", Feed.TYPE_RSS2, "kp-inbox-play-feed-" + unique, null, null,
                feedUrl, System.currentTimeMillis());
        FeedItem item = new FeedItem(0, "KP Inbox Play Item", guid, "http://kp.test/item",
                new Date(), FeedItem.NEW, feed);
        item.setMedia(new FeedMedia(0, item, 3600000, 0, 12345, "audio/mp3",
                null, mediaUrl, 0, null, 0, 0));
        feed.setItems(Collections.singletonList(item));

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();

        FeedItem persisted = DBReader.getFeedItemByGuidOrEpisodeUrl(guid, mediaUrl);
        assertNotNull("seeding failed", persisted);
        FeedItem withFeed = DBReader.getFeedItem(persisted.getId());
        assertNotNull("seeding failed", withFeed);
        assertTrue("precondition: seeded episode should be in the inbox", withFeed.isNew());
        return withFeed;
    }

    @Test
    public void playbackPosition_keepsNewFlagWhenEnabled() {
        UserPreferences.setForkKeepNewOnDownload(true);
        FeedItem item = seedNewItem("position-keep");

        item.getMedia().setPosition(5000);

        assertTrue("playing an episode must not push it out of the inbox", item.isNew());
    }

    @Test
    public void playbackPosition_dropsNewFlagWhenDisabled() {
        UserPreferences.setForkKeepNewOnDownload(false);
        FeedItem item = seedNewItem("position-drop");

        item.getMedia().setPosition(5000);

        assertFalse("with the switch off, upstream behavior must be untouched", item.isNew());
    }

    @Test
    public void savingPlaybackPosition_keepsNewFlagInDatabaseWhenEnabled() {
        UserPreferences.setForkKeepNewOnDownload(true);
        FeedItem item = seedNewItem("save-keep");

        // position 0 is the first save after playback starts, the case that reaches the database
        PlayableUtils.saveCurrentPosition(item.getMedia(), 0, System.currentTimeMillis());
        DBWriter.tearDownTests();

        assertTrue("saving the playback position must not push the episode out of the inbox",
                DBReader.getFeedItem(item.getId()).isNew());
    }

    @Test
    public void savingPlaybackPosition_dropsNewFlagInDatabaseWhenDisabled() {
        UserPreferences.setForkKeepNewOnDownload(false);
        FeedItem item = seedNewItem("save-drop");

        PlayableUtils.saveCurrentPosition(item.getMedia(), 0, System.currentTimeMillis());
        DBWriter.tearDownTests();

        assertFalse("with the switch off, upstream behavior must be untouched",
                DBReader.getFeedItem(item.getId()).isNew());
    }
}
