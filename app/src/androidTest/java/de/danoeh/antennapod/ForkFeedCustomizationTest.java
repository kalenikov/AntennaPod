package de.danoeh.antennapod;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.danoeh.antennapod.storage.preferences.ForkFeedCustomization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Fork feature test: pinning feeds and cover overrides (letter / custom text / custom image)
 * stored in ForkFeedCustomization. Pure logic on the target context, no activities.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ForkFeedCustomizationTest {

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        ForkFeedCustomization.init(context);
        for (long id = 1; id <= 5; id++) {
            ForkFeedCustomization.setPinned(id, false);
            ForkFeedCustomization.setCoverOverride(id, null);
        }
    }

    @Test
    public void pinnedFeedsFloatToTop() {
        ForkFeedCustomization.setPinned(3, true);
        ForkFeedCustomization.setPinned(5, true);
        assertTrue(ForkFeedCustomization.isPinned(3));
        assertFalse(ForkFeedCustomization.isPinned(1));

        // Same wrapping as DBReader.getNavDrawerData: pinned first, then base order.
        List<Long> ids = new ArrayList<>(Arrays.asList(1L, 2L, 3L, 4L, 5L));
        ids.sort((lhs, rhs) -> {
            int pinned = ForkFeedCustomization.comparePinned(lhs, rhs);
            return pinned != 0 ? pinned : Long.compare(lhs, rhs);
        });
        assertEquals(Arrays.asList(3L, 5L, 1L, 2L, 4L), ids);

        ForkFeedCustomization.setPinned(3, false);
        assertFalse(ForkFeedCustomization.isPinned(3));
    }

    @Test
    public void coverOverrideModes() {
        // No override: real cover, no tile text.
        assertNull(ForkFeedCustomization.getCoverTileText(1, "Download"));
        assertNull(ForkFeedCustomization.getCoverImagePath(1));

        // Letter: first letter of the title, uppercased.
        ForkFeedCustomization.setCoverOverride(1, ForkFeedCustomization.COVER_LETTER);
        assertEquals("D", ForkFeedCustomization.getCoverTileText(1, "download"));

        // Custom text wins over the title.
        ForkFeedCustomization.setCoverOverride(1, ForkFeedCustomization.COVER_TEXT_PREFIX + "даун");
        assertEquals("даун", ForkFeedCustomization.getCoverTileText(1, "Download"));

        // Custom image: path round-trips, no tile text.
        ForkFeedCustomization.setCoverOverride(1, ForkFeedCustomization.COVER_IMAGE_PREFIX + "/data/x.img");
        assertNull(ForkFeedCustomization.getCoverTileText(1, "Download"));
        assertEquals("/data/x.img", ForkFeedCustomization.getCoverImagePath(1));

        // Back to default.
        ForkFeedCustomization.setCoverOverride(1, null);
        assertNull(ForkFeedCustomization.getCoverOverride(1));
    }
}
