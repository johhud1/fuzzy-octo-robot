package com.example.passivelocationtester;

import java.lang.ref.WeakReference;
import java.sql.Date;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.os.RemoteException;
import android.text.format.DateUtils;
import android.util.Log;

import com.google.android.gms.maps.model.MarkerOptions;



public class MarkerDBWorker{
    static String TAG = "MarkerDBWorker";
    static final int getDBMarkerInfo = 1;

    public Handler mainThreadHandler;
    public Context mContext;
    Looper mServiceLooper;
    public Handler mServiceHandler;
    public Messenger mMessenger;
    SQLiteDatabase mDB;

    private final static class ServiceHandler extends Handler {
        WeakReference<MarkerDBWorker> outerClass;
        public ServiceHandler(Looper looper, WeakReference<MarkerDBWorker> outerClass) {
            super(looper);
            this.outerClass = outerClass;
        }


        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
            case getDBMarkerInfo:
                long ts = msg.getData().getLong(LFnC.WTGetDBMarkerTSKey);
                long te = msg.getData().getLong(LFnC.WTGetDBMarkerTEKey);
                boolean noMarkerMerge = msg.getData().getBoolean(LFnC.WTNoMarkerMergingKey);
                run(ts, te, noMarkerMerge, outerClass);
            }
        }
    }


    // this class will perform the necessary db query and location set analysis,
    // finally sending a message containing the set of markers to the
    // MainActivityDBResultHandler
    // for placement on the map in the mainactivity.
    public MarkerDBWorker(Handler mainHandler, Context c) {

        mContext = c;
        mMessenger = new Messenger(mainHandler);
        // Handler that receives messages from the thread

        // Start up the thread running the worker. Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block. We also make it
        // background priority so CPU-intensive work will not disrupt our UI.


        // Get the HandlerThread's Looper and use it for our Handler
        HandlerThread thread = new HandlerThread("PassiveLocationDBMarkerWorker",
                                           Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper, new WeakReference<MarkerDBWorker>(this));

    }


    private static void run(long timeStart, long timeEnd, boolean noMarkerMerge, WeakReference<MarkerDBWorker> thisClass) {
        String tag = ":run";
        Log.d(TAG+tag, "noMarkerMerge: "+noMarkerMerge);
        SQLiteDatabase db = new LocationDB(thisClass.get().mContext).getReadableDatabase();
        Message m;

        long avgFixInterval = 0;
        if (db == null) {
            Log.e(TAG + tag, "addDBElementsToMap got null db!");
        }
        // mMap.setOnMapClickListener(this);
        Log.d(TAG + tag, "dbPath: " + db.getPath());
        Cursor mcurs = null;
        Log.d(TAG + tag, "got startTime(" + DateUtils.formatDateTime(thisClass.get().mContext, timeStart, 0)
                         + ") and endTime(" + DateUtils.formatDateTime(thisClass.get().mContext, timeEnd, 0) + ")");
        if (timeStart == 0) {
            mcurs =
                db.query(LocationDB.LOCATION_TABLE_NAME, null, LocationDB.KEY_DATE + " < "
                                                               + timeEnd, null, null, null,
                         LocationDB.KEY_DATE + " ASC");
        } else {
            mcurs =
                db.query(LocationDB.LOCATION_TABLE_NAME, null, LocationDB.KEY_DATE + " BETWEEN "
                                                               + timeStart + " AND " + timeEnd,
                         null, null, null, LocationDB.KEY_DATE + " ASC");
        }
        if (!mcurs.moveToFirst()) {
            Log.d(TAG + tag, "addDBElementsToMap; cursor is empty (is the DB emtpy?)");
            return;
        }

        long date;
        MarkerOptions mo = new MarkerOptions();
        mo.draggable(false);
        long intFixTimeTally = 0;
        long prevFixTime = 0;
        LocationInfo prevLoc = null;
        long aLocDuration = 0;
        int latInd = mcurs.getColumnIndexOrThrow(LocationDB.KEY_LAT);
        int longInd = mcurs.getColumnIndexOrThrow(LocationDB.KEY_LONG);
        int tsInd = mcurs.getColumnIndexOrThrow(LocationDB.KEY_DATE);
        int provInd = mcurs.getColumnIndexOrThrow(LocationDB.KEY_PROVIDER_TYPE);
        int accInd = mcurs.getColumnIndex(LocationDB.KEY_ACCURACY);
        int markerCount = 0;
        LocationInfo firstLoc = NewLocInfoFromDB(mcurs, latInd, longInd, tsInd, provInd, accInd);
        try {
            while (!mcurs.isAfterLast()) {
                LocationInfo candidateLoc = NewLocInfoFromDB(mcurs, latInd, longInd, tsInd, provInd, accInd);
//                Lat = mcurs.getFloat(latInd);
//                Long = mcurs.getFloat(longInd);
//                if (accInd != -1) {
//                    accuracy = mcurs.getFloat(accInd);
//                }
                date = mcurs.getLong(tsInd);
                long timeGap = 0;
                Log.d(TAG, "got date as: " + date);
                if (prevFixTime != 0) {
                    timeGap = (date - prevFixTime);
                    StringBuilder sb = new StringBuilder();
                    android.support.v4.util.TimeUtils.formatDuration(date - prevFixTime, sb);
                    Log.d(TAG + tag,
                          "timeDifference: " + (date - prevFixTime) + " format:" + sb.toString());

                    intFixTimeTally += timeGap;
                    aLocDuration += timeGap;
                }

//                provider = mcurs.getString(provInd);
//                String title = (new Date(date).toLocaleString());
//                String snippet = "Accuracy: " + accuracy + " Provider: " + provider;


                if(isMoving(candidateLoc, prevLoc, timeGap) || noMarkerMerge || mcurs.isLast()){
                    // put the marker and accuracy together in the message
                    Bundle b = new Bundle();
                    firstLoc.markersMerged = markerCount;
                    firstLoc.setDuration(aLocDuration);
                    b.putParcelable(LFnC.WThrdLocationInfoMessageKey, firstLoc);
                    Log.d(TAG, " sending location marker info with LocationInfo: "+firstLoc.toString());
                    m = Message.obtain();
                    m.what = MainActivityDBResultHandler.locInfoID;
                    m.setData(b);
                    thisClass.get().mMessenger.send(m);
                    // markerInfo.put(map.addMarker(mo).getId(), accuracy);
                    aLocDuration = 0;
                    markerCount = -1;
                    firstLoc = candidateLoc;
                }
                markerCount++;
                prevFixTime = date;
                prevLoc = candidateLoc;
                mcurs.moveToNext();
            }

            Log.d(TAG + tag, "fixTimeTally: " + intFixTimeTally + " count:" + mcurs.getPosition());
            avgFixInterval = intFixTimeTally / mcurs.getPosition();
            StringBuilder sb = new StringBuilder();
            android.support.v4.util.TimeUtils.formatDuration(avgFixInterval, sb);
            String avgFixIntString = thisClass.get().mContext.getString(R.string.avg_fig_int_prefix) + sb.toString();
            String npTVString = thisClass.get().mContext.getString(R.string.number_of_pings_prefix) + mcurs.getPosition();

            //initialize and send message containing
            Bundle b = new Bundle();
            b.putString(LFnC.WThrdAvgFixIntKey, avgFixIntString);
            b.putString(LFnC.WThrdNPString, npTVString);
            m = Message.obtain();
            m.what = MainActivityDBResultHandler.avgFixAndNumPointsIntID;
            m.setData(b);
            thisClass.get().mMessenger.send(m);


        } catch (RemoteException e) {
            e.printStackTrace();
        } finally {
            db.close();
        }
    }


    private static LocationInfo NewLocInfoFromDB(Cursor mcurs, int latInd, int longInd, int tsInd, int provInd, int accInd) {
        float Lat, Long, accuracy = -1;
        long date = mcurs.getLong(tsInd);
        Lat = mcurs.getFloat(latInd);
        Long = mcurs.getFloat(longInd);
        String provider = mcurs.getString(provInd);
        String title = (new Date(date).toLocaleString());
        if (accInd != -1) {
            accuracy = mcurs.getFloat(accInd);
        }
        String snippet = "Accuracy: " + accuracy + " Provider: " + provider;
        return new LocationInfo(Lat, Long,
                         provider,
                         snippet, title,
                         accuracy);
    }


    private static boolean isMoving(LocationInfo pos, LocationInfo prevLoc, long timeGap) {
        if (prevLoc==null){
            return false;
        }

        float[] results = new float[3];
        Location.distanceBetween(pos.lat, pos.lng, prevLoc.lat, prevLoc.lng, results); //meters distance held in results[0]
        //if velocity is over 1 m/s or distance difference > 25 than 'its moving'
        if (results[0] < LFnC.isMovingDistanceThreshold){
            return false;
        } else {
            return true;
        }
    }

}
