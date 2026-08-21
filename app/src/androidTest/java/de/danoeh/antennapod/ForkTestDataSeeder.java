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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
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

    /**
     * Seeds one Pinchflat-style feed whose episodes carry a YouTube link, plus one episode without
     * a link, so the "open on YouTube" context menu entry can be checked in both states.
     */
    @Test
    public void seedYoutubeFeed() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        UserPreferences.init(context);

        String feedUrl = "http://192.168.1.87:8945/sources/c32bde4b-8837-4023-9175-110d5bc4e71d/feed.xml";
        Feed feed = new Feed(0, null, "Download", "https://youtube.com/playlist?list=PLSeed",
                "Тестовая YouTube-лента", null, "KP Seeder", "ru", Feed.TYPE_RSS2, "kp-seed-yt",
                null, null, feedUrl, System.currentTimeMillis());
        feed.setImageUrl(Feed.PREFIX_GENERATIVE_COVER + feedUrl);
        feed.setState(Feed.STATE_SUBSCRIBED);

        List<FeedItem> items = new ArrayList<>();
        items.add(youtubeItem(feed, "yt-with-link", "Выпуск с ссылкой на YouTube",
                "https://www.youtube.com/watch?v=UxnSVXq8Rwk"));
        items.add(youtubeItem(feed, "yt-without-link", "Выпуск без ссылки", null));
        feed.setItems(items);

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();

        assertTrue("seeding produced no feeds", !DBReader.getFeedList().isEmpty());
    }

    /**
     * Seeds one subscription with three episodes in the inbox whose media files are real, playable
     * audio, so that pressing play in the emulator actually starts playback. Needed to check that
     * the "keep episode in the inbox" switch survives playback, which a stub media file cannot show.
     */
    @Test
    public void seedInboxPlayable() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        UserPreferences.init(context);

        String feedUrl = "http://kp.seed/inbox-playable.xml";
        Feed feed = new Feed(0, null, "Входящие: проверка", "http://kp.seed/inbox-playable",
                "Эпизоды с настоящим аудио", null, "KP Seeder", "ru", Feed.TYPE_RSS2,
                "kp-seed-inbox-playable", null, null, feedUrl, System.currentTimeMillis());
        feed.setImageUrl(Feed.PREFIX_GENERATIVE_COVER + feedUrl);
        feed.setState(Feed.STATE_SUBSCRIBED);

        List<FeedItem> items = new ArrayList<>();
        for (int episode = 0; episode < 3; episode++) {
            String guid = "kp-seed-inbox-playable-" + episode;
            FeedItem item = new FeedItem(0, "Проверка «Входящих» — выпуск " + (episode + 1), guid,
                    "http://kp.seed/inbox-playable/" + episode,
                    new Date(System.currentTimeMillis() - episode * 3600000L), FeedItem.NEW, feed);
            File file = writeSilentWav(context, guid + ".wav");
            item.setMedia(new FeedMedia(0, item, 30000, 0, file.length(), "audio/wav",
                    file.getAbsolutePath(), "http://kp.seed/inbox-playable/" + episode + ".wav",
                    System.currentTimeMillis(), null, 0, 0));
            items.add(item);
        }
        feed.setItems(items);

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();

        assertTrue("seeding produced no feeds", !DBReader.getFeedList().isEmpty());
    }

    /** Writes 30 seconds of silence as an 8 kHz mono 8-bit WAV file that the player can open. */
    private File writeSilentWav(Context context, String name) throws Exception {
        File dir = context.getExternalFilesDir("media");
        File file = new File(dir, name);
        int sampleRate = 8000;
        int samples = sampleRate * 30;
        byte[] header = new byte[44];
        ByteBuffer buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put("RIFF".getBytes());
        buffer.putInt(36 + samples);
        buffer.put("WAVE".getBytes());
        buffer.put("fmt ".getBytes());
        buffer.putInt(16);
        buffer.putShort((short) 1);
        buffer.putShort((short) 1);
        buffer.putInt(sampleRate);
        buffer.putInt(sampleRate);
        buffer.putShort((short) 1);
        buffer.putShort((short) 8);
        buffer.put("data".getBytes());
        buffer.putInt(samples);
        byte[] silence = new byte[samples];
        Arrays.fill(silence, (byte) 128);
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(header);
            out.write(silence);
        }
        return file;
    }

    private FeedItem youtubeItem(Feed feed, String guid, String title, String link) {
        FeedItem item = new FeedItem(0, title, guid, link, new Date(), FeedItem.NEW, feed);
        item.setMedia(new FeedMedia(0, item, 1800000, 0, 15000000, "audio/mp3",
                null, "http://192.168.1.87:8945/media/" + guid + ".m4a", 0, null, 0, 0));
        return item;
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
