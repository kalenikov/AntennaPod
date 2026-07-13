package de.danoeh.antennapod.ui.screen.preferences;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.ui.preferences.screen.AnimatedPreferenceFragment;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Fork settings screen: everything KalenikovPod adds on top of stock AntennaPod lives here —
 * root migration from stock, log viewer, feature list, update check and changelog.
 */
public class KalenikovPodPreferencesFragment extends AnimatedPreferenceFragment {
    private static final String TAG = "KalenikovPodPrefs";
    private static final String PREF_ROOT_IMPORT_FROM_STOCK = "prefRootImportFromStock";
    private static final String PREF_VIEW_LOGS = "prefViewLogs";
    private static final String PREF_FORK_FEATURES = "prefForkFeatures";
    private static final String PREF_FORK_UPDATE = "prefForkUpdate";
    private static final String PREF_FORK_CHANGELOG = "prefForkChangelog";

    private Disposable disposable;
    private ProgressDialog progressDialog;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_kalenikovpod);
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage(getContext().getString(R.string.please_wait));

        findPreference(PREF_ROOT_IMPORT_FROM_STOCK).setOnPreferenceClickListener(
                preference -> {
                    confirmRootImport();
                    return true;
                });
        findPreference(PREF_VIEW_LOGS).setOnPreferenceClickListener(
                preference -> {
                    openLogViewer(null);
                    return true;
                });
        findPreference(PREF_FORK_FEATURES).setOnPreferenceClickListener(
                preference -> {
                    startActivity(new Intent(getContext(), ForkFeaturesActivity.class));
                    return true;
                });
        findPreference(PREF_FORK_UPDATE).setOnPreferenceClickListener(
                preference -> {
                    checkForkUpdate();
                    return true;
                });
        findPreference(PREF_FORK_CHANGELOG).setOnPreferenceClickListener(
                preference -> {
                    ForkChangelog.show(getContext());
                    return true;
                });
    }

    @Override
    public void onStart() {
        super.onStart();
        ((PreferenceActivity) getActivity()).getSupportActionBar().setTitle(R.string.kalenikovpod_pref);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (disposable != null) {
            disposable.dispose();
        }
    }

    private void checkForkUpdate() {
        ProgressDialog progress = new ProgressDialog(getContext());
        progress.setMessage(getString(R.string.fork_update_checking));
        progress.setCancelable(false);
        progress.show();
        new Thread(() -> {
            try {
                ForkUpdateChecker.UpdateInfo info =
                        ForkUpdateChecker.checkLatest(ForkUpdateChecker.DEFAULT_API_URL);
                runOnUiThreadIfAdded(() -> {
                    progress.dismiss();
                    if (info.isNewerThanInstalled()) {
                        new MaterialAlertDialogBuilder(getActivity())
                                .setTitle(R.string.fork_update_available_title)
                                .setMessage(getString(R.string.fork_update_available, info.tag))
                                .setNegativeButton(R.string.cancel_label, null)
                                .setPositiveButton(R.string.confirm_label,
                                        (dialog, which) -> downloadAndInstallForkUpdate(info))
                                .show();
                    } else {
                        new MaterialAlertDialogBuilder(getActivity())
                                .setMessage(getString(R.string.fork_update_latest,
                                        ForkUpdateChecker.FORK_VERSION))
                                .setPositiveButton(android.R.string.ok, null)
                                .show();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Fork update check failed", e);
                runOnUiThreadIfAdded(() -> {
                    progress.dismiss();
                    new MaterialAlertDialogBuilder(getActivity())
                            .setMessage(getString(R.string.fork_update_error, e.getMessage()))
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                });
            }
        }).start();
    }

    private void downloadAndInstallForkUpdate(ForkUpdateChecker.UpdateInfo info) {
        ProgressDialog progress = new ProgressDialog(getContext());
        progress.setMessage(getString(R.string.fork_update_downloading, info.tag));
        progress.setCancelable(false);
        progress.show();
        Context appContext = getContext().getApplicationContext();
        new Thread(() -> {
            try {
                File apk = ForkUpdateChecker.downloadApk(appContext, info);
                runOnUiThreadIfAdded(() -> {
                    progress.dismiss();
                    ForkUpdateChecker.installApk(appContext, apk);
                });
            } catch (Exception e) {
                Log.e(TAG, "Fork update download failed", e);
                runOnUiThreadIfAdded(() -> {
                    progress.dismiss();
                    new MaterialAlertDialogBuilder(getActivity())
                            .setMessage(getString(R.string.fork_update_error, e.getMessage()))
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                });
            }
        }).start();
    }

    private void runOnUiThreadIfAdded(Runnable action) {
        Activity activity = getActivity();
        if (activity != null && isAdded()) {
            activity.runOnUiThread(action);
        }
    }

    private void confirmRootImport() {
        new MaterialAlertDialogBuilder(getActivity())
                .setTitle(R.string.root_import_label)
                .setMessage(R.string.root_import_warning)
                .setNegativeButton(R.string.no, null)
                .setPositiveButton(R.string.confirm_label, (dialog, which) -> runRootImport())
                .show();
    }

    private void runRootImport() {
        progressDialog.show();
        disposable = Single.fromCallable(() -> RootMigrator.migrate(getContext()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    progressDialog.dismiss();
                    showRootImportResult(result);
                }, error -> {
                    progressDialog.dismiss();
                    new MaterialAlertDialogBuilder(getContext())
                            .setTitle(R.string.root_import_failed_title)
                            .setMessage(String.valueOf(error))
                            .setPositiveButton(R.string.view_logs_label, (d, w) -> openLogViewer("migration"))
                            .show();
                });
    }

    private void showRootImportResult(RootMigrator.Result result) {
        if (result.isSuccess()) {
            new MaterialAlertDialogBuilder(getContext())
                    .setTitle(R.string.root_import_done_title)
                    .setMessage(R.string.root_import_done_message)
                    .setCancelable(false)
                    .setNeutralButton(R.string.view_logs_label, (d, w) -> openLogViewer("migration"))
                    .setPositiveButton(R.string.restart_label, (d, w) -> forceRestart())
                    .show();
        } else {
            new MaterialAlertDialogBuilder(getContext())
                    .setTitle(R.string.root_import_failed_title)
                    .setMessage(getString(R.string.root_import_failed_title) + " (exit " + result.exitCode + ")")
                    .setPositiveButton(R.string.view_logs_label, (d, w) -> openLogViewer("migration"))
                    .setNegativeButton(android.R.string.ok, null)
                    .show();
        }
    }

    private void openLogViewer(String tab) {
        Intent intent = new Intent(getContext(), LogViewerActivity.class);
        if (tab != null) {
            intent.putExtra(LogViewerActivity.EXTRA_TAB, tab);
        }
        startActivity(intent);
    }

    private void forceRestart() {
        PackageManager pm = getContext().getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage(getContext().getPackageName());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().getApplicationContext().startActivity(intent);
        Runtime.getRuntime().exit(0);
    }
}
