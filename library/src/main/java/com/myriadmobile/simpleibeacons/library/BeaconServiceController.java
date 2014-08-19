package com.myriadmobile.simpleibeacons.library;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Class that is used to intiate and deactivate the beacon scanning service.
 * Should be called and passed a BeaconConfig object along with context.
 */
public class BeaconServiceController {

    public static final String SCAN_INTERVAL_PREF = "com.usolu.ibeaconservice.scan_interval";
    public static final String SCAN_TIME_PREF = "com.usolu.ibeaconservice.scan_time";
    public static final String FAST_SCAN_INTERVAL_PREF = "com.usolu.ibeaconservice.fast_scan_interval";
    public static final String EXPIRATION_INTERVAL_PREF = "com.usolu.ibeaconservice.expiration_interval";

    public static void startBeaconService(Context context, int scanInterval, int expirationInterval, int scanTime, int fastScanInterval) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null.");
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putInt(SCAN_INTERVAL_PREF, scanInterval).apply();
        prefs.edit().putInt(EXPIRATION_INTERVAL_PREF, expirationInterval).apply();
        prefs.edit().putInt(SCAN_TIME_PREF, scanTime).apply();
        prefs.edit().putInt(FAST_SCAN_INTERVAL_PREF, fastScanInterval).apply();
        Intent startService = new Intent(context, BeaconService.class);
        context.stopService(startService);
        context.startService(startService);
    }

    public static void stopBeaconService(Context context){
        if(context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        Intent stopService = new Intent(context, BeaconService.class);
        context.stopService(stopService);
    };

}