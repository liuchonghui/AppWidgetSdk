package com.solo.appwidget.clock;

import android.app.Activity;
import android.os.Bundle;

import com.solo.appwidget.AppWidget;

public class Launcher extends Activity {

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		AppWidget.closeAll(this, SimpleClockWidget.class);
		SimpleClockWidget.showWidgetAsSreenShot(this);
		finish();
	}

}
