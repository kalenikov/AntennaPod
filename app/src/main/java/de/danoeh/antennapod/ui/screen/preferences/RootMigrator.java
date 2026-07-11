package de.danoeh.antennapod.ui.screen.preferences;

import android.content.Context;
import android.util.Log;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Fork feature: copies all data (database + every shared_prefs file) from the stock
 * AntennaPod install into this fork, using root. Lets the user migrate subscriptions,
 * episode states, per-feed settings and all global settings without starting from scratch.
 */
public final class RootMigrator {
    private static final String TAG = "RootMigrator";
    public static final String SOURCE_PKG = "de.danoeh.antennapod";

    private RootMigrator() {
    }

    public static class Result {
        public final int exitCode;
        public final String output;

        Result(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }

        public boolean isSuccess() {
            return exitCode == 0;
        }
    }

    public static File logsDir(Context context) {
        File dir = new File(context.getExternalFilesDir(null), "logs");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static File migrationLogFile(Context context) {
        return new File(logsDir(context), "migration.log");
    }

    public static String readMigrationLog(Context context) {
        File file = migrationLogFile(context);
        if (!file.exists()) {
            return "";
        }
        try {
            return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "Could not read migration log: " + e;
        }
    }

    /** Runs the migration script under {@code su} and stores the full output in the log file. */
    public static Result migrate(Context context) {
        String dst = context.getPackageName();
        String script = buildScript(SOURCE_PKG, dst);
        Result result = runSu(script);

        String stamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
        String header = "=== KalenikovPod migration ===\n"
                + "time: " + stamp + "\n"
                + "source: " + SOURCE_PKG + "\n"
                + "target: " + dst + "\n"
                + "exit code: " + result.exitCode + "\n"
                + "==============================\n";
        try {
            FileUtils.writeStringToFile(migrationLogFile(context), header + result.output, StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.e(TAG, "Failed to write migration log", e);
        }
        return result;
    }

    /** Recent device logcat (needs root); used by the "App" tab of the log viewer. */
    public static String readAppLogcat() {
        Result r = runSu("logcat -d -v time -t 2000\n");
        if (r.output == null || r.output.trim().isEmpty()) {
            return "logcat empty or root denied (exit " + r.exitCode + ")";
        }
        return r.output;
    }

    private static String buildScript(String src, String dst) {
        // set -x traces every command so the log is fully diagnostic.
        return "set -x\n"
                + "SRC=" + src + "\n"
                + "DST=" + dst + "\n"
                + "S=/data/data/$SRC\n"
                + "D=/data/data/$DST\n"
                + "if [ ! -d \"$S\" ]; then echo 'ERROR: stock AntennaPod (' $SRC ') is not installed'; exit 2; fi\n"
                + "am force-stop $SRC\n"
                + "own=$(stat -c '%u:%g' \"$D\")\n"
                + "echo \"target owner: $own\"\n"
                + "mkdir -p \"$D/databases\"\n"
                + "rm -f \"$D/databases/Antennapod.db\" \"$D/databases/Antennapod.db-wal\""
                + " \"$D/databases/Antennapod.db-shm\" \"$D/databases/Antennapod.db-journal\"\n"
                + "cp -a \"$S/databases/Antennapod.db\"* \"$D/databases/\" 2>/dev/null\n"
                + "mkdir -p \"$D/shared_prefs\"\n"
                + "rm -f \"$D/shared_prefs/\"*.xml\n"
                + "cp -a \"$S/shared_prefs/.\" \"$D/shared_prefs/\" 2>/dev/null\n"
                + "if [ -f \"$D/shared_prefs/${SRC}_preferences.xml\" ]; then"
                + " mv \"$D/shared_prefs/${SRC}_preferences.xml\" \"$D/shared_prefs/${DST}_preferences.xml\"; fi\n"
                + "sed -i '/prefDataFolder/d' \"$D/shared_prefs/${DST}_preferences.xml\" 2>/dev/null\n"
                + "chown -R \"$own\" \"$D/databases\" \"$D/shared_prefs\"\n"
                + "restorecon -R \"$D/databases\" \"$D/shared_prefs\" 2>/dev/null\n"
                + "echo '--- databases ---'; ls -la \"$D/databases\"\n"
                + "echo '--- shared_prefs ---'; ls -la \"$D/shared_prefs\"\n"
                + "echo 'OK: migrated db + shared_prefs'\n";
    }

    private static Result runSu(String script) {
        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder("su");
            pb.redirectErrorStream(true);
            process = pb.start();
            try (OutputStream os = process.getOutputStream()) {
                os.write((script + "\nexit $?\n").getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
            String output = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
            int code = process.waitFor();
            return new Result(code, output);
        } catch (Exception e) {
            String message = "Failed to run su: " + e + "\n"
                    + "Root (Magisk/KernelSU) is required and must grant access to KalenikovPod.";
            Log.e(TAG, message, e);
            if (process != null) {
                process.destroy();
            }
            return new Result(-1, message);
        }
    }
}
