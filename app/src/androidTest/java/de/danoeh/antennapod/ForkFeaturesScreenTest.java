package de.danoeh.antennapod;

import android.content.Context;
import android.content.Intent;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import de.danoeh.antennapod.ui.screen.preferences.ForkFeaturesActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

/**
 * Smoke test for the fork's "features list" screen: launches ForkFeaturesActivity and asserts a
 * known feature entry is actually rendered. Guards against the screen crashing on open or the
 * hand-maintained list failing to resolve its string resources.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ForkFeaturesScreenTest {

    @Test
    public void featuresScreen_showsFeatureEntries() {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(ctx, ForkFeaturesActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(intent);

        onView(withText(R.string.fork_feat_watchedsync_title)).check(matches(isDisplayed()));
        onView(withText(R.string.fork_feat_delete_title)).check(matches(isDisplayed()));
    }
}
