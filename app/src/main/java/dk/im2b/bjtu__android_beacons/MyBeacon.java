package dk.im2b.bjtu__android_beacons;

import com.orm.SugarRecord;

/**
 * Created by silverbaq on 3/20/17.
 */

public class MyBeacon extends SugarRecord {

    private String proximityUUID;
    private int major;
    private int minor;
    private int rssi;
    private int measuredPower;

    public MyBeacon() {
    }

    public MyBeacon(String proximityUUID, int major, int minor, int rssi, int mesuredPower){
        this.setProximityUUID(proximityUUID);
        this.setMajor(major);
        this.setMinor(minor);
        this.setRssi(rssi);
        this.setMeasuredPower(mesuredPower);
    }

    public String getProximityUUID() {
        return proximityUUID;
    }

    public void setProximityUUID(String proximityUUID) {
        this.proximityUUID = proximityUUID;
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
}
