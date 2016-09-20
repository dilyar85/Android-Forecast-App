package com.dilyar.weather.app.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.util.Log;

import com.dilyar.weather.app.ForecastFragment;
import com.dilyar.weather.app.notification.MyNotification;
import com.dilyar.weather.app.R;
import com.dilyar.weather.app.data.WeatherContract;
import com.dilyar.weather.app.server.LeanCloud;
import com.dilyar.weather.app.utility.Utility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.List;
import java.util.Vector;

public class SunshineSyncAdapter extends AbstractThreadedSyncAdapter implements LeanCloud.LeanCloudListener {

    public final static String LOG_TAG = com.dilyar.weather.app.sync.SunshineSyncAdapter.class.getSimpleName();


    private static final int IS_SYNCABLE = 1;
    private static final int NOT_SYNCABLE = 0;

    public static final int SERVER_STATUS_OK = 0;
    public static final int SERVER_DOWN = 1;
    public static final int SERVER_INVALID_CITY = 2;
    public static final int SERVER_UNKNOWN = 3;
    public static final int LOCATION_NULL = 4;


    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SERVER_STATUS_OK, SERVER_DOWN, SERVER_INVALID_CITY, SERVER_UNKNOWN, LOCATION_NULL})
    public @interface LocationStatusCode {
    }



    @SuppressWarnings("ResourceType")
    public static
    @LocationStatusCode
    int getServerStatus(Context context) {

        return PreferenceManager.getDefaultSharedPreferences(context).getInt(
                context.getString(R.string.pref_location_status_key), SERVER_UNKNOWN);
    }



    public static void setServerStatus(Context context, @LocationStatusCode int locationStatus, boolean isForeground) {

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(context.getString(R.string.pref_location_status_key), locationStatus);
        if (isForeground) {
            editor.commit();
        } else {
            editor.apply();
        }
    }




    public SunshineSyncAdapter(Context context, boolean autoInitialize) {

        super(context, autoInitialize);
    }



    public static void initializeSyncAdapter(Context context) {

        getSyncAccount(context);
    }



    /**
     * Helper method to get the fake account to be used with SyncAdapter, or make a new one
     * if the fake account doesn't exist yet.  If we make a new account, we call the
     * onAccountCreated method so we can initialize things.
     *
     * @param context The context used to access the account service
     * @return a fake account.
     */
    public static Account getSyncAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        // If the password doesn't exist, the account doesn't exist
        if (accountManager.getPassword(newAccount) == null) {

        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call ContentResolver.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */
            setAutoSyncing(context);
        }
        return newAccount;
    }

