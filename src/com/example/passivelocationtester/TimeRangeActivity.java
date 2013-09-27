package com.example.passivelocationtester;

import java.sql.Date;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.jjoe64.graphview.CustomLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewStyle;
import com.jjoe64.graphview.OnValuesSelectedListener;
import com.jjoe64.graphview.SelectableBinBarGraphView;



public class TimeRangeActivity extends Activity implements OnSeekBarChangeListener {
    public static final long timeBoxSize = 1 * LFnC.HOUR;
    public static final long viewPortSize = LFnC.DAY; //one day in milliseconds

    SelectableBinBarGraphView graphView;

    SharedPreferences prefs;

    protected class PassiveLocationGraphOnValueSelectedListener implements OnValuesSelectedListener{
        String tag = getClass().getName();
        @Override
        public void OnValuesSelected(long valueSelectionStart, long valueSelectionEnd) {
            Log.d(tag, " onValuesSelected: valueSelectionStart: " + valueSelectionStart + " valueSelectionEnd: " + valueSelectionEnd);
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

        //get application shared preferences
        prefs = (SharedPreferences) getSharedPreferences(LFnC.PREF_KEY, MODE_PRIVATE);
        if(prefs.getBoolean(LFnC.PREF_FIRST_TIME_RANGE_ACTIVITY_KEY, true)){
            //first time user is in Time view activity
            Editor e = prefs.edit();
            e.putBoolean(LFnC.PREF_FIRST_TIME_RANGE_ACTIVITY_KEY, false);
            e.commit();
            Helpers.buildBasicMessageAlertDialog(this, R.string.welcome, R.string.first_time_timeview_message);
        }
        // get location data
        SQLiteDatabase db = new LocationDB(this).getReadableDatabase();
        Cursor mcurs =
            db.query(LocationDB.LOCATION_TABLE_NAME, new String[] { LocationDB.KEY_DATE }, null,
                     null, null, null, LocationDB.KEY_DATE + " ASC");

        int rows = mcurs.getCount();
        Log.d(tag, "all time requests cursor has: " + rows + " rows");
        int dateIndex = mcurs.getColumnIndex(LocationDB.KEY_DATE);
        if (mcurs.moveToFirst()) {
            long firstDate = mcurs.getLong(dateIndex);
            GraphViewData[] data = new GraphViewData[mcurs.getCount()];
            int i = 0;
            data[i] = new GraphViewData(firstDate, 1);
            while(mcurs.moveToNext()){
                i++;
                long date = mcurs.getLong(dateIndex);
                data[i] = new GraphViewData(date, 1);
            }

            //data = DataBinningHelper.binData(data, timeBoxSize);
            graphView = new SelectableBinBarGraphView(this, "Location Requests", false, LFnC.HOUR,
                                                                new PassiveLocationGraphOnValueSelectedListener());
            //graphView.setManualYAxisBounds(50d, 0d);
            // add data
            graphView.addSeries(new GraphViewSeries(data));
            graphView.setCustomLabelFormatter(new CustomLabelFormatter() {

                @Override
                public String formatLabel(double value, boolean isValueX) {
                    long date = (long)value;
                    if(isValueX){
                        return new Date(date).toLocaleString();
                    }
                    //TODO: this isn't good. Should find a better solution to the problem of unnecessarily high precision y-labels
                    String l =String.valueOf(value);
                    return l.substring(0, Math.min(l.length(), 4));
                }
            });
            graphView.setViewPort(System.currentTimeMillis()-viewPortSize, viewPortSize);
            graphView.setScrollable(true);
            graphView.setGraphViewStyle(new GraphViewStyle(Color.BLACK, Color.BLACK, Color.DKGRAY));
            // optional - activate scaling / zooming
            graphView.setScalable(true);

            LinearLayout layout = (LinearLayout) findViewById(R.id.time_range);
            layout.addView(graphView);

            SeekBar sk = (SeekBar) findViewById(R.id.bin_size_skbar);
            sk.setOnSeekBarChangeListener(this);
            setBinSizeTextView();
        } else {
            Log.d(tag, "cursor is empty");
        }
        db.close();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        //lets give the seekbar 5 states for now. 30 min. 1hr. 3 hr. 12 hr. 24 hour.

        if(progress < 20){
            graphView.setBinSize(30 * LFnC.MINUTE);
            //binSizeTV.setText(binSizeString + "30 min");
        } else if( (20 <= progress) && (progress < 40)){
            graphView.setBinSize(LFnC.HOUR);
            //binSizeTV.setText(binSizeString + "1 hour");
        } else if( (40 <= progress) && (progress <60)){
            graphView.setBinSize(3 * LFnC.HOUR);
            //binSizeTV.setText(binSizeString + "3 hours");
        } else if( (60 <= progress) && (progress < 80)){
            //binSizeTV.setText(binSizeString + "12 hours");
            graphView.setBinSize(12 * LFnC.HOUR);
        } else if( (80 <= progress)){
            graphView.setBinSize(LFnC.DAY);
            //binSizeTV.setText(binSizeString + "24 hour");
        }
        setBinSizeTextView();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub

    }
    protected void setBinSizeTextView(){
        long size = graphView.getBinSize();
        if(size == -1){
            Log.e(getClass().getName(), "set bin Size text view failure. Seems like bin Graph has num bins set, not bin size");
            return;
        }
        String binSizeString = getText(R.string.binsize_prefix).toString();
        TextView binSizeTV = (TextView) findViewById(R.id.binsize_textview);
        if(binSizeTV == null){
            Log.e(getClass().getName(), "error in setBinSizeTextView, binSizeTextView couldn't be found (== null)");
        }
        if((size/LFnC.MINUTE) < 60){
            binSizeTV.setText(binSizeString+ size/LFnC.MINUTE+ " min");
        } else if(size/LFnC.HOUR < 24){
            binSizeTV.setText(binSizeString+ size/LFnC.HOUR + " hours");
        } else{
            binSizeTV.setText(binSizeString + size/LFnC.DAY + " days");
        }
    }
}
