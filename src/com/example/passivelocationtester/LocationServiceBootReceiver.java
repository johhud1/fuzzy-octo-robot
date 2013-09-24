package com.example.passivelocationtester;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class LocationServiceBootReceiver extends BroadcastReceiver {
    String tag = getClass().getName();
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(tag, "received a boot_completed broadcast. Restarting " + LocTrackerService.class.getName());
        Intent i = new Intent().setClass(context, LocTrackerService.class);
        context.startService(i);
    }

}
