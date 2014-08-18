package com.myriadmobile.simpleibeacons;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Parcel;
import android.util.Log;
import android.widget.ListView;

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

        BeaconServiceController.startBeaconService(this, 20000, 60000, 5000, 5000);



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
            if(extras != null) {
                Log.d("tester", "here");

                if(intent.getAction() == BeaconService.BEACON_DETECTED_RECEIVER_ACTION) {
                    Beacon beacon = extras.getParcelable(BeaconService.BEACON_RECEIVER_EXTRA);

                    Log.d("tester", "there");

                    if(beacons.contains(beacon)) {
                        beacons.remove(beacon);
                        beacons.add(beacon);
                        adapter.notifyDataSetChanged();
                    } else {
                        beacons.add(beacon);

                    }
                    adapter.notifyDataSetChanged();



                } else if(intent.getAction() == BeaconService.BEACON_EXPIRATION_RECEIVER_ACTION) {
                    Beacon beacon = extras.getParcelable(BeaconService.BEACON_RECEIVER_EXTRA);

                    if(beacons.contains(beacon)) {
                        Log.d("tester", "gonner");
                        beacons.remove(beacon);
                        adapter.notifyDataSetChanged();
                    }
                }
            }
        }
    }

}
