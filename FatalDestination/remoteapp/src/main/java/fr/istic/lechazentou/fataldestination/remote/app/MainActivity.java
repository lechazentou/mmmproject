package fr.istic.lechazentou.fataldestination.remote.app;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import fr.istic.lechazentou.fataldestination.connection.bluetooth.BluetoothService;
import fr.istic.lechazentou.fataldestination.connection.bluetooth.DeviceListActivity;

import android.widget.TextView;


public class MainActivity extends Activity implements SensorEventListener {
    private SensorManager sensorManager;
    private boolean goodPos = false;
    private TextView textView;
    private long lastUpdate = 0;
    private float last_x, last_y, last_z;



    /** Called when the activity is first created. */

    private static final String TAG = "RemoteApp";

    private static final int ACTION_SEND = 1;

    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothService bluetoothService = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote);
        textView = (TextView) findViewById(R.id.textViewInfo);
        badPosition();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        lastUpdate = System.currentTimeMillis();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null){
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
        }
    }

    public void onStart() {
        super.onStart();
        Log.i(TAG, "Starting remote");
        if (!bluetoothAdapter.isEnabled()) {
            Intent requestBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(requestBluetooth, REQUEST_ENABLE_BT);
        } else {
            if (bluetoothService == null) {
                bluetoothService = new BluetoothService(this, handler);
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            getAccelerometer(event);
        }
        textView = (TextView) findViewById(R.id.textViewInfo);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        lastUpdate = System.currentTimeMillis();
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
                textView.setBackgroundColor(Color.RED);
                sendSignal();
            }
            else if (-5 > z || z > 5 || x >2){
                textView.setBackgroundColor(Color.WHITE);
                goodPos = false;
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
        handler.sendEmptyMessage(ACTION_SEND);
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

    private final Handler handler = new Handler();

    public void onActivityResult(int requestCode, int resultCode, Intent intent){
        switch (requestCode){
            case REQUEST_CONNECT_DEVICE:
                if (resultCode == Activity.RESULT_OK){
                    String address = intent.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
                    bluetoothService.connect(device);
                }
                break;
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK){
                    bluetoothService = new BluetoothService(this, handler);
                }
        }
    }
}

