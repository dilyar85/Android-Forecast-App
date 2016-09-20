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

import android.database.Cursor;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.dilyar.weather.app.adapter.ForecastAdapter;
import com.dilyar.weather.app.data.WeatherContract;
import com.dilyar.weather.app.sync.SunshineSyncAdapter;
import com.dilyar.weather.app.utility.ImageLoader;
import com.dilyar.weather.app.utility.Utility;

import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Encapsulates fetching the forecast and displaying it as a {@link ListView} layout.
 */
public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        ImageLoader.CallbackListener {

    public static final String LOG_TAG = ForecastFragment.class.getSimpleName();

    private static final String[] FORECAST_COLUMNS = {

            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.LocationEntry.COLUMN_INPUT_LOCATION_NAME,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_CODE,
            WeatherContract.LocationEntry.COLUMN_COORD_LAT,
            WeatherContract.LocationEntry.COLUMN_COORD_LONG,
            WeatherContract.LocationEntry.COLUMN_CITY_NAME,
            WeatherContract.WeatherEntry.COLUMN_CURRENT_TEMP,
            WeatherContract.WeatherEntry.COLUMN_CLOUDINESS,
            WeatherContract.WeatherEntry.COLUMN_WIND_SPEED,
            WeatherContract.WeatherEntry.COLUMN_HUMIDITY,
            WeatherContract.WeatherEntry.COLUMN_DEGREES
    };
    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
    // must change.
    public static final int COL_WEATHER_ID = 0;
    public static final int COL_WEATHER_DATE = 1;
    public static final int COL_WEATHER_DESC = 2;
    public static final int COL_WEATHER_MAX_TEMP = 3;
    public static final int COL_WEATHER_MIN_TEMP = 4;
    public static final int COL_LOCATION_SETTING = 5;
    public static final int COL_WEATHER_CONDITION_ID = 6;
    public static final int COL_COORD_LAT = 7;
    public static final int COL_COORD_LONG = 8;
    public static final int COL_CITY_NAME = 9;
    public static final int COL_CURRENT_TEMP = 10;
    public static final int COL_CLOUDINESS = 11;
    public static final int COL_WIND_SPEED = 12;
    public static final int COL_HUMIDITY = 13;
    public static final int COL_WIND_DIRECTION = 14;


    private static final String[] PHOTO_COLUMNS = {
            WeatherContract.PhotoEntry.TABLE_NAME + "." + WeatherContract.PhotoEntry._ID,
            WeatherContract.PhotoEntry.COLUMN_PHOTO_URL,
            WeatherContract.PhotoEntry.COLUMN_PHOTO_OWNER,
            WeatherContract.PhotoEntry.COLUMN_PHOTO_TITLE,
            WeatherContract.PhotoEntry.COLUMN_PHOTO_DATE,
            WeatherContract.PhotoEntry.COLUMN_PHOTO_COUNT
    };

    static final int COL_PHOTO_ID = 0;
    static final int COL_PHOTO_URL = 1;
    static final int COL_PHOTO_OWNER = 2;
    static final int COL_PHOTO_TITLE = 3;
    static final int COL_PHOTO_DATE = 4;
    static final int COL_PHOTO_COUNT = 5;

    @BindView(R.id.fragment_listview)
    ListView mForecastListview;
    @BindView(R.id.background_imageview)
    ImageView mBackgroundImageview;
    @BindView(R.id.swipe_refresh_layout)
    SwipeRefreshLayout mRefreshLayout;
    @BindView(R.id.image_owner_textview)
    TextView mImageOwnerTextview;
    @BindView(R.id.empty_textview)
    TextView mEmptyTextview;

    private static final int FORECAST_LOADER = 0;
    private static final int PHOTO_LOADER = 1;

    private ForecastAdapter mForecastAdapter;

    public static final String BUNDLE_KEY_CITY_NAME = "bundle_key_city_name";
    private String mInputLocationName;



    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Log.i(LOG_TAG, "onCreate()");

    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_main, container, false);
        ButterKnife.bind(this, view);

        initView();

        Log.i(LOG_TAG, "onCreateView()");

        return view;
    }



    private void initView() {

        //Init ListView and set its adapter
        mForecastAdapter = new ForecastAdapter(getActivity(), null, 0);
        mForecastListview.setEmptyView(mEmptyTextview);
        mForecastListview.setAdapter(mForecastAdapter);

        // listen refresh event
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                // start refresh
                if (mInputLocationName != null) {
                    Log.d(LOG_TAG, "Start refreshing: " + mInputLocationName);
                    Utility.setUpdatedManually(getActivity(), true);
                    SunshineSyncAdapter.syncImmediatelyWithLocationName(getActivity(), mInputLocationName);
                }
            }
        });

        mForecastListview.setOnScrollListener(new AbsListView.OnScrollListener() {

            private boolean listViewIsAtTop;



            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }



            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

                //Avoid doing refresh at the middle
                int topRowVerticalPosition = (mForecastListview == null || mForecastListview.getChildCount() == 0) ? 0 : mForecastListview.getChildAt(0).getTop();
                listViewIsAtTop = firstVisibleItem == 0 && topRowVerticalPosition >= 0;
                mRefreshLayout.setEnabled(listViewIsAtTop);
                //Hide photo owner's textview when scroll down
                if (!listViewIsAtTop) {
                    mImageOwnerTextview.setVisibility(View.GONE);
                } else {
                    mImageOwnerTextview.setVisibility(View.VISIBLE);
                }

            }
        });

    }



    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);
        Log.i(LOG_TAG, "onActivityCreated()");
        ImageLoader.getInstance().setCallbackListener(this);
        initData();

    }



    private void initData() {

        String inputLocationName = getArguments().getString(BUNDLE_KEY_CITY_NAME);
        if (inputLocationName == null) {
            SunshineSyncAdapter.setServerStatus(getActivity(), SunshineSyncAdapter.LOCATION_NULL, true);
            updateEmptyTextview();
        } else {
            mInputLocationName = inputLocationName;

//            SunshineSyncAdapter.syncImmediatelyWithLocationName(getActivity(), inputLocationName);

            getLoaderManager().initLoader(FORECAST_LOADER, null, this);
            getLoaderManager().initLoader(PHOTO_LOADER, null, this);

        }

    }



    @Override
    public void onResume() {

        Log.i(LOG_TAG, "onResume()");
        restartLoaders();
        super.onResume();

    }



    private void updateEmptyTextview() {

        mEmptyTextview.setGravity(Gravity.CENTER);

        if (mForecastAdapter.getCount() == 0) {
            String defaultErrorMessage = getString(R.string.empty_textview_default_message);

            switch (SunshineSyncAdapter.getServerStatus(getActivity())) {
                case SunshineSyncAdapter.LOCATION_NULL:
                    defaultErrorMessage = getString(R.string.empty_textview_no_city);
                    break;
                case SunshineSyncAdapter.SERVER_DOWN:
                    defaultErrorMessage = getString(R.string.empty_textview_server_down);
                    break;
                case SunshineSyncAdapter.SERVER_INVALID_CITY:
                    defaultErrorMessage = getString(R.string.empty_textview_server_invalid_id);
                    break;
                case SunshineSyncAdapter.SERVER_UNKNOWN:
                    defaultErrorMessage = getString(R.string.empty_textview_server_unknown);
                    break;
                default:
                    if (!Utility.isNetworkAvailable(getActivity())) {
                        defaultErrorMessage = getString(R.string.empty_textview_network_not_available);
                    }
            }
            mEmptyTextview.setText(defaultErrorMessage);
        }

    }



    /**
     * Only create loader when mInputLocationName is not null
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        mRefreshLayout.setRefreshing(true);

        Log.d(LOG_TAG, "onCreateLoader() called. Loader id: " + id);
        Log.d(LOG_TAG, "loader city: " + this.mInputLocationName);

        switch (id) {
            case FORECAST_LOADER:
                // Sort order:  Ascending, by date.
                String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
                Uri weatherUriWithLocationAndStartDate = WeatherContract.WeatherEntry
                        .buildWeatherLocationWithDateInQueryParameter(mInputLocationName, System.currentTimeMillis());

                Log.d(LOG_TAG, "Create Weather loader uri: " + weatherUriWithLocationAndStartDate);
                return new CursorLoader(getActivity(),
                        weatherUriWithLocationAndStartDate,
                        FORECAST_COLUMNS,
                        null,
                        null,
                        sortOrder);

            case PHOTO_LOADER:
                Uri photoUriWithLocation = WeatherContract.PhotoEntry.buildPhotoUriWithLocation(mInputLocationName);
                Log.d(LOG_TAG, "Create Photo loader uri: " + photoUriWithLocation);

                return new CursorLoader(getActivity(),
                        photoUriWithLocation,
                        PHOTO_COLUMNS,
                        null,
                        null,
                        null
                );

            default:
                Log.e(LOG_TAG, "Cannot find loader of id: " + id);
                return null;
        }

    }



    @Override
    public void onLoadFinished(final Loader<Cursor> cursorLoader, final Cursor cursor) {

        Log.d(LOG_TAG, "onLoaderFinished(). Loader id: " + cursorLoader.getId());
        Log.d(LOG_TAG, "loader city: " + this.mInputLocationName);
        mRefreshLayout.setRefreshing(false);

        switch (cursorLoader.getId()) {
            case FORECAST_LOADER:
                //Update forecast listview
                mForecastAdapter.swapCursor(cursor);
                break;

            case PHOTO_LOADER:

                updateBackgroundView(cursor);

                break;

            default:
                Log.e(LOG_TAG, "Unexpected loader finished. Loader id: " + cursorLoader.getId());
                break;
        }

    }



    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

        Log.d(LOG_TAG, "onLoaderRest() Called. City: " + mInputLocationName);

        if (cursorLoader.getId() == FORECAST_LOADER) {
            mForecastAdapter.swapCursor(null);
            Log.d(LOG_TAG, "FORECAST_LOADER reset");
        } else if (cursorLoader.getId() == PHOTO_LOADER) {
            mBackgroundImageview.setImageDrawable(getResources().getDrawable(R.drawable.default_background));
            Log.d(LOG_TAG, "PHOTO_LOADER reset");
        }

    }



    private void updateBackgroundView(Cursor cursor) {

        Log.d(LOG_TAG, mInputLocationName + " updateBackgroundView() called");

        if (cursor != null) {

            int imagesCount = cursor.getCount();
            if (imagesCount > 0) {
                if (Utility.isUpdatedManually(getActivity())) {
                    Utility.setCurrentPhotoCursorIndex(getActivity(), mInputLocationName, new Random().nextInt(imagesCount), true);
                    Utility.setUpdatedManually(getActivity(), false);
                }
                int index = Utility.getCurrentPhotoCursorIndex(getActivity(), mInputLocationName);
                if (cursor.moveToPosition(index)) {
                    //TODO: Add Transition while changing new image
                    String imageUrl = cursor.getString(COL_PHOTO_URL);
                    ImageLoader.getInstance().loadImageWithUrl(imageUrl, mBackgroundImageview);

                    Log.d(LOG_TAG, "image Url from updateBackgroundView(): " + imageUrl);
                    Utility.setImageFillScreen(getActivity(), mBackgroundImageview);

                    mBackgroundImageview.setColorFilter(getResources().getColor(R.color.background_filter), PorterDuff.Mode.MULTIPLY);

                    String imageOwner = cursor.getString(COL_PHOTO_OWNER);
                    mImageOwnerTextview.setText(Utility.formatImageOwnerText(getActivity(), imageOwner));

                } else {
                    Log.e(LOG_TAG, "Photo cursor cannot move to position: " + index);
                }
            }
        } else {
            Log.e(LOG_TAG, "UpdateBackgroundView() failed, photo cursor is null.");
        }

    }



    //Listener method from ImageLoader
    @Override
    public void finishLoadingImage() {

//        mRefreshLayout.setRefreshing(false);

    }



    private void restartLoaders() {

        getLoaderManager().restartLoader(FORECAST_LOADER, null, this);
        getLoaderManager().restartLoader(PHOTO_LOADER, null, this);

    }
    //private void updateLoaders() {
//
//        if (getLoaderManager().getLoader(FORECAST_LOADER) == null) {
//            getLoaderManager().initLoader(FORECAST_LOADER, null, this);
//            getLoaderManager().initLoader(PHOTO_LOADER, null, this);
//        } else {
//            getLoaderManager().restartLoader(FORECAST_LOADER, null, this);
//            getLoaderManager().restartLoader(PHOTO_LOADER, null, this);
//        }
//
//    }

}