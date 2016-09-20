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
package com.dilyar.weather.app.utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.view.Display;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

import com.dilyar.weather.app.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class Utility {

    public static final String LOG_TAG = Utility.class.getSimpleName();



    public static void saveResult(SharedPreferences.Editor editor, boolean isForeground) {

        if (isForeground) {
            editor.commit();
        } else {
            editor.apply();

        }
    }



    public static List<String> getAllCityNamesFromSP(Context context) {

        int cityNamesSize = Utility.getCityNamesSize(context);
        if (cityNamesSize <= 0) {
            return null;
        } else {
            List<String> cityNames = new ArrayList<>();
            for (int i = 0; i < cityNamesSize; i++) {
                cityNames.add(Utility.getCityNameByIndex(context, i));
            }
            return cityNames;
        }

    }



    public static int getIndexOfLocation(Context context, String inputLocationFromNotification) {

        List<String> locations = getAllCityNamesFromSP(context);
        if (locations != null) {
            for (int i = 0; i < locations.size(); i++) {
                if (locations.get(i).equals(inputLocationFromNotification)) {
                    return i;
                }
            }
        }
        return 0;

    }



    public static void saveCityNameToSP(Context context, String cityNme, boolean isForeground) {

        int cityNamesSize = Utility.getCityNamesSize(context);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(context.getString(R.string.pref_city_name_index) + cityNamesSize, cityNme);
        editor.putInt(context.getString(R.string.pref_city_names_size), cityNamesSize + 1);

        saveResult(editor, isForeground);
    }



    //Delete the city name in SP and return the name.
    //Return null if there is no city to delete.
    public static String deleteCityNameFromSP(Context context, int cityNameIndex, boolean isForeground) {

        int cityNamesSize = Utility.getCityNamesSize(context);
        if (cityNamesSize <= 0) {
            return null;
        } else {

            String deletedCity = Utility.getCityNameByIndex(context, cityNameIndex);
            //First delete given index's city name. Then reset the index after the given index
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(context.getString(R.string.pref_city_name_index) + cityNameIndex, null);

            for (int i = cityNameIndex + 1; i <= cityNamesSize; i++) {
                String currentCityName = Utility.getCityNameByIndex(context, i);
                int newIndex = i - 1;
                editor.putString(context.getString(R.string.pref_city_name_index) + newIndex, currentCityName);
            }
            editor.putInt(context.getString(R.string.pref_city_names_size), cityNamesSize - 1);

            saveResult(editor, isForeground);
            return deletedCity;
        }

    }



    public static int getCityNamesSize(Context context) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt(context.getString(R.string.pref_city_names_size), 0);

    }



    public static String getCityNameByIndex(Context context, int index) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_city_name_index) + index, null);

    }



    public static boolean isCelsius(Context context) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_weather_units_key), context.getString(R.string.pref_weather_units_celsius))
                .equals(context.getString(R.string.pref_weather_units_celsius));
    }



    public static long getLastNotificationTime(Context context) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String lastNotificationKey = context.getString(R.string.pref_last_notification);
        return prefs.getLong(lastNotificationKey, 0);
    }



    public static void setLastNotificationTime(Context context, long timeInMills) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String lastNotificationKey = context.getString(R.string.pref_last_notification);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(lastNotificationKey, timeInMills);
        saveResult(editor, false);

    }



    public static boolean displayNotifications(Context context) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String displayNotificationsKey = context.getString(R.string.pref_enable_notifications_key);

        return prefs.getBoolean(displayNotificationsKey,
                Boolean.parseBoolean(context.getString(R.string.pref_enable_notifications_default)));

    }



    public static String getNotificationCity(Context context) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_notification_city_key), null);
    }



    public static void setNotificationCity(Context context, String cityName, boolean isForeground) {

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences((context));
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(context.getString(R.string.pref_notification_city_key), cityName);
        saveResult(editor, isForeground);
    }



    public static String getNotificationType(Context context) {

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString(context.getString(R.string.pref_notification_type_key),
                context.getString(R.string.pref_notification_type_default));

    }



    public static String formatTemperature(Context context, double temperature, boolean isMetric) {

        double temp;
        if (!isMetric) {
            temp = 9 * temperature / 5 + 32;
        } else {
            temp = temperature;
        }
        return context.getString(R.string.format_temperature, temp);
    }



    public static String formatImageOwnerText(Context context, String ownerName) {

        String text = context.getString(R.string.format_image_owner, ownerName);
        int defaultLength = 34;
        if (text.length() > defaultLength) {

            int index = text.lastIndexOf("from") > 0 ? text.lastIndexOf("from") : defaultLength;

            text = text.substring(0, index);
        }

        return text;
    }



    // Format used for storing dates in the database.  ALso used for converting those strings
    // back into date objects for comparison/processing.
    public static String getFriendlyDayString(Context context, long dateInMillis) {
        // The day string for forecast uses the following logic:
        // For today: "Today, June 8"
        // For tomorrow:  "Tomorrow"
        // For the next 5 days: "Wednesday" (just the day name)
        // For all days after that: "Mon Jun 8"

        Time time = new Time();
        time.setToNow();
        long currentTime = System.currentTimeMillis();
        int julianDay = Time.getJulianDay(dateInMillis, time.gmtoff);
        int currentJulianDay = Time.getJulianDay(currentTime, time.gmtoff);

        // If the date we're building the String for is today's date, the format
        // is "Today, June 24"
        if (julianDay == currentJulianDay) {
            String today = context.getString(R.string.today);
            int formatId = R.string.format_full_friendly_date;
            return String.format(context.getString(
                    formatId,
                    today,
                    getFormattedMonthDay(context, dateInMillis)));
        } else if (julianDay < currentJulianDay + 7) {
            // If the input date is less than a week in the future, just return the day name.
            return getDayName(context, dateInMillis);
        } else {
            // Otherwise, use the form "Mon Jun 3"
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(dateInMillis);
        }
    }



    /**
     * Given a day, returns just the name to use for that day.
     * E.g "today", "tomorrow", "wednesday".
     *
     * @param context      Context to use for resource localization
     * @param dateInMillis The date in milliseconds
     * @return
     */
    public static String getDayName(Context context, long dateInMillis) {
        // If the date is today, return the localized version of "Today" instead of the actual
        // day name.

        Time t = new Time();
        t.setToNow();
        int julianDay = Time.getJulianDay(dateInMillis, t.gmtoff);
        int currentJulianDay = Time.getJulianDay(System.currentTimeMillis(), t.gmtoff);
        if (julianDay == currentJulianDay) {
            return context.getString(R.string.today);
        } else if (julianDay == currentJulianDay + 1) {
            return context.getString(R.string.tomorrow);
        } else {
            Time time = new Time();
            time.setToNow();
            // Otherwise, the format is just the day of the week (e.g "Wednesday".
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE");
            return dayFormat.format(dateInMillis);
        }
    }



    /**
     * Converts db date format to the format "Month day", e.g "June 24".
     *
     * @param context      Context to use for resource localization
     * @param dateInMillis The db formatted date string, expected to be of the form specified
     *                     in Utility.DATE_FORMAT
     * @return The day in the form of a string formatted "December 6"
     */
    public static String getFormattedMonthDay(Context context, long dateInMillis) {

        Time time = new Time();
        time.setToNow();
        SimpleDateFormat dbDateFormat = new SimpleDateFormat(Utility.DATE_FORMAT);
        SimpleDateFormat monthDayFormat = new SimpleDateFormat("MMMM dd");
        String monthDayString = monthDayFormat.format(dateInMillis);
        return monthDayString;
    }



    public static final String DATE_FORMAT = "yyyyMMdd";

