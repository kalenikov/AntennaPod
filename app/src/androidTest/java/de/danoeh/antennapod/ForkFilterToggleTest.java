package de.danoeh.antennapod;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.danoeh.antennapod.model.feed.SubscriptionsFilter;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.screen.subscriptions.ForkFilterToggle;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Fork feature test: the toolbar filter button toggles only the "counter greater zero"
 * subscriptions-filter property and leaves other filter properties untouched.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ForkFilterToggleTest {

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        UserPreferences.init(context);
    }

    @Test
    public void togglesCounterFilterOnAndOff() {
        UserPreferences.setSubscriptionsFilter(new SubscriptionsFilter(""));
        assertFalse(ForkFilterToggle.isCounterFilterActive());

        assertTrue("first toggle must enable the filter", ForkFilterToggle.toggleCounterFilter());
        assertTrue(UserPreferences.getSubscriptionsFilter().showIfCounterGreaterZero);
        assertTrue(UserPreferences.getSubscriptionsFilter().isEnabled());

        assertFalse("second toggle must disable it", ForkFilterToggle.toggleCounterFilter());
        assertFalse(UserPreferences.getSubscriptionsFilter().showIfCounterGreaterZero);
    }

    @Test
    public void keepsOtherFilterProperties() {
        UserPreferences.setSubscriptionsFilter(
                new SubscriptionsFilter(SubscriptionsFilter.ENABLED_UPDATES));
        ForkFilterToggle.toggleCounterFilter();
        SubscriptionsFilter filter = UserPreferences.getSubscriptionsFilter();
        assertTrue(filter.showIfCounterGreaterZero);
        assertTrue("unrelated filter property must survive", filter.showUpdatedEnabled);

        ForkFilterToggle.toggleCounterFilter();
        filter = UserPreferences.getSubscriptionsFilter();
        assertFalse(filter.showIfCounterGreaterZero);
        assertTrue(filter.showUpdatedEnabled);
        UserPreferences.setSubscriptionsFilter(new SubscriptionsFilter(""));
    }
}
