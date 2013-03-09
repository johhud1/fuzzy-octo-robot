package com.example.passivelocationtester;

import java.sql.Date;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.util.TimeFormatException;
import android.util.TimeUtils;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CameraPositionCreator;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;



public class MainActivity extends Activity implements OnMarkerClickListener,
    OnSeekBarChangeListener, LocationListener {
    Context mContext = this;
    LocationDB mLocDB;
    SQLiteDatabase mDB;
    int mDefaultPastLocProgress = 10;
    GoogleMap mMap;
    MainActivityDBResultHandler mHandler;
    MarkerDBWorker mDBWorker;
    Circle mCurCircle;
    String TAG = "MainActivity";
    private HashMap<String, Float> markerIDToAccuracyHM = new HashMap<String, Float>();
    protected boolean mShowAllLocationMarkers = false;
    protected boolean mNoMarkerMerge = false;
    long mStartShowingLocTime = 0;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupShowAllLocationsButton();
        setupShowMarkerSeekbar();
        setupNoMarkerMergeButton();
        SharedPreferences mPrefs = getSharedPreferences("prefs", MODE_PRIVATE);

        startService(new Intent().setClass(this, LocTrackerService.class));

        MapFragment mfrag = (MapFragment) getFragmentManager().findFragmentById(R.id.mainMapFrag);
        mMap = mfrag.getMap();
        mHandler = new MainActivityDBResultHandler(mMap, markerIDToAccuracyHM, this);
        mDBWorker = new MarkerDBWorker(mHandler, this);

        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        lm.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, this, null);

        mMap.setOnMarkerClickListener(this);
        if (mPrefs.getBoolean("first", true)) {
            mPrefs.edit().putBoolean("first", false);
        } else {

        }

    }


    private void setupNoMarkerMergeButton() {
        CheckBox b = (CheckBox) findViewById(R.id.noMarkerMergeCB);
        b.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mNoMarkerMerge = true;
                } else {
                    mNoMarkerMerge = false;
                }

            }
        });

    }


    private void setupShowMarkerSeekbar() {
        SeekBar sb = (SeekBar) findViewById(R.id.HowFarBackSkBr);
        sb.setOnSeekBarChangeListener(this);
        sb.setProgress(mDefaultPastLocProgress);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }


    private void drawDBElementsOnMap(GoogleMap map, SQLiteDatabase db) {
        long timeEnd = System.currentTimeMillis();
        drawDBElementsOnMap(map, timeEnd, 0);
    }


    private void drawDBElementsOnMap(GoogleMap map, long timeEnd, long timeStart) {
        Message m = new Message();
        Bundle b = new Bundle();
        m.setTarget(mDBWorker.mServiceHandler);
        b.putLong(LFnC.WTGetDBMarkerTEKey, timeEnd);
        b.putLong(LFnC.WTGetDBMarkerTSKey, timeStart);
        b.putBoolean(LFnC.WTNoMarkerMergingKey, mNoMarkerMerge);
        m.what = MarkerDBWorker.getDBMarkerInfo;
        m.setData(b);
        m.sendToTarget();

    }


    @Override
    public boolean onMarkerClick(Marker marker) {
        float accuracy = markerIDToAccuracyHM.get(marker.getId());
        if (mMap == null) {
            Log.e(TAG, "onMarkerClick: Map is null!!");
            return false;
        }
        LatLng ll = marker.getPosition();
        Double daccuracy = new Double(accuracy);
        if (mCurCircle == null) {
            CircleOptions co = new CircleOptions();
            co.strokeColor(Color.parseColor("#90" + "33b5e5"));
            co.fillColor(Color.parseColor("#900099cc"));
            co.strokeWidth(5);
            co.center(ll);
            co.radius(daccuracy);
            mCurCircle = mMap.addCircle(co);
        } else {
            mCurCircle.setCenter(ll);
            mCurCircle.setRadius(daccuracy);
        }
        return false;
    }


    private void setupShowAllLocationsButton() {
        Button ShowAllLocButton = (Button) findViewById(R.id.ShowAllLocationFixButton);
        ShowAllLocButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mMap.clear();
                drawDBElementsOnMap(mMap, new LocationDB(mContext).getReadableDatabase());
            }
        });
    }


    protected void updateDBElementsOnMap(GoogleMap mMap2, SQLiteDatabase readableDatabase) {
        String tag = "updateDBElementsOnMap";
        Log.d(TAG + ":" + tag, "");

    }


    public void onShowSkBarBtnClick(View v) {
        if (mMap == null) {
            Log.e(TAG, "onShowSkBarBtnClick, map is null!");
            return;
        }
        // SQLiteDatabase db = mLocDB.getReadableDatabase();
        mMap.clear();
        drawDBElementsOnMap(mMap, System.currentTimeMillis(), mStartShowingLocTime);
        // db.close();
    }


    @Override
    public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
        String tag = "onProgressChanged";
        Log.d(TAG + ":" + tag, "progress now: " + progress);
        long duration = DateUtils.HOUR_IN_MILLIS * progress;
        Date showLocsAfter = new Date(System.currentTimeMillis() - duration);
        TextView tv = (TextView) findViewById(R.id.HowFarBackSkBrTV);
        tv.setText("Only display location fixes after: " + showLocsAfter.toLocaleString());
        mStartShowingLocTime = System.currentTimeMillis() - duration;
    }


    @Override
    public void onStartTrackingTouch(SeekBar arg0) {
        // TODO Auto-generated method stub

    }


    @Override
    public void onStopTrackingTouch(SeekBar arg0) {
        // TODO Auto-generated method stub

    }


    @Override
    public void onLocationChanged(Location location) {
        LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(ll, 17, 0, 0)));
    }


    @Override
    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub

    }


    @Override
    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub

    }


    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub

    }
}
