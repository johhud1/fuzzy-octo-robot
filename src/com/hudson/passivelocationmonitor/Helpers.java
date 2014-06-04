package com.hudson.passivelocationmonitor;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

public class Helpers {

    public static AlertDialog buildBasicMessageAlertDialog(Context c, int title, int message){
        AlertDialog.Builder b = new AlertDialog.Builder(c);
        b.setTitle(title).setMessage(message).setNegativeButton("Close", new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                //do nothing, dismiss is handled automatically
            }
        });

        return b.create();
    }
}
