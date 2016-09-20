package com.dilyar.weather.app.server;
import android.content.Context;
import android.util.Log;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVOSCloud;
import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.AVQuery;
import com.avos.avoscloud.FindCallback;

import org.json.JSONArray;

import java.util.List;

/**
 * Created by Dilyar on 6/30/16.
 */
public class LeanCloud {

    static final String LOG_TAG = LeanCloud.class.getSimpleName();

    private static final String LEANCLOUD_COLUMN_IMAGES_INFO = "imagesInfo";
    private static final String LEANCLOUD_COLUMN_CITY_NAMES = "cityName";
    private static final String LEANCLOUD_CLASS_CITY_IMAGES = "CityImages";

    public interface LeanCloudListener {
        void getCityImagesInfoDone(String inputLocationName, JSONArray imagesInfo);
    }

    public static void initLeanCloudService(Context context) {
        // 初始化参数依次为 this, AppId, AppKey
        AVOSCloud.initialize(context,"isRwncmm6u0rhrJ3S84u1XYL-MdYXbMMI","ly7SJW8u1hJiGBmMHgF7PC5U");
        AVOSCloud.useAVCloudUS();
    }


    private static LeanCloud mInstance;

    private LeanCloudListener mLeanCloudListener;

    public void setCallbackListener(LeanCloudListener leanCloudListener) {

        this.mLeanCloudListener = leanCloudListener;

    }



    public static LeanCloud getInstance() {

        if (mInstance == null) {
            mInstance = new LeanCloud();
        }
        return mInstance;
    }

    //Only call listener's method if has images
    public void downloadCityImagesInfo(final String cityName) {

        AVQuery<AVObject> query = new AVQuery<>(LEANCLOUD_CLASS_CITY_IMAGES);
        query.whereEqualTo(LEANCLOUD_COLUMN_CITY_NAMES, cityName);
        query.findInBackground(new FindCallback<AVObject>() {

            @Override
            public void done(List<AVObject> list, AVException e) {
                if (e == null && list.size() > 0) {

                    AVObject cityObject = list.get(0);
//                    String cityName = cityObject.getString(LEANCLOUD_CLASS_CITY_IMAGES);
                    JSONArray imagesInfo = cityObject.getJSONArray(LEANCLOUD_COLUMN_IMAGES_INFO);
                    mLeanCloudListener.getCityImagesInfoDone(cityName, imagesInfo);

                } else {
                    Log.e(LOG_TAG, "No such city found in leanCloud: " + cityName );
                }

            }
        });
    }

}
