package com.goniometer;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class Home extends Activity implements SensorEventListener {
    private SensorManager mgr;
    private Sensor accel;
    private Sensor compass;
    private Sensor orient;
    private TextView orientation;
    private boolean ready = false;
    private float[] accelValues = new float[3];
    private float[] compassValues = new float[3];
    private float[] inR = new float[9];
    private float[] inclineMatrix = new float[9];
    private float[] orientationValues = new float[3];
    private float[] prefValues = new float[3];
    private float mAzimuth;
    private double mInclination;
    private int counter;
    private int mRotation;
    private double angleDifference;
    private double angle;
    private double initDeviceAngle = 0;
    private float[] initDeviceAngleF = new float[3];
    private float[][] workingAngle = new float[3][3];

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        initDeviceAngleF = null;
        
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.main);

        /*
         * Calibrate button will work by saving the current angle (method call
         * in the XML), and displaying savedValue - currentValue which equals 0.
         * When there is a change in the angle, savedValue - currentValue will
         * be the difference. If this number is negative, we will multiple by
         * (-1) as negative values are of no use to us in this app.
         */

        Button calibrate = (Button) findViewById(R.id.btn1);
        orientation = (TextView) findViewById(R.id.orientation);

        mgr = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        accel = mgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        compass = mgr.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        orient = mgr.getDefaultSensor(Sensor.TYPE_ORIENTATION);

        WindowManager window = (WindowManager) this
                .getSystemService(WINDOW_SERVICE);
        int apiLevel = Integer.parseInt(Build.VERSION.SDK);

        if (apiLevel < 8) {
            mRotation = window.getDefaultDisplay().getOrientation();
        }

        else {
            mRotation = window.getDefaultDisplay().getRotation();
        }

    }

    /*
     * For those who don't know, SENSOR_DELAY has a variety of options that change how
     * frequently the sensors are updated. Game is the fastest setting. 
     */
    @Override
    protected void onResume() {
        mgr.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME);
        mgr.registerListener(this, compass, SensorManager.SENSOR_DELAY_GAME);
        mgr.registerListener(this, orient, SensorManager.SENSOR_DELAY_GAME);
        super.onResume();
    }

    @Override
    protected void onPause() {
        mgr.unregisterListener(this, accel);
        mgr.unregisterListener(this, compass);
        mgr.unregisterListener(this, orient);
        super.onPause();
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // ignore
    }

    public void onSensorChanged(SensorEvent event) {
        // Need to get both accelerometer and compass
        // before we can determine our orientationValues

        switch (event.sensor.getType()) {
        case Sensor.TYPE_ACCELEROMETER:
            for (int i = 0; i < 3; i++) {
                accelValues[i] = event.values[i];
                
            }
            if (compassValues[0] != 0)
                ready = true;
            break;
        case Sensor.TYPE_MAGNETIC_FIELD:
            for (int i = 0; i < 2; i++) {
                compassValues[i] = event.values[i];
            }
            if (accelValues[2] != 0)
                ready = true;
            break;
        case Sensor.TYPE_ORIENTATION:
            for (int i = 0; i < 2; i++) {
                orientationValues[i] = event.values[i];
            }
            break;
        }

        if (!ready)
            return;
        if (SensorManager.getRotationMatrix(inR, inclineMatrix, accelValues,
                compassValues)) {
            // got a good rotation matrix
            SensorManager.getOrientation(inR, prefValues);
            mInclination = SensorManager.getInclination(inclineMatrix);
            // Display every 10th value
            if (counter++ % 10 == 0) {
                doUpdate(null);
                counter = 1;
            }

        }
    }

    public void doUpdate(View view) {
        if (!ready)
            return;
        
        // for calibration purposes
        if(initDeviceAngleF == null){
        angle = Math.atan2(accelValues[0], accelValues[1])/(Math.PI/180);
        angleDifference = angle;
        }else {
            workingAngle = vectorMultiplication(accelValues,initDeviceAngleF);
            angleDifference = workingAngle[1][0];
        }

        // multiple by -1 to get the positive value
        //if (angleDifference < 0)
            //angleDifference = angleDifference;// * (-1);

        // display angle to one decimal place
        String msg = String.format("Angle: %7.1f \n", angleDifference);
        orientation.setText(msg);
        orientation.invalidate();
    }

    public void storeValue(View view) {
        for (int i=0; i< 3; i++)
        {
            initDeviceAngleF [i]= accelValues[i];  
        }
        initDeviceAngle = Math.atan2(accelValues[0], accelValues[1])/(Math.PI/180);
        doUpdate(null);
    }
    
    public static float[][] vectorMultiplication (float[] m1, float[] m2) {
        if (m1.length != m2.length)
            throw new IllegalArgumentException("Vectors need to have the same length");
        float[][] m = new float[m1.length][m1.length];
        for (int i=0; i<m1.length; i++)
            for (int j=0; j<m1.length; j++)
                m[i][j] = (m1[i]*m2[j]);
        return m;
    }

}