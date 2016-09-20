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
package com.dilyar.weather.app.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

public class WeatherProvider extends ContentProvider {

    final static String LOG_TAG = ContentProvider.class.getSimpleName();
    // The URI Matcher used by this content provider.
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private com.dilyar.weather.app.data.WeatherDbHelper mOpenHelper;

    static final int WEATHER = 100;
    static final int WEATHER_WITH_LOCATION = 101;
    static final int WEATHER_WITH_LOCATION_AND_DATE = 102;
    static final int LOCATION = 300;
    static final int PHOTO = 400;
    static final int PHOTO_WITH_LOCATION = 401;

    private static final SQLiteQueryBuilder sWeatherByInputLocationQueryBuilder;

    static {
        sWeatherByInputLocationQueryBuilder = new SQLiteQueryBuilder();

        //This is an inner join which looks like
        //weather INNER JOIN location ON weather.location_id = location._id
        sWeatherByInputLocationQueryBuilder.setTables(
                WeatherContract.WeatherEntry.TABLE_NAME + " INNER JOIN " +
                        WeatherContract.LocationEntry.TABLE_NAME +
                        " ON " + WeatherContract.WeatherEntry.TABLE_NAME +
                        "." + WeatherContract.WeatherEntry.COLUMN_LOC_KEY +
                        " = " + WeatherContract.LocationEntry.TABLE_NAME +
                        "." + WeatherContract.LocationEntry._ID);
    }

    private static final SQLiteQueryBuilder sPhotoByInputLocationQUeryBuilder;
    static {
        sPhotoByInputLocationQUeryBuilder = new SQLiteQueryBuilder();
    }


    //location.location_setting = ?
    private static final String sLocationSettingSelection =
            WeatherContract.LocationEntry.TABLE_NAME +
                    "." + WeatherContract.LocationEntry.COLUMN_INPUT_LOCATION_NAME + " = ? ";

    //photos.locationSetting = ?
    private static final String PHOTO_WITH_INPUT_LOCATION =
            WeatherContract.PhotoEntry.TABLE_NAME + "." +
                    WeatherContract.PhotoEntry.COLUMN_PHOTO_CITY_NAME + " = ? ";



    //location.location_setting = ? AND date >= ?
    private static final String sLocationSettingWithStartDateSelection =
            WeatherContract.LocationEntry.TABLE_NAME +
                    "." + WeatherContract.LocationEntry.COLUMN_INPUT_LOCATION_NAME + " = ? AND " +
                    WeatherContract.WeatherEntry.COLUMN_DATE + " >= ? ";

    //location.location_setting = ? AND date < ?
    private static final String sLocationIdWithBeforeDateSelection =
                WeatherContract.WeatherEntry.COLUMN_LOC_KEY + " = ? AND " +
                    WeatherContract.WeatherEntry.COLUMN_DATE + " <= ? ";

    //location.location_setting = ? AND date = ?
    private static final String sLocationNameAndDaySelection =
            WeatherContract.LocationEntry.TABLE_NAME +
                    "." + WeatherContract.LocationEntry.COLUMN_INPUT_LOCATION_NAME + " = ? AND " +
                    WeatherContract.WeatherEntry.COLUMN_DATE + " = ? ";






