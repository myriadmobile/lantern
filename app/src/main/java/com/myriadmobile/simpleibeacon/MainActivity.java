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

package com.myriadmobile.simpleibeacon;

import android.app.Activity;
import android.os.Bundle;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Parcel;
import android.util.Log;
import android.widget.ListView;

import com.myriadmobile.simpleibeacons.library.Beacon;
import com.myriadmobile.simpleibeacons.library.BeaconService;
import com.myriadmobile.simpleibeacons.library.BeaconServiceController;

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
    private List<Beacon> beacons;

    /**
     * The adapter for the listview.
     */
    private BeaconAdapter adapter;

    /**
     * The receiver for detected beacons.
     */
    private BeaconReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        beacons = new ArrayList<Beacon>();
        listView = (ListView)findViewById(R.id.listview);


        adapter = new BeaconAdapter(this, R.layout.beacon_item, beacons);
        listView.setAdapter(adapter);

        receiver = new BeaconReceiver();

        IntentFilter intentFilter = new IntentFilter(BeaconService.BEACON_DETECTED_RECEIVER_ACTION);
        intentFilter.addAction(BeaconService.BEACON_EXPIRATION_RECEIVER_ACTION);

        registerReceiver(receiver, intentFilter);

        BeaconServiceController.startBeaconService(this, 20000, 60000, 5000, 5000, null);
    }


    /**
     * Unregister receivers and stops beacon service.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        BeaconServiceController.stopBeaconService(this);
        unregisterReceiver(receiver);
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

                if (intent.getAction() == BeaconService.BEACON_DETECTED_RECEIVER_ACTION) {
                    Beacon beacon = extras.getParcelable(BeaconService.BEACON_RECEIVER_EXTRA);
                    if (beacons.contains(beacon)) {
                        beacons.remove(beacon);
                        beacons.add(beacon);
                        adapter.notifyDataSetChanged();
                    } else {
                        beacons.add(beacon);

                    }
                    adapter.notifyDataSetChanged();


                } else if (intent.getAction() == BeaconService.BEACON_EXPIRATION_RECEIVER_ACTION) {
                    Beacon beacon = extras.getParcelable(BeaconService.BEACON_RECEIVER_EXTRA);
                    if (beacons.contains(beacon)) {
                        beacons.remove(beacon);
                        adapter.notifyDataSetChanged();
                    }
                }
            }
        }
    }

}