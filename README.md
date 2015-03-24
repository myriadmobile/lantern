![Lantern](https://github.com/myriadmobile/lantern/raw/master/res/lantern_banner.png)
==============

Introduction
-------
A simple iBeacon wrapper library for Android.


Features
-------

### Configuration
The parameters for starting a scan are as follows:

| Type      | Name                 | Description                                                                          |
| :--:      | :------------------- | :----------------------------------------------------------------------------------- |
| Context   | `context`            | Context from the app.                                                                |
| int       | `scanInterval`       | The amount of time in milliseconds between scans when no beacons have been detected. |
| int       | `expirationInterval` | The amount of time in milliseconds a beacon will remain active since the last time it was detected. |
| int       | `scanTime`           | The amount of time in milliseconds a scan will take.                                 |
| int       | `fastScanInterval`   | The amount of time in milliseconds between scans while there is an active beacon.    |
| String    | `uuidFilter`         | A string that filters which beacons that are detected are broadcast. If this value is null, then all beacons will be broadcast. |

Usage
-------
To start scanning for a beacon, instantiate Lantern and use its builder class to change scanning settings. The settings listed in this example are also the default settings. Then call the startScan method. This will begin the servce.
```java
public class MyActivity extends Activity {
    @Override
    public void onCreate() {
        super.onCreate();
        Lantern lantern = new Lantern.Builder(this)
            .ofType(Lantern.BeaconType.IBEACON)
            .withScanInterval(20000)
            .withExpirationInterval(60000)
            .withScanTime(5000)
            .withFastScanInterval(5000)
            .withUuidFilter(null)
            .build();

            lantern.startScan();
    }
}
```

If `lantern.startScan()` is called more than once in an app,
the service will stop and restart. NO pending beacon expiration broadcasts will be sent
if the service is stopped.

If bluetooth is not enabled when the service is started, it will automatically stop.

The service will continue to scan until the device reboots, or until `lantern.stopScan()` is called. `onDestroy` is a great place for this.
```java
public class MyActivity extends Activity {
    @Override
    public void onDestroy() {
        super.onDestroy();

        lantern.stopScan();
    }
}
```

Documentation
-------
### How it Works
The service will scan using the input intervals, until a beacon is detected. Once a beacon is detected,
it is considered an active beacon and the service will then scan using the fast scan interval,
until there are no longer any active beacons.
A beacon remains active until the expiration interval on that beacon has been reached. Every time a beacon is detected,
it’s expiration interval is extended. Then the regular scan interval will be used until another beacon has been detected.

### Getting Detected Beacon Results
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
            if (intent.getAction().equals(BeaconService.BEACON_DETECTED_RECEIVER_ACTION)) {
                Beacon beacon = extras.getParcelable(BeaconService.BEACON_RECEIVER_EXTRA);
                // Do something.
            }
        }
    }
}
```

### Getting Expired Beacon Results
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
            if (intent.getAction().equals(BeaconService.BEACON_EXPIRATION_RECEIVER_ACTION)) {
                Beacon beacon = extras.getParcelable(BeaconService.BEACON_RECEIVER_EXTRA);
                // Do something.
            }
        }
    }
}
```

### Getting Service Status Changes
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
                if (intent.getAction().equals(BeaconService.BEACON_SERVICE_STATUS_ACTION)) {
                    int beaconStatus = extras.getInt(BeaconService.BEACON_SERVICE_STATUS_CHANGE_EXTRA);
                    switch (beaconStatus) {
                        case BeaconService.BEACON_STATUS_OFF:
                            scanningStatus.setText("Service Off");
                            break;
                        case BeaconService.BEACON_STATUS_SCANNING:
                            scanningStatus.setText("Service Scanning");
                            break;
                        case BeaconService.BEACON_STATUS_FAST_SCANNING:
                            scanningStatus.setText("Service Fast Scanning");
                            break;
                        case BeaconService.BEACON_STATUS_NOT_SCANNING:
                            scanningStatus.setText("Service Not Scanning");
                            break;
                    }
                }
            }
        }
    }
```

Dependencies
-------
Currently, this is not available elsewhere. You'll need to work some crafty Git magic or copy & paste the project in order to take advantage of it.

Roadmap
-------


Bugs and Feedback
-------
Have you found a bug? We'd sincerely appreciate an issue opened with as much detail as possible about the problem. Additionally, if you have a rad idea for a feature, tweak, or configurable aspect, create an issue! We'd love to hear from you. Fair warning: we may not agree the feature or tweak is a rad idea and close the issue, in which case you should maintain your own fork with your own changes.

Contributors
-------
### Lead
[louie2107](https://github.com/louie2107)

Would you like to contribute? Fork us and send a pull request! Be sure to checkout our issues first.

FAQ
-------
> Why Lantern?

iBeacons are kinda like lanterns, and this simplifies... something. It's just a name, man!

![Lantern](https://github.com/myriadmobile/lantern/raw/master/res/lantern.png)

License
--------
The MIT License (MIT)

Copyright (c) 2014 Myriad Mobile, www.myriadmobile.com

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