    private Cursor getPhotoByInputLocation(Uri uri, String[] projection, String sortOrder) {

        String inputLocation = WeatherContract.PhotoEntry.getLocationSettingFromUri(uri);
        String[] selectionArgds = new String[]{inputLocation};
        String selection = WeatherContract.PhotoEntry.TABLE_NAME +
                "." + WeatherContract.PhotoEntry.COLUMN_PHOTO_CITY_NAME + " = ? ";
        return sPhotoByInputLocationQUeryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgds,
                null,
                null,
                sortOrder);

    }







    static UriMatcher buildUriMatcher() {

        // All paths added to the UriMatcher have a corresponding code to return when a match is
        // found.  The code passed into the constructor represents the code to return for the root
        // URI.  It's common to use NO_MATCH as the code for this case.
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = WeatherContract.CONTENT_AUTHORITY;

        // For each type of URI added, create a corresponding code.
        matcher.addURI(authority, WeatherContract.PATH_WEATHER, WEATHER);
        matcher.addURI(authority, WeatherContract.PATH_WEATHER + "/*", WEATHER_WITH_LOCATION);
        matcher.addURI(authority, WeatherContract.PATH_WEATHER + "/*/#", WEATHER_WITH_LOCATION_AND_DATE);

        matcher.addURI(authority, WeatherContract.PATH_LOCATION, LOCATION);

        matcher.addURI(authority, WeatherContract.PATH_PHOTO, PHOTO);
        matcher.addURI(authority, WeatherContract.PATH_PHOTO + "/*", PHOTO_WITH_LOCATION);

        return matcher;
    }



    /*
        Students: We've coded this for you.  We just create a new WeatherDbHelper for later use
        here.
     */
    @Override
    public boolean onCreate() {

        mOpenHelper = new WeatherDbHelper(getContext());
        return true;
    }




    @Override
    public String getType(Uri uri) {

        // Use the Uri Matcher to determine what kind of URI this is.
        final int match = sUriMatcher.match(uri);

        switch (match) {

            case WEATHER_WITH_LOCATION_AND_DATE:
                return WeatherContract.WeatherEntry.CONTENT_ITEM_TYPE;
            case WEATHER_WITH_LOCATION:
                return WeatherContract.WeatherEntry.CONTENT_TYPE;
            case WEATHER:
                return WeatherContract.WeatherEntry.CONTENT_TYPE;
            case LOCATION:
                return WeatherContract.LocationEntry.CONTENT_ITEM_TYPE;
            case PHOTO:
                return WeatherContract.PhotoEntry.CONTENT_TYPE;
            case PHOTO_WITH_LOCATION:
                return WeatherContract.PhotoEntry.CONTENT_TYPE;

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    private Cursor getWeatherByLocationSetting(Uri uri, String[] projection, String sortOrder) {

        String inputLocation = WeatherContract.WeatherEntry.getInputLocationFromUri(uri);
        long startDate = WeatherContract.WeatherEntry.getDateParamFromUri(uri);

        String[] selectionArgs;
        String selection;

        if (startDate == 0) {
            selection = sLocationSettingSelection;
            selectionArgs = new String[]{inputLocation};
        } else {
            selection = sLocationSettingWithStartDateSelection;
            selectionArgs = new String[]{inputLocation, Long.toString(startDate)};

        }

        return sWeatherByInputLocationQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }



    private Cursor getWeatherByLocationSettingAndDate(Uri uri, String[] projection, String sortOrder) {

        String locationSetting = WeatherContract.WeatherEntry.getInputLocationFromUri(uri);
        long date = WeatherContract.WeatherEntry.getDateFromUri(uri);

        return sWeatherByInputLocationQueryBuilder.query(
                mOpenHelper.getReadableDatabase(),
                projection,
                sLocationNameAndDaySelection,
                new String[]{locationSetting, Long.toString(date)},
                null,
                null,
                sortOrder
        );
    }


    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {

        // Here's the switch statement that, given a URI, will determine what kind of request it is,
        // and query the database accordingly.
        Cursor retCursor;

        switch (sUriMatcher.match(uri)) {

            // "weather/*/#"
            case WEATHER_WITH_LOCATION_AND_DATE: {
                retCursor = getWeatherByLocationSettingAndDate(uri, projection, sortOrder);
                break;
            }
            // "weather/*"
            case WEATHER_WITH_LOCATION: {
                retCursor = getWeatherByLocationSetting(uri, projection, sortOrder);

                break;
            }
            // "weather"
            case WEATHER: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        WeatherContract.WeatherEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );

                break;
            }
            // "location"
            case LOCATION: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        WeatherContract.LocationEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            // "photo"
            case PHOTO: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        WeatherContract.PhotoEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            //"photo" with location.
            case PHOTO_WITH_LOCATION: {
                String locationSetting = WeatherContract.PhotoEntry.getLocationSettingFromUri(uri);

                retCursor = mOpenHelper.getReadableDatabase().query(
                        WeatherContract.PhotoEntry.TABLE_NAME,
                        projection,
                        PHOTO_WITH_INPUT_LOCATION,
                        new String[]{locationSetting},
                        null,
                        null,
                        sortOrder
                );
                break;
            }
//            //"photo" with location and index.
//            case PHOTO_WITH_INDEX: {
//                String locationSetting = WeatherContract.PhotoEntry.getInputLocationFromUri(uri);
//                String photoIndex = WeatherContract.PhotoEntry.getPhotoIndexFromUri(uri);
//
//                retCursor = mOpenHelper.getReadableDatabase().query(
//                        WeatherContract.PhotoEntry.TABLE_NAME,
//                        projection,
//                        PHOTO_WITH_LOCATION_AND_INDEX_SELECTION,
//                        new String[]{locationSetting, photoIndex},
//                        null,
//                        null,
//                        sortOrder
//                );
//                break;
//            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        Log.d(LOG_TAG, "setNotificationUri: " + uri);
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }



    @Override
    public Uri insert(Uri uri, ContentValues values) {


        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri;

        switch (match) {
            case WEATHER: {
//                getJulianDate(values);
                long _id = db.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, values);
                if (_id > 0)
                    returnUri = WeatherContract.WeatherEntry.buildWeatherUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }

//
            case LOCATION: {
                long _id = db.insert(WeatherContract.LocationEntry.TABLE_NAME, null, values);
                if (_id > 0)
                    returnUri = WeatherContract.LocationEntry.buildLocationUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case PHOTO: {
                long _id = db.insert(WeatherContract.PhotoEntry.TABLE_NAME, null, values);
                if (_id > 0) {
                    returnUri = WeatherContract.PhotoEntry.buildPhotoUri(_id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            }
            case PHOTO_WITH_LOCATION: {
                long _id = db.insert(WeatherContract.PhotoEntry.TABLE_NAME, null, values);
                if (_id > 0) {
                    returnUri = WeatherContract.PhotoEntry.buildPhotoUri(_id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;

            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        Log.d(LOG_TAG, "notifyChange from insert. Uri: " + uri);
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }



    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {

        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int returnCount = 0;
        switch (match) {

            case WEATHER_WITH_LOCATION:
                db.beginTransaction();
                try {
                    for (ContentValues value : values) {
//                        getJulianDate(value);
                        long _id = db.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, value);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                break;


            case PHOTO_WITH_LOCATION:

                db.beginTransaction();
                try {
                    for (ContentValues value : values) {
                        long _id = db.insert(WeatherContract.PhotoEntry.TABLE_NAME, null, value);
                        if (_id != -1) {
                            returnCount++;

                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                break;

            default:
                returnCount = super.bulkInsert(uri, values);

        }
        Log.d(LOG_TAG, "notifyChange() from bulk insert. uri: " + uri);

        getContext().getContentResolver().notifyChange(uri, null);
        return returnCount;
    }



    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {


        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsDeleted;
        // this makes delete all rows return the number of rows deleted
        if (null == selection) selection = "1";
        switch (match) {
            case WEATHER:
                rowsDeleted = db.delete(
                        WeatherContract.WeatherEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case LOCATION:
                rowsDeleted = db.delete(
                        WeatherContract.LocationEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case PHOTO:
                rowsDeleted = db.delete(
                        WeatherContract.PhotoEntry.TABLE_NAME, selection, selectionArgs);
                break;

            case WEATHER_WITH_LOCATION:
                String deletedCity = WeatherContract.WeatherEntry.getInputLocationFromUri(uri);

                long locationId = WeatherContract.LocationEntry.getInputLocationId(getContext(),deletedCity);
                long deletedEndDay = WeatherContract.WeatherEntry.getDateParamFromUri(uri);


                rowsDeleted = db.delete(WeatherContract.WeatherEntry.TABLE_NAME,
                        sLocationIdWithBeforeDateSelection,
                        new String[]{Long.toString(locationId), Long.toString(deletedEndDay)});
                break;

            case PHOTO_WITH_LOCATION:
                String cityName = WeatherContract.PhotoEntry.getLocationSettingFromUri(uri);
                rowsDeleted = db.delete(WeatherContract.PhotoEntry.TABLE_NAME,
                        WeatherContract.PhotoEntry.COLUMN_PHOTO_CITY_NAME + " = ?",
                        new String[]{cityName});
                break;

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        // Because a null deletes all rows
        if (rowsDeleted != 0) {
            Log.d(LOG_TAG, "Deleted item count: " + rowsDeleted + " NotifyChange to uri: " + uri);
            getContext().getContentResolver().notifyChange(uri, null);
        } else {
            Log.d(LOG_TAG, "rowsDeleted == 0, will not notifyChange to uri");
        }
        return rowsDeleted;
    }




    @Override
    public int update(
            Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsUpdated;

        switch (match) {
            case WEATHER:
//                getJulianDate(values);
                rowsUpdated = db.update(WeatherContract.WeatherEntry.TABLE_NAME, values, selection,
                        selectionArgs);
                break;
            case LOCATION:
                rowsUpdated = db.update(WeatherContract.LocationEntry.TABLE_NAME, values, selection,
                        selectionArgs);
                break;
            case PHOTO:
                rowsUpdated = db.update(WeatherContract.PhotoEntry.TABLE_NAME, values, selection, selectionArgs);
                break;

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

}