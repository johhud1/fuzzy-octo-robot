package com.example.passivelocationtester;

import java.sql.Date;
import java.util.prefs.Preferences;

import com.jjoe64.graphview.BarGraphView;
import com.jjoe64.graphview.BinBarGraphView;
import com.jjoe64.graphview.CustomLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewStyle;
import com.jjoe64.graphview.LineGraphView;
import com.jjoe64.graphview.OnValuesSelectedListener;
import com.jjoe64.graphview.SelectableBinBarGraphView;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.Preference;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.LinearLayout;



public class TimeRangeActivity extends Activity {
    public static final long timeBoxSize = 1 * LFnC.HOUR;
    public static final long viewPortSize = LFnC.DAY; //one day in milliseconds

    SharedPreferences prefs;

    protected class PassiveLocationGraphOnValueSelectedListener implements OnValuesSelectedListener{
        String tag = getClass().getName();
        @Override
        public void OnValuesSelected(long valueSelectionStart, long valueSelectionEnd) {
            Log.d(tag, "onValuesSelected: valueSelectionStart: " + valueSelectionStart + " valueSelectionEnd: " + valueSelectionEnd);
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
            GraphView graphView = new SelectableBinBarGraphView(this, "Location Requests", false, LFnC.HOUR,
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
                    return String.valueOf(value);
                }
            });
            // set view port, start=2, size=40
            graphView.setViewPort(System.currentTimeMillis()-viewPortSize, viewPortSize);
            graphView.setScrollable(true);
            graphView.setGraphViewStyle(new GraphViewStyle(Color.BLACK, Color.BLACK, Color.DKGRAY));
            // optional - activate scaling / zooming
            graphView.setScalable(true);

            LinearLayout layout = (LinearLayout) findViewById(R.id.time_range);
            layout.addView(graphView);
        } else {
            Log.d(tag, "cursor is empty");
        }
        db.close();
    }
}
