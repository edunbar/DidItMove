package com.example.ericdunbar.diditmove;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.example.ericdunbar.diditmove.MyService.MyBinder;


public class MainActivity extends AppCompatActivity
        implements com.example.ericdunbar.diditmove.MyServiceTask.ResultCallback {

    public static final int DISPLAY_NUMBER = 10;
    private Handler mUiHandler;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Rect mSurfaceSize;
    Button btnExit;
    Button btnClear;


    private static final String LOG_TAG = "MainActivity";

    // Service connection variables.
    private boolean serviceBound;
    private MyService myService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mUiHandler = new Handler(getMainLooper(), new UiCallback());
        serviceBound = false;
        // Prevents the screen from dimming and going to sleep.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);


        btnExit =(Button)findViewById(R.id.btnExit);
        btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (serviceBound) {
                    if (myService != null) {
                        myService.removeResultCallback(MainActivity.this);
                    }
                    Log.i("MyService", "Unbinding");
                    unbindService(serviceConnection);
                    serviceBound = false;
                }

                Log.i(LOG_TAG, "Stopping.");
                Intent intent = new Intent(MainActivity.this, MyService.class);
                stopService(intent);
                Log.i(LOG_TAG, "Stopped.");
                finish();
            }
        });

        btnClear = (Button) findViewById(R.id.btnClear);
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyService myService = new MyService();
                myService.clearButtonPressed();
            }
        });

    }


    @Override
    protected void onResume() {
        super.onResume();
        // Starts the service, so that the service will only stop when explicitly stopped.
        Intent intent = new Intent(this, MyService.class);
        startService(intent);
        bindMyService();
    }

    private void bindMyService() {
        // We are ready to show images, and we should start getting the bitmaps
        // from the motion detection service.
        // Binds to the service.
        Log.i(LOG_TAG, "Starting the service");
        Intent intent = new Intent(this, MyService.class);
        Log.i("LOG_TAG", "Trying to bind");
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }


    // Service connection code.
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder serviceBinder) {
            // We have bound to the camera service.
            MyBinder binder = (MyBinder) serviceBinder;
            myService = binder.getService();
            serviceBound = true;
            // Let's connect the callbacks.
            Log.i("MyService", "Bound succeeded, adding the callback");
            myService.addResultCallback(MainActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            serviceBound = false;
        }
    };

    @Override
    protected void onPause() {
        if (serviceBound) {
            if (myService != null) {
                myService.removeResultCallback(this);
            }
            Log.i("MyService", "Unbinding");
            unbindService(serviceConnection);
            serviceBound = false;
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (serviceBound) {
            if (myService != null) {
                myService.removeResultCallback(this);
            }
            Log.i("MyService", "Unbinding");
            unbindService(serviceConnection);
            serviceBound = false;
        }
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        if (serviceBound) {
            if (myService != null) {
                myService.removeResultCallback(this);
            }
            Log.i("MyService", "Unbinding");
            unbindService(serviceConnection);
            serviceBound = false;
        }
        super.onStop();
    }


    /**
     * This function is called from the service thread.  To process this, we need
     * to create a message for a handler in the UI thread.
     */
    @Override
    public void onResultReady(ServiceResult result) {
        if (result != null) {
            Log.i(LOG_TAG, "Preparing a message for " + result.intValue);
        } else {
            Log.e(LOG_TAG, "Received an empty result!");
        }
        mUiHandler.obtainMessage(DISPLAY_NUMBER, result).sendToTarget();
    }

    /**
     * This Handler callback gets the message generated above.
     * It is used to display the integer on the screen.
     */
    private class UiCallback implements Handler.Callback {
        @Override
        public boolean handleMessage(Message message) {
            if (message.what == DISPLAY_NUMBER) {
                // Gets the result.
                ServiceResult result = (ServiceResult) message.obj;
                // Displays it.
                if (result != null) {
                    Log.i(LOG_TAG, "Displaying: " + result.intValue);
                    TextView tv = (TextView) findViewById(R.id.textView);
                    int check = result.intValue;
                    if (check == 0) {
                        tv.setText("Phone Did Not Move");
                        // Tell the worker that the bitmap is ready to be reused
                        if (serviceBound && myService != null) {
                            Log.i(LOG_TAG, "Releasing result holder for " + result.intValue);
                            myService.releaseResult(result);
                        }
                    } else if (check == 1) {
                        tv.setText("Phone Moved!");
                        // Tell the worker that the bitmap is ready to be reused
                        if (serviceBound && myService != null) {
                            Log.i(LOG_TAG, "Releasing result holder for " + result.intValue);
                            myService.releaseResult(result);
                        }
                    }
                }
                else {
                    Log.e(LOG_TAG, "Error: received empty message!");
                }
            }
            return true;
        }
    }

}