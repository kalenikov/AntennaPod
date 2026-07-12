package de.danoeh.antennapod;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import de.danoeh.antennapod.ui.screen.preferences.RootMigrator;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Instrumented test for the fork's root migration. Runs on a rooted emulator; the harness
 * seeds the stock AntennaPod data before invoking this. Tests the real code path
 * (su + nsenter + copy + DB rewrite) by calling RootMigrator directly — no UI coordinates.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class RootMigrationTest {

    private static final String STOCK = "de.danoeh.antennapod";

    @Test
    public void migrate_copiesDbPrefsAndMedia() throws Exception {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String pkg = ctx.getPackageName();

        RootMigrator.Result result = RootMigrator.migrate(ctx);
        assertTrue("migration did not succeed:\n" + result.output, result.isSuccess());

        // DB: FeedMedia.file_url rewritten to this package, none left pointing at stock.
        File db = ctx.getDatabasePath("Antennapod.db");
        assertTrue("migrated DB missing", db.exists());
        SQLiteDatabase sdb = SQLiteDatabase.openDatabase(db.getPath(), null, SQLiteDatabase.OPEN_READONLY);
        int rewritten = count(sdb, "SELECT COUNT(*) FROM FeedMedia WHERE file_url LIKE '%/" + pkg + "/%'");
        int stale = count(sdb, "SELECT COUNT(*) FROM FeedMedia WHERE file_url LIKE '%/" + STOCK + "/%'");
        sdb.close();
        assertTrue("no rewritten media rows (seed missing?)", rewritten > 0);
        assertEquals("stale stock paths remain in DB", 0, stale);

        // Prefs: the migrated default prefs file (renamed to this package) has the seeded marker.
        File prefs = new File(ctx.getApplicationInfo().dataDir, "shared_prefs/" + pkg + "_preferences.xml");
        assertTrue("migrated prefs file missing", prefs.exists());
        String content = new String(Files.readAllBytes(prefs.toPath()), StandardCharsets.UTF_8);
        assertTrue("prefs marker not migrated", content.contains("kp_test_marker"));

        // Media: the seeded downloaded file was copied into this app's external files dir.
        File media = new File(ctx.getExternalFilesDir(null), "media/test.mp3");
        assertTrue("downloaded media file not migrated", media.exists());
    }

    private static int count(SQLiteDatabase db, String sql) {
        try (Cursor c = db.rawQuery(sql, null)) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }
}
