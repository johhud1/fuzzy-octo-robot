package com.hudson.passivelocationmonitor.activities;

import java.sql.Date;
import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import com.hudson.passivelocationmonitor.Helpers;
import com.hudson.passivelocationmonitor.LFnC;
import com.hudson.passivelocationmonitor.LocTrackerService;
import com.hudson.passivelocationmonitor.LocationDB;
import com.hudson.passivelocationmonitor.MainActivityDBResultHandler;
import com.hudson.passivelocationmonitor.MarkerDBWorker;
import com.hudson.passivelocationmonitor.R;
import com.hudson.passivelocationmonitor.R.id;
import com.hudson.passivelocationmonitor.R.layout;
import com.hudson.passivelocationmonitor.R.string;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.MapFragment;

import android.support.v4.app.FragmentManager;

import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import android.support.v4.app.FragmentActivity;

public class MainActivity extends FragmentActivity implements
		OnMarkerClickListener, LocationListener {
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
	SharedPreferences mPrefs;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		setupShowAllLocationsButton();
		// setupShowMarkerSeekbar();
		setupNoMarkerMergeButton();
		mPrefs = getSharedPreferences(LFnC.PREF_KEY, MODE_PRIVATE);

		startService(new Intent().setClass(this, LocTrackerService.class));

		SupportMapFragment mfrag = (SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.mainMapFrag);
		mMap = mfrag.getMap();
		mHandler = new MainActivityDBResultHandler(mMap, markerIDToAccuracyHM,
				this);
		mDBWorker = new MarkerDBWorker(mHandler, this);

		LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
		lm.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, this, null);

		mMap.setOnMarkerClickListener(this);
		if (mPrefs.getBoolean(LFnC.PREF_FIRST_TIME_MAIN_ACTIVITY_KEY, true)) {
			Editor e = mPrefs.edit();
			e.putBoolean(LFnC.PREF_FIRST_TIME_MAIN_ACTIVITY_KEY, false);
			e.commit();
			// this is the first time opening the application (I think)
			Helpers.buildBasicMessageAlertDialog(this, R.string.welcome,
					R.string.first_time_mapview_message).show();
		} else {
			// not first time opening the application

		}

	}

	@Override
	public void onResume() {
		super.onResume();
		mCurCircle = null;
		mMap.clear();
		long timeEnd = mPrefs.getLong(LFnC.PREF_MARKER_END_KEY,
				System.currentTimeMillis());
		long timeStart = mPrefs.getLong(LFnC.PREF_MARKER_START_KEY, 0);
		drawDBElementsOnMap(mMap, timeEnd, timeStart);
	}
/*
	private void setShowingLocationPingsBetweenText(long timeStart, long timeEnd) {
		String tag = getClass().getName()
				+ ":setShowingLocationPingsBetweenText";
		TextView tv = (TextView) findViewById(R.id.LocRequestDisplayRangeTV);
		
		if (tv != null) {
			Log.d(tag, "setting location request range TextView to :" + msg);
			tv.setText(getString(R.string.loc_request_displayrange_prefix)
					+ msg);
		} else {
			Log.e(tag,
					" location request TextView could not be found! (was null)");
		}

	}
*/
	private void setupNoMarkerMergeButton() {
		CheckBox b = (CheckBox) findViewById(R.id.noMarkerMergeCB);
		b.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked) {
					mNoMarkerMerge = true;
				} else {
					mNoMarkerMerge = false;
				}

			}
		});

	}

	private void drawDBElementsOnMap(GoogleMap map, SQLiteDatabase db) {
		long timeEnd = System.currentTimeMillis();
		drawDBElementsOnMap(map, timeEnd, 0);
	}

	private void drawDBElementsOnMap(GoogleMap map, long timeEnd, long timeStart) {
		// set stats to none, will be updated with result in
		// MainActivityDBResultHandler
		resetStats();
		TextView tv = (TextView) findViewById(R.id.LocRequestDisplayRangeTV);
		tv.setText(getString(R.string.loc_request_displayrange_prefix) + getString(R.string.calculating));
		// send get marker info message to MarkerDBWorker
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

	private void resetStats() {
		TextView numPingsTV = (TextView) findViewById(R.id.NumberOfPingsTV);
		TextView avgIntTV = (TextView) findViewById(R.id.TotalAvgLocationIntervalTV);
		numPingsTV.setText(getText(R.string.number_of_pings_prefix) +getString(R.string.calculating));
		avgIntTV.setText(getText(R.string.avg_fig_int_prefix) + getString(R.string.calculating));

	}

	@Override
	public boolean onMarkerClick(Marker marker) {
		String tag = ":onMarkerClick";
		float accuracy = markerIDToAccuracyHM.get(marker.getId());
		Log.d(TAG + tag, "using ID:" + marker.getId() + " got accuracy:"
				+ accuracy + " from HashMap");
		if (mMap == null) {
			Log.e(TAG, "onMarkerClick: Map is null!!");
			return false;
		}
		LatLng ll = marker.getPosition();
		Double daccuracy = Double.valueOf(accuracy);
		if (mCurCircle == null) {
			Log.d(TAG + tag, "mCurCircle is null. Constructing new one.");
			CircleOptions co = new CircleOptions();
			co.strokeColor(Color.parseColor("#90" + "33b5e5"));
			co.fillColor(Color.parseColor("#900099cc"));
			co.strokeWidth(5);
			co.center(ll);
			co.radius(daccuracy);
			mCurCircle = mMap.addCircle(co);
		} else {
			Log.d(TAG + tag, "mCurCircle is not null. reposition old one.");
			mCurCircle.setCenter(ll);
			mCurCircle.setRadius(daccuracy);
		}
		return false;
	}

	private void setupShowAllLocationsButton() {
		Button ShowAllLocButton = (Button) findViewById(R.id.ShowAllLocationFixButton);
		ShowAllLocButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mMap.clear();
				mCurCircle = null;
				drawDBElementsOnMap(mMap,
						new LocationDB(mContext).getReadableDatabase());
			}
		});
	}

	protected void updateDBElementsOnMap(GoogleMap mMap2,
			SQLiteDatabase readableDatabase) {
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
		mCurCircle = null;
		TextView nOfPTV = (TextView) findViewById(R.id.NumberOfPingsTV);
		nOfPTV.setText("0");
		drawDBElementsOnMap(mMap, System.currentTimeMillis(),
				mStartShowingLocTime);
		// db.close();
	}

	@Override
	public void onLocationChanged(Location location) {
		LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
		mMap.animateCamera(CameraUpdateFactory
				.newCameraPosition(new CameraPosition(ll, 15, 0, 0)));
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
