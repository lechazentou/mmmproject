package fr.istic.lechazentou.fataldestination.spinner.app;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.Toast;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.facebook.widget.LoginButton;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import fr.istic.lechazentou.fataldestination.connection.bluetooth.BluetoothConstants;
import fr.istic.lechazentou.fataldestination.connection.bluetooth.BluetoothService;
import fr.istic.lechazentou.fataldestination.connection.bluetooth.DeviceListActivity;
import fr.istic.lechazentou.fataldestination.spinner.map.MapFragment;


public class MainActivity extends ActionBarActivity {

    private static final String TAG = "MainActivity";

    // Intent request codes
    private static final int REQUEST_ENABLE_BT = 3;
    /**
     * Name of the connected device
     */
    private String connectedDeviceName = null;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the chat services
     */
    private BluetoothService bluetoothService = null;

    /**
     * Facebook login button
     */
    private LoginButton loginBtn;

    /**
     * Facebook helper
     */
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
        //Disable date picker
        DatePicker datePicker = (DatePicker)findViewById(R.id.date_picker);
        datePicker.setEnabled(false);

        //Initialize login button
        loginBtn = (LoginButton) findViewById(R.id.fb_login_button);
        loginBtn.setReadPermissions(Arrays.asList("email", "user_friends", "user_birthday"));
        loginBtn.setUserInfoChangedCallback(new LoginButton.UserInfoChangedCallback() {
            @Override
            public void onUserInfoFetched(GraphUser user) {
                if (user != null) {
                    Toast.makeText(getApplicationContext(), "You are currently logged in as " + user.getName() + user.getBirthday(), Toast.LENGTH_SHORT).show();
                    loginBtn.setVisibility(View.GONE);
                } else {
                    Toast.makeText(getApplicationContext(), "You are not logged in.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    //Facebook status callback for logging
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        uiHelper.onSaveInstanceState(outState);
    }

    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setup() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else if (bluetoothService == null) {
            setup();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bluetoothService != null) {
            bluetoothService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        uiHelper.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (bluetoothService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (bluetoothService.getState() == BluetoothService.STATE_NONE) {
                // Start the Bluetooth chat services
                bluetoothService.start();
            }
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        uiHelper.onPause();
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
    }public void compatibleSpin() {
        if(Build.VERSION.SDK_INT >= 19)
            highSpin();
        else
            lowSpin();
    }

    private void lowSpin() {
        final DatePicker datePicker = (DatePicker)findViewById(R.id.date_picker);
        int LowY = Calendar.getInstance().get(Calendar.YEAR);
        int year = new Random().nextInt(50) + LowY;
        int month = new Random().nextInt(12 -
                (LowY == year ? Calendar.getInstance().get(Calendar.MONTH) : 1)) +
                (LowY == year ? Calendar.getInstance().get(Calendar.MONTH) : 1);
        int day = new Random().nextInt(31 -
                (year == LowY && month == Calendar.getInstance().get(Calendar.MONTH) ? Calendar.getInstance().get(Calendar.DAY_OF_MONTH) : 1)) +
                (year == LowY && month == Calendar.getInstance().get(Calendar.MONTH) ? Calendar.getInstance().get(Calendar.DAY_OF_MONTH) : 1);
        datePicker.updateDate(year, month, day);
        Request request = Request.newMyFriendsRequest(Session.getActiveSession(), new Request.GraphUserListCallback() {

            @Override
            public void onCompleted(List<GraphUser> users, Response response) throws Exception {
                int friend = new Random().nextInt(users.size());
                Log.i("mustang", "response; " + response.toString());
                Log.i("mustang", "UserListSize: " + users.size());
                Log.i("mustang", users.get(friend).getId() + " " + users.get(friend).getFirstName() + " " + users.get(friend).getLastName());
                String userID = users.get(friend).getId();
                new DownloadImageTask(MainActivity.this, users.get(friend).getFirstName()).execute("https://graph.facebook.com/" + userID + "/picture?type=large");

            }
        });
        Bundle bundle = request.getParameters();
        bundle.putString("fields", "id,first_name,last_name");
        request.executeAsync();
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void highSpin() {
        final DatePicker datePicker = (DatePicker)findViewById(R.id.date_picker);
        datePicker.clearAnimation();
        datePicker.animate()
                .rotationX(0)
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
        Request request = Request.newMyFriendsRequest(Session.getActiveSession(), new Request.GraphUserListCallback() {

            @Override
            public void onCompleted(List<GraphUser> users, Response response) throws Exception {
                int friend = new Random().nextInt(users.size());
                Log.i("mustang", "response; " + response.toString());
                Log.i("mustang", "UserListSize: " + users.size());
                Log.i("mustang", users.get(friend).getId() + " " + users.get(friend).getFirstName() + " " + users.get(friend).getLastName());
                String userID = users.get(friend).getId();
                new DownloadImageTask(MainActivity.this, users.get(friend).getFirstName()).execute("https://graph.facebook.com/" + userID + "/picture?type=large");

            }
        });
        Bundle bundle = request.getParameters();
        bundle.putString("fields", "id,first_name,last_name");
        request.executeAsync();

    }

    /**
     * Display a marker on the map containing the name of the user and his profile photo
     *
     * @param userName The first name of the user
     * @param bitmap The profile picture of the user
     */
    public void displayMarker(String userName, Bitmap bitmap) {
        //Random to long and lat max
        ((MapFragment)getFragmentManager().findFragmentById(R.id.map_fragment))
                .createMarketWithPerson(new Random().nextDouble() * 90 * (new Random().nextBoolean() ? 1 : -1),
                        new Random().nextDouble() * 180 * (new Random().nextBoolean() ? 1 : -1),
                        bitmap, userName);
    }

    /**
     * Set up the UI and background operations for chat.
     */
    private void setup() {
        Log.d(TAG, "setup()");

        // Initialize the BluetoothChatService to perform bluetooth connections
        bluetoothService = new BluetoothService(this.getApplicationContext(), mHandler);
    }

    /**
     * Makes this device discoverable.
     */
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
        final ActionBar actionBar = this.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        final ActionBar actionBar = this.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BluetoothConstants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, connectedDeviceName));
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case BluetoothConstants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    Toast.makeText(getApplicationContext(), readMessage,
                            Toast.LENGTH_SHORT).show();
                    highSpin();
                    break;
                case BluetoothConstants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    connectedDeviceName = msg.getData().getString(BluetoothConstants.DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + connectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case BluetoothConstants.MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(BluetoothConstants.TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        uiHelper.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setup();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    /**
     * Establish connection with other device
     *
     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        bluetoothService.connect(device, secure);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_spinner, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == R.id.discoverable) {
            ensureDiscoverable();
            return true;
        }
        return false;
    }
}
