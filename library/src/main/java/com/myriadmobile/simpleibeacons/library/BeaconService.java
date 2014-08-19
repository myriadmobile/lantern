package com.myriadmobile.simpleibeacons.library;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Service that performs bluetooth low energy scans, if something is detected it is determined
 * if that object is an iBeacon. If so, it sends a broadcast containing the beacon,
 * and an expiration broadcast once that beacon is no longer detected for a certain amount of time,
 * which also contains that beacon. If a beacon's RSSI changes, meaning the distance is likely
 * different, a broadcast with that beacon and it's updated RSSI is also sent.
 */
public class BeaconService extends Service {

    /**
     * Tag for detected beacon broadcast.
     */
    public static final String BEACON_DETECTED_RECEIVER_ACTION = "beacon_detected_receiver_action";

    /**
     * Tag for expired beacon broadcast.
     */
    private static final String BEACON_EXPIRATION_RECEIVER_PRIVATE = "beacon_expiration_receiver_action_private";

    /**
     * Tag for expired beacon broadcast.
     */
    public static final String BEACON_EXPIRATION_RECEIVER_ACTION = "beacon_expiration_receiver_action";

    /**
     * Tag to get beacon out of extras.
     */
    public static final String BEACON_RECEIVER_EXTRA = "beacon_receiver_extra";

    /**
     * The device's bluetooth adapter.
     */
    private BluetoothAdapter bluetoothAdapter;

    /**
     * List of currently active beacons.
     */
    private List<Beacon> detectedBeacons;

    /**
     * Callback when a bluetooth low energy device is detected.
     */
    private BluetoothAdapter.LeScanCallback scanCallback;

    /**
     * Whether the serivce is currently scanning for beacons.
     */
    private boolean isScanning;

    /**
     * Toggles between scan and no scan.
     */
    private boolean scanToggle = true;

    /**
     * Is true when near a beacon. Decreases the interval between scans.
     */
    private boolean inFastScanMode = false;

    /**
     * The handler for the scanning runnable.
     */
    private Handler scanHandler;

    /**
     * The runnable for the scanning.
     */
    private Runnable scanRunnable;

    /**
     * The time in milliseconds between the scans.
     */
    private int scanInterval;

    /**
     * The interval between scans when in fast scan mode.
     */
    private int fastScanInterval;

    /**
     * The amount of time to scan for.
     */
    private int scanTime;

    /**
     * The time in milliseconds that a beacon will remain active since the last time it was detected.
     */
    private int expirationInterval;

    /**
     * The shared preferences that are written to by the beacon service config object.
     */
    private SharedPreferences prefs;

    /**
     * Broadcast receiver called when a beacon is expired.
     */
    private ExpirationReceiver expirationReceiver;

    /**
     * Logging constants.
     */
    private final boolean LOG_ENABLED = true;
    private final String LOG_TAG = getClass().getSimpleName();




