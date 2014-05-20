package com.solo.appwidget.clock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BaseOnBootReceiver extends BroadcastReceiver {

	public BaseOnBootReceiver() {
		super();
	}

	@Override
	public void onReceive(final Context context, Intent intent) {
//		if ("android.intent.action.BOOT_COMPLETED".equalsIgnoreCase(intent
//				.getAction())) {
			Log.d("ClockWidget", "onReceive____________" + intent.getAction());
			// context.startActivity(new Intent(context, Launcher.class));
//			context.startService(new Intent(context, SimpleClockWidget.class));
			SimpleClockWidget.showWidgetAsSreenShot(context);
//		}
	}
}