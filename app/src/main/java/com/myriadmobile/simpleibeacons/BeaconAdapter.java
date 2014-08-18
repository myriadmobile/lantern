package com.myriadmobile.simpleibeacons;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.myriadmobile.simpleibeacons.Beacon;

import java.util.List;

/**
 * Created by lumis on 8/15/2014.
 */
public class BeaconAdapter  extends BaseAdapter {

    private List<Beacon> data;
    private Context context;
    private int layoutResourceId;

    public BeaconAdapter(Context context, int layoutResourceId, List<Beacon> data) {
        this.context = context;
        this.data = data;
        this.layoutResourceId = layoutResourceId;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Beacon getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        final ViewHolder holder;

        Beacon beacon = data.get(position);

        if (convertView == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();

            convertView = inflater.inflate(layoutResourceId, viewGroup, false);
            holder = new ViewHolder();


            holder.mac = (TextView)convertView.findViewById(R.id.tv_beacon_mac);
            holder.distance = (TextView)convertView.findViewById(R.id.beacon_distance);
            holder.uuid = (TextView)convertView.findViewById(R.id.tv_beacon_uuid);
            holder.minor = (TextView)convertView.findViewById(R.id.beacon_minor);
            holder.major = (TextView)convertView.findViewById(R.id.beacon_major);
            holder.rssi = (TextView)convertView.findViewById(R.id.beacon_rssi);

            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.mac.setText("Mac: " + beacon.getBluetoothAddress());
        holder.uuid.setText("UUID: " + beacon.getUuid());
        holder.distance.setText("Promimity: " + Beacon.proximityToString(beacon.getProximity()));
        holder.minor.setText("Minor: " + beacon.getMinor());
        holder.major.setText("Major: " + beacon.getMajor());
        holder.rssi.setText("RSSI: " + beacon.getRssi());

        return convertView;
    }

    private static class ViewHolder {
        TextView mac;
        TextView uuid;
        TextView distance;
        TextView minor;
        TextView major;
        TextView rssi;
    }


}
