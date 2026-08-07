package de.danoeh.antennapod;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.view.ContextThemeWrapper;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.screen.subscriptions.ForkCompactList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Fork feature test: compact subscription list mode. Checks the sizing contract and, most
 * importantly, that applying and reverting on the same view restores the original sizes -
 * the holders are recycled, so a one-way mutation would leave stale rows behind.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ForkCompactListTest {

    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        UserPreferences.init(context);
        ForkCompactList.setEnabled(false);
    }

    @After
    public void tearDown() {
        ForkCompactList.setEnabled(false);
    }

    @Test
    public void defaultsToDisabled() {
        assertFalse(ForkCompactList.isEnabled());
    }

    @Test
    public void preferenceRoundTrip() {
        ForkCompactList.setEnabled(true);
        assertTrue(ForkCompactList.isEnabled());
        ForkCompactList.setEnabled(false);
        assertFalse(ForkCompactList.isEnabled());
    }

    @Test
    public void appliesOnlyToSingleColumnList() {
        assertTrue("list mode is a single column", ForkCompactList.appliesTo(1));
        for (int columns = 2; columns <= 5; columns++) {
            assertFalse("grid must stay untouched", ForkCompactList.appliesTo(columns));
        }
    }

    @Test
    public void compactSizesAreSmallerThanNormal() {
        assertEquals(56, ForkCompactList.coverDp(false));
        assertEquals(32, ForkCompactList.coverDp(true));
        assertEquals(8, ForkCompactList.rowPaddingDp(false));
        assertEquals(6, ForkCompactList.rowPaddingDp(true));
        assertTrue("compact cover must be clearly smaller",
                ForkCompactList.coverDp(true) < ForkCompactList.coverDp(false));
        assertTrue("compact row padding must be smaller",
                ForkCompactList.rowPaddingDp(true) < ForkCompactList.rowPaddingDp(false));
        assertEquals(2, ForkCompactList.titleMaxLines(false));
        assertEquals("a two-line title would not let the row shrink",
                1, ForkCompactList.titleMaxLines(true));
    }

    @Test
    @UiThreadTest
    public void applyAndRevertOnSameView() {
        Context themed = new ContextThemeWrapper(context, R.style.Theme_AntennaPod_Light);
        View row = LayoutInflater.from(themed).inflate(R.layout.subscription_list_item, null, false);
        View cover = row.findViewById(R.id.coverImage);
        TextView title = row.findViewById(R.id.titleLabel);
        TextView count = row.findViewById(R.id.countViewPill);
        View pinIcon = row.findViewById(R.id.pinIcon);
        View errorIcon = row.findViewById(R.id.errorIcon);
        assertNotNull(cover);

        View coverContainer = (View) cover.getParent().getParent();

        ForkCompactList.setEnabled(true);
        ForkCompactList.apply(row, cover, title, count, pinIcon, errorIcon, 1);
        assertEquals(dp(32), coverContainer.getLayoutParams().height);
        assertEquals(dp(6), row.getPaddingTop());
        assertEquals(1, title.getMaxLines());

        ForkCompactList.setEnabled(false);
        ForkCompactList.apply(row, cover, title, count, pinIcon, errorIcon, 1);
        assertEquals("recycled row must return to the normal cover size",
                dp(56), coverContainer.getLayoutParams().height);
        assertEquals(dp(8), row.getPaddingTop());
        assertEquals(2, title.getMaxLines());
    }

    @Test
    @UiThreadTest
    public void gridRowsAreNotTouched() {
        Context themed = new ContextThemeWrapper(context, R.style.Theme_AntennaPod_Light);
        View row = LayoutInflater.from(themed).inflate(R.layout.subscription_list_item, null, false);
        View cover = row.findViewById(R.id.coverImage);
        TextView title = row.findViewById(R.id.titleLabel);
        TextView count = row.findViewById(R.id.countViewPill);
        View coverContainer = (View) cover.getParent().getParent();
        int originalHeight = coverContainer.getLayoutParams().height;

        ForkCompactList.setEnabled(true);
        ForkCompactList.apply(row, cover, title, count, null, null, 3);
        assertEquals(originalHeight, coverContainer.getLayoutParams().height);
    }

    private int dp(float value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
