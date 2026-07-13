package de.danoeh.antennapod;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import de.danoeh.antennapod.ui.screen.preferences.ForkChangelog;
import de.danoeh.antennapod.ui.screen.preferences.ForkUpdateChecker;
import de.danoeh.antennapod.ui.screen.preferences.PreferenceActivity;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Fork feature tests for the changelog: the "What's new" decision logic fires once after an
 * update and records the seen version (tested directly — MainActivity never reaches RESUMED
 * under instrumentation because of system dialogs), and the new KalenikovPod settings section
 * opens with all fork entries, including the changelog dialog itself.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ForkChangelogTest {

    private static final String PREFS = "MainActivityPrefs";
    private static final String PREF_LAST_SEEN = "prefLastSeenForkVersion";

    private SharedPreferences prefs() {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @Test
    public void shownOnceAfterUpdate() {
        SharedPreferences prefs = prefs();
        prefs.edit().putString(PREF_LAST_SEEN, "fork.1").commit();
        assertTrue("older recorded version must trigger the dialog",
                ForkChangelog.shouldShowAndMark(prefs, false));
        assertEquals(ForkUpdateChecker.FORK_VERSION, prefs.getString(PREF_LAST_SEEN, null));
        assertFalse("second launch on the same version must not trigger again",
                ForkChangelog.shouldShowAndMark(prefs, false));
    }

    @Test
    public void notShownOnFreshInstall() {
        SharedPreferences prefs = prefs();
        prefs.edit().remove(PREF_LAST_SEEN).commit();
        assertFalse("fresh install must not show the changelog",
                ForkChangelog.shouldShowAndMark(prefs, true));
        assertEquals("fresh install must still record the current version",
                ForkUpdateChecker.FORK_VERSION, prefs.getString(PREF_LAST_SEEN, null));
    }

    @Test
    public void kalenikovPodSettingsSectionOpensAndShowsChangelog() {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(ctx, PreferenceActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try (ActivityScenario<PreferenceActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withText(R.string.kalenikovpod_pref)).perform(click());
            onView(withText(R.string.fork_update_label)).check(matches(isDisplayed()));
            onView(withText(R.string.view_logs_label)).check(matches(isDisplayed()));
            onView(withText(R.string.fork_changelog_label)).perform(click());
            onView(withText(R.string.fork_changelog_title)).check(matches(isDisplayed()));
        }
    }
}
