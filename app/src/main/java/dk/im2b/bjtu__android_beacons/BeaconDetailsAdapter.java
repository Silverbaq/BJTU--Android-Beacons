package dk.im2b.bjtu__android_beacons;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.estimote.sdk.Beacon;

import java.util.List;

/**
 * Created by silverbaq on 8/6/16.
 */
public class BeaconDetailsAdapter extends BaseAdapter {

    Activity mActivity;
    List<Beacon> beacon;

    public BeaconDetailsAdapter(Activity activity, List<Beacon> beaconList) {
        this.mActivity = activity;
        this.beacon = beaconList;
    }

    @Override
    public int getCount() {
        return beacon.size();
    }

    @Override
    public Object getItem(int i) {
        return beacon.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    private static class ViewHolder {
        TextView tvRSSI, tvTX, tvDistance, tvUUID, tvMajor, tvMinor;

    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder holder;

        if (view == null) {
            view = mActivity.getLayoutInflater().inflate(R.layout.adapter_beacondetails, viewGroup, false);

            holder = new ViewHolder();
            holder.tvRSSI = (TextView) view.findViewById(R.id.adapter_beacondetails_tvRSSI);
            holder.tvTX = (TextView) view.findViewById(R.id.adapter_beacondetails_tvTX);
            holder.tvDistance = (TextView) view.findViewById(R.id.adapter_beacondetails_tvDistance);
            holder.tvMajor = (TextView) view.findViewById(R.id.adapter_beacondetails_tvMajor);
            holder.tvMinor = (TextView) view.findViewById(R.id.adapter_beacondetails_tvMinor);
            holder.tvUUID = (TextView) view.findViewById(R.id.adapter_beacondetails_tvUUID);

            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        Beacon b = (Beacon) getItem(i);
        holder.tvUUID.setText(""+b.getProximityUUID().toString());
        holder.tvMajor.setText(""+b.getMajor());
        holder.tvMinor.setText(""+b.getMinor());
        holder.tvRSSI.setText(""+b.getRssi());
        holder.tvTX.setText(""+b.getMeasuredPower());
        holder.tvDistance.setText(String.format("%.2f", getDistance(b.getRssi(), b.getMeasuredPower())));

        return view;
    }

    double getDistance(int rssi, int txPower) {
    /*
     * RSSI = TxPower - 10 * n * lg(d)
     * n = 2 (in free space)
     *
     * d = 10 ^ ((TxPower - RSSI) / (10 * n))
     */

        return Math.pow(10d, ((double) txPower - rssi) / (10 * 2));
    }


}
