package com.example.passivelocationtester;

public class LFnC {

    public static final String DBName = "passiveLocationDB";
    public static final String packageName = "passivelocationtester";
    public static final String WThrdLocationInfoMessageKey = "locationInfoArray";
    public static final String WThrdAvgFixIntKey = "WTAvgFixIntervalKey";
    public static final String WThrdNPString = "WTNumPointsStringKey";
    public static final String WTGetDBMarkerTSKey = "getMarkerTSKey";
    public static final String WTGetDBMarkerTEKey = "getMarkerTEKey";
    public static final String WTNoMarkerMergingKey = "noMarkerMergingKey";

    //constants that determing whether a marker is distant enough from previous to warrant a new marker
    public static final int isMovingSpeedThreshold = 1;   //speed (meter/sec) that if exceeded, new marker will be placed
    public static final int isMovingDistanceThreshold = 25; //distance (meters) that if exceeded will cause new marker to be placed

    public LocationDB db;

}
