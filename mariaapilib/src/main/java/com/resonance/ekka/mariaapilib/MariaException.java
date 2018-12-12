package com.resonance.ekka.mariaapilib;

import android.util.Log;

public class MariaException extends Exception {
    MariaException(String message){
        super(message);
       // Log.d(MainActivity.DBG_TAG, "MariaException " + message);
    }
}
