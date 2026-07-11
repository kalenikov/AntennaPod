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
 *
 * Android 11+ isolates each app's /data/data view in its own mount namespace, so a root
 * shell spawned from this app cannot see the stock app's data dir. We therefore run the
 * work inside init's (global) mount namespace via nsenter.
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
        Result result;
        try {
            File scriptFile = new File(context.getFilesDir(), "kp_migrate.sh");
            FileUtils.writeStringToFile(scriptFile, buildScript(SOURCE_PKG, dst), StandardCharsets.UTF_8);
            result = runSuInGlobalNamespace(scriptFile.getAbsolutePath());
        } catch (Exception e) {
            result = new Result(-1, "Failed to prepare migration: " + e);
        }

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
        Result r = runSuScript("logcat -d -v time -t 2000\n");
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
                + "S=\n"
                + "for c in \"/data/data/$SRC\" \"/data/user/0/$SRC\"; do"
                + " if [ -d \"$c\" ]; then S=\"$c\"; break; fi; done\n"
                + "D=/data/data/$DST\n"
                + "[ -d \"$D\" ] || D=/data/user/0/$DST\n"
                + "if [ -z \"$S\" ]; then\n"
                + "  echo \"ERROR: stock AntennaPod ($SRC) data dir not found\"\n"
                + "  echo '--- packages matching antennapod ---'; pm list packages 2>/dev/null | grep -i antennapod\n"
                + "  echo '--- /data/data ---'; ls -la /data/data 2>/dev/null | grep -i antennapod\n"
                + "  echo '--- /data/user/0 ---'; ls -la /data/user/0 2>/dev/null | grep -i antennapod\n"
                + "  exit 2\n"
                + "fi\n"
                + "echo \"source=$S target=$D\"\n"
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

    /**
     * Executes the script at {@code scriptPath} as root inside init's global mount namespace
     * (so other apps' /data/data dirs are visible), falling back to the plain namespace and
     * to Magisk's --mount-master if nsenter is unavailable.
     */
    private static Result runSuInGlobalNamespace(String scriptPath) {
        String p = "'" + scriptPath + "'";
        String command =
                "if command -v nsenter >/dev/null 2>&1; then\n"
                + "  nsenter --mount=/proc/1/ns/mnt -- sh " + p + "\n"
                + "else\n"
                + "  sh " + p + "\n"
                + "fi\n";
        return runSuScript(command);
    }

    private static Result runSuScript(String script) {
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
