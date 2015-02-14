package com.myriadmobile.library.lantern;

public abstract class Beacon {

    /**
     * Array for hex conversion.
     */
    public static char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    /**
     * Less than half a meter.
     */
    public static final int PROXIMITY_IMMEDIATE = 1;

    /**
     * Between half and four meters.
     */
    public static final int PROXIMITY_NEAR = 2;

    /**
     * More than four meters.
     */
    public static final int PROXIMITY_FAR = 3;

    /**
     * Distance unknown.
     */
    public static final int PROXIMITY_UNKNOWN = 0;

    /**
     * UUID of beacon.
     */
    public String uuid = null;

    /**
     * 16 bit integer major of beacon.
     */
    public int major = -1;

    /**
     * 16 bit integer minor of beacon.
     */
    public int minor = -1;

    /**
     * Far, near, immediate, or unknown of beacon.
     */
    public Integer proximity = -1;

    /**
     * Estimated distance of beacon.
     */
    public Double distance = -1.0;

    /**
     * RSSI of beacon.
     */
    public int rssi = -1;

    /**
     * Tx power of beacon.
     */
    public int txPower = -1;

    /**
     * Mac address of beacon.
     */
    public String bluetoothAddress = null;

    public long expirationTime = -1;

    public Beacon() {

    }

    public Beacon(String uuid, int major, int minor, int txPower, int rssi) {
        this.uuid = uuid.toLowerCase();
        this.major = major;
        this.minor = minor;
        this.rssi = rssi;
        this.txPower = txPower;
    }

//    public abstract void prepareCallback();

}
