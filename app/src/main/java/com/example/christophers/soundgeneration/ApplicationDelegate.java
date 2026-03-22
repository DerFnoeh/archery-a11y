package com.example.christophers.soundgeneration;

import android.app.Application;

import com.example.christophers.soundgeneration.SoundPipeline.BluetoothHandler;

/**
 * Created by christopherS on 04.05.2017.
 */

public class ApplicationDelegate extends Application {

    public BluetoothHandler btHandler;

    @Override
    public void onCreate()
    {
        super.onCreate();
        btHandler = new BluetoothHandler();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        btHandler.closeBT(false);
    }
}