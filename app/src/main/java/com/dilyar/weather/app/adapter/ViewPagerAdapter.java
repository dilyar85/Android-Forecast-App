package com.dilyar.weather.app.adapter;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.dilyar.weather.app.ForecastFragment;
import com.dilyar.weather.app.utility.Utility;

/**
 * Created by Dilyar on 8/11/16.
 */
public class ViewPagerAdapter extends FragmentStatePagerAdapter {

    private static final String LOG_TAG = ViewPagerAdapter.class.getSimpleName();

    private Context mContext;


    public ViewPagerAdapter(Context context, FragmentManager fragmentManager) {
        super(fragmentManager);
        mContext = context;
    }

    @Override
    public Fragment getItem(int i) {

        String cityName = Utility.getCityNameByIndex(mContext, i);

        Fragment fragment = new ForecastFragment();
        Bundle args = new Bundle();
        args.putString(ForecastFragment.BUNDLE_KEY_CITY_NAME, cityName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getCount() {

        int fragmentCount = Utility.getCityNamesSize(mContext);
        //Make sure at least one fragment is shown
        if (fragmentCount == 0) {
            fragmentCount = 1;
        }
        return fragmentCount;
    }


    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }



}
