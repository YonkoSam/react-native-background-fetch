package com.transistorsoft.rnbackgroundfetch;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.facebook.react.bridge.*;
import com.facebook.react.modules.core.RCTNativeAppEventEmitter;
import com.transistorsoft.tsbackgroundfetch.BackgroundFetch;
import com.transistorsoft.tsbackgroundfetch.BackgroundFetchConfig;
import com.transistorsoft.tsbackgroundfetch.LifecycleManager;

public class RNBackgroundFetchModule extends ReactContextBaseJavaModule implements ActivityEventListener, LifecycleEventListener {
    public static final String TAG = "RNBackgroundFetch";
    private static final String EVENT_FETCH = "fetch";
    private static final String JOB_SERVICE_CLASS = HeadlessTask.class.getName();
    private static final String FETCH_TASK_ID = "react-native-background-fetch";
    private boolean initialized = false;
    private BackgroundFetch backgroundFetchInstance = null;

 public RNBackgroundFetchModule(ReactApplicationContext reactContext) {
        super(reactContext);
        Log.d(TAG, "[RNBackgroundFetch constructor]");
        Activity activity = getCurrentActivity();
        if (activity != null) {
            String mainActivityClass = "MainActivity";
            if (activity.getClass().getSimpleName().equals(mainActivityClass)) {
                Log.d(TAG, "Setting MainActivity as React main activity for LifecycleManager");
                LifecycleManager.getInstance().setReactMainActivityClass(activity.getClass());
                LifecycleManager.getInstance().setCurrentActivity(activity);
            }
        }
        reactContext.addLifecycleEventListener(this);
    }

    public static void setCurrentActivity(Activity activity) {
        LifecycleManager.getInstance().setCurrentActivity(activity);
    }


     @Override
      public String getName() {
          return TAG;
      }

      @ReactMethod
      public void configure(ReadableMap options, final Callback success, final Callback failure) {
          BackgroundFetch adapter = getAdapter();

          BackgroundFetch.Callback callback = new BackgroundFetch.Callback() {
              @Override public void onFetch(String taskId) {
                  WritableMap params = new WritableNativeMap();
                  params.putString("taskId", taskId);
                  params.putBoolean("timeout", false);
                  getReactApplicationContext().getJSModule(RCTNativeAppEventEmitter.class).emit(EVENT_FETCH, params);
              }
              @Override public void onTimeout(String taskId) {
                  WritableMap params = new WritableNativeMap();
                  params.putString("taskId", taskId);
                  params.putBoolean("timeout", true);
                  getReactApplicationContext().getJSModule(RCTNativeAppEventEmitter.class).emit(EVENT_FETCH, params);
              }
          };
          adapter.configure(buildConfig(options)
                  .setTaskId(FETCH_TASK_ID)
                  .setIsFetchTask(true)
                  .build(), callback);

          success.invoke(BackgroundFetch.STATUS_AVAILABLE);
      }

      @ReactMethod
      public void scheduleTask(ReadableMap options, final Callback success, final Callback failure) {
          BackgroundFetch adapter = getAdapter();
          adapter.scheduleTask(buildConfig(options).build());
          success.invoke(true);
      }

      @ReactMethod
      public void start(Callback success, Callback failure) {
          BackgroundFetch adapter = getAdapter();
          adapter.start(FETCH_TASK_ID);
          success.invoke(adapter.status());
      }

      @ReactMethod
      public void stop(String taskId, Callback success, Callback failure) {
          if (taskId == null) taskId = FETCH_TASK_ID;
          BackgroundFetch adapter = getAdapter();
          adapter.stop(taskId);
          success.invoke(true);
      }

      @ReactMethod
      public void status(Callback success) {
          BackgroundFetch adapter = getAdapter();
          success.invoke(adapter.status());
      }

      @ReactMethod
      public void finish(String taskId) {
          BackgroundFetch adapter = getAdapter();
          adapter.finish(taskId);
      }

      @ReactMethod
      public void addListener(String event) {
          // Keep:  Required for RN built-in NativeEventEmitter calls.
      }

      @ReactMethod
      public void removeListeners(Integer count) {
          // Keep:  Required for RN built-in NativeEventEmitter calls.
      }

