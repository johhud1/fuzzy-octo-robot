package com.example.passivelocationtester;

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
    static final int avgFixIntID = 2;
    static final int numPointsID = 3;

    String TAG = "MainActivityDBResultHandler";
    GoogleMap mMap;
    Activity mActivity;


    public MainActivityDBResultHandler(GoogleMap map, Activity a) {
        mMap = map;
        mActivity = a;
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
                MarkerOptions mo = new MarkerOptions();
                mo.position(new LatLng(Lat, Long));
                mo.title(locInfo.title);
                mo.snippet(locInfo.snippet);
                mMap.addMarker(mo);
            } else {
                Log.e(TAG + tag, "LocationInfo case, locInfo is null!");
            }
            break;
        case avgFixIntID:
            String avgFixIntString = b.getString(LFnC.WThrdAvgFixIntKey);
            if (avgFixIntString != null) {
                avgIntTv.setText(avgFixIntString);
            } else {
                Log.e(TAG + tag, "avgFixInterval case: data is null!");
            }
            break;
        case numPointsID:
            String npTVString = b.getString(LFnC.WThrdNPString);
            if (npTVString != null) {
                npTv.setText(npTVString);
            } else {
                Log.e(TAG + tag, "npString case. npTVString is null!");
            }
        }
    }
}
