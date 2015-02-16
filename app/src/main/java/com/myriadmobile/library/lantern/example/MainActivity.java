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

package com.myriadmobile.library.lantern.example;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.myriadmobile.library.lantern.BeaconService;
import com.myriadmobile.library.lantern.IBeacon;
import com.myriadmobile.library.lantern.Lantern;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity {

    /**
     * The listview showing the detected beacons.
     */
    private ListView listView;

    /**
     * The list of detected beacons.
     */
    private List<IBeacon> beacons;

    /**
     * The adapter for the listview.
     */
    private BeaconAdapter adapter;

    /**
     * The receiver for detected beacons.
     */
    private BeaconReceiver receiver;

    /**
     * The receiver for service status changes.
     */
    private ServiceStatusReceiver statusReceiver;

    /**
     * Textview to show user the status of the service.
     */
    private TextView scanningStatus;

    /**
     * Switch to allow user to turn on and off the scanning service.
     */
    private Switch scanToggle;

    //Create the actual Lantern object
    final Lantern lantern = new Lantern.Builder(MainActivity.this)
            .ofType(Lantern.BeaconType.IBEACON)
            .withScanInterval(20000)
            .withExpirationInterval(60000)
            .withScanTime(5000)
            .withFastScanInterval(5000)
            .withUuidFilter(null)
            .build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        beacons = new ArrayList<IBeacon>();
        listView = (ListView)findViewById(R.id.lv_beacons);

        adapter = new BeaconAdapter(this, R.layout.beacon_item, beacons);
        listView.setAdapter(adapter);

        scanningStatus = (TextView)findViewById(R.id.tv_status);
        scanToggle = (Switch)findViewById(R.id.swtScan);

        // Create the receivers to catch broadcasts from the service.
        receiver = new BeaconReceiver();
        statusReceiver = new ServiceStatusReceiver();

        // Create the intent filters to get only the broadcasts from the service.
        IntentFilter intentFilter = new IntentFilter(BeaconService.BEACON_DETECTED_RECEIVER_ACTION);
        intentFilter.addAction(BeaconService.BEACON_EXPIRATION_RECEIVER_ACTION);
        IntentFilter statusIntentFilter = new IntentFilter(BeaconService.BEACON_SERVICE_STATUS_ACTION);

        // Register the receivers for the service.
        registerReceiver(statusReceiver, statusIntentFilter);
        registerReceiver(receiver, intentFilter);

        scanToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean on) {
                if (on) {
                    // Start the service.
                    lantern.startScan();
                } else {
                    // Stop the service.
                    lantern.stopScan();
                }
            }
        });

    }

    /**
     * Unregister receivers and stops beacon service.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        lantern.stopScan();
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
        if (statusReceiver != null) {
            unregisterReceiver(statusReceiver);
        }
    }

    /**
     * Receives detected beacons and adds them to the listview.
     * Also receives the expiration of beacons and removes them from the listview.
     */
    public class BeaconReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                if (intent.getAction().equals(BeaconService.BEACON_DETECTED_RECEIVER_ACTION)) {
                    IBeacon iBeacon = extras.getParcelable(BeaconService.BEACON_RECEIVER_EXTRA);
                    if (beacons.contains(iBeacon)) {
                        beacons.remove(iBeacon);
                        beacons.add(iBeacon);
                        adapter.notifyDataSetChanged();
                    } else {
                        beacons.add(iBeacon);
                    }
                    adapter.notifyDataSetChanged();
                } else if (intent.getAction().equals(BeaconService.BEACON_EXPIRATION_RECEIVER_ACTION)) {
                    IBeacon beacon = extras.getParcelable(BeaconService.BEACON_RECEIVER_EXTRA);
                    if (beacons.contains(beacon)) {
                        beacons.remove(beacon);
                        adapter.notifyDataSetChanged();
                    }
                }
            }
        }
    }

    /**
     * Receives status changes from the service.
     * Displays the current status in the status textview.
     */
    public class ServiceStatusReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                if (intent.getAction().equals(BeaconService.BEACON_SERVICE_STATUS_ACTION)) {
                    int beaconStatus = extras.getInt(BeaconService.BEACON_SERVICE_STATUS_CHANGE_EXTRA);
                    switch (beaconStatus) {
                        case BeaconService.BEACON_STATUS_OFF:
                            scanToggle.setChecked(false);
                            scanningStatus.setText(R.string.off);
                            break;
                        case BeaconService.BEACON_STATUS_SCANNING:
                            scanningStatus.setText(R.string.scanning);
                            break;
                        case BeaconService.BEACON_STATUS_FAST_SCANNING:
                            scanningStatus.setText(R.string.fast_scanning);
                            break;
                        case BeaconService.BEACON_STATUS_NOT_SCANNING:
                            scanningStatus.setText(R.string.not_scanning);
                            break;
                    }
                }
            }
        }
    }
}