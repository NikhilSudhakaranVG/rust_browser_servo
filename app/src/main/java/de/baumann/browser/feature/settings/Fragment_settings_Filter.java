package de.baumann.browser.feature.settings;

import android.os.Bundle;

import de.baumann.browser.R;

public class Fragment_settings_Filter extends BasePreferenceFragment {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference_filter, rootKey);
    }
}