package de.invisibletower.footnavi;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.IntDef;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class BTNavigator extends Service implements Runnable, BluetoothLeUart.Callback {

    Thread mWorkerThread;
    private BluetoothLeUart mUart;

    boolean mShouldStop;


    public BTNavigator() {
        mWorkerThread = new Thread(this);
        mWorkerThread.start();


        mUart = new BluetoothLeUart(getApplicationContext());
    }

    @Override
    public void run() {
        // connect
        mUart.registerCallback(this);
        mUart.connectFirstAvailable();

        // please never return

        mUart.unregisterCallback(this);
        mUart.disconnect();

    }


    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    // Handler for mouse click on the send button.
    public void sendClick(String message) {
        StringBuilder stringBuilder = new StringBuilder();

        if (true) {
            message = "\n" + message;
        }

        // We can only send 20 bytes per packet, so break longer messages
        // up into 20 byte payloads
        int len = message.length();
        int pos = 0;
        while(len != 0) {
            stringBuilder.setLength(0);
            if (len>=20) {
                stringBuilder.append(message.toCharArray(), pos, 20 );
                len-=20;
                pos+=20;
            }
            else {
                stringBuilder.append(message.toCharArray(), pos, len);
                len = 0;
            }
            mUart.send(stringBuilder.toString());
        }
        // Terminate with a newline character if requests
        if (true) {
            stringBuilder.setLength(0);
            stringBuilder.append("\n");
            mUart.send(stringBuilder.toString());
        }
    }




    // UART Callback event handlers.
    @Override
    public void onConnected(BluetoothLeUart uart) {
        // Called when UART device is connected and ready to send/receive data.
        // writeLine("Connected!");
        // Enable the send button
    }

    @Override
    public void onConnectFailed(BluetoothLeUart uart) {
        // Called when some error occured which prevented UART connection from completing.
        //writeLine("Error connecting to device!");
    }

    @Override
    public void onDisconnected(BluetoothLeUart uart) {
        // Called when the UART device disconnected.
        //writeLine("Disconnected!");
    }

    @Override
    public void onReceive(BluetoothLeUart uart, BluetoothGattCharacteristic rx) {
        // Called when data is received by the UART.
        //writeLine("Received: " + rx.getStringValue(0));
    }

    @Override
    public void onDeviceFound(BluetoothDevice device) {
        // Called when a UART device is discovered (after calling startScan).
        //writeLine("Found device : " + device.getAddress());
        //writeLine("Waiting for a connection ...");
    }

    @Override
    public void onDeviceInfoAvailable() {
        //writeLine(uart.getDeviceInfo());
    }

}