//    public static String getFormattedWind(Context context, float windSpeed, float degrees) {
//
//        int windFormat;
//        if (Utility.isCelsius(context)) {
//            windFormat = R.string.format_wind_kmh;
//        } else {
//            windFormat = R.string.format_wind_mph;
//            windSpeed = .621371192237334f * windSpeed;
//        }
//
//        // From wind direction in degrees, determine compass direction as a string (e.g NW)
//        // You know what's fun, writing really long if/else statements with tons of possible
//        // conditions.  Seriously, try it!
//        String direction = "Unknown";
//        if (degrees >= 337.5 || degrees < 22.5) {
//            direction = "N";
//        } else if (degrees >= 22.5 && degrees < 67.5) {
//            direction = "NE";
//        } else if (degrees >= 67.5 && degrees < 112.5) {
//            direction = "E";
//        } else if (degrees >= 112.5 && degrees < 157.5) {
//            direction = "SE";
//        } else if (degrees >= 157.5 && degrees < 202.5) {
//            direction = "S";
//        } else if (degrees >= 202.5 && degrees < 247.5) {
//            direction = "SW";
//        } else if (degrees >= 247.5 && degrees < 292.5) {
//            direction = "W";
//        } else if (degrees >= 292.5 && degrees < 337.5) {
//            direction = "NW";
//        }
//        return String.format(context.getString(windFormat), windSpeed, direction);
//    }

