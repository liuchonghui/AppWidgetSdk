package com.solo.appwidget;

import android.view.View;

import com.solo.appwidget.AppWidget.AppWidgetLayoutParams;

public class WindowController {
	protected View window;
	protected View snapshot;
	protected Window host;

	public WindowController(View window, View snapshot) {
		this.window = window;
		this.snapshot = snapshot;
	}
	
	public void setWindow(Window window) {
		host = window;
	}

	public int getScreenWidth() {
		return host.displayWidth;
	}

	public int getScreenHeight() {
		return host.displayHeight;
	}
	
	public AppWidgetLayoutParams getLayoutParams() {
		return (AppWidgetLayoutParams) window.getLayoutParams();
	}

	public void handleMove() {
		
	}
}
