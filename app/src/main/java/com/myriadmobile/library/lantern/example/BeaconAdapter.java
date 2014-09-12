/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Myriad Mobile
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.myriadmobile.library.lantern.example;


import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.myriadmobile.library.lantern.Beacon;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

/**
 * Adapter to show the users beacons that have been detected.
 */
public class BeaconAdapter  extends BaseAdapter {

    private List<Beacon> data;
    private Context context;
    private int layoutResourceId;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm a");


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


            holder.tvMac = (TextView)convertView.findViewById(R.id.tv_beacon_mac);
            holder.tvDistance = (TextView)convertView.findViewById(R.id.tv_beacon_distance);
            holder.tvUuid = (TextView)convertView.findViewById(R.id.tv_beacon_uuid);
            holder.tvMinor = (TextView)convertView.findViewById(R.id.tv_beacon_minor);
            holder.tvMajor = (TextView)convertView.findViewById(R.id.tv_beacon_major);
            holder.tvRssi = (TextView)convertView.findViewById(R.id.tv_beacon_rssi);
            holder.tvExpiration = (TextView)convertView.findViewById(R.id.tv_beacon_expiration);
            holder.linearRoot = (LinearLayout)convertView.findViewById(R.id.lnr_beacon_root);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Calendar expireTime = Calendar.getInstance();

        expireTime.setTimeInMillis(beacon.getExpirationTime());

        holder.tvMac.setText(context.getString(R.string.mac) + beacon.getBluetoothAddress());
        holder.tvUuid.setText(context.getString(R.string.uuid) + beacon.getUuid());
        holder.tvDistance.setText(context.getString(R.string.proximity) + Beacon.proximityToString(beacon.getProximity()));
        holder.tvMinor.setText(context.getString(R.string.minor) + beacon.getMinor());
        holder.tvMajor.setText(context.getString(R.string.major) + beacon.getMajor());
        holder.tvRssi.setText(context.getString(R.string.rssi) + beacon.getRssi());
        holder.tvExpiration.setText(context.getString(R.string.expiration) + simpleDateFormat.format(expireTime.getTime()));

        switch (beacon.getProximity()) {
            case Beacon.PROXIMITY_UNKNOWN:
                holder.linearRoot.setBackgroundColor(context.getResources().getColor(R.color.beacon_unkown));
                break;
            case Beacon.PROXIMITY_FAR:
                holder.linearRoot.setBackgroundColor(context.getResources().getColor(R.color.beacon_far ));
                break;
            case Beacon.PROXIMITY_NEAR:
                holder.linearRoot.setBackgroundColor(context.getResources().getColor(R.color.beacon_near));
                break;
            case Beacon.PROXIMITY_IMMEDIATE:
                holder.linearRoot.setBackgroundColor(context.getResources().getColor(R.color.beacon_immediate));
                break;
        }

        return convertView;
    }

    private static class ViewHolder {
        LinearLayout linearRoot;
        TextView tvMac;
        TextView tvUuid;
        TextView tvDistance;
        TextView tvMinor;
        TextView tvMajor;
        TextView tvRssi;
        TextView tvExpiration;
    }


}