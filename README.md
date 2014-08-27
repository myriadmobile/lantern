Lantern
==============

A simple iBeacon wrapper library for Android


Configuration
-------------

The parameters for starting a scan are as follows: 

Context `context` : Context from the app.

int `scanInterval` : The amount of time in milliseconds between scans when no beacons have been detected.

int `expirationInterval` : The amount of time in milliseconds a beacon will remain active since the last time it was detected.

int `scanTime` : The amount of time in milliseconds a scan will take.

int `fastScanInterval` : The amount of time in milliseconds etween scans while there is an active beacon.

String `uuidFilter` : A string that filters which beacons that are detected are broadcast. If this value is null,
then all beacons will be broadcast.

How To Use
----------

To start scanning for beacon call `BeaconServiceController.startBeaconService(context, scanInterval, expirationInterval, scanTime, fastScanInterval, uuidFilter)` for example: 

```java
public class MyActivity extends Activity {
    @Override
    public void onCreate() {
        super.onCreate();

        BeaconServiceController.startBeaconService(this, 20000, 60000, 7000, 5000, null);
    }
}
```

If `BeaconServiceController.startBeaconService` is called more than once in an app, 
the service will stop and restart. NO pending beacon expiration broadcasts will be sent
if the service is stopped.

If bluetooth is not enabled when the service is started, it will automatically stop.

The service will continue to scan until the device reboots, or `BeaconServiceController.stopBeaconService(context)` is called.

To stop the service call `BeaconServiceController.stopBeaconService` for example: 
```java
public class MyActivity extends Activity {
    @Override
    public void onCreate() {
        super.onCreate();

        BeaconServiceController.stopBeaconService(this);
    }
}
```

How It Works
------------

The service will scan using the input intervals, until a beacon is detected. Once a beacon is detected,
it is considered an active beacon and the service will then scan using the fast scan interval,
until there are no longer any active beacons.
A beacon remains active until the expiration interval on that beacon has been reached. Every time a beacon is detected,
it’s expiration interval is extended. Then the regular scan interval will be used until another beacon has been detected.

Getting Detected Beacon Results
-------------------------------

In order to be notified when a beacon has been detected, a broadcast receiver must be registered,
and listening for the action `BeaconService.BEACON_DETECTED_RECEIVER_ACTION` . 
When the broadcast receiver has been called, the beacon object can be retrieved from the extras in the intent.
The tag to get the beacon is `BeaconService.BEACON_RECEIVER_EXTRA` . The broadcast for a beacon will be sent every
time the beacon’s RSSI is changed, which indicates that the distance of the beacon has been changed,
if there is not change in the RSSI value, this broadcast will not be sent.

Example of obtaining a beacon detected from scan results:

```java
...
BeaconReceiver receiver = new BeaconReceiver();

IntentFilter intentFilter = new IntentFilter();
intentFilter.addAction(BeaconService.BEACON_DETECTED_RECEIVER_ACTION);
registerReceiver(receiver, intentFilter);
...

public class BeaconReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        Bundle extras = intent.getExtras();
        if (extras != null) {
            if (intent.getAction() == BeaconService.BEACON_DETECTED_RECEIVER_ACTION) {
                Beacon beacon = extras.getParcelable(BeaconService.BEACON_RECEIVER_EXTRA);
                // Do something.
            } 
        }
    }
}
```

Getting Expired Beacon Results
------------------------------

In order to be notified when a beacon has expired, a broadcast receiver must be registered,
and listening for the action `BeaconService.BEACON_EXPIRATION_RECEIVER_ACTION` . 
When the broadcast receiver has been called, the beacon object can be retrieved from the extras in the intent. 
The tag to get the beacon is `BeaconService.BEACON_RECEIVER_EXTRA` .

Example of obtaining a beacon expiration from scan results:

```java
...
BeaconExpirationReceiver receiver = new BeaconExpirationReceiver();

IntentFilter intentFilter = new IntentFilter();
intentFilter.addAction(BeaconService.BEACON_EXPIRATION_RECEIVER_ACTION);
registerReceiver(receiver, intentFilter);
...

public class BeaconExpirationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        Bundle extras = intent.getExtras();
        if (extras != null) {
            if (intent.getAction() == BeaconService.BEACON_EXPIRATION_RECEIVER_ACTION) {
                Beacon beacon = extras.getParcelable(BeaconService.BEACON_RECEIVER_EXTRA);
                // Do something.
            } 
        }
    }
}
```

Getting Service Status Changes
-------------------------------

In order to be notified when the beacon scan service status has changed,
a broadcast receiver must be registered, and listening for the action `BeaconService.BEACON_SERVICE_STATUS_ACTION` . 
When the broadcast receiver has been called, the the status code can be retrieved with the 
the tag `BeaconService.BEACON_SERVICE_STATUS_CHANGE_EXTRA` . That code can be compared with the following constants:

`BeaconService.BEACON_STATUS_OFF` - The service has been turned off.

`BeaconService.BEACON_STATUS_SCANNING` - The service is in normal scan mode.

`BeaconService.BEACON_STATUS_FAST_SCANNING` - The service is in fast scan mode.

`BeaconService.BEACON_STATUS_NOT_SCANNING` - The service is active, but is in the interval between scans.

Example of obtaining the status of the beacon scan service:

```java
...
ServiceStatusReceiver statusReceiver = new ServiceStatusReceiver();

IntentFilter statusIntentFilter = new IntentFilter(BeaconService.BEACON_SERVICE_STATUS_ACTION);
registerReceiver(statusReceiver, statusIntentFilter);
...

public class ServiceStatusReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                if (intent.getAction() == BeaconService.BEACON_SERVICE_STATUS_ACTION) {
                    int beaconStatus = extras.getInt(BeaconService.BEACON_SERVICE_STATUS_CHANGE_EXTRA);
                        switch (beaconStatus) {
                            case BeaconService.BEACON_STATUS_OFF:
                                // Do something.
                                break;
                            case BeaconService.BEACON_STATUS_SCANNING:
                                // Do something.
                                break;
                            case BeaconService.BEACON_STATUS_FAST_SCANNING:
                                // Do something.
                                break;
                            case BeaconService.BEACON_STATUS_NOT_SCANNING:
                                // Do something.
                                break;
                        }
                }
            }
        }
    }
```
