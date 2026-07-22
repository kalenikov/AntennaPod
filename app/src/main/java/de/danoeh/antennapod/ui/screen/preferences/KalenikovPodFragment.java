package de.danoeh.antennapod.ui.screen.preferences;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.appbar.MaterialToolbar;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;

/**
 * Fork navigation destination: hosts {@link KalenikovPodPreferencesFragment} as a full navigation
 * screen (bottom nav / "More" / drawer), in addition to its existing entry point under Settings.
 */
public class KalenikovPodFragment extends Fragment {
    public static final String TAG = "KalenikovPodFragment";

    private boolean displayUpArrow;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.kalenikovpod_fragment, container, false);
        MaterialToolbar toolbar = root.findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.kalenikovpod_pref);

        displayUpArrow = getParentFragmentManager().getBackStackEntryCount() != 0;
        if (savedInstanceState != null) {
            displayUpArrow = savedInstanceState.getBoolean("up_arrow");
        }
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setupToolbarToggle(toolbar, displayUpArrow);
        }

        if (savedInstanceState == null) {
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.kalenikovPodPreferencesContainer, new KalenikovPodPreferencesFragment())
                    .commit();
        }
        return root;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("up_arrow", displayUpArrow);
    }
}
