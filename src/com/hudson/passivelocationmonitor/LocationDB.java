package com.hudson.passivelocationmonitor;

import java.io.File;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;



public class LocationDB extends SQLiteOpenHelper {
    private String TAG = "LocationDB";
    private Context mContext;
    private static File myFilesDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                                       + "/Android/data/" + LFnC.packageName + "/files");
    private boolean externalGood = myFilesDir.mkdirs();
    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = LFnC.DBName;
    public static final String LOCATION_TABLE_NAME = "Locations";
    public static final String KEY_LAT = "latKey";
    public static final String KEY_LONG = "longKey";
    public static final String KEY_DATE = "date";
    public static final String KEY_PROVIDER_TYPE = "provider";
    public static final String KEY_ACCURACY = "acc";
    private static final String LOCATION_TABLE_CREATE = "CREATE TABLE " + LOCATION_TABLE_NAME
                                                        + " ("+ KEY_LAT + " REAL, "
                                                        + KEY_LONG + " REAL, "
                                                        + KEY_DATE + " BIGINT, "
                                                        + KEY_ACCURACY + " REAL, "
                                                        + KEY_PROVIDER_TYPE + " TEXT);";


    public LocationDB(Context context) {
        //myFilesDir+ "/"  +
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
        Log.d(TAG, "in constructor db with name: " +DATABASE_NAME + " version:"+DATABASE_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "onCreate creating db with SQL statement (now in onCreate)");
        db.execSQL(LOCATION_TABLE_CREATE);

    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub

    }

}
