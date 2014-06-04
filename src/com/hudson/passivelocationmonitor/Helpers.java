package com.hudson.passivelocationmonitor;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.util.Log;

public class Helpers {
	private static final String tag = "Helpers";

	public static AlertDialog buildBasicMessageAlertDialog(Context c,
			int title, int message) {
		AlertDialog.Builder b = new AlertDialog.Builder(c);
		b.setTitle(title).setMessage(message)
				.setNegativeButton("Close", new DismissListener());

		return b.create();
	}

	public static AlertDialog buildMultiPageMessageAlertDialog(Context c,
			int[] titles, int[] messagePages) {
		if (titles.length == 0 || messagePages.length == 0) {
			Log.e("Helper",
					"tried to build multiPageMessageAlertDialog with 0 any titles or messages!");
			return null; // no
		}
		AlertDialog[] ads = new AlertDialog[titles.length];
		ads[0] = new AlertDialog.Builder(c).setTitle(titles[0])
				.setMessage(messagePages[0])
				.setNeutralButton("Close", new DismissListener()).create();
		AlertDialog prev = ads[0];
		for (int i = 1; i < titles.length; i++) {
			AlertDialog next = new AlertDialog.Builder(c)
					.setTitle(titles[i])
					.setMessage(messagePages[i])
					.setNegativeButton("Prev", new ShowAlertDialogPageListener(prev))
					.setNeutralButton("Close", new DismissListener()).create();
			prev.setButton(DialogInterface.BUTTON_POSITIVE, "Next",
					new ShowAlertDialogPageListener(next));
			prev = next;
		}
		return ads[0];
	}

	private static class DismissListener implements OnClickListener {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			// Do nothing is dismiss
		}

	}

	private static class ShowAlertDialogPageListener implements OnClickListener {
		AlertDialog d;

		public ShowAlertDialogPageListener(AlertDialog d) {
			this.d = d;
		}

		@Override
		public void onClick(DialogInterface dialog, int which) {
			dialog.dismiss();
			d.show();

		}

	}
}
