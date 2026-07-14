package de.danoeh.antennapod.ui.screen.subscriptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.danoeh.antennapod.event.UnreadItemsUpdateEvent;
import de.danoeh.antennapod.model.feed.SubscriptionsFilter;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import org.greenrobot.eventbus.EventBus;

/**
 * Fork feature: toolbar button that toggles the "counter greater zero" subscriptions filter —
 * the same property as the checkbox in the filter dialog. Other filter properties are kept
 * untouched. Static and UI-free so tests can drive it without an activity.
 */
public class ForkFilterToggle {

    private ForkFilterToggle() {
    }

    public static boolean isCounterFilterActive() {
        return UserPreferences.getSubscriptionsFilter().showIfCounterGreaterZero;
    }

    /** Flips the counter filter and returns the new state (true = filter now active). */
    public static boolean toggleCounterFilter() {
        SubscriptionsFilter filter = UserPreferences.getSubscriptionsFilter();
        List<String> properties = new ArrayList<>(Arrays.asList(filter.getValues()));
        boolean enable = !filter.showIfCounterGreaterZero;
        if (enable) {
            properties.add(SubscriptionsFilter.COUNTER_GREATER_ZERO);
        } else {
            properties.remove(SubscriptionsFilter.COUNTER_GREATER_ZERO);
        }
        UserPreferences.setSubscriptionsFilter(
                new SubscriptionsFilter(properties.toArray(new String[0])));
        EventBus.getDefault().post(new UnreadItemsUpdateEvent());
        return enable;
    }
}
