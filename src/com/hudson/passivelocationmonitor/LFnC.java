package com.hudson.passivelocationmonitor;

public class LFnC {

    public static final long SECOND = 1000;
    public static final long MINUTE = 60 * SECOND;
    public static final long HOUR = 60 * MINUTE;
    public static final long DAY = 24 * HOUR;

    public static final String DBName = "passiveLocationDB";
    public static final String packageName = "passivelocationtester";

    //keys for messages communicating with worker thread
    public static final String WThrdLocationInfoMessageKey = "locationInfoArray";
    public static final String WThrdAvgFixIntKey = "WTAvgFixIntervalKey";
    public static final String WThrdNPString = "WTNumPointsStringKey";
    public static final String WTGetDBMarkerTSKey = "getMarkerTSKey";
    public static final String WTGetDBMarkerTEKey = "getMarkerTEKey";
    public static final String WTNoMarkerMergingKey = "noMarkerMergingKey";
    public static final String tsKey = "startTimeStamp";
    public static final String teKey = "endTimeStamp";

    //keys for accessing sharedPreference values
    public static final String PREF_KEY = "prefs";
    public static final String PREF_FIRST_TIME_MAIN_ACTIVITY_KEY = "firsttimemain";
    public static final String PREF_FIRST_TIME_RANGE_ACTIVITY_KEY = "firsttimerange";
    public static final String PREF_MARKER_START_KEY = "startshowmarkerprefkey";
    public static final String PREF_MARKER_END_KEY = "endshowmarkerpefkey";

    //constants that determing whether a marker is distant enough from previous to warrant a new marker
    public static final int isMovingSpeedThreshold = 1;   //speed (meter/sec) that if exceeded, new marker will be placed
    public static final int isMovingDistanceThreshold = 25; //distance (meters) that if exceeded will cause new marker to be placed


    public LocationDB db;

}
