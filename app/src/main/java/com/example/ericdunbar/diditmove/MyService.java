package com.example.ericdunbar.diditmove;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.FloatMath;
import android.util.Log;
import java.util.Calendar;
import java.util.Date;


import com.example.ericdunbar.diditmove.MyServiceTask.ResultCallback;

/**
 * Created by EricDunbar on 12/4/17.
 */

public class MyService extends Service implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    float [] gravity = new float[2];
    float [] linear_acceleration = new float[2];
    public static int counter = 0;

    private static final String LOG_TAG = "MyService";

    // Handle to notification manager.
    private NotificationManager notificationManager;
    private int ONGOING_NOTIFICATION_ID = 1; // This cannot be 0. So 1 is a good candidate.

    // Motion detector thread and runnable.
    private Thread myThread;
    private MyServiceTask myTask;

    // Binder given to clients
    private final IBinder myBinder = new MyBinder();

    // Binder class.
    public class MyBinder extends Binder {
        MyService getService() {
            // Returns the underlying service.
            return MyService.this;
        }
    }

    public MyService() {
    }

    @Override
    public void onCreate() {

        Log.i(LOG_TAG, "Service is being created");

        // Display a notification about us starting.  We put an icon in the status bar.
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        // Creates the thread running the camera service.
        myTask = new MyServiceTask(getApplicationContext());
        myThread = new Thread(myTask);
        myThread.start();

    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        final float alpha = (float) 0.8;

        // Isolate the force of gravity with the low-pass filter.
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];

        // Remove the gravity contribution with the high-pass filter.
        linear_acceleration[0] = event.values[0] - gravity[0];
        linear_acceleration[1] = event.values[1] - gravity[1];

        if(linear_acceleration[0] > 1) {
            Log.d(LOG_TAG, "movement: " + linear_acceleration[0]);
            counter = 1;
        }

        if(linear_acceleration[1] > 1) {
            Log.d(LOG_TAG, "movement: " + linear_acceleration[1]);
            counter = 1;
        }

    }

    public static int itMoved() {
        return counter;
    }

    public static void reset() {
        counter = 0;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void clearButtonPressed() {

        MyServiceTask myServiceTask = new MyServiceTask(this);
        myServiceTask.resetClock();
        MyService.reset();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(LOG_TAG, "Service is being bound");
        // Returns the binder to this service.
        return myBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.i(LOG_TAG, "Received start id " + startId + ": " + intent);
        // We start the task thread.
        if (!myThread.isAlive()) {
            myThread.start();

        }

        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
        notificationManager.cancel(ONGOING_NOTIFICATION_ID);
        Log.i(LOG_TAG, "Stopping.");
        // Stops the motion detector.
        myTask.stopProcessing();
        Log.i(LOG_TAG, "Stopped.");
    }

    // Interface to be able to subscribe to the bitmaps by the service.

    public void releaseResult(ServiceResult result) {
        myTask.releaseResult(result);
    }

    public void addResultCallback(ResultCallback resultCallback) {
        myTask.addResultCallback(resultCallback);
    }

    public void removeResultCallback(ResultCallback resultCallback) {
        myTask.removeResultCallback(resultCallback);
    }

    // Interface which sets recording on/off.
    public void setTaskState(boolean b) {
        myTask.setTaskState(b);
    }

}