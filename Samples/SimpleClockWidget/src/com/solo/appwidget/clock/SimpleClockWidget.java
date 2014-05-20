package com.solo.appwidget.clock;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.RemoteViews;

import com.solo.appwidget.AppWidget;
import com.solo.appwidget.StandOutFlags;
import com.solo.appwidget.Window;
import com.solo.appwidget.WindowController;
import com.solo.appwidget.WindowModel;

public final class SimpleClockWidget extends AppWidget {

	public static final int BASE_CODE = 1000;
	public static final int STARTUP_CODE = BASE_CODE + 1;
	public static final int HIDE_CODE = BASE_CODE + 2;
	public static final int CLOSE_CODE = BASE_CODE + 3;
	public static final int STARTUP_SCREENSHOT_CODE = BASE_CODE + 4;

	protected boolean runnable = true;

	SparseArray<WindowModel> mFolders;
	Animation mFadeOut, mFadeIn;

	NotificationManager nm;
	Notification notifier;
	PendingIntent pendButtonIntent;
	boolean switchOn = false;

	MyBroadcastActionListener mBroadcastActionListener;
	SensorManager mSensorManager;

	// MySensorEventListener mSensorEventListener;
	// MyOrientationEventListener mOrientationEventListener;

	public static void showWidget(Context context) {
		sendData(context, SimpleClockWidget.class, DISREGARD_ID, STARTUP_CODE,
				null, null, DISREGARD_ID);
	}

	public static void showWidgetAsSreenShot(Context context) {
		sendData(context, SimpleClockWidget.class, DISREGARD_ID,
				STARTUP_SCREENSHOT_CODE, null, null, DISREGARD_ID);
	}

	public static void hideWidget(Context context) {
		sendData(context, SimpleClockWidget.class, DISREGARD_ID, HIDE_CODE,
				null, null, DISREGARD_ID);
	}

	public static void closeWidget(Context context) {
		sendData(context, SimpleClockWidget.class, DISREGARD_ID, CLOSE_CODE,
				null, null, DISREGARD_ID);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d("Window", "onCreate()");

		mFadeOut = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
		mFadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);

		mFadeOut.setDuration(100L);
		mFadeIn.setDuration(100L);

		mFadeIn.setAnimationListener(new AnimationEndListener() {
			@Override
			public void onAnimationEnd(Animation animation) {
				runnable = true;
			}
		});

		pendButtonIntent = PendingIntent.getBroadcast(this, 0, new Intent(
				getDefaultBroadcastAction()), 0);

		mBroadcastActionListener = new MyBroadcastActionListener();
		registerReceiver(mBroadcastActionListener, new IntentFilter(
				getDefaultBroadcastAction()));

		// mSensorEventListener = new MySensorEventListener();
		// mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		// mSensorManager.registerListener(mSensorEventListener,
		// mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
		// SensorManager.SENSOR_DELAY_NORMAL);

