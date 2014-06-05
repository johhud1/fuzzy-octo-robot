package com.hudson.passivelocationmonitor.activities;

import java.sql.Date;
import java.text.SimpleDateFormat;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.hudson.passivelocationmonitor.Helpers;
import com.hudson.passivelocationmonitor.LFnC;
import com.hudson.passivelocationmonitor.LocationDB;
import com.hudson.passivelocationmonitor.R;
import com.hudson.passivelocationmonitor.R.id;
import com.hudson.passivelocationmonitor.R.layout;
import com.hudson.passivelocationmonitor.R.string;
import com.jjoe64.graphview.CustomLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewStyle;
import com.jjoe64.graphview.OnValuesSelectedListener;
import com.jjoe64.graphview.SelectableBinBarGraphView;

public class TimeRangeActivity extends Activity implements
		OnSeekBarChangeListener, OnSharedPreferenceChangeListener {
	public static final long timeBoxSize = 1 * LFnC.HOUR;
	public static final long viewPortSize = LFnC.DAY; // one day in milliseconds

	SelectableBinBarGraphView graphView;

	SharedPreferences p;
	// check if manual binning is enabled
	boolean binEnabled;

	SharedPreferences prefs;

	protected class PassiveLocationGraphOnValueSelectedListener implements
			OnValuesSelectedListener {
		String tag = getClass().getName();

		@Override
		public void OnValuesSelected(long valueSelectionStart,
				long valueSelectionEnd) {
			Log.d(tag, " onValuesSelected: valueSelectionStart: "
					+ valueSelectionStart + " valueSelectionEnd: "
					+ valueSelectionEnd);
			Editor editor = prefs.edit();
			editor.putLong(LFnC.PREF_MARKER_START_KEY, valueSelectionStart);
			editor.putLong(LFnC.PREF_MARKER_END_KEY, valueSelectionEnd);
			editor.commit();
		}

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		final String tag = getClass().getName() + ":onCreate";
		super.onCreate(savedInstanceState);
		setContentView(R.layout.time_range);
		p = PreferenceManager.getDefaultSharedPreferences(this);
		p.registerOnSharedPreferenceChangeListener(this);
		binEnabled = p.getBoolean(getString(R.string.binSize_prefKey), false);
		// get application shared preferences
		prefs = (SharedPreferences) getSharedPreferences(LFnC.PREF_KEY,
				MODE_PRIVATE);
		if (prefs.getBoolean(LFnC.PREF_FIRST_TIME_RANGE_ACTIVITY_KEY, true)) {
			// first time user is in Time view activity
			Editor e = prefs.edit();
			e.putBoolean(LFnC.PREF_FIRST_TIME_RANGE_ACTIVITY_KEY, false);
			e.commit();
			Helpers.buildBasicMessageAlertDialog(this, R.string.welcome,
					R.string.first_time_timeview_message).show();
		}
		// get location data
		SQLiteDatabase db = new LocationDB(this).getReadableDatabase();
		Cursor mcurs = db.query(LocationDB.LOCATION_TABLE_NAME,
				new String[] { LocationDB.KEY_DATE }, null, null, null, null,
				LocationDB.KEY_DATE + " ASC");

		int rows = mcurs.getCount();
		Log.d(tag, "all time requests cursor has: " + rows + " rows");
		int dateIndex = mcurs.getColumnIndex(LocationDB.KEY_DATE);

		// get layout to which we will add either graph of location data, or 'no
		// data to graph'
		LinearLayout layout = (LinearLayout) findViewById(R.id.time_range);

		if (mcurs.moveToFirst()) {
			long firstDate = mcurs.getLong(dateIndex);
			GraphViewData[] data = new GraphViewData[mcurs.getCount()];
			int i = 0;
			data[i] = new GraphViewData(firstDate, 1);
			while (mcurs.moveToNext()) {
				i++;
				long date = mcurs.getLong(dateIndex);
				data[i] = new GraphViewData(date, 1);
			}

			graphView = new SelectableBinBarGraphView(this,
					"Location Requests", binEnabled, binEnabled ? 20
							: LFnC.HOUR,
					new PassiveLocationGraphOnValueSelectedListener());
			// graphView.setManualYAxisBounds(50d, 0d);
			// add data
			graphView.addSeries(new GraphViewSeries(data));
			graphView.setCustomLabelFormatter(new CustomLabelFormatter() {

				@Override
				public String formatLabel(double value, boolean isValueX) {
					long date = (long) value;

					if (isValueX) {
						return new SimpleDateFormat("kk:mm dd/MM/yy")
								.format(new Date(date)); // new
															// Date(date).toLocaleString();
					}
					// TODO: this isn't good. Should find a better solution to
					// the problem of unnecessarily high precision y-labels
					String l = String.valueOf(value);
					return l.substring(0, Math.min(l.length(), 4));
				}
			});
			graphView.setViewPort(System.currentTimeMillis() - viewPortSize,
					viewPortSize);
			graphView.setScrollable(true);
			graphView.setGraphViewStyle(new GraphViewStyle(Color.BLACK,
					Color.BLACK, Color.DKGRAY));
			// optional - activate scaling / zooming
			graphView.setScalable(true);
			layout.addView(graphView);
			SeekBar sk = (SeekBar) findViewById(R.id.bin_size_skbar);
			sk.setOnSeekBarChangeListener(this);
			//setBinSizeTextView();
		} else {
			Log.d(tag, "cursor is empty");
			TextView emptyGraph = new TextView(this);
			emptyGraph
					.setText("No Location Requests have been made yet, nothing to graph");
			layout.addView(emptyGraph);
		}
		db.close();
		changeLayoutForBinSizePref(getString(R.string.binSize_prefKey));
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		p.unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		// lets give the seekbar 5 states for now. 30 min. 1hr. 3 hr. 12 hr. 24
		// hour.
		long[] binSizeVals = { 30 * LFnC.MINUTE, LFnC.HOUR, 3 * LFnC.HOUR,
				12 * LFnC.HOUR, LFnC.DAY, 7 * LFnC.DAY };
		int max = seekBar.getMax();
		float range = Float.valueOf(max + 1) / binSizeVals.length;
		int index = (int) Math.floor(Float.valueOf(progress) / range);
		graphView.setBinSize(binSizeVals[index]);
		//setBinSizeTextView();
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
	}
/*
	protected void setBinSizeTextView() {
		long size = graphView.getBinSize();
		if (size == -1) {
			Log.e(getClass().getName(),
					"set bin Size text view failure. Seems like bin Graph has num bins set, not bin size");
			return;
		}
		String binSizeString = getText(R.string.binsize_prefix).toString();
		TextView binSizeTV = (TextView) findViewById(R.id.binsize_textview);
		if (binSizeTV == null) {
			Log.e(getClass().getName(),
					"error in setBinSizeTextView, binSizeTextView couldn't be found (== null)");
		}
		if ((size / LFnC.MINUTE) < 60) {
			binSizeTV.setText(binSizeString + size / LFnC.MINUTE + " min");
		} else if (size / LFnC.HOUR < 24) {
			binSizeTV.setText(binSizeString + size / LFnC.HOUR + " hours");
		} else {
			binSizeTV.setText(binSizeString + size / LFnC.DAY + " days");
		}
	}*/

	@Override
	public void onResume() {
		super.onResume();

	}

	@Override
	public void onPause() {
		super.onPause();

	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		String tag = getClass().getName() + " :onSharedPreferenceChanged";
		changeLayoutForBinSizePref(key);
	}

	private void changeLayoutForBinSizePref(String key) {
		String tag = getClass().getName() + " :changeLayoutForBinSizePref";
		if (key.equals(getString(R.string.binSize_prefKey))) {
			binEnabled = p.getBoolean(getString(R.string.binSize_prefKey),
					false);
			if (!binEnabled) {
				// hide bin size seekbar if bin disabled
				Log.d(tag,
						"manual binning disabled. removing bin size seek bar");
				findViewById(R.id.bin_size_skbar).setVisibility(View.GONE);
				graphView.setNumBins(20);
			} else {
				Log.d(tag,
						"manual binning enabled. making bin size seek bar visible");
				findViewById(R.id.bin_size_skbar).setVisibility(View.VISIBLE);
				graphView.setBinSize(LFnC.HOUR);
			}
			// TODO: remove old graphView from layout, construct and add new one
			// with proper binning

		}

	}
}
