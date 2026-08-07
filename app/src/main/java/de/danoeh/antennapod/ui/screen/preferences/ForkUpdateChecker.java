package de.danoeh.antennapod.ui.screen.preferences;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.net.common.AntennapodHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;

/**
 * Checks GitHub Releases of the fork for a newer build and downloads the release APK.
 * The fork has no update channel of its own (not on F-Droid/Play), so the app asks
 * the GitHub API directly and hands the downloaded APK to the system installer.
 */
public class ForkUpdateChecker {
    /** Fork version of this build. Must be bumped in every release commit, together with the tag. */
    public static final String FORK_VERSION = "fork.35";

    public static final String DEFAULT_API_URL =
            "https://api.github.com/repos/kalenikov/AntennaPod/releases/latest";
    private static final String APK_ASSET_NAME = "app-free-release.apk";
    private static final Pattern FORK_NUMBER = Pattern.compile("fork\\.(\\d+)");

    public static class UpdateInfo {
        public final String tag;
        public final int forkNumber;
        public final String apkUrl;

        public UpdateInfo(String tag, int forkNumber, String apkUrl) {
            this.tag = tag;
            this.forkNumber = forkNumber;
            this.apkUrl = apkUrl;
        }

        public boolean isNewerThanInstalled() {
            return forkNumber > parseForkNumber(FORK_VERSION);
        }
    }

    public static int parseForkNumber(@Nullable String s) {
        if (s == null) {
            return -1;
        }
        Matcher m = FORK_NUMBER.matcher(s);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    /**
     * Fetches the latest release from the GitHub API. The URL is a parameter so an
     * instrumented test can point it at a local HTTP server.
     */
    @NonNull
    public static UpdateInfo checkLatest(String apiUrl) throws IOException {
        Request request = new Request.Builder().url(apiUrl)
                .header("Accept", "application/vnd.github+json").build();
        try (Response response = AntennapodHttpClient.getHttpClient().newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("GitHub API error: HTTP " + response.code());
            }
            JSONObject release = new JSONObject(response.body().string());
            String tag = release.getString("tag_name");
            String apkUrl = null;
            JSONArray assets = release.optJSONArray("assets");
            for (int i = 0; assets != null && i < assets.length(); i++) {
                JSONObject asset = assets.getJSONObject(i);
                if (APK_ASSET_NAME.equals(asset.optString("name"))) {
                    apkUrl = asset.getString("browser_download_url");
                    break;
                }
            }
            if (apkUrl == null) {
                throw new IOException("Release " + tag + " has no " + APK_ASSET_NAME + " asset");
            }
            return new UpdateInfo(tag, parseForkNumber(tag), apkUrl);
        } catch (org.json.JSONException e) {
            throw new IOException("Unexpected GitHub API response: " + e.getMessage(), e);
        }
    }

    /** Downloads the release APK to app-external storage and returns the file. */
    @NonNull
    public static File downloadApk(Context context, UpdateInfo info) throws IOException {
        File dir = new File(context.getExternalFilesDir(null), "updates");
        if (dir.isDirectory()) {
            File[] old = dir.listFiles();
            for (int i = 0; old != null && i < old.length; i++) {
                //noinspection ResultOfMethodCallIgnored
                old[i].delete();
            }
        } else if (!dir.mkdirs()) {
            throw new IOException("Cannot create " + dir);
        }
        File apk = new File(dir, String.format(Locale.US, "kalenikovpod-%s.apk", info.tag));
        Request request = new Request.Builder().url(info.apkUrl).build();
        try (Response response = AntennapodHttpClient.getHttpClient().newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("APK download failed: HTTP " + response.code());
            }
            try (BufferedSink sink = Okio.buffer(Okio.sink(apk))) {
                sink.writeAll(response.body().source());
            }
        }
        return apk;
    }

    /** Hands the downloaded APK to the system package installer. */
    public static void installApk(Context context, File apk) {
        Uri uri = FileProvider.getUriForFile(context,
                context.getString(R.string.provider_authority), apk);
        Intent intent = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
