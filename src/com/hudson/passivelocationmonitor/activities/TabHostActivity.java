package com.hudson.passivelocationmonitor.activities;

import com.hudson.passivelocationmonitor.Helpers;
import com.hudson.passivelocationmonitor.R;
import com.hudson.passivelocationmonitor.R.drawable;
import com.hudson.passivelocationmonitor.R.id;
import com.hudson.passivelocationmonitor.R.layout;
import com.hudson.passivelocationmonitor.R.menu;
import com.hudson.passivelocationmonitor.R.string;
import com.hudson.passivelocationmonitor.R.xml;
import com.hudson.passivelocationmonitor.fragments.SettingsFragment;

import android.app.AlertDialog;
import android.app.LauncherActivity;
import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TabHost;

public class TabHostActivity extends TabActivity {

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		setContentView(R.layout.tabhost_layout);

		Resources res = getResources(); // Resource object to get Drawables
		TabHost tabHost = getTabHost(); // The activity TabHost
		TabHost.TabSpec spec; // Resusable TabSpec for each tab
		Intent intent; // Reusable Intent for each tab

		// Create an Intent to launch an Activity for the tab (to be reused)
		intent = new Intent().setClass(this, MainActivity.class);

		// Initialize a TabSpec for each tab and add it to the TabHost
		spec = tabHost
				.newTabSpec("map")
				.setIndicator(getText(R.string.mapview_activity_indicator),
						res.getDrawable(R.drawable.ic_launcher))
				.setContent(intent);
		tabHost.addTab(spec);

		// Do the same for the other tab
		intent = new Intent().setClass(this, TimeRangeActivity.class);
		spec = tabHost
				.newTabSpec("timerange")
				.setIndicator(getText(R.string.timerange_activity_indicator),
						res.getDrawable(R.drawable.ic_launcher))
				.setContent(intent);
		tabHost.addTab(spec);

		tabHost.setCurrentTab(2);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main_menu, menu);
		return true;
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_about:
                AlertDialog dialog = Helpers.buildBasicMessageAlertDialog(this, R.string.menu_about, R.string.about_dialog_message);
                dialog.show();
                break;
            case R.id.menu_settings:
            	startActivity(new Intent(this, SettingsActivity.class));
            	break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }
}
