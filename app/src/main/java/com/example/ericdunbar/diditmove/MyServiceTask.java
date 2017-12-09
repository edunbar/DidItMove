package com.example.ericdunbar.diditmove;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.util.Log;

import java.awt.font.TextAttribute;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by EricDunbar on 12/4/17.
 */

public class MyServiceTask implements Runnable {


    public static final String LOG_TAG = "MyServiceTask";
    private boolean running;
    private Context context;
    public boolean moved;
    private boolean move;
    private static long firstDate = System.currentTimeMillis();
    private int first_accel_time = 0;
    private int valueOfIfMoved;

    private Set<ResultCallback> resultCallbacks = Collections.synchronizedSet(
            new HashSet<ResultCallback>());
    private ConcurrentLinkedQueue<ServiceResult> freeResults =
            new ConcurrentLinkedQueue<ServiceResult>();

    public MyServiceTask(Context _context) {
        context = _context;
        // Put here what to do at creation.
    }


    @Override
    public void run() {


        running = true;
        while (running) {
            // Sleep a tiny bit.
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                e.getLocalizedMessage();
            }

            first_accel_time = getTime();

            if (first_accel_time < 30) {
                MyService.reset();
            }

            int itMoved = (MyService.itMoved());

            move = didItMove(first_accel_time, itMoved);

            if (move) {
                valueOfIfMoved = 1;
            }
            else {
                valueOfIfMoved = 0;
            }

            Log.i(LOG_TAG, "Sending current time: " + valueOfIfMoved);
            notifyResultCallback(valueOfIfMoved);
        }
    }


    public boolean didItMove(int first_accel_time, int itMoved) {


        if (itMoved == 1 && first_accel_time > 30) {
            return true;
        }
        else {
            return false;
        }
    }


    public void resetClock() {
        firstDate = System.currentTimeMillis();
    }

    public int getTime(){
        SimpleDateFormat constantMins = new SimpleDateFormat("mm");
        SimpleDateFormat constantSecs = new SimpleDateFormat("ss");

        String firstMinutes = constantMins.format(firstDate);
        String firstSeconds = constantSecs.format(firstDate);

        int firstMin = Integer.parseInt(firstMinutes);
        int firstSec = Integer.parseInt(firstSeconds);

        int startingTime = (firstMin * 60) + firstSec;

        long date = System.currentTimeMillis();
        SimpleDateFormat mins = new SimpleDateFormat("mm");
        SimpleDateFormat secs = new SimpleDateFormat("ss");

        String minutes = mins.format(date);
        String seconds = secs.format(date);

        int secondMin = Integer.parseInt(minutes);
        int secondSec = Integer.parseInt(seconds);

        int time = (secondMin * 60) + secondSec;

        return time - startingTime;
    }


    public void addResultCallback(ResultCallback resultCallback) {
        Log.i(LOG_TAG, "Adding result callback");
        resultCallbacks.add(resultCallback);
    }


    public void removeResultCallback(ResultCallback resultCallback) {
        Log.i(LOG_TAG, "Removing result callback");
        // We remove the callback...
        resultCallbacks.remove(resultCallback);
        // ...and we clear the list of results.
        // Note that this works because, even though mResultCallbacks is a synchronized set,
        // its cardinality should always be 0 or 1 -- never more than that.
        // We have one viewer only.
        // We clear the buffer, because some result may never be returned to the
        // free buffer, so using a new set upon reattachment is important to avoid
        // leaks.
        freeResults.clear();
    }

    // Creates result bitmaps if they are needed.
    private void createResultsBuffer() {
        // I create some results to talk to the callback, so we can reuse these instead of creating new ones.
        // The list is synchronized, because integers are filled in the service thread,
        // and returned to the free pool from the UI thread.
        freeResults.clear();
        for (int i = 0; i < 10; i++) {
            freeResults.offer(new ServiceResult());
        }
    }

    // This is called by the UI thread to return a result to the free pool.
    public void releaseResult(ServiceResult r) {
        Log.i(LOG_TAG, "Freeing result holder for " + r.intValue);
        freeResults.offer(r);
    }

    public void stopProcessing() {
        running = false;
    }

    public void setTaskState(boolean b) {
        // Do something with b.
    }

    /**
     * Call this function to return the integer i to the activity.
     * @param i
     */
    private void notifyResultCallback(int i) {
        if (!resultCallbacks.isEmpty()) {
            // If we have no free result holders in the buffer, then we need to create them.
            if (freeResults.isEmpty()) {
                createResultsBuffer();
            }
            ServiceResult result = freeResults.poll();
            // If we got a null result, we have no more space in the buffer,
            // and we simply drop the integer, rather than sending it back.
            if (result != null) {
                result.intValue = i;
                for (ResultCallback resultCallback : resultCallbacks) {
                    Log.i(LOG_TAG, "calling resultCallback for " + result.intValue);
                    resultCallback.onResultReady(result);
                }
            }
        }
    }

    public interface ResultCallback {
        void onResultReady(ServiceResult result);
    }
}
