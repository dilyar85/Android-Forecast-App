package com.dilyar.weather.app;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.dilyar.weather.app.notification.MyNotification;
import com.dilyar.weather.app.sync.SunshineSyncAdapter;
import com.dilyar.weather.app.utility.Utility;

import java.util.List;

/**
 * Created by Dilyar on 8/29/16.
 */
public class SettingActivity extends Activity {

    private static final String LOG_TAG = SettingActivity.class.getSimpleName();



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

//        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }



    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {

            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);
            //Init notification_city pref based on city names in SharedPreference
            initNotificationCityPreference();
            //Init all list preference summary
            initAllListPreferenceSummaryToValue();

        }

        private void initNotificationCityPreference() {

            ListPreference notificationCityPref = (ListPreference) findPreference(getString(R.string.pref_notification_city_key));

            //Set entries and values
            List<String> cityNames = Utility.getAllCityNamesFromSP(getActivity());
            if (cityNames != null) {
                CharSequence[] entries = cityNames.toArray(new CharSequence[cityNames.size()]);
                CharSequence[] entryValues = cityNames.toArray(new CharSequence[cityNames.size()]);
                notificationCityPref.setEntries(entries);
                notificationCityPref.setEntryValues(entryValues);
                notificationCityPref.setDefaultValue(entries[0]);
                Log.d(LOG_TAG, "initNotificationCityPreference() called");
                Log.d(LOG_TAG, "Default value: " + entryValues[0]);
            } else {
                Log.e(LOG_TAG, "getAllCityNamesFromSP() return null");
                notificationCityPref.setEnabled(false);
            }

        }

        /**
         * Attaches a listener so the summary is always updated with the preference value.
         * Also fires the listener once, to initialize the summary (so it shows up before the value
         * is changed.)
         */
        private void initAllListPreferenceSummaryToValue() {

            setListPreferenceSummary(getString(R.string.pref_weather_units_key));
            setListPreferenceSummary(getString(R.string.pref_notification_city_key));
            setListPreferenceSummary(getString(R.string.pref_notification_type_key));
            setListPreferenceSummary(getString(R.string.pref_weather_units_key));
//            onSharedPreferenceChanged(sharedPreferences, getString(R.string.pref_wind_units_key));
            setListPreferenceSummary(getString(R.string.pref_data_sync_frequency_key));

        }


        @Override
        public void onResume() {
            // Set the listener to watch for value changes.
            PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this);

            super.onResume();

        }

//
//



        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

            Log.e(LOG_TAG, "onSharedPreferenceChanged() called. Key: " + key);
            setListPreferenceSummary(key);

            //MyNotification category preferences
            String notificationEnableKey = getString(R.string.pref_enable_notifications_key);
            String notificationTypeKey = getString(R.string.pref_notification_type_key);
            String notificationCityKey = getString(R.string.pref_notification_city_key);

            if (key.equals(notificationEnableKey) || key.equals(notificationTypeKey) ||
                    key.equals(notificationCityKey)) {
                Log.d(LOG_TAG, "enable_notification preference is changed");
                boolean isEnabled = sharedPreferences.getBoolean(notificationEnableKey, true);
                if (isEnabled) {
                    //Display the notification with latest data
                    SunshineSyncAdapter.syncImmediately(getActivity(), null);
                } else {
                    //Disappear notification immediately
                    MyNotification.cancelNotification(getActivity());
                }

            }

            if (key.equals(getString(R.string.pref_data_sync_frequency_key))) {
                Log.d(LOG_TAG, "Data Update Frequency preference is changed");
                String updateFrequency = sharedPreferences.getString(key, getString(R.string.pref_data_sync_frequency_default));
                Preference updateAllCitiesPref = findPreference(getString(R.string.pref_enable_sync_all_key));
                if (updateFrequency.equals(getString(R.string.pref_data_sync_manually))) {
                    //Cannot set dependency cause it is not a checkbox preference
                    updateAllCitiesPref.setEnabled(false);
                    SunshineSyncAdapter.stopSyncing(getActivity());
                } else {
                    updateAllCitiesPref.setEnabled(true);
                    SunshineSyncAdapter.setAutoSyncing(getActivity());

                }
            }

        }



        //Set list preference summary
        private void setListPreferenceSummary(String preferenceKey) {

            Preference preference = findPreference(preferenceKey);

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list (since they have separate labels/values).
                ListPreference listPreference = (ListPreference) preference;

                String value = ((ListPreference) preference).getValue();

                int prefIndex = listPreference.findIndexOfValue(value);
                if (prefIndex >= 0) {
                    preference.setSummary(listPreference.getEntries()[prefIndex]);
                } else {
                    Log.e(LOG_TAG, "value: " + value + " does not exist in the entries");
                }
            }

        }



        @Override
        public void onPause() {

            PreferenceManager.getDefaultSharedPreferences(getActivity()).unregisterOnSharedPreferenceChangeListener(this);
            super.onPause();
        }

    }

}
