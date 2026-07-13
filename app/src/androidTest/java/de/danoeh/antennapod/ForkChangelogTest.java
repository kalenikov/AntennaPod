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

import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.ui.screen.preferences.ForkUpdateChecker;
import de.danoeh.antennapod.ui.screen.preferences.PreferenceActivity;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;

/**
 * Fork feature tests: the "What's new" changelog dialog pops up once after an update
 * (stored fork version older than the running build) and stays away otherwise; the new
 * KalenikovPod settings section opens and shows the fork entries.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ForkChangelogTest {

    private static final String PREFS = "MainActivityPrefs";
    private static final String PREF_LAST_SEEN = "prefLastSeenForkVersion";
    private static final String PREF_IS_FIRST_LAUNCH = "prefMainActivityIsFirstLaunch";

    private SharedPreferences prefs() {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @Test
    public void changelogShownAfterUpdate() {
        prefs().edit().putBoolean(PREF_IS_FIRST_LAUNCH, false)
                .putString(PREF_LAST_SEEN, "fork.1").commit();
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withText(R.string.fork_changelog_title)).check(matches(isDisplayed()));
            assertEquals(ForkUpdateChecker.FORK_VERSION, prefs().getString(PREF_LAST_SEEN, null));
        }
    }

    @Test
    public void changelogNotShownWhenUpToDate() {
        prefs().edit().putBoolean(PREF_IS_FIRST_LAUNCH, false)
                .putString(PREF_LAST_SEEN, ForkUpdateChecker.FORK_VERSION).commit();
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withText(R.string.fork_changelog_title)).check(doesNotExist());
        }
    }

    @Test
    public void kalenikovPodSettingsSectionOpens() {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(ctx, PreferenceActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try (ActivityScenario<PreferenceActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withText(R.string.kalenikovpod_pref)).perform(click());
            onView(withText(R.string.fork_update_label)).check(matches(isDisplayed()));
            onView(withText(R.string.fork_changelog_label)).check(matches(isDisplayed()));
            onView(withText(R.string.view_logs_label)).check(matches(isDisplayed()));
        }
    }
}
