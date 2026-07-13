package de.danoeh.antennapod;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import de.danoeh.antennapod.ui.screen.preferences.ForkSettingsTransfer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Fork feature test: root-free settings transfer. Exercises the static export/import logic
 * directly (no activities) — the export zip must omit device-specific files and keys, and a
 * round-trip import must restore transferred values while keeping the target's own gpodder
 * device ID.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ForkSettingsTransferTest {

    private static final String GPODDER_PREFS = "gpodder.net";
    private static final String DEVICE_ID_KEY = "de.danoeh.antennapod.preferences.gpoddernet.deviceID";
    private static final String USERNAME_KEY = "de.danoeh.antennapod.preferences.gpoddernet.username";

    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    private Map<String, String> unzip(byte[] data) throws Exception {
        Map<String, String> entries = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(data))) {
            ZipEntry entry;
            byte[] chunk = new byte[8192];
            while ((entry = zip.getNextEntry()) != null) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int n;
                while ((n = zip.read(chunk)) != -1) {
                    buffer.write(chunk, 0, n);
                }
                entries.put(entry.getName(), buffer.toString(StandardCharsets.UTF_8.name()));
            }
        }
        return entries;
    }

    @Test
    public void exportOmitsDeviceSpecificData() throws Exception {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString("prefDataFolder", "/storage/emulated/0/donor")
                .putString("prefSomeTheme", "dark")
                .commit();
        context.getSharedPreferences(GPODDER_PREFS, Context.MODE_PRIVATE).edit()
                .putString(DEVICE_ID_KEY, "donor-device")
                .putString(USERNAME_KEY, "serj")
                .commit();
        context.getSharedPreferences("MainActivityPrefs", Context.MODE_PRIVATE).edit()
                .putString("prefLastSeenForkVersion", "fork.8")
                .commit();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int count = ForkSettingsTransfer.exportSettings(context, out);
        assertTrue("some settings files must be exported", count > 0);
        Map<String, String> entries = unzip(out.toByteArray());

        assertFalse("MainActivityPrefs must not be exported",
                entries.containsKey("MainActivityPrefs.xml"));
        assertFalse("sync timestamps must not be exported",
                entries.containsKey("synchronization.xml"));
        String defaultPrefs = entries.get(context.getPackageName() + "_preferences.xml");
        assertNotNull(defaultPrefs);
        assertFalse("data folder path must be stripped", defaultPrefs.contains("prefDataFolder"));
        assertTrue(defaultPrefs.contains("prefSomeTheme"));
        String gpodder = entries.get(GPODDER_PREFS + ".xml");
        assertNotNull(gpodder);
        assertFalse("device ID must be stripped", gpodder.contains(DEVICE_ID_KEY));
        assertTrue("account credentials must be exported", gpodder.contains("serj"));
    }

    @Test
    public void importRestoresValuesButKeepsOwnDeviceId() throws Exception {
        SharedPreferences defaultPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences gpodder = context.getSharedPreferences(GPODDER_PREFS, Context.MODE_PRIVATE);
        defaultPrefs.edit().putString("prefSomeTheme", "dark").commit();
        gpodder.edit().putString(DEVICE_ID_KEY, "donor-device").putString(USERNAME_KEY, "serj").commit();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ForkSettingsTransfer.exportSettings(context, out);

        defaultPrefs.edit().putString("prefSomeTheme", "light").commit();
        gpodder.edit().putString(DEVICE_ID_KEY, "target-device").remove(USERNAME_KEY).commit();

        try (InputStream in = new ByteArrayInputStream(out.toByteArray())) {
            int count = ForkSettingsTransfer.importSettings(context, in);
            assertTrue(count > 0);
        }

        // The running process caches SharedPreferences, so verify the files on disk (the real
        // flow restarts the app right after importing).
        String defaultXml = readPrefsFile(context.getPackageName() + "_preferences.xml");
        assertTrue("transferred value must be back", defaultXml.contains("dark"));
        String gpodderXml = readPrefsFile(GPODDER_PREFS + ".xml");
        assertTrue("credentials must be transferred", gpodderXml.contains("serj"));
        assertTrue("target's own device ID must survive", gpodderXml.contains("target-device"));
        assertFalse("donor's device ID must not leak in", gpodderXml.contains("donor-device"));
    }

    private String readPrefsFile(String name) throws Exception {
        java.io.File file = new java.io.File(
                context.getApplicationInfo().dataDir + "/shared_prefs", name);
        try (java.io.FileInputStream in = new java.io.FileInputStream(file)) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int n;
            while ((n = in.read(chunk)) != -1) {
                buffer.write(chunk, 0, n);
            }
            return buffer.toString(StandardCharsets.UTF_8.name());
        }
    }
}
