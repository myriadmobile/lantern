package com.myriadmobile.library.lantern;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/*
* This is the primary class exposed to the end user (The other being Beacon). Everything will
* be done from here. A single instance of Lantern will only only work for a single kind of beacon.
*/
public class Lantern {

    public enum BeaconType {
        IBEACON
    }

    /**
     * Preference tags for scanning
     */
    public static final String PREF_SCAN_INTERVAL = "com.myriadmobile.library.lantern.scan_interval";
    public static final String PREF_SCAN_TIME = "com.myriadmobile.library.lantern.scan_time";
    public static final String PREF_FAST_SCAN_INTERVAL = "com.myriadmobile.library.lantern.fast_scan_interval";
    public static final String PREF_EXPIRATION_INTERVAL = "com.myriadmobile.library.lantern.expiration_interval";
    public static final String PREF_UUID_FILTER = "com.myriadmobile.library.lantern.uuid_filter";


    //    private Beacon beacon;
    private BeaconService beaconService;
    private Context context;
    private BeaconType beaconType;
    private int scanInterval;
    private int expirationInterval;
    private int scanTime;
    private int fastScanInterval;
    private String[] uuidFilter;

    /**
     * Starts the beacon scanning service. All time parameters are in milliseconds.
     * <p/>
     * context            Context of from the app.
     * scanInterval       The interval between scans when there are no active beacons.
     * expirationInterval The time it takes for an active beacon to expire.
     * scanTime           The amount of time the scan takes.
     * fastScanInterval   The interval between scans when there are active beacons.
     * uuidFilter         The UUID to filter broadcasts. If null send all broadcasts.
     */
    private Lantern(Builder builder) {
        this.context = builder.context;
        this.beaconType = builder.beaconType;
        this.scanInterval = builder.scanInterval;
        this.expirationInterval = builder.expirationInterval;
        this.scanTime = builder.scanTime;
        this.fastScanInterval = builder.fastScanInterval;
        this.uuidFilter = builder.uuidFilter;
    }

    public void startScan() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putInt(PREF_SCAN_INTERVAL, scanInterval).apply();
        prefs.edit().putInt(PREF_EXPIRATION_INTERVAL, expirationInterval).apply();
        prefs.edit().putInt(PREF_SCAN_TIME, scanTime).apply();
        prefs.edit().putInt(PREF_FAST_SCAN_INTERVAL, fastScanInterval).apply();
        //TODO Allow this to work for more than just a single String
        prefs.edit().putString(PREF_UUID_FILTER, null).apply();
        Intent startService = new Intent(context, BeaconService.class);
        context.stopService(startService);
        context.startService(startService);
    }

    public void stopScan() {
        Intent stopService = new Intent(context, BeaconService.class);
        context.stopService(stopService);
    }


    public static class Builder {
        private Context context;
        private BeaconType beaconType;
        private int scanInterval;
        private int expirationInterval;
        private int scanTime;
        private int fastScanInterval;
        private String[] uuidFilter;

        public Builder(Context context) {
            if (context == null) {
                throw new IllegalArgumentException("Context cannot be null");
            }
            this.context = context;
            beaconType = BeaconType.IBEACON;
            scanInterval = 20000;
            expirationInterval = 60000;
            scanTime = 5000;
            fastScanInterval = 5000;
            uuidFilter = null;
        }

        public Lantern build() {
            return new Lantern(this);
        }

        public Builder ofType(BeaconType beaconType) {
            this.beaconType = beaconType;
            return this;
        }

        public Builder withScanInterval(int milliseconds) {
            scanInterval = milliseconds;
            return this;
        }

        public Builder withExpirationInterval(int milliseconds) {
            expirationInterval = milliseconds;
            return this;
        }

        public Builder withScanTime(int milliseconds) {
            scanTime = milliseconds;
            return this;
        }

        public Builder withFastScanInterval(int milliseconds) {
            fastScanInterval = milliseconds;
            return this;
        }

        public Builder withUuidFilter(String[] uuidFilter) {
            this.uuidFilter = uuidFilter;
            return this;
        }
    }
}
