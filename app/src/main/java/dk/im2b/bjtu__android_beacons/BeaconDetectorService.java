package dk.im2b.bjtu__android_beacons;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.List;

public class BeaconDetectorService extends Service implements LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    // Location
    Location mCurrentLocation;
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    private static final long INTERVAL = 1000 * 10;
    private static final long FASTEST_INTERVAL = 1000 * 5;

    // Beacon
    private BeaconManager beaconManager;
    private Region region;

    private String TAG = "BeaconDetectorService";


    public BeaconDetectorService() {
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // Location Listener
        createLocationRequest();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mGoogleApiClient.connect();

        //  Listening for beacons
        beaconManager = new BeaconManager(getApplicationContext());
        region = new Region("ranged region", null, null, null);

        // iBeacon
        beaconManager.setRangingListener(new BeaconManager.RangingListener() {
            @Override
            public void onBeaconsDiscovered(Region region, List<Beacon> list) {
                // Check if beacon is in list (have been registered)
                Log.d(TAG, "Checking Database");
                for (Beacon b : list) {
                    Log.d(TAG, "beacon Nearby!!!! : " + b);
                    checkDatabase(b);
                }
                MyApp.beaconList = list;
            }
        });

        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                beaconManager.startRanging(region);
            }
        });



        Log.d(TAG, "Service started");

        // Let it continue running until it is stopped.
        //Toast.makeText(this, "Location Service Started", Toast.LENGTH_LONG).show();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        beaconManager.stopRanging(region);
        beaconManager.disconnect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        PendingResult<Status> pendingResult = LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


    @Override
    public void onLocationChanged(Location location) {
        if (location.getAccuracy() < 20) {
            mCurrentLocation = location;
        }
    }

    private void checkDatabase(Beacon b) {
        // checks if a Beacon is pressent in the database, if not it will add it
        MyBeacon beacon = null;
        try {
            beacon = MyBeacon.find(MyBeacon.class, "uuid = ? and minor = ? and major = ?", b.getProximityUUID().toString(), "" + b.getMinor(), "" + b.getMajor()).get(0);
            //Toast.makeText(this, ""+beacon, Toast.LENGTH_SHORT).show();
        } catch (Exception ex) {
            beacon = null;
        }
        if (mCurrentLocation != null) {
            //Toast.makeText(this, "Is null", Toast.LENGTH_SHORT).show();
            if (beacon == null) {
                beacon = new MyBeacon(b.getProximityUUID().toString(), b.getMajor(), b.getMinor(), b.getRssi(), b.getMeasuredPower(), mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
                beacon.save();

            } else { // If the beacon is pressent - check if we are closer now and update the entry if nessaserry
                double myDistance = calcDistance(beacon.getRssi(), beacon.getMeasuredPower());
                double newDistance = calcDistance(b.getRssi(), b.getMeasuredPower());

                // if we are closer, update the entry in the database and on the map
                if (newDistance < myDistance) {
                    beacon.setMeasuredPower(b.getMeasuredPower());
                    beacon.setRssi(b.getRssi());
                    beacon.setLatitude(mCurrentLocation.getLatitude());
                    beacon.setLongitude(mCurrentLocation.getLongitude());
                    beacon.save();
                   // Toast.makeText(this, "works!", Toast.LENGTH_SHORT).show();

                }
            }
            // Send broadcast that will update UI Map
            sendBroadcast();
        }
    }

    private double calcDistance(int rssi, int txPower) {
    /*
     * RSSI = TxPower - 10 * n * lg(d)
     * n = 2 (in free space)
     *
     * d = 10 ^ ((TxPower - RSSI) / (10 * n))
     */

        return Math.pow(10d, ((double) txPower - rssi) / (10 * 2));
    }

    private void sendBroadcast(){
        // local broadcast - information about activity for view
        Intent intentForBroadcast = new Intent("MapUpdate");
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intentForBroadcast);

    }

}