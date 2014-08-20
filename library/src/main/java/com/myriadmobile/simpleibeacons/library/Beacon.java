package com.myriadmobile.simpleibeacons.library;


import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Class that represents an iBeacon.
 */
public class Beacon implements Parcelable {

    /**
     * Array for hex conversion.
     */
    private static final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};

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
    private String uuid;

    /**
     * 16 bit integer major of beacon.
     */
    private int major;

    /**
     * 16 bit integer minor of beacon.
     */
    private int minor;

    /**
     * Far, near, immediate, or unknown of beacon.
     */
    private Integer proximity;

    /**
     * Estimated distance of beacon.
     */
    private Double distance;

    /**
     * RSSI of beacon.
     */
    private int rssi;

    /**
     * Tx power of beacon.
     */
    private int txPower;

    /**
     * Mac address of beacon.
     */
    private String bluetoothAddress;


    public static char[] getHexArray() {
        return hexArray;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public int getMajor() {
        return major;
    }

    public void setMajor(int major) {
        this.major = major;
    }

    public int getMinor() {
        return minor;
    }

    public void setMinor(int minor) {
        this.minor = minor;
    }

    public Integer getProximity() {
        return proximity;
    }

    public void setProximity(Integer proximity) {
        this.proximity = proximity;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public int getTxPower() {
        return txPower;
    }

    public void setTxPower(int txPower) {
        this.txPower = txPower;
    }

    public String getBluetoothAddress() {
        return bluetoothAddress;
    }

    public void setBluetoothAddress(String bluetoothAddress) {
        this.bluetoothAddress = bluetoothAddress;
    }


    Beacon(String uuid, int major, int minor, int txPower, int rssi) {
        this.uuid = uuid.toLowerCase();
        this.major = major;
        this.minor = minor;
        this.rssi = rssi;
        this.txPower = txPower;
    }

    Beacon() {

    }

    @Override
    public int hashCode() {
        return minor;
    }

    /**
     * Two beacons are the same, with major, minor, and uuid being equal.
     * @param that The other beacon being tested for equality.
     * @return Whether the beacons are equal.
     */
    @Override
    public boolean equals(Object that) {
        if (!(that instanceof Beacon)) {
            return false;
        }
        Beacon thatBeacon = (Beacon) that;
        return (thatBeacon.getMajor() == this.getMajor() &&
                thatBeacon.getMinor() == this.getMinor() &&
                thatBeacon.getUuid().equals(this.getUuid()));
    }

    /**
     * Finds the distance of a beacon.
     * @param rssi The RSSI of a beacon.
     * @param txPower The calibrated tx power of a beacon.
     * @return The distance calculated of the beacon.
     */
    public static double calculateDistance(int txPower, double rssi) {
        if (rssi == 0) {
            return -1.0;
        }

        double ratio = rssi*1.0/txPower;
        if (ratio < 1.0) {
            return Math.pow(ratio,10);
        }
        else {
            double distance =  (0.89976)*Math.pow(ratio,7.7095) + 0.111;
            return distance;
        }
    }

    /**
     * Finds the proximity value of a beacon.
     * @param distance The distance of the beacon.
     * @return The proximity that was calculated.
     */
    public static int calculateProximity(double distance) {
        if (distance < 0) {
            return PROXIMITY_UNKNOWN;
        }
        if (distance < 0.5 ) {
            return PROXIMITY_IMMEDIATE;
        }
        if (distance <= 4.0) {
            return PROXIMITY_NEAR;
        }
        return PROXIMITY_FAR;

    }

    /**
     * Returns the human readable proximity.
     * @param proximity The proximity of the beacon.
     * @return The human readable proximity.
     */
    public static String proximityToString(int proximity) {
        if(proximity == 1) {
            return "Immediate";
        }
        else if(proximity == 2) {
            return "Near";
        }
        else if(proximity == 3) {
            return "Far";
        }
        return "Unknown";
    }


    /**
     * Converts bytes to hex.
     * @param bytes The bytes to be converted.
     * @return The hex that was converted from the bytes.
     */
    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for ( int j = 0; j < bytes.length; j++ ) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Returns a beacon object from the data obtained from a low energy scan.
     * @param rssi The RSSI of the beacon.
     * @param device The beacon device.
     * @param scanData The data obtained from the scan.
     * @return The beacon object.
     */
    public static Beacon fromScanData(byte[] scanData, int rssi, BluetoothDevice device) {
        int startByte = 2;
        boolean patternFound = false;
        while (startByte <= 5) {
            if (((int)scanData[startByte+2] & 0xff) == 0x02 && ((int)scanData[startByte+3] & 0xff) == 0x15) {
                patternFound = true;
                break;
            }
            startByte++;
        }


        if (!patternFound) {
            return null;
        }

        Beacon iBeacon = new Beacon();

        iBeacon.major = (scanData[startByte+20] & 0xff) * 0x100 + (scanData[startByte+21] & 0xff);
        iBeacon.minor = (scanData[startByte+22] & 0xff) * 0x100 + (scanData[startByte+23] & 0xff);
        iBeacon.txPower = (int)scanData[startByte+24]; // this one is signed
        iBeacon.rssi = rssi;
        iBeacon.distance = calculateDistance(iBeacon.txPower, iBeacon.rssi);
        iBeacon.proximity = calculateProximity(calculateDistance(iBeacon.txPower, iBeacon.rssi));


        byte[] proximityUuidBytes = new byte[16];
        System.arraycopy(scanData, startByte+4, proximityUuidBytes, 0, 16);
        String hexString = bytesToHex(proximityUuidBytes);
        StringBuilder sb = new StringBuilder();
        sb.append(hexString.substring(0,8));
        sb.append("-");
        sb.append(hexString.substring(8,12));
        sb.append("-");
        sb.append(hexString.substring(12,16));
        sb.append("-");
        sb.append(hexString.substring(16,20));
        sb.append("-");
        sb.append(hexString.substring(20,32));
        iBeacon.uuid = sb.toString();

        if (device != null) {
            iBeacon.bluetoothAddress = device.getAddress();
        }

        return iBeacon;
    }


    protected Beacon(Parcel in) {
        uuid = in.readString();
        major = in.readInt();
        minor = in.readInt();
        proximity = in.readByte() == 0x00 ? null : in.readInt();
        distance = in.readByte() == 0x00 ? null : in.readDouble();
        rssi = in.readInt();
        txPower = in.readInt();
        bluetoothAddress = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(uuid);
        dest.writeInt(major);
        dest.writeInt(minor);
        if (proximity == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeInt(proximity);
        }
        if (distance == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeDouble(distance);
        }
        dest.writeInt(rssi);
        dest.writeInt(txPower);
        dest.writeString(bluetoothAddress);
    }

    @SuppressWarnings("unused")
    public static final Creator<Beacon> CREATOR = new Creator<Beacon>() {
        @Override
        public Beacon createFromParcel(Parcel in) {
            return new Beacon(in);
        }

        @Override
        public Beacon[] newArray(int size) {
            return new Beacon[size];
        }
    };
}