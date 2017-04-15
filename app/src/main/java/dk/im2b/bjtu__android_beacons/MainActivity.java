package dk.im2b.bjtu__android_beacons;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener,
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


    private String TAG = "MainActivity";

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


        // Make first beacon for the database
        MyBeacon beacon = new MyBeacon("FIRST", 0, 0, 0, 0, 0, 0);
        beacon.save();
        // Remove it again
        MyBeacon.delete(beacon);


        // Map
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        createLocationRequest();

        // Beacon ListView
        lvBeacon = (ListView) findViewById(R.id.activityMain_ListviewBeacon);
        beaconAdapter = new BeaconDetailsAdapter(MainActivity.this, new ArrayList<Beacon>());
        lvBeacon.setAdapter(beaconAdapter);

        //  Listening for beacons
        beaconManager = new BeaconManager(MainActivity.this);
        region = new Region("ranged region", null, null, null);

        // iBeacon
        beaconManager.setRangingListener(new BeaconManager.RangingListener() {
            @Override
            public void onBeaconsDiscovered(Region region, List<Beacon> list) {
                // Check if beacon is in list (have been registered)
                for (Beacon b : list) {
                    Log.d(TAG, "beacon Nearby!!!! : " + b);
                    checkDatabase(b);
                }
                // update beacon adapter
                beaconAdapter.updateItems(list);



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
        MyBeacon beacon = null;
        try {
/*
            beacon = Select.from(MyBeacon.class)
                    .where(Condition.prop("proximityUUID").eq(b.getProximityUUID().toString()),
                            Condition.prop("minor").eq(b.getMinor()),
                            Condition.prop("major").eq(b.getMajor()))
                    .first();
*/
            beacon = MyBeacon.find(MyBeacon.class, "uuid = ? and minor = ? and major = ?", b.getProximityUUID().toString(), "" + b.getMinor(), "" + b.getMajor()).get(0);
            //Toast.makeText(this, ""+beacon, Toast.LENGTH_SHORT).show();
        } catch (Exception ex) {
            beacon = null;
        }
        if (mCurrentLocation != null) {

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
                }
            }
        }
        addAndUpdateBeaconsOnMap();
    }

    private void addAndUpdateBeaconsOnMap() {
        mMap.clear();

        List<MyBeacon> beacons = MyBeacon.listAll(MyBeacon.class);
        for (MyBeacon beacon : beacons) {
            LatLng position = new LatLng(beacon.getLatitude(), beacon.getLongitude());
            mMap.addMarker(new MarkerOptions().position(position)
                    .title("Distance: " + String.format("%.2f", calcDistance(beacon.getRssi(), beacon.getMeasuredPower())) + " meters"));
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


    /*** Menu ***/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.my_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.database_log:
                Intent intent = new Intent(MainActivity.this, DatabaseActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
