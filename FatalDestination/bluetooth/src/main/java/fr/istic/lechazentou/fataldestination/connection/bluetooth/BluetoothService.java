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
import java.util.UUID;

/**
 * Created by erwann on 11/03/15.
 */
public class BluetoothService {

    private static final String TAG = "Bluetooth Connect Service";
    private static final String NAME = "FatalDestinationBluetooth";
    private static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    private static final int ACTION_SEND = 1;

    private BluetoothAdapter bluetoothAdapter;
    private Handler handler;
    private int state;
    private AcceptThread acceptThread;
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
        if (acceptThread == null){
            acceptThread = new AcceptThread();
            acceptThread.start();
        }
        setState(STATE_LISTEN);
    }

    public synchronized void connect(BluetoothDevice device) {
        if (state == STATE_CONNECTING) {
            if (connectThread != null) {connectThread.cancel(); connectThread = null;}
        }
        if (connectedThread != null) {connectedThread.cancel(); connectedThread = null;}
        connectThread = new ConnectThread(device);
        connectThread.start();
        setState(STATE_CONNECTING);
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {

        if (connectThread != null) {connectThread.cancel(); connectThread = null;}

        if (connectedThread != null) {connectedThread.cancel(); connectedThread = null;}

        if (acceptThread != null) {acceptThread.cancel(); acceptThread = null;}

        connectedThread = new ConnectedThread(socket);
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
        if (acceptThread != null) {acceptThread.cancel(); acceptThread = null;}
        setState(STATE_NONE);
    }

    private void connectionFailed() {
        setState(STATE_LISTEN);
        //Message msg = handler.obtainMessage(BluetoothChat.MESSAGE_TOAST);
        //Bundle bundle = new Bundle();
        //bundle.putString(BluetoothChat.TOAST, "Unable to connect device");
        //msg.setData(bundle);
        //handler.sendMessage(msg);
    }

    private void connectionLost() {
        setState(STATE_LISTEN);
        //Message msg = handler.obtainMessage(BluetoothChat.MESSAGE_TOAST);
        //Bundle bundle = new Bundle();
        //bundle.putString(BluetoothChat.TOAST, "Device connection was lost");
        //msg.setData(bundle);
        //handler.sendMessage(msg);
    }

    private class AcceptThread extends Thread {

        private final BluetoothServerSocket serverSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            try {
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e){
                Log.e(TAG, "listen failed", e);
            }
            serverSocket = tmp;
        }

        public void run(){
            setName("AcceptThread");
            BluetoothSocket socket = null;

            while (state != STATE_CONNECTED){
                try {
                    socket = serverSocket.accept();
                }catch (IOException e){
                    Log.e(TAG, "accept() failed", e);
                    break;
                }
            }

            if (socket != null){
                synchronized (BluetoothService.this){
                    switch (state){
                        case STATE_LISTEN:
                        case STATE_CONNECTING:
                            connected(socket, socket.getRemoteDevice());
                            break;
                        case STATE_NONE:
                        case STATE_CONNECTED:
                            try {
                                socket.close();
                            } catch (IOException e){
                                Log.e(TAG, "Could not close socket", e);
                            }
                            break;
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

        public ConnectThread(BluetoothDevice device) {
            this.device = device;
            BluetoothSocket tmp = null;

            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e){
                Log.e(TAG, "create() failed", e);
            }
            socket = tmp;
        }

        public void run(){
            setName("ConnectThread");

            bluetoothAdapter.cancelDiscovery();

            try {
                socket.connect();
            } catch (IOException e){
                connectionFailed();
                try {
                    socket.close();
                } catch (IOException e1){
                    Log.e(TAG, "unable to close socket during connection fail", e1);
                }
            }

            synchronized (BluetoothService.this) {
                connectedThread = null;
            }

            connected(socket, device);
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

        public ConnectedThread(BluetoothSocket socket) {
            this.socket = socket;
        }

        public void run() {
            while (true) {
                handler.obtainMessage(ACTION_SEND)
                        .sendToTarget();
            }
        }

        public void send() {
                handler.obtainMessage(ACTION_SEND)
                        .sendToTarget();
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
