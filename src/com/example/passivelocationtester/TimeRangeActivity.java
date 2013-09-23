package com.example.passivelocationtester;

import java.sql.Date;

import com.jjoe64.graphview.BarGraphView;
import com.jjoe64.graphview.BinBarGraphView;
import com.jjoe64.graphview.CustomLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;
import com.jjoe64.graphview.SelectableBinBarGraphView;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.LinearLayout;



public class TimeRangeActivity extends Activity {
    public static final long timeBoxSize = 1 * LFnC.HOUR;
    public static final long viewPortSize = LFnC.DAY; //one day in milliseconds


    @Override
    public void onCreate(Bundle savedInstanceState) {
        final String tag = getClass().getName() + ":onCreate";
        super.onCreate(savedInstanceState);
        setContentView(R.layout.time_range);

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
            GraphView graphView = new SelectableBinBarGraphView(this, "Location Requests", false, LFnC.HOUR);
            graphView.setManualYAxisBounds(50d, 0d);
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
