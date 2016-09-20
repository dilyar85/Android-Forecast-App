package com.dilyar.weather.app.notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import com.dilyar.weather.app.MainActivity;
import com.dilyar.weather.app.R;
import com.dilyar.weather.app.data.WeatherContract;
import com.dilyar.weather.app.utility.Utility;

import java.util.Calendar;

/**
 * Created by Dilyar on 9/6/16.
 */
public class MyNotification {

    private static final int WEATHER_NOTIFICATION_ID = 3004;

    public static final String INTENT_EXTRA_INPUT_LOCATION_KEY = "notification_city";



    private static final String[] NOTIFY_WEATHER_PROJECTION = new String[]{
            WeatherContract.WeatherEntry.COLUMN_WEATHER_CODE,
            WeatherContract.WeatherEntry.COLUMN_LONG_DESC,
            WeatherContract.LocationEntry.COLUMN_CITY_NAME,
            WeatherContract.WeatherEntry.COLUMN_CURRENT_TEMP,
            WeatherContract.LocationEntry.COLUMN_INPUT_LOCATION_NAME
    };
    // these indices must match the projection
    private static final int COL_WEATHER_CODE = 0;
    private static final int COL_LONG_DESC = 1;
    private static final int COL_CITY_NAME = 2;
    private static final int COL_CURRENT_TEMP = 3;
    private static final int COL_INPUT_LOCATION = 4;


    public static void notifyWeather(Context context, String updatedDataCity) {

        boolean needToPushNotification = checkNotificationStatus(context, updatedDataCity);
        if (needToPushNotification) {
            pushNotification(context, Utility.getNotificationCity(context));
        }

    }

    public static void cancelNotification(Context context){
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancelAll();
    }

    //Return true if need to display notification and notification is available, otherwise return false.
    public static boolean checkNotificationStatus(Context context, String updatedDataCity) {

        boolean displayNotifications = Utility.displayNotifications(context);
        if (displayNotifications) {
            String notificationCity = Utility.getNotificationCity(context);

            if (notificationCity != null) {
                //Will not push notification when other city's data is updated
                if (!updatedDataCity.equals(notificationCity)) {
                    return false;
                }
                String notificationType = Utility.getNotificationType(context);
                if (notificationType.equals(context.getString(R.string.pref_notification_type_always))) {
                    return true;
                } else if (notificationType.equals(context.getString(R.string.pref_notification_type_daily))) {
                    int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                    //Push notification in the morning every day if it is daily
                    if (currentHour >= 4 && currentHour <= 8) {
                        return true;
                    }
                    long lastSync = Utility.getLastNotificationTime(context);
                    long timePastInDay = (System.currentTimeMillis() - lastSync) / 86400000;
                    return timePastInDay >= 1;
                }
                //MyNotification type is "smart", need to handle it well.
                else {
                    return weatherIsChangedRapidly(context, notificationCity);
                }
            }
        }
        return false;
    }



    public static void pushNotification(Context context, String notificationCity) {


        Uri weatherUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(notificationCity, System.currentTimeMillis());

        Cursor cursor = context.getContentResolver().query(weatherUri, NOTIFY_WEATHER_PROJECTION, null, null, null);

        if (cursor.moveToFirst()) {
            int weatherId = cursor.getInt(COL_WEATHER_CODE);
            double currentTemp = cursor.getDouble(COL_CURRENT_TEMP);
            String longDesc = cursor.getString(COL_LONG_DESC);
            String cityName = cursor.getString(COL_CITY_NAME);

            int iconId = Utility.getIconResourceForWeatherCondition(weatherId);
            Bitmap weatherConditionImage = BitmapFactory.decodeResource(context.getResources(), iconId);
            //TODO: Change icon to app icon


            boolean isMetric = Utility.isCelsius(context);
            String title = Utility.formatTemperature(context, currentTemp, isMetric) + " in " +
                    cityName;


            // Define the text of the forecast.
            String contentText = Utility.toTitleCase(longDesc);

            // build notification here.
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                    .setLargeIcon(weatherConditionImage)
                    .setSmallIcon(iconId)
                    .setContentTitle(title)
                    .setContentText(contentText);



            Intent resultIntent = new Intent(context, MainActivity.class);
            resultIntent.putExtra(INTENT_EXTRA_INPUT_LOCATION_KEY, cursor.getString(COL_INPUT_LOCATION));
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
            stackBuilder.addNextIntent(resultIntent);

            PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(
                    0, PendingIntent.FLAG_UPDATE_CURRENT
            );

            mBuilder.setContentIntent(resultPendingIntent);

            NotificationManager mNotificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(WEATHER_NOTIFICATION_ID, mBuilder.build());

            //refreshing last sync
            Utility.setLastNotificationTime(context, System.currentTimeMillis());
        }
        cursor.close();

    }





    //Tell if weather changed rapidly.
    public static boolean weatherIsChangedRapidly(Context context, String notificationCity) {

        Uri weatherUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(notificationCity, System.currentTimeMillis());

        Cursor cursor = context.getContentResolver().query(weatherUri,
                new String[]{WeatherContract.WeatherEntry.COLUMN_WEATHER_CODE},
                null,
                null,
                null);
        if (cursor.moveToFirst()) {
            int weatherCode = cursor.getInt(0);
            if (weatherCode >= 200 && weatherCode < 300) {
                //Thunderstorm
                return true;
            } else if (weatherCode >= 300 && weatherCode < 400) {
                //Drizzle
                return false;
            }else if (weatherCode >= 400 && weatherCode < 500) {
                //Missing data
                return false;
            }else if (weatherCode >= 500 && weatherCode < 600) {
                //Rain
                return true;
            }else if (weatherCode >= 600 && weatherCode < 700) {
                //Snow
                return true;
            }else if (weatherCode >= 700 && weatherCode < 800) {
                //Atmosphere
                return true;
            }else if (weatherCode >= 800 && weatherCode < 900) {
                //Clear if 800, otherwise clouds.
                return false;
            }
            else if (weatherCode >= 900 && weatherCode < 1000) {
                //Additional(good and bad).
                return false;
            } else {
                return false;
            }
        }

        cursor.close();

        return false;
    }
}
