package dk.im2b.bjtu__android_beacons;

import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.Toast;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.SystemRequirementsChecker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.orm.query.Condition;
import com.orm.query.Select;

import java.util.List;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    // Map
    private GoogleMap mMap;
    Location mCurrentLocation;
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    private static final long INTERVAL = 1000 * 10;
    private static final long FASTEST_INTERVAL = 1000 * 5;


    // Beacon
    private BeaconManager beaconManager;
    private Region region;


    private String TAG = "MAIN";

    ListView lvBeacon;
    BeaconDetailsAdapter beaconAdapter;

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //MyBeacon beacon = new MyBeacon("",0,0,0,0,0,0);
        //beacon.save();

        // Map
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        createLocationRequest();

        // Beacon ListView && Listening for beacons
        lvBeacon = (ListView) findViewById(R.id.activityMain_ListviewBeacon);
        beaconManager = new BeaconManager(MainActivity.this);
        region = new Region("ranged region", null, null, null);

        // iBeacon
        beaconManager.setRangingListener(new BeaconManager.RangingListener() {
            @Override
            public void onBeaconsDiscovered(Region region, List<Beacon> list) {
                if (!list.isEmpty()) {
                    // Check if beacon is in list (have been registered)
                    for (Beacon b : list) {
                        Log.d(TAG, "beacon Nearby!!!! : " + b);
                        checkDatabase(b);
                    }
                    beaconAdapter = new BeaconDetailsAdapter(MainActivity.this, list);
                    lvBeacon.setAdapter(beaconAdapter);

                }
            }
        });

        beaconManager.startRanging(region);

        // Location Listener
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mGoogleApiClient.connect();
    }

    private void checkDatabase(Beacon b) {
        // checks if a Beacon is pressent in the database, if not it will add it
        MyBeacon beacon = Select.from(MyBeacon.class)
                .where(Condition.prop("proximityUUID").eq(b.getProximityUUID().toString()),
                        Condition.prop("minor").eq(b.getMinor()),
                        Condition.prop("major").eq(b.getMajor()))
                .first();
        if (beacon == null){
            beacon = new MyBeacon(b.getProximityUUID().toString(), b.getMajor(), b.getMinor(), b.getRssi(), b.getMeasuredPower(), mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
            beacon.save();

        } else { // If the beacon is pressent - check if we are closer now and update the entry if nessaserry
            double myDistance = calcDistance(beacon.getRssi(), beacon.getMeasuredPower());
            double newDistance = calcDistance(b.getRssi(), b.getMeasuredPower());

            // if we are closer, update the entry in the database and on the map
            if (newDistance < myDistance){
                beacon.setMeasuredPower(b.getMeasuredPower());
                beacon.setRssi(b.getRssi());
                beacon.setLatitude(mCurrentLocation.getLatitude());
                beacon.setLongitude(mCurrentLocation.getLongitude());
                beacon.save();

                addAndUpdateBeaconsOnMap();
            }
        }
    }

    private void addAndUpdateBeaconsOnMap() {
        mMap.clear();

        List<MyBeacon> beacons = MyBeacon.listAll(MyBeacon.class);
        for (MyBeacon beacon : beacons){
            LatLng position = new LatLng(beacon.getLatitude(),beacon.getLongitude());
            mMap.addMarker(new MarkerOptions().position(position)
                    .title(beacon.getProximityUUID()));
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

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
        mMap.setMyLocationEnabled(true);
        addAndUpdateBeaconsOnMap();
    }

    @Override
    protected void onResume() {
        super.onResume();

        SystemRequirementsChecker.checkWithDefaultDialogs(this);
        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                beaconManager.startRanging(region);
            }
        });
    }


    @Override
    protected void onStop() {
        super.onStop();
        beaconManager.stopRanging(region);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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

            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(latitude, longitude)));
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


}