      @Override
      public void onHostResume() {
          if (!initialized) {
              initializeBackgroundFetch();
          }
      }

      @Override
      public void onHostPause() {
      }

      @Override
      public void onNewIntent(Intent intent) {
      }

      @Override
      public void onHostDestroy() {
          LifecycleManager.getInstance().setHeadless(true);
          initialized = false;
      }

      @Override
      public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {

      }

      private BackgroundFetchConfig.Builder buildConfig(ReadableMap options) {
          BackgroundFetchConfig.Builder config = new BackgroundFetchConfig.Builder();
          if (options.hasKey(BackgroundFetchConfig.FIELD_MINIMUM_FETCH_INTERVAL)) {
              config.setMinimumFetchInterval(options.getInt(BackgroundFetchConfig.FIELD_MINIMUM_FETCH_INTERVAL));
          }
          if (options.hasKey(BackgroundFetchConfig.FIELD_TASK_ID)) {
              config.setTaskId(options.getString(BackgroundFetchConfig.FIELD_TASK_ID));
          }
          if (options.hasKey(BackgroundFetchConfig.FIELD_DELAY)) {
              Integer delay = options.getInt(BackgroundFetchConfig.FIELD_DELAY);
              config.setDelay(delay.longValue());
          }
          if (options.hasKey(BackgroundFetchConfig.FIELD_STOP_ON_TERMINATE)) {
              config.setStopOnTerminate(options.getBoolean(BackgroundFetchConfig.FIELD_STOP_ON_TERMINATE));
          }
          if (options.hasKey(BackgroundFetchConfig.FIELD_FORCE_ALARM_MANAGER)) {
              config.setForceAlarmManager(options.getBoolean(BackgroundFetchConfig.FIELD_FORCE_ALARM_MANAGER));
          }
          if (options.hasKey(BackgroundFetchConfig.FIELD_START_ON_BOOT)) {
              config.setStartOnBoot(options.getBoolean(BackgroundFetchConfig.FIELD_START_ON_BOOT));
          }
          if (options.hasKey("enableHeadless") && options.getBoolean("enableHeadless")) {
              config.setJobService(JOB_SERVICE_CLASS);
          }
          if (options.hasKey(BackgroundFetchConfig.FIELD_REQUIRED_NETWORK_TYPE)) {
              config.setRequiredNetworkType(options.getInt(BackgroundFetchConfig.FIELD_REQUIRED_NETWORK_TYPE));
          }
          if (options.hasKey(BackgroundFetchConfig.FIELD_REQUIRES_BATTERY_NOT_LOW)) {
              config.setRequiresBatteryNotLow(options.getBoolean(BackgroundFetchConfig.FIELD_REQUIRES_BATTERY_NOT_LOW));
          }
          if (options.hasKey(BackgroundFetchConfig.FIELD_REQUIRES_CHARGING)) {
              config.setRequiresCharging(options.getBoolean(BackgroundFetchConfig.FIELD_REQUIRES_CHARGING));
          }
          if (options.hasKey(BackgroundFetchConfig.FIELD_REQUIRES_DEVICE_IDLE)) {
              config.setRequiresDeviceIdle(options.getBoolean(BackgroundFetchConfig.FIELD_REQUIRES_DEVICE_IDLE));
          }
          if (options.hasKey(BackgroundFetchConfig.FIELD_REQUIRES_STORAGE_NOT_LOW)) {
              config.setRequiresStorageNotLow(options.getBoolean(BackgroundFetchConfig.FIELD_REQUIRES_STORAGE_NOT_LOW));
          }
          if (options.hasKey(BackgroundFetchConfig.FIELD_PERIODIC)) {
              config.setPeriodic(options.getBoolean(BackgroundFetchConfig.FIELD_PERIODIC));
          }
          return config;
      }

      private void initializeBackgroundFetch() {
          Activity activity = getCurrentActivity();
          if (activity == null) {
              return;
          }
          LifecycleManager.getInstance().setCurrentActivity(activity);
          initialized = true;
      }

      private BackgroundFetch getAdapter() {
          return BackgroundFetch.getInstance(getReactApplicationContext());
      }
}