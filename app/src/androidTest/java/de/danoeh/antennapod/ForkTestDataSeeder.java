package de.danoeh.antennapod;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.feed.SortOrder;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.storage.database.PodDBAdapter;
import de.danoeh.antennapod.storage.preferences.ForkFeedCustomization;
import de.danoeh.antennapod.storage.preferences.UserPreferences;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Not a test but a seeder: fills the database with enough subscriptions and history entries to
 * inspect list layouts in the emulator. Run explicitly, never as part of the regular suite:
 * am instrument -w -e class de.danoeh.antennapod.ForkTestDataSeeder#seedSubscriptions ...
 *
 * Covers are drawn offline. Most feeds use the generative cover URL prefix that the app itself
 * renders as a coloured gradient, a few use real PNG files written to the app's own files dir,
 * and one has no cover at all so the text fallback tile shows up. No network, no sync server.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ForkTestDataSeeder {

    private static final String[] TITLES = {
            "Радио-Т",
            "Подлодка",
            "Запуск завтра",
            "Александр Герасимов | Психология отношений",
            "The Changelog: Software Development, Open Source",
            "Тинькофф Журнал",
            "Что случилось",
            "Sound Judgment",
            "Мы обречены",
            "Дневник хирурга: истории из операционной и не только",
            "Лайфхакер",
            "Bookmarked",
            "Так вышло",
            "Deep Questions with Cal Newport",
    };

    private static final int[] COLORS = {
            0xFF3F51B5, 0xFF009688, 0xFFE91E63, 0xFFFF9800, 0xFF4CAF50,
            0xFF9C27B0, 0xFF00BCD4, 0xFFF44336, 0xFF795548, 0xFF607D8B,
    };

    @Test
    public void seedSubscriptions() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        UserPreferences.init(context);
        ForkFeedCustomization.init(context);

        for (int i = 0; i < TITLES.length; i++) {
            String feedUrl = "http://kp.seed/feed-" + i + ".xml";
            Feed feed = new Feed(0, null, TITLES[i], "http://kp.seed/" + i, "Тестовая подписка " + i,
                    null, "KP Seeder", "ru", Feed.TYPE_RSS2, "kp-seed-" + i, null, null,
                    feedUrl, System.currentTimeMillis());
            feed.setImageUrl(coverFor(context, i, feedUrl));
            feed.setState(Feed.STATE_SUBSCRIBED);
            if (i == 4) {
                feed.setLastUpdateFailed(true);
            }

            List<FeedItem> items = new ArrayList<>();
            int episodeCount = 3 + (i % 6);
            for (int episode = 0; episode < episodeCount; episode++) {
                int playState = episode % 3 == 0 ? FeedItem.PLAYED
                        : (episode % 3 == 1 ? FeedItem.NEW : FeedItem.UNPLAYED);
                FeedItem item = new FeedItem(0, TITLES[i] + " — выпуск " + (episode + 1),
                        "kp-seed-" + i + "-" + episode, "http://kp.seed/" + i + "/" + episode,
                        new Date(System.currentTimeMillis() - episode * 86400000L), playState, feed);
                boolean downloaded = playState == FeedItem.PLAYED && episode % 2 == 0;
                String localFile = downloaded
                        ? writeMedia(context, "kp-seed-" + i + "-" + episode + ".mp3").getAbsolutePath()
                        : null;
                item.setMedia(new FeedMedia(0, item, 1800000, 0, 15000000, "audio/mp3",
                        localFile, "http://kp.seed/" + i + "/" + episode + ".mp3",
                        downloaded ? System.currentTimeMillis() : 0, null, 0, 0));
                items.add(item);
            }
            feed.setItems(items);

            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.setCompleteFeed(feed);
            adapter.close();
        }

        List<Feed> saved = DBReader.getFeedList();
        assertTrue("seeding produced no feeds", saved.size() >= TITLES.length);
        ForkFeedCustomization.setPinned(saved.get(0).getId(), true);
        ForkFeedCustomization.setCoverOverride(saved.get(3).getId(),
                ForkFeedCustomization.COVER_TEXT_PREFIX + "Герасимов");
    }

    /**
     * Fills the playback history with all three cases the "delete played" action has to handle:
     * played and downloaded, played but never downloaded, and unplayed.
     */
    @Test
    public void seedHistory() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        UserPreferences.init(context);

        List<Feed> feeds = DBReader.getFeedList();
        assertTrue("run seedSubscriptions first", !feeds.isEmpty());

        int seeded = 0;
        for (Feed feed : feeds) {
            List<FeedItem> items = DBReader.getFeedItemList(feed, FeedItemFilter.unfiltered(),
                    SortOrder.DATE_NEW_OLD, 0, 3);
            for (FeedItem item : items) {
                if (item.getMedia() == null) {
                    continue;
                }
                DBWriter.addItemToPlaybackHistory(item.getMedia(),
                        new Date(System.currentTimeMillis() - seeded * 3600000L)).get();
                seeded++;
            }
        }
        assertTrue("no history entries were seeded", seeded > 0);
    }

    private String coverFor(Context context, int index, String feedUrl) throws Exception {
        if (index % 5 == 4) {
            return null;
        }
        if (index % 5 == 3) {
            return "file://" + writeCover(context, index).getAbsolutePath();
        }
        return Feed.PREFIX_GENERATIVE_COVER + feedUrl;
    }

    private File writeCover(Context context, int index) throws Exception {
        File dir = new File(context.getFilesDir(), "fork-testdata");
        dir.mkdirs();
        File file = new File(dir, "cover-" + index + ".png");
        Bitmap bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(COLORS[index % COLORS.length]);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setTextSize(140);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(String.valueOf(index), 150, 200, paint);
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        bitmap.recycle();
        return file;
    }

    private File writeMedia(Context context, String name) throws Exception {
        File dir = context.getExternalFilesDir("media");
        File file = new File(dir, name);
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write("TESTMEDIA".getBytes());
        }
        return file;
    }
}
