package com.example.passivelocationtester;

import android.os.Parcel;
import android.os.Parcelable;



public class LocationInfo implements Parcelable {


    String title;       //index 0
    String snippet;     //index 1
    String provider;    //index 2
    float accuracy;     //index 3
    float lat;          //index 4
    float lng;          //index 5
    long duration;
    int markersMerged;

    private LocationInfo(Parcel in) {
        in.setDataPosition(0);
        String[] strs = new String[3];
        float[] flts = new float[3];
        in.readStringArray(strs);
        in.readFloatArray(flts);
        title = strs[0];
        snippet = strs[1];
        provider = strs[2];
        accuracy = flts[0];
        lat = flts[1];
        lng = flts[2];
        duration = in.readLong();
        markersMerged = in.readInt();
    }

    @Override
    public String toString(){
        return "{title: "+title+", snippet: "+snippet+", provider: "+provider+
            ", accuracy: "+accuracy+", lat: "+lat+"lng: "+lng+", duration: "+duration+", markersMerged: "+markersMerged+"}";
    }

    @Override
    public int describeContents() {
        // TODO Auto-generated method stub
        return 0;
    }

    public static final Parcelable.Creator<LocationInfo> CREATOR =
        new Parcelable.Creator<LocationInfo>() {
            public LocationInfo createFromParcel(Parcel in) {
                return new LocationInfo(in);
            }


            public LocationInfo[] newArray(int size) {
                return new LocationInfo[size];
            }
        };


    @Override
    public void writeToParcel(Parcel out, int arg1) {
        String[] strs = new String[3];
        float[] flts = new float[3];
        strs[0] = title;
        strs[1] = snippet;
        strs[2] = provider;
        flts[0] = accuracy;
        flts[1] = lat;
        flts[2] = lng;
        out.writeStringArray(strs);
        out.writeFloatArray(flts);
        out.writeLong(duration);
        out.writeInt(markersMerged);
    }

    public void setDuration(long d){
        duration = d;
    }

    public LocationInfo(float lat, float lng, String p, String snip, String t, float acc){
        constructor(lat, lng, p, snip, t, acc, (long)0, 0);
    }
    public LocationInfo(float lat, float lng, String p, String snip, String t, float acc, long d){
        constructor(lat, lng, p, snip, t, acc, d, 0);
    }
    public LocationInfo(float lat, float lng, String p, String snip, String t, float acc, long d, int mm){
        constructor(lat, lng, p, snip, t, acc, d, mm);
    }

    private void constructor(float lat, float lng, String p, String snip, String t, float acc, long d, int markersMerged){
        this.lat = lat;
        this.lng = lng;
        provider = p;
        snippet = snip;
        title = t;
        accuracy = acc;
        duration = d;
        this.markersMerged = markersMerged;
    }

}
