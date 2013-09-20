package com.example.passivelocationtester;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import android.app.Activity;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;



public class MainActivityDBResultHandler extends Handler {

    static final int locInfoID = 1;
    static final int avgFixAndNumPointsIntID = 2;

    String TAG = "MainActivityDBResultHandler";
    GoogleMap mMap;
    Activity mActivity;
    HashMap<String, Float> markerIDToAccuracyHM;


    public MainActivityDBResultHandler(GoogleMap map, HashMap<String, Float> markerIDToAccuracyHM, Activity a) {
        mMap = map;
        mActivity = a;
        this.markerIDToAccuracyHM = markerIDToAccuracyHM;
    }


    public void handleMessage(Message msg) {
        String tag = ":handleMessage";
        TextView npTv = (TextView) mActivity.findViewById(R.id.NumberOfPingsTV);
        TextView avgIntTv = (TextView) mActivity.findViewById(R.id.TotalAvgLocationIntervalTV);

        // data should be a bundle containing the parcelable class LocationInfo,
        // or string containing avgInterval or number of points
        Bundle b = msg.getData();

        // get the data out, check what it is, and take appropriate action
        switch (msg.what) {
        case locInfoID:
            LocationInfo locInfo = b.getParcelable(LFnC.WThrdLocationInfoMessageKey);
            if (locInfo != null) {
                Log.d(TAG + tag, "got LocationInfo with values: {lat:" + locInfo.lat + " lng:"
                                 + locInfo.lng + " accuracy:" + locInfo.accuracy + " title:"
                                 + locInfo.title + " snippet:" + locInfo.snippet + " provider:"
                                 + locInfo.provider);
                float Lat = locInfo.lat;
                float Long = locInfo.lng;
                float acc = locInfo.accuracy;
                long duration = locInfo.duration;
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                String locDuration = sdf.format(new Date(duration - TimeZone.getDefault().getRawOffset()));
                Log.d(TAG+tag, "Duration: " + locDuration);
                MarkerOptions mo = new MarkerOptions();
                mo.position(new LatLng(Lat, Long));
                mo.title(locInfo.title + " Duration:"+locDuration);
                mo.snippet(locInfo.snippet+" Merged:"+locInfo.markersMerged);
                String id = mMap.addMarker(mo).getId();
                Log.d(TAG+tag, "added marker to IDtoAccuracy HashMap with id:"+id);
                markerIDToAccuracyHM.put(id, acc);
            } else {
                Log.e(TAG + tag, "LocationInfo case, locInfo is null!");
            }
            break;
        case avgFixAndNumPointsIntID:
            String avgFixIntString = b.getString(LFnC.WThrdAvgFixIntKey);
            if (avgFixIntString != null) {
                avgIntTv.setText(avgFixIntString);
            } else {
                Log.e(TAG + tag, "avgFixInterval case: data is null!");
            }
            String npTVString = b.getString(LFnC.WThrdNPString);
            if (npTVString != null) {
                npTv.setText(npTVString);
            } else {
                Log.e(TAG + tag, "npString case. npTVString is null!");
            }
            break;
        }
    }
}