    @Override
    public void onCreate() {
        super.onCreate();

        if (LOG_ENABLED) {
            Log.d(LOG_TAG, "service created");
        }

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        scanInterval = prefs.getInt(BeaconServiceController.SCAN_INTERVAL_PREF, 20000);
        expirationInterval = prefs.getInt(BeaconServiceController.EXPIRATION_INTERVAL_PREF, 60000);
        fastScanInterval = prefs.getInt(BeaconServiceController.FAST_SCAN_INTERVAL_PREF, 5000);
        scanTime = prefs.getInt(BeaconServiceController.SCAN_TIME_PREF, 5000);


        final BluetoothManager bluetoothManager =
                (BluetoothManager) this.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        scanHandler = new Handler();
        scanRunnable = new Runnable() {
            @Override
            public void run() {
                if (LOG_ENABLED) {
                    Log.d(LOG_TAG, "runnable hit");
                }
                scanForBeacons();
            }
        };

        scanCallback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                if (LOG_ENABLED) {
                    Log.d(LOG_TAG, "callback");
                }
                Beacon temp = Beacon.fromScanData(scanRecord, rssi, device);
                if (temp != null) {
                    if(!detectedBeacons.contains(temp)) {
                        detectedBeacons.add(temp);
                        sendDetectedBeaconBroadcast(temp);
                        setupBeaconExpiration(temp);
                    }
                    else if (detectedBeacons.contains(temp) && rssi != detectedBeacons.get(detectedBeacons.indexOf(temp)).getRssi()) {
                        detectedBeacons.remove(temp);
                        detectedBeacons.add(temp);
                        sendDetectedBeaconBroadcast(temp);
                        setupBeaconExpiration(temp);

                    }
                }


            }
        };
        bluetoothAdapter = getBluetoothAdapter();
        if (bluetoothAdapter != null) {
            detectedBeacons = new ArrayList<Beacon>();
            expirationReceiver = new ExpirationReceiver();
            IntentFilter intentFilter = new IntentFilter(BeaconService.BEACON_DETECTED_RECEIVER_ACTION);
            intentFilter.addAction(BeaconService.BEACON_EXPIRATION_RECEIVER_PRIVATE);
            registerReceiver(expirationReceiver, intentFilter);
            scanForBeacons();
        } else {
            stopSelf();
        }

    }

    /**
     * Obtains the bluetooth adapter from the system.
     * @return The systems default bluetooth adapter.
     */
    private BluetoothAdapter getBluetoothAdapter() {
        final BluetoothManager bluetoothManager =
                (BluetoothManager) this.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);

        if(bluetoothAdapter.isEnabled()) {
            bluetoothAdapter = bluetoothManager.getAdapter();
            return bluetoothAdapter;
        }
        return null;
    }

    /**
     * Called when service is destroyed and stop all scanning.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (LOG_ENABLED) {
            Log.d(LOG_TAG, "destroyed");
        }
        unregisterReceiver(expirationReceiver);
        bluetoothAdapter.stopLeScan(scanCallback);
        scanHandler.removeCallbacksAndMessages(null);

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    /**
     * Scans for beacons. Either it is scanning, or waiting to scan.
     */
    private void scanForBeacons() {
        if (detectedBeacons.size() == 0) {
            inFastScanMode = false;
        } else {
            inFastScanMode = true;
        }
        if (scanToggle) {
            if (LOG_ENABLED)Log.d(LOG_TAG, "scanning toggle true");
            isScanning = true;
            scanToggle = false;
            bluetoothAdapter.startLeScan(scanCallback);
            scanHandler.postDelayed(scanRunnable, scanTime);
        } else {
            if (LOG_ENABLED)Log.d(LOG_TAG, "scanning toggle false");
            isScanning = false;
            scanToggle = true;
            bluetoothAdapter.stopLeScan(scanCallback);
            if (inFastScanMode) {
                scanHandler.postDelayed(scanRunnable, fastScanInterval);
            } else {
                scanHandler.postDelayed(scanRunnable, scanInterval);
            }
        }
    }

    /**
     * Sends a broadcast with the beacon that was detected.
     * @param beacon The beacons to be sent in the broadcast.
     */
    private void sendDetectedBeaconBroadcast(Beacon beacon) {
        if (LOG_ENABLED)Log.d(LOG_TAG, "broadcast detected");
        Intent intent = new Intent();
        Bundle extras = new Bundle();
        extras.putParcelable(BEACON_RECEIVER_EXTRA, beacon);
        intent.putExtras(extras);
        intent.setAction(BEACON_DETECTED_RECEIVER_ACTION);
        sendBroadcast(intent);
    }


    /**
     * Sets an alarm for a beacon expiration broadcast to be sent, an amount of time in the future
     * that the expiration time is set.
     * @param beacon The beacon to be sent during the expiration broadcast.
     */
    private void setupBeaconExpiration(Beacon beacon) {
        if (LOG_ENABLED)Log.d(LOG_TAG, "broadcast expired");

        Parcel parcel = Parcel.obtain();
        beacon.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        Intent intent = new Intent();
        intent.putExtra(BEACON_RECEIVER_EXTRA, parcel.marshall());

        intent.setAction(BEACON_EXPIRATION_RECEIVER_PRIVATE);
        PendingIntent startPIntent = PendingIntent.getBroadcast(getApplicationContext(), beacon.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarm = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        alarm.set(AlarmManager.RTC_WAKEUP, Calendar.getInstance().getTimeInMillis() + expirationInterval, startPIntent);

    }

    private class ExpirationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == BeaconService.BEACON_EXPIRATION_RECEIVER_PRIVATE) {
                byte[] byteArrayExtra = intent.getByteArrayExtra(BeaconService.BEACON_RECEIVER_EXTRA);
                Parcel parcel = Parcel.obtain();
                parcel.unmarshall(byteArrayExtra, 0, byteArrayExtra.length);
                parcel.setDataPosition(0);
                Beacon beacon = Beacon.CREATOR.createFromParcel(parcel);
                detectedBeacons.remove(beacon);

                Bundle extras = new Bundle();
                extras.putParcelable(BEACON_RECEIVER_EXTRA, beacon);
                Intent expireIntent = new Intent();
                expireIntent.setAction(BEACON_EXPIRATION_RECEIVER_ACTION);
                expireIntent.putExtras(extras);
                sendBroadcast(expireIntent);
            }
        }
    }






}