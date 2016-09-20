package com.dilyar.weather.app.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Point;
import android.support.v4.widget.CursorAdapter;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dilyar.weather.app.ForecastFragment;
import com.dilyar.weather.app.R;
import com.dilyar.weather.app.utility.Utility;

/**
 * {@link ForecastAdapter} exposes a list of weather forecasts
 * from a {@link Cursor} to a {@link android.widget.ListView}.
 */
public class ForecastAdapter extends CursorAdapter {

    public static final String LOG_TAG = ForecastAdapter.class.getSimpleName();



    private static final int VIEW_TYPE_COUNT = 2;
    private static final int VIEW_TYPE_TODAY = 0;
    private static final int VIEW_TYPE_FUTURE_DAY = 1;

    /**
     * Cache of the children views for a forecast list item.
     */
    public static class ViewHolder {
        public final ImageView iconView;
        public final TextView dateView;
        public final TextView descriptionView;
        public final TextView highTempView;
        public final TextView lowTempView;
        public final TextView currentTempView;
        public final TextView cityNameView;
//        public final TextView humidityView;
//        public final TextView cloudinessView;
//        public final TextView windSpeedView;
//        public final ImageView upArrowView;

        public ViewHolder(View view) {

            iconView = (ImageView) view.findViewById(R.id.list_item_icon);
            dateView = (TextView) view.findViewById(R.id.list_item_date_textview);
            descriptionView = (TextView) view.findViewById(R.id.list_item_description);
            highTempView = (TextView) view.findViewById(R.id.list_item_high_textview);
            lowTempView = (TextView) view.findViewById(R.id.list_item_low_textview);
            currentTempView = (TextView) view.findViewById(R.id.list_item_currentTemp_textview);
            cityNameView = (TextView) view.findViewById(R.id.list_item_city_textview);
//            humidityView = (TextView) view.findViewById(R.id.list_item_humidity_textview);
//            cloudinessView = (TextView) view.findViewById(R.id.list_item_cloudiness_textview);
//            windSpeedView = (TextView) view.findViewById(R.id.list_item_windspeed_textview);
//            upArrowView = (ImageView) view.findViewById(R.id.upwards_arrow_imageview);

        }
    }

    public ForecastAdapter(Context context, Cursor cursor, int flags) {

        super(context, cursor, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {


        View view;

        int layoutType = getItemViewType(cursor.getPosition());

        if (layoutType == VIEW_TYPE_TODAY) {

            view = LayoutInflater.from(context).inflate(R.layout.list_item_forecast_today, parent, false);

            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = windowManager.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int height = size.y;

            view.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height));

        } else {
            view = LayoutInflater.from(context).inflate(R.layout.list_item_forecast, parent, false);
        }

        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {


        ViewHolder viewHolder = (ViewHolder) view.getTag();
        // Read user preference for metric or imperial temperature units
        boolean isMetric = Utility.isCelsius(context);

        int viewType = getItemViewType(cursor.getPosition());

        if (viewType == VIEW_TYPE_TODAY) {

            viewHolder.cityNameView.setText(cursor.getString(ForecastFragment.COL_CITY_NAME));


            viewHolder.descriptionView.setText(cursor.getString(ForecastFragment.COL_WEATHER_DESC));

            double dayTemp = cursor.getDouble(ForecastFragment.COL_CURRENT_TEMP);
            viewHolder.currentTempView.setText(Utility.formatTemperature(context, dayTemp, isMetric));

//            viewHolder.upArrowView.setColorFilter(Color.parseColor("#ffffff"));
//
//
//            viewHolder.humidityView.setText(Utility.getFormattedHumidity(
//                    mContext, cursor.getFloat(ForecastFragment.COL_HUMIDITY)));
//
//            viewHolder.cloudinessView.setText(Utility.getFormattedCloudiness(
//                    mContext, cursor.getFloat(ForecastFragment.COL_CLOUDINESS)));
//
//            viewHolder.windSpeedView.setText(Utility.getFormattedWind
//                    (mContext, cursor.getFloat(ForecastFragment.COL_WIND_SPEED),
//                            cursor.getFloat(ForecastFragment.COL_WIND_DIRECTION)));

        }
        else if (viewType == VIEW_TYPE_FUTURE_DAY) {
            double maxTemp = cursor.getDouble(ForecastFragment.COL_WEATHER_MAX_TEMP);
            double minTemp = cursor.getDouble(ForecastFragment.COL_WEATHER_MIN_TEMP);
            viewHolder.highTempView.setText(Utility.formatTemperature(context, maxTemp, isMetric));
            viewHolder.lowTempView.setText(Utility.formatTemperature(context, minTemp, isMetric));
        }

        viewHolder.dateView.setText(Utility.getFriendlyDayString(context, cursor.getLong(ForecastFragment.COL_WEATHER_DATE)));
        viewHolder.iconView.setImageResource(Utility.getIconResourceForWeatherCondition( cursor.getInt(ForecastFragment.COL_WEATHER_CONDITION_ID)));



    }

    @Override
    public int getItemViewType(int position) {

        return position == 0 ? VIEW_TYPE_TODAY : VIEW_TYPE_FUTURE_DAY;
    }

    @Override
    public int getViewTypeCount() {

        return VIEW_TYPE_COUNT;
    }

}