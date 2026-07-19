package de.danoeh.antennapod.ui.screen.feed;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fork helper: maps a Pinchflat RSS feed download URL to the URL that opens the
 * source page in the Pinchflat web UI.
 *
 * <p>A Pinchflat feed URL looks like {@code http://host:8945/sources/<uuid>/feed}. The
 * feed only carries the source <em>uuid</em>, while the Pinchflat source page is served
 * by numeric id ({@code /sources/<id>}). The Pinchflat fork exposes a redirect endpoint
 * {@code /sources/uuid/<uuid>} that resolves the uuid to its numeric-id page, so we only
 * have to rewrite the feed URL here — no hardcoded host and no id lookup on the client.
 */
public final class ForkPinchflat {

    private static final Pattern PINCHFLAT_FEED = Pattern.compile(
            "^(https?://[^/]+)/sources/([0-9a-fA-F-]{36})/feed/?(\\?.*)?$");

    private ForkPinchflat() {
    }

    /**
     * @param downloadUrl the feed's download URL
     * @return the Pinchflat source page URL, or {@code null} if this is not a Pinchflat feed
     */
    public static String sourcePageUrl(String downloadUrl) {
        if (downloadUrl == null) {
            return null;
        }
        Matcher matcher = PINCHFLAT_FEED.matcher(downloadUrl.trim());
        if (!matcher.matches()) {
            return null;
        }
        return matcher.group(1) + "/sources/uuid/" + matcher.group(2);
    }
}