//    /**
//     * Helper method to schedule the sync adapter periodic execution
//     */
//    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
//
//        Account account = getSyncAccount(context);
//        String authority = context.getString(R.string.content_authority);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            // we can enable inexact timers in our periodic sync
//            SyncRequest request = new SyncRequest.Builder().
//                    syncPeriodic(syncInterval, flexTime).
//                    setSyncAdapter(account, authority).
//                    setExtras(new Bundle()).build();
//            ContentResolver.requestSync(request);
//        } else {
//        ContentResolver.addPeriodicSync(account,
//                authority, new Bundle(), syncInterval);
//        }
//    }



    public static void setAutoSyncing(Context context) {

        int syncIntervalInHour = 0;
        String syncIntervalInHourStr = Utility.getSyncFrequencyInHour(context);
        if (syncIntervalInHourStr.length() == 1) {
            syncIntervalInHour = Integer.parseInt(syncIntervalInHourStr);
        }
        if (syncIntervalInHour == 0) {
            //Stop setting auto sync
            return;
        }

        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);

        //Frequency the sync should be performed is in seconds
        ContentResolver.setIsSyncable(account, authority, IS_SYNCABLE);
        ContentResolver.addPeriodicSync(account, authority,
                new Bundle(), syncIntervalInHour * 60 * 60);
        /*
         * Without calling setSyncAutomatically, our periodic sync will not be enabled.
         */
        ContentResolver.setSyncAutomatically(account, authority, true);
        Log.e(LOG_TAG, "setSyncAutomatically, period: " + syncIntervalInHour);
    }



    public static void stopSyncing(Context context) {

        Log.e(LOG_TAG, "stopSyncing is called. ");

        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);


        ContentResolver.setIsSyncable(account,authority,NOT_SYNCABLE);

        ContentResolver.cancelSync(getSyncAccount(context), authority);

    }



    public static void syncImmediatelyWithLocationName(Context context, String locationName) {

        Bundle bundle = new Bundle();
        bundle.putString(ForecastFragment.BUNDLE_KEY_CITY_NAME, locationName);

        syncImmediately(context, bundle);

    }



    public static void syncImmediately(Context context, Bundle bundle) {

        if (bundle == null) {
            bundle = new Bundle();
        }
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);

        ContentResolver.setIsSyncable(account,authority,IS_SYNCABLE);

        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);

    }



    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {

        Log.e(LOG_TAG, "onPerformSync Called.");
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        Log.e(LOG_TAG, "Time: " + hour + ":" + minute + " " + second);

//        String inputLocation = Utility.getDefaultSharedPreferencesMultiProcessLocation(getContext());
        String inputLocation = extras.getString(ForecastFragment.BUNDLE_KEY_CITY_NAME);

        //Check if it is synced manually
        if (inputLocation != null) {
            Log.e(LOG_TAG, "sync city: " + inputLocation);
            fetchData(inputLocation);
        } else {
            Log.e(LOG_TAG, "No input location. Start the process of syncing automatically");
            fetchDataAutomatically(getContext());

        }

        boolean isSyncingManually = Utility.getSyncFrequencyInHour(getContext()).equals(getContext().getString(R.string.pref_data_sync_manually));
        if (isSyncingManually) {
            ContentResolver.setIsSyncable(account,authority,NOT_SYNCABLE);
        }
    }



    private void fetchDataAutomatically(Context context) {

        boolean updateAllCities = Utility.isUpdatingAllCities(context);
        if (updateAllCities) {
            //Need to fetch all cities data
            List<String> cities = Utility.getAllCityNamesFromSP(context);
            if (cities != null) {
                for (String cityName : cities) {
                    fetchData(cityName);
                }
            }
        } else {
            //Fetch notification city's data
            String notificationCity = Utility.getNotificationCity(context);
            if (notificationCity != null) {
                fetchData(notificationCity);
            } else {
                Log.e(LOG_TAG, "notification city is null, fetchDataAutomatically returned doing nothing");
            }

        }

    }



    /**
     * Helper method to handle insertion of a new location in the weather database.
     *
     * @param inputLocationName Name identifying cities in database
     * @param cityName          Formal City Name from OpenWeatherMap API
     * @param lat               the latitude of the city
     * @param lon               the longitude of the city
     * @return the row ID of the added location.
     */
    long checkLocation(String inputLocationName, String cityName, double lat, double lon) {

        //Check if user wants to download images:
//        boolean downloadImages = Utility.getDownLoadImagesPref(Context);
        //Fetch Photos by sending formal city name
        fetchCityPhotos(cityName);

        long locationId = WeatherContract.LocationEntry.getInputLocationId(getContext(), inputLocationName);

        if (locationId == 0) {
            // First create a ContentValues object to hold the data you want to insert.
            ContentValues locationValues = new ContentValues();

            // Then add the data, along with the corresponding name of the data type,
            // so the content provider knows what kind of value is being inserted.
            locationValues.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME, cityName);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_INPUT_LOCATION_NAME, inputLocationName);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT, lat);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG, lon);

            // Finally, insert location data into the database.
            Uri insertedUri = getContext().getContentResolver().insert(
                    WeatherContract.LocationEntry.CONTENT_URI,
                    locationValues
            );

            // The resulting URI contains the ID for the row.  Extract the locationId from the Uri.
            locationId = ContentUris.parseId(insertedUri);
        }

        return locationId;
    }



    private void fetchData(String inputLocationName) {

        final String OWM_BASE_URL_FUTURE = "http://api.openweathermap.org/data/2.5/forecast/daily?";
        final String OWM_BASE_URL_TODAY = "http://api.openweathermap.org/data/2.5/weather?";

        final String QUERY_PARAM = "q";
        String futureWeatherUrl = Uri.parse(OWM_BASE_URL_FUTURE).buildUpon()
                .appendQueryParameter(QUERY_PARAM, inputLocationName)
                .build()
                .toString();
        String todayWeatherUrl = Uri.parse(OWM_BASE_URL_TODAY).buildUpon()
                .appendQueryParameter(QUERY_PARAM, inputLocationName)
                .build()
                .toString();
        fetchWeatherData(futureWeatherUrl, todayWeatherUrl, inputLocationName);

    }



    /**
     * HTTP request to fetch  weather information and city photos.
     */
    private void fetchWeatherData(String futureUrl, String todayUrl, String inputLocationName) {

        String futureWeatherJsonStr = fetchFutureWeather(futureUrl);
        String todayWeatherJsonStr = fetchTodayWeather(todayUrl);

        if (futureWeatherJsonStr.length() > 0 && todayWeatherJsonStr.length() > 0) {

            Vector<ContentValues> cvVector = parseWeatherJson(todayWeatherJsonStr, futureWeatherJsonStr, inputLocationName);
            if (cvVector != null) {
                saveWeatherData(cvVector,inputLocationName);
            }

        }
    }



    private void saveWeatherData(Vector<ContentValues> cVVector, String inputLocation) {


        int inserted;

        // Insert into database
        if (cVVector.size() > 0) {
            ContentValues[] cvArray = new ContentValues[cVVector.size()];
            cVVector.toArray(cvArray);

            ContentValues values = cvArray[0];

//            if (values.containsKey(WeatherContract.WeatherEntry.COLUMN_DATE)) {
            long firstDateInValue = values.getAsLong(WeatherContract.WeatherEntry.COLUMN_DATE);



            Uri weatherUriWithStartDate = WeatherContract.WeatherEntry.buildWeatherLocationWithDateInQueryParameter(inputLocation, firstDateInValue);

            inserted = getContext().getContentResolver().bulkInsert(weatherUriWithStartDate, cvArray);


            // delete old data of this syncing city to avoid building up an endless history
            long yesterdayInJulianFormat = WeatherContract.getPreviousJulianDate(System.currentTimeMillis());

            getContext().getContentResolver().delete(
                    WeatherContract.WeatherEntry.buildWeatherLocationWithDateInQueryParameter(inputLocation, yesterdayInJulianFormat),
                    null,
                    null);

            setServerStatus(getContext(), SERVER_STATUS_OK, false);

            Log.d(LOG_TAG, "FetchWeatherTask Complete. " + inserted + " Inserted");
        }

        MyNotification.notifyWeather(getContext(), inputLocation);

    }



    /**
     * HTTP request to fetch future weather information.
     */
    private String fetchFutureWeather(String futureUrl) {

        String futureJsonString = "";

        if (futureUrl.length() == 0) {
            return futureJsonString;
        }
        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.

        String format = "json";
        String units = "metric";
        int numDays = 14;

        final String FORMAT_PARAM = "mode";
        final String UNITS_PARAM = "units";
        final String DAYS_PARAM = "cnt";
        final String APPID_PARAM = "APPID";

        try {
            // Construct the URL for the OpenWeatherMap query for 14 days.
            // Possible parameters are avaiable at OWM's forecast API page, at
            // http://openweathermap.org/API#forecast

            Uri builtUri = Uri.parse(futureUrl).buildUpon()
                    .appendQueryParameter(FORMAT_PARAM, format)
                    .appendQueryParameter(UNITS_PARAM, units)
                    .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                    .appendQueryParameter(APPID_PARAM, "8ad21085b684a314bdcd9f12a3399e21")
                    .build();

            URL finalUrl = new URL(builtUri.toString());
            Log.d(LOG_TAG, "fetching future weather...  URL: " + finalUrl);

            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) finalUrl.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                setServerStatus(getContext(), SERVER_DOWN, false);
                return futureJsonString;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                setServerStatus(getContext(), SERVER_DOWN, false);
                return futureJsonString;
            }
            //Fetch Today Weather
            futureJsonString = buffer.toString();

        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the weather data, there's no point in attempting
            // to parse it.

            setServerStatus(getContext(), SERVER_DOWN, false);

        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }

        return futureJsonString;
    }



    private String fetchTodayWeather(String url) {

        String todayForecastJsonStr = "";
        // If there's no zip code, there's nothing to look up.  Verify size of params.
        if (url.length() == 0) {
            return todayForecastJsonStr;
        }

        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.

        String format = "json";
        String units = "metric";
        final String FORMAT_PARAM = "mode";
        final String UNITS_PARAM = "units";
        final String APPID_PARAM = "APPID";
        //Get today weather data from OWM
        try {
            Uri builtUri = Uri.parse(url).buildUpon()
                    .appendQueryParameter(FORMAT_PARAM, format)
                    .appendQueryParameter(UNITS_PARAM, units)
                    .appendQueryParameter(APPID_PARAM, "8ad21085b684a314bdcd9f12a3399e21")
                    .build();

            URL finalUrl = new URL(builtUri.toString());
            Log.d(LOG_TAG, "fetching today weather...  URL: " + finalUrl);
            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) finalUrl.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();
            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return todayForecastJsonStr;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return todayForecastJsonStr;
            }
            todayForecastJsonStr = buffer.toString();

        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage());
            // If the code didn't successfully get the weather data, there's no point in attempting
            // to parse it.
            setServerStatus(getContext(), SERVER_DOWN, false);

        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }

        return todayForecastJsonStr;
    }



    /**
     * Parse the json string and return an array of ContentValuesn
     */
    private Vector<ContentValues> parseWeatherJson(String todayJsonStr, String futureJsonStr, String inputLocationName) {

        //OWM API Parameters
        final String OWM_WEATHER = "weather";
        final String OWM_WEATHER_ID = "id";
        final String OWM_SHORT_DESC = "main";
        final String OWM_LONG_DESC = "description";
        final String OWM_PRESSURE = "pressure";
        final String OWM_HUMIDITY = "humidity";
        final String OWM_WIND_SPEED = "speed";
        final String OWM_WIND_DIRECTION = "deg";
        final String OWM_CITY_NAME = "name";
        final String OWM_CLOUDINESS = "clouds";
        final String OWM_COORD = "coord";
        final String OWM_MESSAGE_CODE = "cod";
        final String OWM_LATITUDE = "lat";
        final String OWM_LONGITUDE = "lon";


        //Today exclusive
        final String OWM_TEMPERATURE = "temp";
        final String OWM_TODAY_MAX = "temp_max";
        final String OWM_TODAY_MIN = "temp_min";
        final String OWM_TODAY_CLOUDINESS = "all";

        //Future exclusive
        final String OWM_LIST = "list";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";

        try {

            JSONObject todayJson = new JSONObject(todayJsonStr);
            JSONObject futureJson = new JSONObject(futureJsonStr);

            //Check error message from server
            if (todayJson.has(OWM_MESSAGE_CODE)) {
                int errorCode = todayJson.getInt(OWM_MESSAGE_CODE);
                switch (errorCode) {
                    case HttpURLConnection.HTTP_OK:
                        break;
                    case HttpURLConnection.HTTP_NOT_FOUND:
                        setServerStatus(getContext(), SERVER_INVALID_CITY, false);
                        return null;
                    default:
                        setServerStatus(getContext(), SERVER_DOWN, false);
                        return null;
                }
            }
            if (futureJson.has(OWM_MESSAGE_CODE)) {
                int errorCode = futureJson.getInt(OWM_MESSAGE_CODE);
                switch (errorCode) {
                    case HttpURLConnection.HTTP_OK:
                        break;
                    case HttpURLConnection.HTTP_NOT_FOUND:
                        setServerStatus(getContext(),SERVER_INVALID_CITY, false);
                        return null;
                    default:
                        setServerStatus(getContext(), SERVER_DOWN, false);
                        return null;
                }
            }

            String cityName = todayJson.getString(OWM_CITY_NAME);
            JSONObject cityCoord = todayJson.getJSONObject(OWM_COORD);
            double lon = cityCoord.getDouble(OWM_LONGITUDE);
            double lat = cityCoord.getDouble(OWM_LATITUDE);

//            String locationString = Utility.getDefaultSharedPreferencesMultiProcessLocation(getContext());
            //Check location name in database
            long locationId = checkLocation(inputLocationName, cityName, lat, lon);

            //Today Weather Json Data
            JSONArray todayWeatherArray = todayJson.getJSONArray(OWM_WEATHER);
            JSONObject todayWeatherJson = todayWeatherArray.getJSONObject(0);
            int todayWeatherId = todayWeatherJson.getInt(OWM_WEATHER_ID);
            String todayDescription = todayWeatherJson.getString(OWM_SHORT_DESC);
            String todayLongDescription = todayWeatherJson.getString(OWM_LONG_DESC);
            JSONObject todayMainJson = todayJson.getJSONObject(OWM_SHORT_DESC);
            double todayTemp = todayMainJson.getDouble(OWM_TEMPERATURE);
            double todayMax = todayMainJson.getDouble(OWM_TODAY_MAX);
            double todayMin = todayMainJson.getDouble(OWM_TODAY_MIN);
            int todayHumidity = todayMainJson.getInt(OWM_HUMIDITY);
            double todayPressure = todayMainJson.getDouble(OWM_PRESSURE);

//            JSONObject windJson = todayJson.getJSONObject(OWM_TODAY_WIND);
//            double todayWindSpeed = windJson.getDouble(OWM_WIND_SPEED);
//            int todayWindDirection = windJson.getInt(OWM_WIND_DIRECTION);

            JSONObject cloudJson = todayJson.getJSONObject(OWM_CLOUDINESS);
            int todayCloudiness = cloudJson.getInt(OWM_TODAY_CLOUDINESS);

            // OWM returns daily forecasts based upon the local time of the city that is being
            // asked for, which means that we need to know the GMT offset to translate this data
            // properly.
            // Since this data is also sent in-order and the first day is always the
            // current day, we're going to take advantage of that to get a nice
            // normalized UTC date for all of our weather.




            long currentTime = System.currentTimeMillis();
            long dateTime;

            // Insert the new weather information into the database
            JSONArray weatherArray = futureJson.getJSONArray(OWM_LIST);
            Vector<ContentValues> cVVector = new Vector<>(weatherArray.length());

            for (int i = 0; i < weatherArray.length(); i++) {

                ContentValues weatherValues = new ContentValues();

                dateTime = WeatherContract.getNextNumberNJulianDate(currentTime, i);

                if (i == 0) {
                    weatherValues.put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY, locationId);
                    weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DATE, dateTime);
                    weatherValues.put(WeatherContract.WeatherEntry.COLUMN_CURRENT_TEMP, todayTemp);
                    weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_CODE, todayWeatherId);
                    weatherValues.put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC, todayDescription);
                    weatherValues.put(WeatherContract.WeatherEntry.COLUMN_LONG_DESC, todayLongDescription);
                    weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, todayMax);
                    weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, todayMin);
                    weatherValues.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, todayHumidity);
