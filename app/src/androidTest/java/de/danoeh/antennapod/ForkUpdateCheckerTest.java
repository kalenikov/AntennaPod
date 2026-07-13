package de.danoeh.antennapod;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.danoeh.antennapod.ui.screen.preferences.ForkUpdateChecker;
import fi.iki.elonen.NanoHTTPD;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Fork feature test: the update checker parses a GitHub "latest release" response and
 * correctly decides whether it is newer than the installed build. Runs against a local
 * HTTP server — no network.
 */
@RunWith(AndroidJUnit4.class)
public class ForkUpdateCheckerTest {

    private FakeGitHub server;

    private static class FakeGitHub extends NanoHTTPD {
        volatile String json;

        FakeGitHub() {
            super(0);
        }

        @Override
        public Response serve(IHTTPSession session) {
            return new Response(Response.Status.OK, "application/json", json);
        }

        String url() {
            return "http://127.0.0.1:" + getListeningPort() + "/latest";
        }
    }

    private static String releaseJson(String tag) {
        return "{\"tag_name\":\"" + tag + "\",\"assets\":["
                + "{\"name\":\"other.txt\",\"browser_download_url\":\"http://x/other.txt\"},"
                + "{\"name\":\"app-free-release.apk\","
                + "\"browser_download_url\":\"http://x/" + tag + "/app-free-release.apk\"}]}";
    }

    @Before
    public void setUp() throws Exception {
        server = new FakeGitHub();
        server.start();
    }

    @After
    public void tearDown() {
        server.stop();
    }

    @Test
    public void newerReleaseIsDetected() throws Exception {
        server.json = releaseJson("v3.11.4-fork.99");
        ForkUpdateChecker.UpdateInfo info = ForkUpdateChecker.checkLatest(server.url());
        assertEquals("v3.11.4-fork.99", info.tag);
        assertEquals(99, info.forkNumber);
        assertEquals("http://x/v3.11.4-fork.99/app-free-release.apk", info.apkUrl);
        assertTrue(info.isNewerThanInstalled());
    }

    @Test
    public void currentReleaseIsNotAnUpdate() throws Exception {
        server.json = releaseJson("v3.11.4-" + ForkUpdateChecker.FORK_VERSION);
        ForkUpdateChecker.UpdateInfo info = ForkUpdateChecker.checkLatest(server.url());
        assertFalse(info.isNewerThanInstalled());
    }
}
