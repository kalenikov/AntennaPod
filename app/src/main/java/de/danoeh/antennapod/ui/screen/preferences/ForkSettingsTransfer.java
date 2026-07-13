package de.danoeh.antennapod.ui.screen.preferences;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Root-free settings transfer between phones running this fork: exports the app's own
 * shared_prefs XML files into a zip and imports such a zip back, so a configured phone can act
 * as the donor for one without root. Device-bound values never travel: the whole
 * MainActivityPrefs/synchronization files are skipped and the gpodder device ID, data folder
 * and automatic-export folder entries are stripped line-by-line (same trick as RootMigrator's
 * sed). On import the target keeps its own gpodder device ID. Static and UI-free so tests can
 * drive it without launching an activity.
 */
public class ForkSettingsTransfer {
    private static final String TAG = "ForkSettingsTransfer";

    private static final List<String> SKIPPED_FILES = Arrays.asList(
            "MainActivityPrefs.xml", "synchronization.xml");
    private static final String GPODDER_PREFS_FILE = "gpodder.net.xml";
    private static final String GPODDER_DEVICE_ID_KEY =
            "de.danoeh.antennapod.preferences.gpoddernet.deviceID";
    private static final List<String> STRIPPED_KEYS = Arrays.asList(
            "prefDataFolder", "prefAutomaticExportFolder", GPODDER_DEVICE_ID_KEY);
    private static final String DEFAULT_PREFS_SUFFIX = "_preferences.xml";

    private ForkSettingsTransfer() {
    }

    private static File sharedPrefsDir(Context context) {
        return new File(context.getApplicationInfo().dataDir, "shared_prefs");
    }

    /** Zips all shared_prefs XML files (minus device-specific files/keys) into the stream. */
    public static int exportSettings(Context context, OutputStream out) throws IOException {
        File[] files = sharedPrefsDir(context).listFiles(
                (dir, name) -> name.endsWith(".xml") && !SKIPPED_FILES.contains(name));
        if (files == null || files.length == 0) {
            throw new IOException("No settings files found");
        }
        int count = 0;
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (File file : files) {
                String content;
                try (FileInputStream fis = new FileInputStream(file)) {
                    content = readAll(fis);
                }
                zip.putNextEntry(new ZipEntry(file.getName()));
                zip.write(stripDeviceSpecificKeys(content).getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
                count++;
            }
        }
        Log.i(TAG, "Exported " + count + " settings files");
        return count;
    }

    /**
     * Unzips settings over the app's shared_prefs. The donor's default prefs file is renamed to
     * this package's name (covers debug/release package suffixes) and the target's own gpodder
     * device ID is preserved. Caller must restart the process afterwards — SharedPreferences
     * are cached in memory.
     */
    public static int importSettings(Context context, InputStream in) throws IOException {
        File dir = sharedPrefsDir(context);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Cannot create " + dir);
        }
        String deviceId = context.getSharedPreferences("gpodder.net", Context.MODE_PRIVATE)
                .getString(GPODDER_DEVICE_ID_KEY, null);
        int count = 0;
        try (ZipInputStream zip = new ZipInputStream(in)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = new File(entry.getName()).getName();
                if (!name.endsWith(".xml") || SKIPPED_FILES.contains(name)) {
                    continue;
                }
                if (name.endsWith(DEFAULT_PREFS_SUFFIX)) {
                    name = context.getPackageName() + DEFAULT_PREFS_SUFFIX;
                }
                String content = stripDeviceSpecificKeys(readAll(zip));
                if (name.equals(GPODDER_PREFS_FILE) && deviceId != null) {
                    content = insertStringEntry(content, GPODDER_DEVICE_ID_KEY, deviceId);
                }
                try (FileOutputStream fos = new FileOutputStream(new File(dir, name))) {
                    fos.write(content.getBytes(StandardCharsets.UTF_8));
                }
                count++;
            }
        }
        if (count == 0) {
            throw new IOException("Archive contains no settings files");
        }
        Log.i(TAG, "Imported " + count + " settings files");
        return count;
    }

    /** Drops lines mentioning device-bound keys; pref XML files are one entry per line. */
    static String stripDeviceSpecificKeys(String xml) {
        StringBuilder result = new StringBuilder(xml.length());
        for (String line : xml.split("\n", -1)) {
            boolean stripped = false;
            for (String key : STRIPPED_KEYS) {
                if (line.contains("\"" + key + "\"")) {
                    stripped = true;
                    break;
                }
            }
            if (!stripped) {
                if (result.length() > 0) {
                    result.append('\n');
                }
                result.append(line);
            }
        }
        return result.toString();
    }

    static String insertStringEntry(String xml, String key, @NonNull String value) {
        String entry = "    <string name=\"" + key + "\">" + value + "</string>\n";
        int end = xml.lastIndexOf("</map>");
        if (end < 0) {
            return xml;
        }
        return xml.substring(0, end) + entry + xml.substring(end);
    }

    private static String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int n;
        while ((n = in.read(chunk)) != -1) {
            buffer.write(chunk, 0, n);
        }
        return buffer.toString(StandardCharsets.UTF_8.name());
    }
}