		// mOrientationEventListener = new MyOrientationEventListener(
		// getApplicationContext());
		// mOrientationEventListener.enable();

	}

	public void destroyAll() {
		if (mBroadcastActionListener != null) {
			unregisterReceiver(mBroadcastActionListener);
		}
		if (pendButtonIntent != null) {
			pendButtonIntent.cancel();
		}
		// if (mSensorEventListener != null) {
		// mSensorManager.unregisterListener(mSensorEventListener);
		// }
		// if (mOrientationEventListener != null) {
		// mOrientationEventListener.disable();
		// }
	}

	protected void onStatusBarSwitchClicked() {
		if (switchOn) {
			RemoteViews rv = notifier.contentView;
			rv.setImageViewResource(R.id.switcher, R.drawable.setting_off);
			hideWidget(getContext());
			switchOn = false;
		} else {
			RemoteViews rv = notifier.contentView;
			rv.setImageViewResource(R.id.switcher, R.drawable.setting_on);
			showWidget(getContext());
			switchOn = true;
		}
		mNotificationManager.notify(getDefaultNotificationId(), notifier);
	}

	@Override
	public String getAppName() {
		return "桌面小时钟";
	}

	@Override
	public int getAppIcon() {
		return R.drawable.ic_launcher;
	}

	@Override
	public WindowController createAndAttachView(final int id, FrameLayout frame) {
		final LayoutInflater inflater = LayoutInflater.from(this);
		final View window = inflater.inflate(R.layout.clock, frame, true);
		return new WindowController(window.findViewById(R.id.window),
				window.findViewById(R.id.snapshot));
	}

	@Override
	public AppWidgetLayoutParams getParams(int id, Window window) {
		int w = 200;
		int h = w;
		int xpos = 50;
		int ypos = xpos;
		final LayoutInflater inflater = LayoutInflater.from(this);
		View view = inflater.inflate(R.layout.clock, null).findViewById(
				R.id.window);
		Drawable drawable = view.getBackground();
		if (drawable != null) {
			w = drawable.getIntrinsicWidth();
			h = drawable.getIntrinsicHeight();

			xpos = window.displayWidth / 2 - w / 2;
			ypos = window.displayHeight / 2 - h / 2;
		}

		return new AppWidgetLayoutParams(id, w, h, xpos, ypos);
	}

	@Override
	public int getFlags(int id) {
		return super.getFlags(id) | StandOutFlags.FLAG_BODY_MOVE_ENABLE
				| StandOutFlags.FLAG_WINDOW_HIDE_ENABLE
				| StandOutFlags.FLAG_WINDOW_EDGE_LIMITS_ENABLE
				| StandOutFlags.FLAG_WINDOW_FOCUSABLE_DISABLE;
	}

	@Override
	public void onReceiveData(int id, int requestCode, Bundle data,
			Class<? extends AppWidget> fromCls, int fromId) {
		switch (requestCode) {
		case STARTUP_CODE:
			loadAllFolders();
			if (mFolders.size() == 0) {
				mFolders.put(DEFAULT_ID, new WindowModel());
				show(DEFAULT_ID);
			} else {
				for (int i = 0; i < mFolders.size(); i++) {
					WindowModel folder = mFolders.get(mFolders.keyAt(i));
					if (folder.shown) {
						show(folder.id);
					}
				}
			}
			break;
		case HIDE_CODE:
			if (mFolders == null || mFolders.size() == 0) {
				return;
			}
			for (int i = 0; i < mFolders.size(); i++) {
				WindowModel folder = mFolders.get(mFolders.keyAt(i));
				if (folder.shown) {
					hide(folder.id);
				}
			}
			break;
		case CLOSE_CODE:
			if (mFolders == null || mFolders.size() == 0) {
				return;
			}
			for (int i = 0; i < mFolders.size(); i++) {
				WindowModel folder = mFolders.get(mFolders.keyAt(i));
				if (folder.shown) {
					close(folder.id);
				}
			}
			break;
		case STARTUP_SCREENSHOT_CODE:
			loadAllFolders();
			if (mFolders.size() == 0) {
				mFolders.put(DEFAULT_ID, new WindowModel());
				showAsScreenShot(DEFAULT_ID);
			} else {
				for (int i = 0; i < mFolders.size(); i++) {
					WindowModel folder = mFolders.get(mFolders.keyAt(i));
					if (folder.shown) {
						showAsScreenShot(folder.id);
					}
				}
			}
			break;
		}
	}

	@Override
	public boolean onShow(final int id, Window window) {
		if (showAsScreenShot) {
			final AppWidgetLayoutParams params = window.getLayoutParams();
			final View folderView = window.findViewById(R.id.window);
			final View screenshot = window.findViewById(R.id.snapshot);
			WindowModel folder = mFolders.get(id);
			if (folder != null && folder.fullSize) {
				folder.fullSize = false;

				folderView.setVisibility(View.GONE);
				Drawable drawable = screenshot.getBackground();

				params.x = 0;
				params.y = params.y + params.height / 2
						- drawable.getIntrinsicHeight() / 2;

				params.width = drawable.getIntrinsicWidth();
				params.height = drawable.getIntrinsicHeight();

				screenshot.setVisibility(View.VISIBLE);
			}
		}

		return false;
	}

	private void loadAllFolders() {
		mFolders = new SparseArray<WindowModel>();
	}

	@Override
	public boolean onTouchBody(final int id, final Window window,
			final View view, MotionEvent event) {
		if (event != null && event.getAction() == MotionEvent.ACTION_MOVE) {
			handleWindowMoveAction(id, window);
			// window.getController().handleMove();
		}
		return false;
	}

	public String getPersistentNotificationTitle(int id) {
		return getAppName() + " 正在运行";
	}

	public String getPersistentNotificationMessage(int id) {
		return "点击停止桌面小时钟";
	}

	public Intent getPersistentNotificationIntent(int id) {
		return AppWidget.getHideIntent(this, SimpleClockWidget.class, id);
	}

	@Override
	public Animation getShowAnimation(int id) {
		if (isExistingId(id)) {
			return AnimationUtils.loadAnimation(this,
					R.anim.anim_slide_in_bottom);
		} else {
			return super.getShowAnimation(id);
		}
	}

	@Override
	public Animation getHideAnimation(int id) {
		return AnimationUtils.loadAnimation(this, R.anim.anim_slide_out_top);
	}

	public Notification getPersistentNotification(int id) {
		int icon = getAppIcon();
		long when = System.currentTimeMillis();
		String contentTitle = getPersistentNotificationTitle(id);
		String contentText = getPersistentNotificationMessage(id);
		String tickerText = String.format("%s: %s", contentTitle, contentText);

		RemoteViews rv = new RemoteViews(getPackageName(),
				R.layout.notification4clock);
		rv.setImageViewResource(R.id.switcher, R.drawable.setting_on);
		rv.setOnClickPendingIntent(R.id.switcher, pendButtonIntent);
		switchOn = true;
		notifier = new Notification(icon, tickerText, when);
		notifier.contentView = rv;

		return notifier;
	}

	public Notification getHiddenNotification(int id) {
		return null;
	}

	protected void handleWindowMoveAction(int id) {
		if (getWindowCache().isCached(id, SimpleClockWidget.class)) {
			Window window = getWindowCache().getCache(id,
					SimpleClockWidget.class);
			window.getCurrentDisplayMetrics();
			handleWindowMoveAction(id, window);
		}
	}

	protected void handleWindowMoveAction(int id, Window window) {
		if (!runnable) {
			return;
		}

		final int screenWidth = window.displayWidth;
		final AppWidgetLayoutParams params = window.getLayoutParams();

		WindowModel model = mFolders.get(id);
		if (params.x <= 0) {
			Log.d("xxxxx", params.x + "," + screenWidth
					+ "=windowToSnapshot---(" + params.width + ","
					+ (screenWidth - params.x) + ")");
			windowGoneSnapShow(id, window, model);
		} else if (params.x >= screenWidth - params.width) {
			Log.d("xxxxx", params.x + "," + screenWidth
					+ "=windowToSnapshot---(" + params.width + ","
					+ (screenWidth - params.x) + ")");
			windowGoneSnapShow(id, window, model);
		} else {
			Log.d("xxxxx", params.x + "," + screenWidth
					+ "=snapshotToWindow+++(" + params.width + ","
					+ (screenWidth - params.x) + ")");
			snapGoneWindowShow(id, window, model);
		}
	}

	protected void windowGoneSnapShow(final int id, final Window window,
			WindowModel model) {
		final View folderView = window.findViewById(R.id.window);
		final View screenshot = window.findViewById(R.id.snapshot);
		final AppWidgetLayoutParams windowParams = window.getLayoutParams();

		if (model.fullSize) {
			model.fullSize = false;

			final Drawable drawable = screenshot.getBackground();
			final int screenWidth = window.displayWidth;

			mFadeOut.setAnimationListener(new FadeOutListener(folderView,
					windowParams, id, window, screenshot, drawable
							.getIntrinsicWidth(),
					drawable.getIntrinsicHeight(), screenWidth));
			folderView.startAnimation(mFadeOut);
			runnable = false;
		}
	}

	protected void snapGoneWindowShow(final int id, final Window window,
			WindowModel model) {
		final View folderView = window.findViewById(R.id.window);
		final View screenshot = window.findViewById(R.id.snapshot);
		final AppWidgetLayoutParams windowParams = window.getLayoutParams();

		if (!model.fullSize) {
			model.fullSize = true;

			final Drawable drawable = folderView.getBackground();
			final int screenWidth = window.displayWidth;

			mFadeOut.setAnimationListener(new FadeOutListener(screenshot,
					windowParams, id, window, folderView, drawable
							.getIntrinsicWidth(),
					drawable.getIntrinsicHeight(), screenWidth));
			screenshot.startAnimation(mFadeOut);
			runnable = false;
		}
	}

	protected class FadeOutListener extends AnimationEndListener implements
			Runnable {
		private View goneView;
		private AppWidgetLayoutParams windowParams;
		private int id;
		private Window window;
		private View visiView;
		private int visiViewWidth;
		private int visiViewHeight;
		private int screenWidth;

		public FadeOutListener(View gone, AppWidgetLayoutParams windowParams,
				int id, Window window, View visi, int visiWidth,
				int visiHeight, int screenWidth) {
			this.goneView = gone;
			this.windowParams = windowParams;
			this.id = id;
			this.window = window;
			this.visiView = visi;
			this.visiViewWidth = visiWidth;
			this.visiViewHeight = visiHeight;
			this.screenWidth = screenWidth;
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			visiView.post(this);
		}

		@Override
		public void run() {
			goneView.setVisibility(View.GONE);
			windowParams.y = windowParams.y + windowParams.height / 2
					- visiViewHeight / 2;
			if (windowParams.x >= screenWidth - window.getWidth()) {
				windowParams.x = screenWidth - visiViewWidth;
			}
			windowParams.width = visiViewWidth;
			windowParams.height = visiViewHeight;
			updateViewLayout(id, windowParams);
			visiView.setVisibility(View.VISIBLE);
			visiView.startAnimation(mFadeIn);
		}
	}

	public class MyBroadcastActionListener extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			new Handler().post(new Runnable() {
				@Override
				public void run() {
					onStatusBarSwitchClicked();
				}
			});
		}
	}

	public class MyOrientationEventListener extends OrientationEventListener {
		public MyOrientationEventListener(Context context) {
			super(context);
		}

		@Override
		public void onOrientationChanged(int orientation) {
			Log.d("Window", "last orientation is " + orientation);
		}
	}

	boolean landscape;

	public class MySensorEventListener implements SensorEventListener {
		@Override
		public void onSensorChanged(SensorEvent event) {
			float[] values = event.values;
			final float x = values[0];
			final float y = values[1];
			if (x > 9 && landscape == false) {
				landscape = true;
				Log.d("Window", "turn left");
			}
			if (y > 9 && landscape == true) {
				landscape = false;
				Log.d("Window", "turn top");
			}
			if (x < -9 && landscape == false) {
				landscape = true;
				Log.d("Window", "turn right");
			}
			if (y < -9 && landscape == true) {
				landscape = false;
				Log.d("Window", "turn bottom");
			}
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}
	}
	
	@Override
	public void onOrientationChanged(boolean isPortrait) {
		for (int i=0; i < mFolders.size(); i++) {
			WindowModel folder = mFolders.get(i);
			handleWindowMoveAction(folder.id);
		}
	}
}
