package com.hudson.passivelocationmonitor;

import com.hudson.passivelocationmonitor.R;
import com.hudson.passivelocationmonitor.activities.TabHostActivity;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;



public class LocTrackerService extends Service implements LocationListener {
    private String TAG = "LocTrackerService";
    private LocationManager LM;
    private SQLiteDatabase mDB;
    private boolean running = false;
    private Context mContext = this;

    @Override
    public void onCreate() {

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (!running) {
            Log.d(TAG, "onStartCommand: service not running, starting it!");
            running = true;
            postServiceRunningNotification();
            // mServiceHandler.sendMessage(msg);
            LM = (LocationManager) getSystemService(LOCATION_SERVICE);
            //makeForeground();
            startPassiveLocService();
            //registerReceiver(new LocationServiceBootReceiver(), new IntentFilter(Intent.ACTION_BOOT_COMPLETED));

        } else {
            Log.d(TAG, "onStartCommand: service running, ignoring startCommand");
        }

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }


    private void postServiceRunningNotification() {
        int NOTIFICATION_ID = 4;
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification n = new Notification.Builder(this).setContentText("Passive Location Tracker Service on")
            .setContentText("Listening for location Requests").build();
        nm.notify(NOTIFICATION_ID, n);
    }


    private void startPassiveLocService() {
        LM.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0, 0, this);
    }


    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    private void makeForeground() {
        int NOTIFICATION_ID = 2;
        Notification notification =
            new Notification(R.drawable.ic_launcher, "starting passive monitoring",
                             System.currentTimeMillis());
        Intent notificationIntent = new Intent(this, TabHostActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
                                    | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notification.setLatestEventInfo(this, "Passive Location Tracker", "Tracking the trackers", pendingIntent);

        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
        int NOTIFICATION_ID = 3;
        Notification notification =
            new Notification.Builder(this).setContentTitle("Passive Location Tracker")
            .setContentText("passive location listener service turned off").build();
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID, notification);
    }

    @Override
    public void onLocationChanged(Location l) {
        mDB = new LocationDB(this).getWritableDatabase();
        if(mDB.isDbLockedByOtherThreads()){
            Log.d(TAG, "onLocationChanged: db locked by other thread!");
        }
        Log.d(TAG, "onLocationChanged: adding location:"+l.toString()+" to db. dbpath: "+mDB.getPath());
        ContentValues cv = new ContentValues();
        cv.put(LocationDB.KEY_LAT, l.getLatitude());
        cv.put(LocationDB.KEY_LONG, l.getLongitude());
        cv.put(LocationDB.KEY_PROVIDER_TYPE, l.getProvider());
        cv.put(LocationDB.KEY_ACCURACY, l.getAccuracy());
        cv.put(LocationDB.KEY_DATE, l.getTime());
        mDB.beginTransaction();
        try {
            mDB.insert(LocationDB.LOCATION_TABLE_NAME, null, cv);
            mDB.setTransactionSuccessful();
            Log.d(TAG, "onLocationChanged: transaction complete, successful");
        }
        finally {
            mDB.endTransaction();
        }
        mDB.close();
    }


    @Override
    public void onProviderDisabled(String arg0) {
        // TODO Auto-generated method stub

    }


    @Override
    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub

    }


    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Toast.makeText(this, "status of location provider(" + provider + ") changed",
                       Toast.LENGTH_LONG).show();

    }

}
