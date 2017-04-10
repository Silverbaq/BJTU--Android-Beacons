package dk.im2b.bjtu__android_beacons;

import com.orm.SugarRecord;

/**
 * Created by silverbaq on 3/20/17.
 */

public class MyBeacon extends SugarRecord {

    // Beacon information
    private String uuid;
    private int major;
    private int minor;
    private int rssi;
    private int measuredPower;

    // Beacon possition
    private double latitude;
    private double longitude;


    public MyBeacon() {
    }

    public String getProximityUUID() {
        return uuid;
    }

    public MyBeacon(String proximityUUID, int major, int minor, int rssi, int mesuredPower, double latitude, double longitude){
        this.setProximityUUID(proximityUUID);
        this.setMajor(major);
        this.setMinor(minor);
        this.setRssi(rssi);
        this.setMeasuredPower(mesuredPower);
        this.setLatitude(latitude);
        this.setLongitude(longitude);
    }

    public void setProximityUUID(String proximityUUID) {
        this.uuid = proximityUUID;
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

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public int getMeasuredPower() {
        return measuredPower;
    }

    public void setMeasuredPower(int measuredPower) {
        this.measuredPower = measuredPower;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}
