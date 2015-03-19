package fr.istic.lechazentou.fataldestination.connection.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Created by erwann on 11/03/15.
 */
public class BluetoothService {

    private static final String TAG = "Bluetooth Connect Service";
    private static final String NAME_SECURE = "FatalDestinationBluetoothSecure";
    private static final String NAME_INSECURE = "FatalDestinationBluetoothInsecure";
    // Unique UUID for this application
    private static final UUID MY_UUID_SECURE =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    private BluetoothAdapter bluetoothAdapter;
    private Handler handler;
    private int state;
    private AcceptThread secureAcceptThread;
    private AcceptThread unsecureAcceptThread;
    private ConnectedThread connectedThread;
    private ConnectThread connectThread;


    public BluetoothService(Context context, Handler handler) {
        this.handler = handler;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        state = STATE_NONE;
    }

    private synchronized void setState(int state){
        this.state = state;
        handler.obtainMessage(state,-1).sendToTarget();
    }

    public synchronized int getState(){
        return state;
    }

    public synchronized void start(){
        if (connectThread != null){
            connectThread.cancel();
            connectThread = null;
        }
        if (connectedThread != null){
            connectedThread.cancel();
            connectedThread = null;
        }

        setState(STATE_LISTEN);

        if (secureAcceptThread == null){
            secureAcceptThread = new AcceptThread(true);
            secureAcceptThread.start();
        }
        if (unsecureAcceptThread == null){
            unsecureAcceptThread = new AcceptThread(false);
            unsecureAcceptThread.start();
        }
    }

    public synchronized void connect(BluetoothDevice device, boolean secure) {
        if (state == STATE_CONNECTING) {
            if (connectThread != null) {connectThread.cancel(); connectThread = null;}
        }
        if (connectedThread != null) {connectedThread.cancel(); connectedThread = null;}
        connectThread = new ConnectThread(device, secure);
        connectThread.start();
        setState(STATE_CONNECTING);
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device, final String socketType) {

        if (connectThread != null) {connectThread.cancel(); connectThread = null;}

        if (connectedThread != null) {connectedThread.cancel(); connectedThread = null;}

        if (secureAcceptThread != null) {secureAcceptThread.cancel(); secureAcceptThread = null;}

        if (unsecureAcceptThread != null) {unsecureAcceptThread.cancel(); unsecureAcceptThread = null;}

        connectedThread = new ConnectedThread(socket, socketType);
        connectedThread.start();

        //Message msg = handler.obtainMessage(BluetoothChat.MESSAGE_DEVICE_NAME);
        //Bundle bundle = new Bundle();
        //bundle.putString(BluetoothChat.DEVICE_NAME, device.getName());
        //msg.setData(bundle);
        //handler.sendMessage(msg);
        setState(STATE_CONNECTED);
    }

    public synchronized void stop() {
        if (connectThread != null) {connectThread.cancel(); connectThread = null;}
        if (connectedThread != null) {connectedThread.cancel(); connectedThread = null;}
        if (secureAcceptThread != null) {secureAcceptThread.cancel(); secureAcceptThread = null;}
        if (unsecureAcceptThread != null) {unsecureAcceptThread.cancel(); unsecureAcceptThread = null;}
        setState(STATE_NONE);
    }

    private void connectionFailed() {
        BluetoothService.this.start();
    }

    private void connectionLost() {
        BluetoothService.this.start();
    }

    public void send(){
        ConnectedThread connectedThread1;
        synchronized (this) {
            if (state != STATE_CONNECTED)return;
            connectedThread1 = connectedThread;
        }
        connectedThread1.send();
    }

    private class AcceptThread extends Thread {

        private final BluetoothServerSocket serverSocket;
        private String socketType;

        public AcceptThread(boolean secure) {
            BluetoothServerSocket tmp = null;
            socketType = secure ? "Secure" : "Insecure";
            try {
                if (secure){
                    tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE, MY_UUID_SECURE);
                }else {
                    tmp = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME_INSECURE, MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket type : "+socketType+" listen() failed", e);
            }
            serverSocket = tmp;
        }

        public void run(){
            setName("AcceptThread"+socketType);
            BluetoothSocket socket = null;

            while (state != STATE_CONNECTED) {
                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "accept() failed", e);
                    break;
                }

                if (socket != null) {
                    synchronized (BluetoothService.this) {
                        switch (state) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                Log.i(TAG,"STATE CONNECTING");
                                connected(socket, socket.getRemoteDevice(), socketType);
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close socket", e);
                                }
                                break;
                        }
                    }
                }
            }
        }

        public void cancel(){
            try {
                serverSocket.close();
            } catch (IOException e){
                Log.e(TAG, "close() server failed", e);
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket socket;
        private final BluetoothDevice device;
        private String socketType;

        public ConnectThread(BluetoothDevice device, boolean secure) {
            this.device = device;
            BluetoothSocket tmp = null;
            socketType = secure ? "Secure" : "Insecure";

            try {
                if (secure){
                    tmp = device.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
                }else {
                    tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID_INSECURE);
                }
            } catch (IOException e){
                Log.e(TAG, "create() failed", e);
            }
            socket = tmp;
        }

        public void run(){
            setName("ConnectThread"+socketType);

            bluetoothAdapter.cancelDiscovery();

            try {
                socket.connect();
            } catch (IOException e){
                try {
                    socket.close();
                } catch (IOException e1){
                    Log.e(TAG, "unable to close socket during connection fail", e1);
                }
                connectionFailed();
                return;
            }

            synchronized (BluetoothService.this) {
                connectThread = null;
            }

            connected(socket, device, socketType);
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e){
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }

    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket socket;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            this.socket = socket;
        }

        public void run() {
            while (true) {
                handler.obtainMessage(BluetoothConstants.ACTION_SEND)
                        .sendToTarget();
            }
        }

        public void send() {
                handler.obtainMessage(BluetoothConstants.ACTION_SEND).sendToTarget();
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }


}
