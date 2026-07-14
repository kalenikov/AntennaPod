package de.danoeh.antennapod.ui.screen.subscriptions;

import android.content.Context;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.event.UnreadItemsUpdateEvent;
import de.danoeh.antennapod.model.feed.FeedCounter;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import org.greenrobot.eventbus.EventBus;

import java.util.Arrays;
import java.util.List;

public class FeedCounterDialog {
    /** Same order as the dialog list (nav_drawer_feed_counter_values): 1 → 2 → 4 → 5 → 3 → 1. */
    private static final FeedCounter[] CYCLE = {FeedCounter.SHOW_NEW, FeedCounter.SHOW_UNPLAYED,
            FeedCounter.SHOW_DOWNLOADED, FeedCounter.SHOW_DOWNLOADED_UNPLAYED, FeedCounter.SHOW_NONE};

    /** Fork feature: the toolbar counter button cycles through the options instead of a dialog. */
    public static FeedCounter nextCounter(FeedCounter current) {
        for (int i = 0; i < CYCLE.length; i++) {
            if (CYCLE[i] == current) {
                return CYCLE[(i + 1) % CYCLE.length];
            }
        }
        return CYCLE[0];
    }

    /** Cycles the counter setting and returns the label of the newly selected option. */
    public static String cycleCounterSetting(Context context) {
        FeedCounter next = nextCounter(UserPreferences.getFeedCounterSetting());
        UserPreferences.setFeedCounterSetting(next);
        EventBus.getDefault().post(new UnreadItemsUpdateEvent());
        List<String> entryValues =
                Arrays.asList(context.getResources().getStringArray(R.array.nav_drawer_feed_counter_values));
        String[] items = context.getResources().getStringArray(R.array.nav_drawer_feed_counter_options);
        return items[entryValues.indexOf("" + next.id)];
    }

    public static void showDialog(Context context) {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(context);
        dialog.setTitle(context.getString(R.string.pref_nav_drawer_feed_counter_title));
        dialog.setNegativeButton(android.R.string.cancel, (d, listener) -> d.dismiss());

        int selected = UserPreferences.getFeedCounterSetting().id;
        List<String> entryValues =
                Arrays.asList(context.getResources().getStringArray(R.array.nav_drawer_feed_counter_values));
        final int selectedIndex = entryValues.indexOf("" + selected);

        String[] items = context.getResources().getStringArray(R.array.nav_drawer_feed_counter_options);
        dialog.setSingleChoiceItems(items, selectedIndex, (d, which) -> {
            if (selectedIndex != which) {
                UserPreferences.setFeedCounterSetting(
                        FeedCounter.fromOrdinal(Integer.parseInt(entryValues.get(which))));
                //Update subscriptions
                EventBus.getDefault().post(new UnreadItemsUpdateEvent());
            }
            d.dismiss();
        });
        dialog.show();
    }
}
