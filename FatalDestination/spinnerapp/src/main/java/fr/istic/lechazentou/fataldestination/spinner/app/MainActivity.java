package fr.istic.lechazentou.fataldestination.spinner.app;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.Toast;

import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.facebook.widget.LoginButton;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Random;

import fr.istic.lechazentou.fataldestination.connection.bluetooth.BluetoothService;
import fr.istic.lechazentou.fataldestination.connection.bluetooth.DeviceListActivity;
import fr.istic.lechazentou.fataldestination.spinner.map.MapFragment;


public class MainActivity extends ActionBarActivity {

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final int ACTION_SEND = 6;
    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothService mBluetoothService = null;

    private LoginButton loginBtn;

    private UiLifecycleHelper uiHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spinner);

        uiHelper = new UiLifecycleHelper(this, statusCallback);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        DatePicker datePicker = (DatePicker)findViewById(R.id.date_picker);
        datePicker.setEnabled(false);
        loginBtn = (LoginButton) findViewById(R.id.fb_login_button);

        loginBtn.setReadPermissions(Arrays.asList("email", "user_friends", "user_birthday"));
        loginBtn.setUserInfoChangedCallback(new LoginButton.UserInfoChangedCallback() {
            @Override
            public void onUserInfoFetched(GraphUser user) {
                if (user != null) {
                    Toast.makeText(getApplicationContext(), "You are currently logged in as " + user.getName() + user.getBirthday(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "You are not logged in.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private Session.StatusCallback statusCallback = new Session.StatusCallback() {
        @Override
        public void call(Session session, SessionState state,
                         Exception exception) {
            if (state.isOpened()) {
                Log.d("MainActivity", "Facebook session opened.");
            } else if (state.isClosed()) {
                Log.d("MainActivity", "Facebook session closed.");
            }
        }
    };

    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            Toast.makeText(getApplicationContext(), "Connecting", Toast.LENGTH_SHORT).show();
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            Toast.makeText(getApplicationContext(), "Not connected", Toast.LENGTH_SHORT).show();
                            break;
                    }
                    break;
                /*case MESSAGE_READ:
                    spin();
                    break;*/
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
                case ACTION_SEND:
                    spin();
                    break;
            }
        }
    };

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        uiHelper.onSaveInstanceState(outState);
    }

    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
        } else {
            if (mBluetoothService == null) mBluetoothService = new BluetoothService(this, mHandler);
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();

        uiHelper.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mBluetoothService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mBluetoothService.getState() == BluetoothService.STATE_NONE) {
                // Start the Bluetooth chat services
                mBluetoothService.start();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        uiHelper.onPause();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent){
        uiHelper.onActivityResult(requestCode, resultCode, intent);
        switch (requestCode){
            case REQUEST_CONNECT_DEVICE_SECURE:
                if (resultCode == Activity.RESULT_OK){
                    connectDevice(intent, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                if (resultCode == Activity.RESULT_OK){
                    connectDevice(intent, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK){
                    mBluetoothService = new BluetoothService(this, mHandler);
                }else {
                    finish();
                }
        }
    }

    private void spin() {
        spin(null);
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void spin(View view) {
        final DatePicker datePicker = (DatePicker)findViewById(R.id.date_picker);
        datePicker.animate()
                .rotationX(1800)
                .setDuration(3000)
                .setUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        int LowY = Calendar.getInstance().get(Calendar.YEAR);
                        int year = new Random().nextInt(50) + LowY;
                        int month = new Random().nextInt(12 -
                                (LowY == year ? Calendar.getInstance().get(Calendar.MONTH) : 1)) +
                                (LowY == year ? Calendar.getInstance().get(Calendar.MONTH) : 1);
                        int day = new Random().nextInt(31 -
                                (year == LowY && month == Calendar.getInstance().get(Calendar.MONTH) ? Calendar.getInstance().get(Calendar.DAY_OF_MONTH) : 1)) +
                                (year == LowY && month == Calendar.getInstance().get(Calendar.MONTH) ? Calendar.getInstance().get(Calendar.DAY_OF_MONTH) : 1);
                        datePicker.updateDate(year, month, day);
                    }
                });
        //Random to long and lat max
        ((MapFragment)getFragmentManager().findFragmentById(R.id.map_fragment))
                .createMarketWithPerson(new Random().nextDouble() * 90 * (new Random().nextBoolean() ? 1 : -1),
                        new Random().nextDouble() * 180 * (new Random().nextBoolean() ? 1 : -1),
                        BitmapFactory.decodeResource(getResources(), R.drawable.macaque), "Jean-Luc");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_spinner, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.scan_secure){
            // Launch the DeviceListActivity to see devices and do scan
            Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
            return true;
        }else if(item.getItemId() == R.id.scan_insecure){
            // Launch the DeviceListActivity to see devices and do scan
            Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
            return true;
        }else if(item.getItemId() == R.id.discoverable) {
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();
        // Stop the Bluetooth chat services
        if (mBluetoothService != null) mBluetoothService.stop();
    }

    private void connectDevice(Intent data, boolean secure){
        String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        mBluetoothService.connect(device, secure);
    }
}
