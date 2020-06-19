package com.hyperether.pointsnaps;

import android.app.Application;

import com.hyperether.pointsnapssdk.repository.PointSnapsSdk;
import com.hyperether.toolbox.HyperConfig;

/**
 * Application class
 *
 * @author Marko Katic
 * @author Slobodan Prijic
 * @version 1.1 - 06/04/2020
 */
public class App extends Application {

    private static App instance;

    public static App getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        new PointSnapsSdk.Builder()
                .setContext(getApplicationContext())
                .build();

        new HyperConfig.Builder()
                .setDebug(true)
                .build(getApplicationContext());
    }
}