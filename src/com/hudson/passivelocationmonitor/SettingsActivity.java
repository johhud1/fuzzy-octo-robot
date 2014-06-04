package com.hudson.passivelocationmonitor;

import com.hudson.passivelocationmonitor.fragments.SettingsFragment;

import android.app.Activity;
import android.os.Bundle;

public class SettingsActivity extends Activity {

	@Override
	public void onCreate(Bundle sas){
		super.onCreate(sas);
		
		getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
	}
}
