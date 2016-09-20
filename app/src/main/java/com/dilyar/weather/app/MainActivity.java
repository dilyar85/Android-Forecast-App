/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dilyar.weather.app;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;
import android.widget.Toast;

import com.dilyar.weather.app.adapter.ViewPagerAdapter;
import com.dilyar.weather.app.notification.MyNotification;
import com.dilyar.weather.app.sync.SunshineSyncAdapter;
import com.dilyar.weather.app.utility.Utility;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends ActionBarActivity implements SearchView.OnQueryTextListener {

    private final String LOG_TAG = MainActivity.class.getSimpleName();

    @BindView(R.id.pager)
    ViewPager mViewPager;

    ViewPagerAdapter mPagerAdapter;



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.i(LOG_TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mPagerAdapter = new ViewPagerAdapter(this, getSupportFragmentManager());
        mViewPager.setAdapter(mPagerAdapter);

        checkIfLaunchedFromNotification();

        SunshineSyncAdapter.initializeSyncAdapter(this);

    }



    private void checkIfLaunchedFromNotification() {

        String inputLocationFromNotification = getIntent().getStringExtra(
                MyNotification.INTENT_EXTRA_INPUT_LOCATION_KEY);
        if (inputLocationFromNotification != null) {
            mViewPager.setCurrentItem(Utility.getIndexOfLocation(this, inputLocationFromNotification));
        }
    }



    @Override
    protected void onNewIntent(Intent intent) {

        Log.i(LOG_TAG, "onNewIntent()");
        super.onNewIntent(intent);

        //Verify the action and get the query
        if (intent.getAction().equals(Intent.ACTION_SEARCH)) {
            String inputLocation = intent.getStringExtra(SearchManager.QUERY);

            //Download weather data from server
            SunshineSyncAdapter.syncImmediatelyWithLocationName(this, inputLocation);
            Log.d(LOG_TAG, "Got searching result: " + inputLocation + ". Start syncing");

            Utility.saveCityNameToSP(this, inputLocation, true);
            mPagerAdapter.notifyDataSetChanged();
            mViewPager.setCurrentItem(Utility.getCityNamesSize(this), false);

            if (Utility.getNotificationCity(this) == null) {
                Log.d(LOG_TAG, "MyNotification city was null, set it to new city: " + inputLocation);
                Utility.setNotificationCity(this, inputLocation, true);
            }

        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingActivity.class));
            return true;
        }

        if (id == R.id.add) {
            Intent searchIntent = new Intent(this, SearchActivity.class);
            startActivity(searchIntent);
            return true;
        }

        if (id == R.id.delete_city) {

            String deletedCity = Utility.deleteCityNameFromSP(this, mViewPager.getCurrentItem(), true);
            if (deletedCity == null) {
                Toast.makeText(this, getString(R.string.toast_no_more_city_to_delete), Toast.LENGTH_SHORT).show();
                return true;
            }
            String notificationCity = Utility.getNotificationCity(this);

            //Change notification city to the first one if there is city available.
            //If all cities are deleted, MyNotification City would be set to null.
            if (deletedCity.equals(notificationCity)) {
                String newNotificationCity = Utility.getCityNameByIndex(this, 0);
                Utility.setNotificationCity(this, newNotificationCity, true);
            }

            mPagerAdapter.notifyDataSetChanged();
        }

        return super.onOptionsItemSelected(item);
    }



    @Override
    public boolean onQueryTextSubmit(String s) {

        return false;
    }



    @Override
    public boolean onQueryTextChange(String s) {

        return false;
    }
}
