package dk.im2b.bjtu__android_beacons;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import java.util.Iterator;
import java.util.List;

public class DatabaseActivity extends AppCompatActivity {


    private TextView tvDisplay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_database);

        tvDisplay = (TextView) findViewById(R.id.dbActivity_tvDisplay);

        Iterator<MyBeacon> myBeaconList = MyBeacon.findAll(MyBeacon.class);

        String text = "";
        while(myBeaconList.hasNext()){
            MyBeacon beacon = myBeaconList.next();
            text += "UUID: "+ beacon.getProximityUUID()+ " Major: "+ beacon.getMajor() + " Minor: "+beacon.getMinor()+ " ID: " + beacon.getId() + " \n\n";
        }
        tvDisplay.setText(text);
    }
}
