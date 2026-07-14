package de.danoeh.antennapod;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.danoeh.antennapod.model.feed.FeedCounter;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.screen.subscriptions.FeedCounterDialog;

import static org.junit.Assert.assertEquals;

/**
 * Fork feature test: the toolbar counter button cycles through all counter modes in the same
 * order as the old dialog and persists the choice.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ForkCounterCycleTest {

    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        UserPreferences.init(context);
    }

    @Test
    public void cyclesThroughAllModesAndWraps() {
        assertEquals(FeedCounter.SHOW_UNPLAYED, FeedCounterDialog.nextCounter(FeedCounter.SHOW_NEW));
        assertEquals(FeedCounter.SHOW_DOWNLOADED, FeedCounterDialog.nextCounter(FeedCounter.SHOW_UNPLAYED));
        assertEquals(FeedCounter.SHOW_DOWNLOADED_UNPLAYED,
                FeedCounterDialog.nextCounter(FeedCounter.SHOW_DOWNLOADED));
        assertEquals(FeedCounter.SHOW_NONE, FeedCounterDialog.nextCounter(FeedCounter.SHOW_DOWNLOADED_UNPLAYED));
        assertEquals(FeedCounter.SHOW_NEW, FeedCounterDialog.nextCounter(FeedCounter.SHOW_NONE));
    }

    @Test
    public void cycleSettingPersistsAndReturnsLabel() {
        UserPreferences.setFeedCounterSetting(FeedCounter.SHOW_NEW);
        String label = FeedCounterDialog.cycleCounterSetting(context);
        assertEquals(FeedCounter.SHOW_UNPLAYED, UserPreferences.getFeedCounterSetting());
        assertEquals(context.getString(R.string.drawer_feed_counter_unplayed), label);

        // Full loop returns to the start.
        for (int i = 0; i < 4; i++) {
            FeedCounterDialog.cycleCounterSetting(context);
        }
        assertEquals(FeedCounter.SHOW_NEW, UserPreferences.getFeedCounterSetting());
    }
}
