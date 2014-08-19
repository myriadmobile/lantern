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

    /**
     * Tag for scan interval pref.
     */
    public static final String SCAN_INTERVAL_PREF = "com.myriadmobile.simpleibeacons.scan_interval";

    /**
     * Tag for scan time pref.
     */
    public static final String SCAN_TIME_PREF = "com.myriadmobile.simpleibeacons.scan_time";

    /**
     * Tag for fast scan interval pref.
     */
    public static final String FAST_SCAN_INTERVAL_PREF = "com.myriadmobile.simpleibeacons.fast_scan_interval";

    /**
     * Tag for expiration interval pref.
     */
    public static final String EXPIRATION_INTERVAL_PREF = "com.myriadmobile.simpleibeacons.expiration_interval";

    /**
     * Tag for uuid interval pref.
     */
    public static final String UUID_FILTER_PREF = "com.myriadmobile.simpleibeacons.uuid_filter";

    /**
     * Starts the beacon scanning service. All time parameters are in milliseconds.
     * @param context Context of from the app.
     * @param scanInterval The interval between scans when there are no active beacons.
     * @param expirationInterval The time it takes for an active beacon to expire.
     * @param scanTime The amount of time the scan takes.
     * @param fastScanInterval The interval between scans when there are active beacons.
     * @param uuidFilter The uuid to filter broadcasts. If null send all broadcasts.
     */
    public static void startBeaconService(Context context, int scanInterval, int expirationInterval, int scanTime, int fastScanInterval, String uuidFilter) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null.");
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putInt(SCAN_INTERVAL_PREF, scanInterval).apply();
        prefs.edit().putInt(EXPIRATION_INTERVAL_PREF, expirationInterval).apply();
        prefs.edit().putInt(SCAN_TIME_PREF, scanTime).apply();
        prefs.edit().putInt(FAST_SCAN_INTERVAL_PREF, fastScanInterval).apply();
        prefs.edit().putString(UUID_FILTER_PREF, uuidFilter).apply();
        Intent startService = new Intent(context, BeaconService.class);
        context.stopService(startService);
        context.startService(startService);
    }

    /**
     * Stops the beacon scan service.
     * @param context The context from the app.
     */
    public static void stopBeaconService(Context context){
        if(context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        Intent stopService = new Intent(context, BeaconService.class);
        context.stopService(stopService);
    };

}