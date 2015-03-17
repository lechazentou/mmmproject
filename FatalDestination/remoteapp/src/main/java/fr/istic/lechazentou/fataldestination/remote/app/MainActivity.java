package fr.istic.lechazentou.fataldestination.remote.app;

import android.app.Activity;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;


public class MainActivity extends Activity implements SensorEventListener {
    private SensorManager sensorManager;
    private boolean goodPos = false;
    private TextView textView;
    private long lastUpdate = 0;
    private float last_x, last_y, last_z;



    /** Called when the activity is first created. */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote);
        textView = (TextView) findViewById(R.id.textViewInfo);
        badPosition();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        lastUpdate = System.currentTimeMillis();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            getAccelerometer(event);
        }

    }

    private void getAccelerometer(SensorEvent sensorEvent) {

        float x = sensorEvent.values[0];
        float y = sensorEvent.values[1];
        float z = sensorEvent.values[2];

        long curTime = System.currentTimeMillis();

        if ((curTime - lastUpdate) > 1500) {
            long diffTime = (curTime - lastUpdate);
            lastUpdate = curTime;

            float speed = Math.abs(x + y + z - last_x - last_y - last_z)/ diffTime * 10000;

            String txt = " X : "  + x;

            txt += "\n Y : "  + y;

            txt += "\n Z : "  + z;

            txt += "\n speed : " + speed;


            if (-2 < x && x < 3 && 8 < y && -5 < z && z < 5){
                goodPosition();
            }
            else if (x < -4 && 8 > y && -5 < z && z < 5 && goodPos){
                sendSignal();
            }
            else if (-5 > z || z > 5 || x >2){
                badPosition();
            }

            //textView.setText(txt);

            last_x = x;
            last_y = y;
            last_z = z;
        }
    }

    private void goodPosition(){
        String txt = "GOOD ! LET'S GO !";
        textView.setText(txt);
        textView.setBackgroundColor(Color.BLUE);
        goodPos = true;
    }

    private void badPosition(){
        String txt = "Put the device in good position";
        textView.setText(txt);
        textView.setBackgroundColor(Color.WHITE);
        goodPos = false;
    }
    private void sendSignal(){
        // TODO
        textView.setBackgroundColor(Color.RED);
    }




    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        // register this class as a listener for the orientation and
        // accelerometer sensors
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        // unregister listener
        super.onPause();
        sensorManager.unregisterListener(this);
    }
}

