package com.hudson.passivelocationmonitor.fragments;

import com.hudson.passivelocationmonitor.R;
import com.hudson.passivelocationmonitor.R.xml;

import android.os.Bundle;
import android.preference.PreferenceFragment;

public class SettingsFragment extends PreferenceFragment {

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preferences);
	}
}
