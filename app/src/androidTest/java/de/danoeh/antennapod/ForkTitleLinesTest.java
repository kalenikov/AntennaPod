package de.danoeh.antennapod;

import android.content.Context;
import androidx.preference.PreferenceManager;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.danoeh.antennapod.storage.preferences.UserPreferences;

import static org.junit.Assert.assertEquals;

/**
 * Fork feature test: the episode-title line count setting defaults to 3 and is clamped to 2..4.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ForkTitleLinesTest {

    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        UserPreferences.init(context);
    }

    private void setPref(String value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(UserPreferences.PREF_FORK_EPISODE_TITLE_LINES, value).commit();
    }

    @Test
    public void defaultsClampsAndReads() {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .remove(UserPreferences.PREF_FORK_EPISODE_TITLE_LINES).commit();
        assertEquals(3, UserPreferences.getForkEpisodeTitleLines());

        setPref("2");
        assertEquals(2, UserPreferences.getForkEpisodeTitleLines());
        setPref("4");
        assertEquals(4, UserPreferences.getForkEpisodeTitleLines());

        setPref("17");
        assertEquals(4, UserPreferences.getForkEpisodeTitleLines());
        setPref("0");
        assertEquals(2, UserPreferences.getForkEpisodeTitleLines());
        setPref("мусор");
        assertEquals(3, UserPreferences.getForkEpisodeTitleLines());

        setPref("3");
    }
}