//    public static String getFormattedCloudiness(Context context, float cloudiness) {
//
//        return String.format(context.getString(R.string.format_cloudiness), cloudiness);
//
//    }
//
//
//
//    public static String getFormattedHumidity(Context context, float humidity) {
//
//        return String.format(context.getString(R.string.format_humidity), humidity);
//    }



    /**
     * Helper method to provide the icon resource id according to the weather condition id returned
     * by the OpenWeatherMap call.
     *
     * @param weatherId from OpenWeatherMap API response
     * @return resource id for the corresponding icon. .
     */

    public static int getIconResourceForWeatherCondition(int weatherId) {

        int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        //Day: 5:00AM ~ 7:00PM
        //Night: 7:00PM ~ 5:00AM
        if (currentHour >= 5 && currentHour < 19) {
            return getIconResourceOfDay(weatherId);
        } else {
            return getIconResourceOfNight(weatherId);
        }

    }



    private static int getIconResourceOfDay(int weatherId) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        if (weatherId >= 200 && weatherId <= 232) {
            return R.drawable.thunderstorm_day;
        } else if ((weatherId >= 300 && weatherId <= 321) || weatherId == 500 || (weatherId >= 520 && weatherId <= 522)) {
            return R.drawable.light_rain_day;
        } else if (weatherId == 501) {
            return R.drawable.medium_rain_together;
        } else if ((weatherId >= 502 && weatherId <= 511) || (weatherId == 531)) {
            return R.drawable.heavy_rain_day;
        } else if (weatherId == 600 || weatherId == 620) {
            return R.drawable.light_snow_day;
        } else if (weatherId == 601 || weatherId == 621) {
            return R.drawable.medium_snow_day;
        } else if (weatherId == 602 || weatherId == 622) {
            return R.drawable.heavy_snow_day;
        } else if (weatherId >= 611 && weatherId <= 616) {
            return R.drawable.sleet_together;
        } else if (weatherId >= 701 && weatherId <= 721) {
            return R.drawable.mist_day;
        } else if (weatherId >= 741 && weatherId <= 761) {
            return R.drawable.fog_day;
        } else if (weatherId == 800) {
            return R.drawable.sunny_day;
        } else if (weatherId == 801) {
            return R.drawable.light_cloud_day;
        } else if (weatherId == 802) {
            return R.drawable.medium_cloud_day;
        } else if (weatherId == 803) {
            return R.drawable.heavy_cloud_day;
        } else if (weatherId == 804) {
            return R.drawable.overcast_cloud_day;
        } else if (weatherId == 906) {
            return R.drawable.hail_together;
        }
        return R.drawable.unknown_together;

    }



    private static int getIconResourceOfNight(int weatherId) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        if (weatherId >= 200 && weatherId <= 232) {
            return R.drawable.thunderstorm_night;
        } else if ((weatherId >= 300 && weatherId <= 321) || weatherId == 500 || (weatherId >= 520 && weatherId <= 522)) {
            return R.drawable.light_rain_night;
        } else if (weatherId == 501) {
            return R.drawable.medium_rain_together;
        } else if ((weatherId >= 502 && weatherId <= 511) || (weatherId == 531)) {
            return R.drawable.heavy_rain_night;
        } else if (weatherId == 600 || weatherId == 620) {
            return R.drawable.light_snow_night;
        } else if (weatherId == 601 || weatherId == 621) {
            return R.drawable.medium_snow_night;
        } else if (weatherId == 602 || weatherId == 622) {
            return R.drawable.heavy_snow_night;
        } else if (weatherId >= 611 && weatherId <= 616) {
            return R.drawable.sleet_together;
        } else if (weatherId >= 701 && weatherId <= 721) {
            return R.drawable.mist_night;
        } else if (weatherId >= 741 && weatherId <= 761) {
            return R.drawable.fog_night;
        } else if (weatherId == 800) {
            return R.drawable.sunny_night;
        } else if (weatherId == 801) {
            return R.drawable.light_cloud_night;
        } else if (weatherId == 802) {
            return R.drawable.medium_cloud_night;
        } else if (weatherId == 803) {
            return R.drawable.heavy_cloud_night;
        } else if (weatherId == 804) {
            return R.drawable.overcast_cloud_night;
        } else if (weatherId == 906) {
            return R.drawable.hail_together;
        }
        return R.drawable.unknown_together;

    }



    public static boolean isNetworkAvailable(Context context) {

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }



    public static void setImageFillScreen(Context context, ImageView imageView) {

        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        ViewGroup.LayoutParams params = imageView.getLayoutParams();
        params.width = width;
        params.height = height;
        imageView.setLayoutParams(params);
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);

//        imageView.setAlpha(0.5f);

    }



    public static String getSyncFrequencyInHour(Context context) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(context.getString(R.string.pref_data_sync_frequency_key),
                context.getString(R.string.pref_data_sync_default_value));
    }



    public static boolean isUpdatingAllCities(Context context) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(context.getString(R.string.pref_enable_sync_all_key),
                Boolean.parseBoolean(context.getString(R.string.pref_enable_sync_all_cities_default)));
    }



    public static String toTitleCase(String givenString) {

        if (givenString != null) {
            String[] arr = givenString.split(" ");
            StringBuffer sb = new StringBuffer();

            for (int i = 0; i < arr.length; i++) {
                sb.append(Character.toUpperCase(arr[i].charAt(0)))
                        .append(arr[i].substring(1)).append(" ");
            }
            return sb.toString().trim();
        }
        return null;
    }



    public static int getCurrentPhotoCursorIndex(Context context, String cityName) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getInt(context.getString(R.string.pref_current_photo_cursor_index_key, cityName),
                Integer.parseInt(context.getString(R.string.pref_current_photo_cursor_index_default)));
    }



    public static void setCurrentPhotoCursorIndex(Context context, String cityName, int index, boolean isForeground) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(context.getString(R.string.pref_current_photo_cursor_index_key, cityName), index);
        saveResult(editor, isForeground);
    }



    public static boolean isUpdatedManually(Context context) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(context.getString(R.string.pref_is_updated_manually_key), true);
    }



    public static void setUpdatedManually(Context context, boolean updateManually) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(context.getString(R.string.pref_is_updated_manually_key), updateManually);
        editor.commit();

    }
}
