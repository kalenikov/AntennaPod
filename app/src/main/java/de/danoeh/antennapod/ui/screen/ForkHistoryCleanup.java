package de.danoeh.antennapod.ui.screen;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.event.MessageEvent;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.feed.SortOrder;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.ui.common.ConfirmationDialog;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.greenrobot.eventbus.EventBus;

import java.util.List;

/**
 * Fork feature: removes played episodes from the playback history and deletes their downloaded
 * files, replacing the stock "clear the whole history" action. Reuses the existing
 * DBWriter.deleteFeedMediaOfItem(context, media, true) (file plus history entry in one call) and
 * DBWriter.deleteFromPlaybackHistory() for episodes that were never downloaded, so no deletion
 * logic is duplicated here.
 */
public class ForkHistoryCleanup {

    private static final String TAG = "ForkHistoryCleanup";
    private static final int PAGE_SIZE = 150;
    private static final int MAX_PAGES = 1000;

    private static final FeedItemFilter FILTER_PLAYED_IN_HISTORY = new FeedItemFilter(
            FeedItemFilter.IS_IN_HISTORY, FeedItemFilter.PLAYED, FeedItemFilter.INCLUDE_ALL_FEED_STATES);

    private ForkHistoryCleanup() {
    }

    public static int countPlayedInHistory() {
        return Math.max(0, DBReader.getTotalEpisodeCount(FILTER_PLAYED_IN_HISTORY));
    }

    /**
     * Deletes every played episode from the playback history, removing the downloaded file when
     * there is one. Blocks until finished, so it must not run on the main thread. Returns the
     * number of episodes that were removed from the history.
     */
    public static int deletePlayedFromHistory(Context context) throws Exception {
        int removed = 0;
        for (int page = 0; page < MAX_PAGES; page++) {
            List<FeedItem> items = DBReader.getEpisodes(
                    0, PAGE_SIZE, FILTER_PLAYED_IN_HISTORY, SortOrder.COMPLETION_DATE_NEW_OLD);
            if (items.isEmpty()) {
                break;
            }
            int removedBefore = removed;
            for (FeedItem item : items) {
                FeedMedia media = item.getMedia();
                if (media == null) {
                    continue;
                }
                if (media.isDownloaded()) {
                    DBWriter.deleteFeedMediaOfItem(context, media, true).get();
                } else {
                    DBWriter.deleteFromPlaybackHistory(item).get();
                }
                removed++;
            }
            if (removed == removedBefore) {
                break;
            }
        }
        return removed;
    }

    /** Asks for confirmation and then runs the cleanup off the main thread. */
    public static void confirmAndRun(Activity activity) {
        Single.fromCallable(ForkHistoryCleanup::countPlayedInHistory)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(count -> {
                    if (count == 0) {
                        EventBus.getDefault().post(new MessageEvent(
                                activity.getString(R.string.fork_delete_played_history_none)));
                        return;
                    }
                    ConfirmationDialog dialog = new ConfirmationDialog(activity,
                            R.string.fork_delete_played_history_label,
                            activity.getString(R.string.fork_delete_played_history_confirm, count)) {
                        @Override
                        public void onConfirmButtonPressed(DialogInterface clickedDialog) {
                            clickedDialog.dismiss();
                            run(activity);
                        }
                    };
                    dialog.createNewDialog().show();
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    private static void run(Activity activity) {
        Context context = activity.getApplicationContext();
        Single.fromCallable(() -> deletePlayedFromHistory(context))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(count -> EventBus.getDefault().post(new MessageEvent(
                                activity.getResources().getQuantityString(
                                        R.plurals.deleted_episode_message, count, count))),
                        error -> Log.e(TAG, Log.getStackTraceString(error)));
    }
}
