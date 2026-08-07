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
import de.danoeh.antennapod.ui.screen.ForkHistoryCleanup;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Fork feature test: "delete played episodes" removes every played episode from the playback
 * history and wipes its downloaded file, while unplayed history entries survive. Covers the gap
 * of the multi-select delete handler, which skips episodes that were never downloaded.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ForkHistoryCleanupTest {

    /** Unique per run: re-seeding the same feed URL would return the previous run's media row. */
    private final String runId = String.valueOf(System.currentTimeMillis());

    private long seedItem(String rawSuffix, int playState, File downloadedFile) throws Exception {
        String suffix = rawSuffix + "-" + runId;
        String feedUrl = "http://kp.test/cleanup-" + suffix + ".xml";
        String mediaUrl = "http://kp.test/cleanup-" + suffix + ".mp3";
        String guid = "kp-cleanup-" + suffix;

        Feed feed = new Feed(0, null, "KP Cleanup Feed " + suffix, "http://kp.test/", "desc",
                null, "author", "en", Feed.TYPE_RSS2, "kp-cleanup-feed-" + suffix, null, null,
                feedUrl, System.currentTimeMillis());
        FeedItem item = new FeedItem(0, "KP Cleanup Item " + suffix, guid, "http://kp.test/item",
                new Date(), playState, feed);
        FeedMedia media = new FeedMedia(0, item, 0, 0, 12345, "audio/mp3",
                downloadedFile == null ? null : downloadedFile.getAbsolutePath(), mediaUrl,
                downloadedFile == null ? 0 : 1, null, 0, 0);
        item.setMedia(media);
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
        Date date = DBReader.getFeedItem(itemId).getMedia().getLastPlayedTimeHistory();
        return date == null ? 0 : date.getTime();
    }

    private File writeTempMedia(Context context, String name) throws Exception {
        File file = new File(context.getExternalFilesDir("media"), name);
        assertNotNull(file.getParentFile());
        file.getParentFile().mkdirs();
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write("TESTMEDIA".getBytes());
        }
        assertTrue("precondition: media file must exist", file.exists());
        return file;
    }

    @Test
    public void deletesPlayedEpisodesAndTheirFiles() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File downloaded = writeTempMedia(context, "kp-cleanup-played-" + runId + ".mp3");

        long playedDownloaded = seedItem("played-downloaded", FeedItem.PLAYED, downloaded);
        long playedOnly = seedItem("played-only", FeedItem.PLAYED, null);
        long unplayed = seedItem("unplayed", FeedItem.UNPLAYED, null);

        assertTrue("precondition: played downloaded is in history", historyValue(playedDownloaded) > 0);
        assertTrue("precondition: played not downloaded is in history", historyValue(playedOnly) > 0);
        assertTrue("precondition: unplayed is in history", historyValue(unplayed) > 0);
        assertTrue("precondition: at least the two played episodes are counted",
                ForkHistoryCleanup.countPlayedInHistory() >= 2);

        ForkHistoryCleanup.deletePlayedFromHistory(context);

        assertEquals("downloaded played episode must leave the history",
                0, historyValue(playedDownloaded));
        assertFalse("its downloaded file must be gone", downloaded.exists());
        assertEquals("played episode without a download must leave the history too",
                0, historyValue(playedOnly));
        assertTrue("unplayed episode must stay in the history", historyValue(unplayed) > 0);
        assertEquals("no played episodes may be left", 0, ForkHistoryCleanup.countPlayedInHistory());
    }

    @Test
    public void returnsZeroOnEmptyHistory() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        ForkHistoryCleanup.deletePlayedFromHistory(context);
        assertEquals("a second run must find nothing and terminate",
                0, ForkHistoryCleanup.deletePlayedFromHistory(context));
    }
}
