package com.example.passivelocationtester;

import java.sql.Date;
import java.util.ArrayList;

import com.google.android.gms.maps.model.MarkerOptions;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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



public class MarkerDBWorker{
    String TAG = "MarkerDBWorker";
    static final int getDBMarkerInfo = 1;

    Handler mainThreadHandler;
    Context mContext;
    Looper mServiceLooper;
    public Handler mServiceHandler;
    Messenger mMessenger;
    SQLiteDatabase mDB;
    long timeStart;
    long timeEnd;

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }


        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
            case getDBMarkerInfo:
                long ts = msg.getData().getLong(LFnC.WTGetDBMarkerTSKey);
                long te = msg.getData().getLong(LFnC.WTGetDBMarkerTEKey);
                go(ts, te);
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
        mServiceHandler = new ServiceHandler(mServiceLooper);

    }


    private void go(long ts, long te){
        timeStart = ts;
        timeEnd = te;
        run();
    }


    private void run() {
        String tag = "run";
        SQLiteDatabase db = new LocationDB(mContext).getReadableDatabase();
        Message m;

        long avgFixInterval = 0;
        if (db == null) {
            Log.e(TAG + tag, "addDBElementsToMap got null db!");
        }
        // mMap.setOnMapClickListener(this);
        Log.d(TAG + tag, "dbPath: " + db.getPath());
        Cursor mcurs = null;
        Log.d(TAG + tag, "got startTime(" + DateUtils.formatDateTime(mContext, timeStart, 0)
                         + ") and endTime(" + DateUtils.formatDateTime(mContext, timeEnd, 0) + ")");
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
        float Lat, Long, accuracy = -1;
        long date;
        String provider;
        MarkerOptions mo = new MarkerOptions();
        mo.draggable(false);

        long fixTimeTally = 0;
        long prevFixTime = 0;
        ArrayList<LocationInfo> markerInfoList = new ArrayList<LocationInfo>();
        int latInd = mcurs.getColumnIndexOrThrow(LocationDB.KEY_LAT);
        int longInd = mcurs.getColumnIndexOrThrow(LocationDB.KEY_LONG);
        int tsInd = mcurs.getColumnIndexOrThrow(LocationDB.KEY_DATE);
        int provInd = mcurs.getColumnIndexOrThrow(LocationDB.KEY_PROVIDER_TYPE);
        int accInd = mcurs.getColumnIndex(LocationDB.KEY_ACCURACY);
        try {
            while (!mcurs.isAfterLast()) {

                Lat = mcurs.getFloat(latInd);
                Long = mcurs.getFloat(longInd);
                if (accInd != -1) {
                    accuracy = mcurs.getFloat(accInd);
                }
                date = mcurs.getLong(tsInd);
                Log.d(TAG, "got date as: " + date);
                if (prevFixTime != 0) {
                    StringBuilder sb = new StringBuilder();
                    android.support.v4.util.TimeUtils.formatDuration(date - prevFixTime, sb);
                    Log.d(TAG + tag,
                          "timeDifference: " + (date - prevFixTime) + " format:" + sb.toString());

                    fixTimeTally += (date - prevFixTime);
                }
                prevFixTime = date;
                provider = mcurs.getString(provInd);
                String title = (new Date(date).toLocaleString());
                String snippet = "Accuracy: " + accuracy + " Provider: " + provider;

                // put the marker and accuracy together in the message
                Bundle b = new Bundle();
                b.putParcelable(LFnC.WThrdLocationInfoMessageKey, new LocationInfo(Lat, Long,
                                                                                   provider,
                                                                                   snippet, title,
                                                                                   accuracy));
                m = Message.obtain();
                m.what = MainActivityDBResultHandler.locInfoID;
                m.setData(b);
                mMessenger.send(m);
                // markerInfo.put(map.addMarker(mo).getId(), accuracy);
                mcurs.moveToNext();
            }

            Log.d(TAG + tag, "fixTimeTally: " + fixTimeTally + " count:" + mcurs.getPosition());
            avgFixInterval = fixTimeTally / mcurs.getPosition();
            StringBuilder sb = new StringBuilder();
            android.support.v4.util.TimeUtils.formatDuration(avgFixInterval, sb);
            String avgFixIntString = "Avg. Fix interval: " + sb.toString();
            String npTVString = "Total Number of Loc Requests: " + mcurs.getPosition();

            //initialize and send message containing
            Bundle b = new Bundle();
            b.putString(LFnC.WThrdAvgFixIntKey, avgFixIntString);
            b.putString(LFnC.WThrdNPString, npTVString);
            m = Message.obtain();
            m.what = MainActivityDBResultHandler.avgFixAndNumPointsIntID;
            m.setData(b);
            mMessenger.send(m);


        } catch (RemoteException e) {
            e.printStackTrace();
        } finally {
            db.close();
        }
        // TODO: move this to the MainActivityDBResultHandler
        // avgIntTv.setText(avgFixIntString);
        // npTv.setText(npTVString);
    }

}
