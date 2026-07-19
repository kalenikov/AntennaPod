package de.danoeh.antennapod;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import de.danoeh.antennapod.ui.screen.feed.ForkPinchflat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Fork feature test: {@link ForkPinchflat#sourcePageUrl} rewrites a Pinchflat RSS feed
 * URL into the Pinchflat source-page deeplink, and returns null for anything else.
 */
@RunWith(AndroidJUnit4.class)
public class ForkPinchflatUrlTest {

    private static final String UUID = "2da1e161-e065-4fd3-8984-e01c4083f1c6";

    @Test
    public void rewritesPinchflatFeedUrl() {
        assertEquals("http://192.168.1.87:8945/sources/uuid/" + UUID,
                ForkPinchflat.sourcePageUrl("http://192.168.1.87:8945/sources/" + UUID + "/feed"));
    }

    @Test
    public void rewritesHttpsAndTrailingSlashAndQuery() {
        assertEquals("https://pf.example.com/sources/uuid/" + UUID,
                ForkPinchflat.sourcePageUrl("https://pf.example.com/sources/" + UUID + "/feed/"));
        assertEquals("https://pf.example.com/sources/uuid/" + UUID,
                ForkPinchflat.sourcePageUrl("https://pf.example.com/sources/" + UUID + "/feed?fresh=1"));
    }

    @Test
    public void returnsNullForNonPinchflatUrls() {
        assertNull(ForkPinchflat.sourcePageUrl(null));
        assertNull(ForkPinchflat.sourcePageUrl(""));
        assertNull(ForkPinchflat.sourcePageUrl("https://www.youtube.com/@PolinaPars"));
        assertNull(ForkPinchflat.sourcePageUrl("https://example.com/feed.xml"));
        // media stream URL is not a feed URL
        assertNull(ForkPinchflat.sourcePageUrl("http://192.168.1.87:8945/media/" + UUID + "/stream"));
    }
}