//                    weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, todayWindSpeed);
                    weatherValues.put(WeatherContract.WeatherEntry.COLUMN_CLOUDINESS, todayCloudiness);
//                    weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DEGREES, todayWindDirection);
                    weatherValues.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, todayPressure);
                    cVVector.add(weatherValues);
                    continue;
                }

                //Future Weather Json Data
                double pressure;
                int humidity;
                int cloudiness;
                double windSpeed;
                double windDirection;
                double high;
                double low;
                String shortDesc;
                String longDesc;
                int weatherId;
                // Get the JSON object representing the day
                JSONObject futureDayForecast = weatherArray.getJSONObject(i);
                // Cheating to convert this to UTC time, which is what we want anyhow
                pressure = futureDayForecast.getDouble(OWM_PRESSURE);
                humidity = futureDayForecast.getInt(OWM_HUMIDITY);
                windSpeed = futureDayForecast.getDouble(OWM_WIND_SPEED);
                windDirection = futureDayForecast.getDouble(OWM_WIND_DIRECTION);
                cloudiness = futureDayForecast.getInt(OWM_CLOUDINESS);
                // Description is in a child array called "weather", which is 1 element long.
                // That element also contains a weather code.
                JSONObject weatherObject =
                        futureDayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                shortDesc = weatherObject.getString(OWM_SHORT_DESC);
                longDesc = weatherObject.getString(OWM_LONG_DESC);
                weatherId = weatherObject.getInt(OWM_WEATHER_ID);

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = futureDayForecast.getJSONObject(OWM_TEMPERATURE);
                high = temperatureObject.getDouble(OWM_MAX);
                low = temperatureObject.getDouble(OWM_MIN);

                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY, locationId);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DATE, dateTime);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_CURRENT_TEMP, 10.0);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC, shortDesc);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_LONG_DESC, longDesc);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_CODE, weatherId);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, high);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, low);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, humidity);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_CLOUDINESS, cloudiness);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DEGREES, windDirection);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, pressure);
                cVVector.add(weatherValues);

            }
            return cVVector;

        } catch (JSONException e) {
            Log.e(LOG_TAG, "parseWeatherJson() failed, caught JSONException: " + e.getMessage());
            setServerStatus(getContext(),SERVER_INVALID_CITY, false);
            return null;
        }

    }



    /**
     * Take the String representing the today forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     */
    private ContentValues parseTodayWeatherDataFromJson(String todayJsonStr) throws JSONException {

        ContentValues weatherValues = new ContentValues();
        //Enter data into todayWeather table.
        final String OWM_TODAY_WEATHER = "weather";
        final String OWM_TODAY_ID = "id";
        final String OWM_TODAY_DESC = "main";
        final String OWM_TODAY_MAIN = "main";
        final String OWM_TODAY_TEMP = "temp";
        final String OWM_MAX_TEMP = "temp_max";
        final String OWM_MIN_TEMP = "temp_min";
        final String OWM_TODAY_PRESSURE = "pressure";
        final String OWM_TODAY_HUMIDITY = "humidity";
        final String OWM_TODAY_WIND = "wind";
        final String OWM_TODAY_WINDSPEED = "speed";
        final String OWM_TODAY_WIND_DIRECTION = "deg";
        final String OWM_TODAY_UPDATE_TIME = "dt";
        final String OWM_TODAY_CITY_NAME = "name";
        final String OWM_TODAY_CLOUDS = "clouds";
        final String OWM_TODAY_CLOUDINESS = "all";

        JSONObject todayJson = new JSONObject(todayJsonStr);
        long tUpdateTime = todayJson.getLong(OWM_TODAY_UPDATE_TIME);
        String cityName = todayJson.getString(OWM_TODAY_CITY_NAME);

        JSONArray todayWeatherArray = todayJson.getJSONArray(OWM_TODAY_WEATHER);
        JSONObject todayWeatherJson = todayWeatherArray.getJSONObject(0);
        int currentWeatherId = todayWeatherJson.getInt(OWM_TODAY_ID);
        String currentDescription = todayWeatherJson.getString(OWM_TODAY_DESC);

        JSONObject todayMainJson = todayJson.getJSONObject(OWM_TODAY_MAIN);
        double currentTemp = todayMainJson.getDouble(OWM_TODAY_TEMP);
        double currentMax = todayMainJson.getDouble(OWM_MAX_TEMP);
        double currentMin = todayMainJson.getDouble(OWM_MIN_TEMP);
        int currentHumidity = todayMainJson.getInt(OWM_TODAY_HUMIDITY);
        double currentPressure = todayMainJson.getDouble(OWM_TODAY_PRESSURE);

        JSONObject windJson = todayJson.getJSONObject(OWM_TODAY_WIND);
        double currentWindSpeed = windJson.getDouble(OWM_TODAY_WINDSPEED);
        double currentWindDirection = windJson.getDouble(OWM_TODAY_WIND_DIRECTION);

        JSONObject cloudJson = todayJson.getJSONObject(OWM_TODAY_CLOUDS);
        int currentCloudiness = cloudJson.getInt(OWM_TODAY_CLOUDINESS);

        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_CURRENT_TEMP, currentTemp);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_CODE, currentWeatherId);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC, currentDescription);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, currentMax);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, currentMin);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, currentHumidity);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, currentWindSpeed);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_CLOUDINESS, currentCloudiness);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DEGREES, currentWindDirection);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, currentPressure);

        return weatherValues;

    }




    private void fetchCityPhotos(String cityName) {

        LeanCloud.getInstance().setCallbackListener(this);
        LeanCloud.getInstance().downloadCityImagesInfo(cityName);

    }



    @Override
    public void getCityImagesInfoDone(String cityName, JSONArray imagesInfo) {

        this.addImagesInfoToContentProvider(cityName, imagesInfo);
    }



    /**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data to construct the Strings needed for the wireframes
     * and save them into "photo" database.
     */
    private void addImagesInfoToContentProvider(String cityName, JSONArray imagesInfo) {

        //Delete existing photos
        getContext().getContentResolver().delete(
                WeatherContract.PhotoEntry.buildPhotoUriWithLocation(cityName),
                null,
                null);

        //Keys for Leancloud's JsonArray.
        final String TITLE = "imageTitle";
        final String URL = "imageUrl";
        final String DATE = "imageDate";
        final String OWNER = "imageOwner";
        int imagesCount = imagesInfo.length();

        Vector<ContentValues> cVVector = new Vector<>(imagesCount);

        for (int i = 0; i < imagesCount; i++) {

            try {
                JSONObject imageInfo = (JSONObject) imagesInfo.get(i);
                String imageUrl = imageInfo.getString(URL);
                String imageTitle = imageInfo.getString(TITLE);
                String imageDate = imageInfo.getString(DATE);
                String imageOwner = imageInfo.getString(OWNER);

                ContentValues imageValue = new ContentValues();
                imageValue.put(WeatherContract.PhotoEntry.COLUMN_PHOTO_COUNT, imagesCount);
                imageValue.put(WeatherContract.PhotoEntry.COLUMN_PHOTO_CITY_NAME, cityName);
                imageValue.put(WeatherContract.PhotoEntry.COLUMN_PHOTO_URL, imageUrl);
                imageValue.put(WeatherContract.PhotoEntry.COLUMN_PHOTO_TITLE, imageTitle);
                imageValue.put(WeatherContract.PhotoEntry.COLUMN_PHOTO_DATE, imageDate);
                imageValue.put(WeatherContract.PhotoEntry.COLUMN_PHOTO_OWNER, imageOwner);

                cVVector.add(imageValue);

            } catch (JSONException e) {
                Log.e(LOG_TAG, "Caught JSONException: " + e.getMessage());
            }
        }

        int inserted;
        // Add to database
        if (cVVector.size() > 0) {
            ContentValues[] cvArray = new ContentValues[cVVector.size()];
            cVVector.toArray(cvArray);
            Uri uriWithUpdateLocationName = WeatherContract.PhotoEntry.buildPhotoUriWithLocation(cityName);
            inserted = getContext().getContentResolver().bulkInsert(uriWithUpdateLocationName, cvArray);
            Log.d(LOG_TAG, "City: " + cityName + " " + inserted + " image info inserted.");

        }

    }

}









    

