package com.dilyar.weather.app.application;
import android.app.Application;

import com.dilyar.weather.app.server.LeanCloud;

/**
 * Created by Dilyar on 6/30/16.
 */
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        LeanCloud.initLeanCloudService(this);
    }


}
