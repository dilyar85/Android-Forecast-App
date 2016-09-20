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

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.format.Time;

/**
 * Defines table and column names for the weather database.
 */
public class WeatherContract {

    // The "Content authority" is a name for the entire content provider, similar to the
    // relationship between a domain name and its website.  A convenient string to use for the
    // content authority is the package name for the app, which is guaranteed to be unique on the
    // device.
    public static final String CONTENT_AUTHORITY = "com.dilyar.weather.app";

    // Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
    // the content provider.
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    // Possible paths (appended to base content URI for possible URI's)
    // For instance, content://com.android.dilyar.weather.app/weather/ is a valid path for
    // looking at weather data. content://com.android.dilyar.weather.app.app/givemeroot/ will fail,
    // as the ContentProvider hasn't been given any information on what to do with "givemeroot".
    // At least, let's hope not.  Don't be that dev, reader.  Don't be that dev.
    public static final String PATH_WEATHER = "weather";
    public static final String PATH_LOCATION = "location";
    public static final String PATH_PHOTO = "photo";

    // To make it easy to query for the exact date, we normalize all dates that go into
    // the database to the start of the the Julian day at UTC.
    public static long getJulianDate(long date) {
        // normalize the start date to the beginning of the (UTC) day
        Time time = new Time();
        time.set(date);
        int julianDay = Time.getJulianDay(date, time.gmtoff);
        return time.setJulianDay(julianDay);
    }



    public static long getPreviousJulianDate(long date) {
        // normalize the start date to the beginning of the (UTC) day
        Time time = new Time();
        time.set(date);
        int julianDay = Time.getJulianDay(date, time.gmtoff);
        return time.setJulianDay(julianDay - 1);

    }



    public static long getNextNumberNJulianDate(long date, int n) {
        Time time = new Time();
        time.set(date);
        int julianDay = Time.getJulianDay(date, time.gmtoff);
        return time.setJulianDay(julianDay + n);
    }



    // Inner class that defines the table contents of the photo table //
    public static final class PhotoEntry implements BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_PHOTO).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PHOTO;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PHOTO;

        public static final String TABLE_NAME = "photo";
        public static final String COLUMN_PHOTO_CITY_NAME = "photo_city_name";
        public static final String COLUMN_PHOTO_COUNT = "total";
        public static final String COLUMN_PHOTO_TITLE = "title";
        public static final String COLUMN_PHOTO_URL = "url";
        public static final String COLUMN_PHOTO_OWNER = "photoOwner";
        public static final String COLUMN_PHOTO_DATE = "photoDate";



        public static Uri buildPhotoUri(long id) {

            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri buildPhotoUriWithLocation(String locationSetting) {

            return CONTENT_URI.buildUpon().appendPath(locationSetting).build();
        }


        public static String getLocationSettingFromUri(Uri uri) {
            return uri.getPathSegments().get(1);
        }



    }


    /* Inner class that defines the table contents of the location table */
    public static final class LocationEntry implements BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_LOCATION).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_LOCATION;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_LOCATION;

        // Table name
        public static final String TABLE_NAME = "location";

        // The location setting string is what will be sent to openweathermap
        // as the location query.
        public static final String COLUMN_INPUT_LOCATION_NAME = "input_location";

        // Human readable location string, provided by the API.  Because for styling,
        // "Mountain View" is more recognizable than 94043.
        public static final String COLUMN_CITY_NAME = "city_name";

        // In order to uniquely pinpoint the location on the map when we launch the
        // map intent, we store the latitude and longitude as returned by openweathermap.
        public static final String COLUMN_COORD_LAT = "coord_lat";
        public static final String COLUMN_COORD_LONG = "coord_long";

        public static Uri buildLocationUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }



        public static long getInputLocationId(Context context, String inputLocationName) {

            // First, check if the location with this city name exists in the db
            Cursor locationCursor = context.getContentResolver().query(
                    WeatherContract.LocationEntry.CONTENT_URI,
                    new String[]{WeatherContract.LocationEntry._ID},
                    WeatherContract.LocationEntry.COLUMN_INPUT_LOCATION_NAME + " = ?",
                    new String[]{inputLocationName},
                    null);

            if (locationCursor.moveToFirst()) {
                int locationIdIndex = locationCursor.getColumnIndex(WeatherContract.LocationEntry._ID);
                long locationId = locationCursor.getLong(locationIdIndex);
                locationCursor.close();
                return locationId;
            } else {
                locationCursor.close();
                return 0;
            }
        }
    }

    /* Inner class that defines the table contents of the weather table */
    public static final class WeatherEntry implements BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_WEATHER).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_WEATHER;


        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_WEATHER;

        public static final String TABLE_NAME = "weather";

        // Column with the foreign key into the location table.
        public static final String COLUMN_LOC_KEY = "location_id";
        // Date, stored as long in milliseconds since the epoch
        public static final String COLUMN_DATE = "date";
        // Weather id as returned by API, to identify the icon to be used
        public static final String COLUMN_WEATHER_CODE = "weather_code";

        // Short description and long description of the weather, as provided by API.
        // e.g "clear" vs "sky is clear".
        public static final String COLUMN_SHORT_DESC = "short_desc";

        // Min, and Max temperatures for the day (stored as floats)
        public static final String COLUMN_MIN_TEMP = "min";
        public static final String COLUMN_MAX_TEMP = "max";

        // Humidity is stored as a float representing percentage
        public static final String COLUMN_HUMIDITY = "humidity";

        // Humidity is stored as a float representing percentage
        public static final String COLUMN_PRESSURE = "pressure";

        // Windspeed is stored as a float representing windspeed  mph
        public static final String COLUMN_WIND_SPEED = "wind";

        // Degrees are meteorological degrees (e.g, 0 is north, 180 is south).  Stored as floats.
        public static final String COLUMN_DEGREES = "degrees";

        //Current today weather information.
        public static final String COLUMN_CURRENT_TEMP = "current_temp";

        //Cloudiness is stored as a float representing percentage
        public static final String COLUMN_CLOUDINESS = "cloudiness";

        //Long Description is stored as a string
        public static final String COLUMN_LONG_DESC = "long_desc";


        public static Uri buildWeatherUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }



        public static Uri buildWeatherLocationWithDateInQueryParameter(String locationName, long startDate) {
            long normalizedDate = getJulianDate(startDate);
            return CONTENT_URI.buildUpon().appendPath(locationName)
                    .appendQueryParameter(COLUMN_DATE, Long.toString(normalizedDate)).build();
        }

        public static Uri buildWeatherLocationWithDate(String locationSetting, long date) {
            return CONTENT_URI.buildUpon()
                    .appendPath(locationSetting)
                    .appendPath(Long.toString(getJulianDate(date))).build();
        }

        public static String getInputLocationFromUri(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        public static long getDateFromUri(Uri uri) {
            return Long.parseLong(uri.getPathSegments().get(2));
        }

        public static long getDateParamFromUri(Uri uri) {
            String dateString = uri.getQueryParameter(COLUMN_DATE);
            if (null != dateString && dateString.length() > 0)
                return Long.parseLong(dateString);
            else
                return 0;
        }
    }
}
