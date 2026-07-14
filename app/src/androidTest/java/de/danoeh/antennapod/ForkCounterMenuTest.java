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
 * Fork feature test: the toolbar counter dropdown persists the selected counter mode for
 * every available option.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ForkCounterMenuTest {

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        UserPreferences.init(context);
    }

    @Test
    public void selectingEachModePersists() {
        for (FeedCounter counter : FeedCounter.values()) {
            FeedCounterDialog.selectCounter(counter);
            assertEquals(counter, UserPreferences.getFeedCounterSetting());
        }
        FeedCounterDialog.selectCounter(FeedCounter.SHOW_NEW);
        assertEquals(FeedCounter.SHOW_NEW, UserPreferences.getFeedCounterSetting());
    }
}
