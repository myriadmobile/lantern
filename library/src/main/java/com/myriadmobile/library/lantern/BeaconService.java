/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Myriad Mobile
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.myriadmobile.library.lantern;


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
    public static final String BEACON_DETECTED_RECEIVER_ACTION = "com.myriadmobile.library.lantern.beacon_detected_receiver_action";

    /**
     * Tag for expired beacon broadcast.
     */
    private static final String BEACON_EXPIRATION_RECEIVER_PRIVATE = "com.myriadmobile.library.lantern.beacon_expiration_receiver_action_private";

    /**
     * Tag for expired beacon broadcast.
     */
    public static final String BEACON_EXPIRATION_RECEIVER_ACTION = "com.myriadmobile.library.lantern.beacon_expiration_receiver_action";

    /**
     * Tag for when the service status changes.
     */
    public static final String BEACON_SERVICE_STATUS_ACTION = "com.myriadmobile.library.lantern.beacon_service_status_action";

    /**
     * Tag to get beacon out of extras.
     */
    public static final String BEACON_RECEIVER_EXTRA = "com.myriadmobile.library.lantern.beacon_receiver_extra";

    /**
     * Tag to get beacon out of extras.
     */
    public static final String BEACON_SERVICE_STATUS_CHANGE_EXTRA = "com.myriadmobile.library.lantern.beacon_service_status_change_extra";

    /**
     * Tag for when the service is turned off status broadcast.
     */
    public static final int BEACON_STATUS_OFF = 0;

    /**
     * Tag for when the service is scanning status broadcast.
     */
    public static final int BEACON_STATUS_SCANNING = 1;

    /**
     * Tag for when the service is fast scanning status broadcast.
     */
    public static final int BEACON_STATUS_FAST_SCANNING = 2;

    /**
     * Tag for when the service is not scanning status broadcast.
     */
    public static final int BEACON_STATUS_NOT_SCANNING = 3;

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
     * If not null, only send broadcasts for beacons that have this uuid.
     */
    private String uuidFilter;

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


    @Override
    public void onCreate() {
        super.onCreate();

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        scanInterval = prefs.getInt(BeaconServiceController.SCAN_INTERVAL_PREF, 20000);
        expirationInterval = prefs.getInt(BeaconServiceController.EXPIRATION_INTERVAL_PREF, 60000);
        fastScanInterval = prefs.getInt(BeaconServiceController.FAST_SCAN_INTERVAL_PREF, 5000);
        scanTime = prefs.getInt(BeaconServiceController.SCAN_TIME_PREF, 5000);
        uuidFilter = prefs.getString(BeaconServiceController.UUID_FILTER_PREF, null);


        scanHandler = new Handler();
        scanRunnable = new Runnable() {
            @Override
            public void run() {
                scanForBeacons();
            }
        };

        // Callback that is called when the scan find something.
        scanCallback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {

                // Try to make a beacon object, if it comes back null,
                // then it is not a beacon, so do nothing.
                Beacon temp = Beacon.fromScanData(scanRecord, rssi, device);
                if (temp != null) {
                    // Check if there is a uuid filter, if there isn't continue,
                    // if there is and it matches the beacon, continue.
                    if (uuidFilter == null || (uuidFilter.equals(temp.getUuid()))) {

                        // Set the beacons expiration time.
                        temp.setExpirationTime(Calendar.getInstance().getTimeInMillis() + expirationInterval);

                        // Check if the beacon is active, if not make it active.
                        if (!detectedBeacons.contains(temp)) {
                            detectedBeacons.add(temp);
                            sendDetectedBeaconBroadcast(temp);
                        }
                        // Check if the beacon is active, if it is check if the distance has changed.
                        // If so, update it and send a new broadcast.
                        else if (detectedBeacons.contains(temp) && rssi != detectedBeacons.get(detectedBeacons.indexOf(temp)).getRssi()) {
                            detectedBeacons.remove(temp);
                            detectedBeacons.add(temp);
                            sendDetectedBeaconBroadcast(temp);
                        }
                        setupBeaconExpiration(temp);
                    }
                }
            }
        };

        // Check if bluetooth is active, if not, stop the service.
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
        bluetoothAdapter = bluetoothManager.getAdapter();
        if(bluetoothAdapter.isEnabled()) {
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

        unregisterReceiver(expirationReceiver);
        bluetoothAdapter.stopLeScan(scanCallback);
        scanHandler.removeCallbacksAndMessages(null);
        sendStatusBroadcast(BEACON_STATUS_OFF);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Sends a broadcast with the status of the service.
     * @param broadcastType The status of the service.
     */
    private void sendStatusBroadcast(int broadcastType) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(BEACON_SERVICE_STATUS_ACTION);
        switch (broadcastType) {
            case BEACON_STATUS_OFF:
                broadcastIntent.putExtra(BEACON_SERVICE_STATUS_CHANGE_EXTRA, BEACON_STATUS_OFF);
                break;
            case BEACON_STATUS_SCANNING:
                broadcastIntent.putExtra(BEACON_SERVICE_STATUS_CHANGE_EXTRA, BEACON_STATUS_SCANNING);
                break;
            case BEACON_STATUS_FAST_SCANNING:
                broadcastIntent.putExtra(BEACON_SERVICE_STATUS_CHANGE_EXTRA, BEACON_STATUS_FAST_SCANNING);
                break;
            case BEACON_STATUS_NOT_SCANNING:
                broadcastIntent.putExtra(BEACON_SERVICE_STATUS_CHANGE_EXTRA, BEACON_STATUS_NOT_SCANNING);
                break;
        }
        sendBroadcast(broadcastIntent);
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
            if (inFastScanMode) {
                sendStatusBroadcast(BEACON_STATUS_FAST_SCANNING);
            } else {
                sendStatusBroadcast(BEACON_STATUS_SCANNING);
            }
            isScanning = true;
            scanToggle = false;
            bluetoothAdapter.startLeScan(scanCallback);
            scanHandler.postDelayed(scanRunnable, scanTime);
        } else {
            sendStatusBroadcast(BEACON_STATUS_NOT_SCANNING);
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

        Parcel parcel = Parcel.obtain();
        beacon.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        Intent intent = new Intent();
        intent.putExtra(BEACON_RECEIVER_EXTRA, parcel.marshall());

        intent.setAction(BEACON_EXPIRATION_RECEIVER_PRIVATE);
        PendingIntent startPIntent = PendingIntent.getBroadcast(getApplicationContext(), beacon.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarm = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        alarm.set(AlarmManager.RTC_WAKEUP, beacon.getExpirationTime(), startPIntent);

    }

    /**
     * Receives expiration broadcasta and sends them to the host app.
     * This is a helper recevier that allows the beacon to be put into a bundle,
     * since there would be an error if the alarmmanager tried to send it.
     */
    private class ExpirationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BeaconService.BEACON_EXPIRATION_RECEIVER_PRIVATE)) {
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