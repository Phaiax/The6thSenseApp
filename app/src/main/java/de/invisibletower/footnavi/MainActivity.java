package de.invisibletower.footnavi;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import java.lang.Thread;

public class MainActivity extends AppCompatActivity implements BluetoothLeUart.Callback {


    // Bluetooth LE UART instance.  This is defined in BluetoothLeUart.java.
    private BluetoothLeUart uart;

    private TextView messages;
    private Button button;
    private ScrollView mScrollView;


    // Write some text to the messages text view.
    // Care is taken to do this on the main UI thread so writeLine can be called from any thread
    // (like the BTLE callback).
    private void writeLine(final CharSequence text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messages.append(text);
                messages.append("\n");
            }
        });
        mScrollView.fullScroll(View.FOCUS_DOWN);
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
            uart.send(stringBuilder.toString());
        }
        // Terminate with a newline character if requests
        if (true) {
            stringBuilder.setLength(0);
            stringBuilder.append("\n");
            uart.send(stringBuilder.toString());
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        button = (Button) findViewById(R.id.button_a);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendClick("A:");
            }
        });
        // Disable the send button until we're connected.
        button.setClickable(false);
        button.setEnabled(false);

        mScrollView = (ScrollView) findViewById(R.id.scrollView2);
        messages = (TextView) findViewById(R.id.text_log);

        // Initialize UART.
        uart = new BluetoothLeUart(getApplicationContext());


        // Enable auto-scroll in the TextView
        messages.setMovementMethod(new ScrollingMovementMethod());


    }

    // OnResume, called right before UI is displayed.  Connect to the bluetooth device.
    @Override
    protected void onResume() {
        super.onResume();
        writeLine("Scanning for devices ...");
        writeLine("...");
        uart.registerCallback(this);
        uart.connectFirstAvailable();
    }

    // OnStop, called right before the activity loses foreground focus.  Close the BTLE connection.
    @Override
    protected void onStop() {
        super.onStop();
        uart.unregisterCallback(this);
        uart.disconnect();
    }

    // UART Callback event handlers.
    @Override
    public void onConnected(BluetoothLeUart uart) {
        // Called when UART device is connected and ready to send/receive data.
        writeLine("Connected!");
        // Enable the send button
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                button.setClickable(true);
                button.setEnabled(true);
            }
        });
    }

    @Override
    public void onConnectFailed(BluetoothLeUart uart) {
        // Called when some error occured which prevented UART connection from completing.
        writeLine("Error connecting to device!");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                button.setClickable(false);
                button.setEnabled(false);
            }
        });
    }

    @Override
    public void onDisconnected(BluetoothLeUart uart) {
        // Called when the UART device disconnected.
        writeLine("Disconnected!");
        // Disable the send button.
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                button.setClickable(false);
                button.setEnabled(false);
            }
        });
    }

    @Override
    public void onReceive(BluetoothLeUart uart, BluetoothGattCharacteristic rx) {
        // Called when data is received by the UART.
        writeLine("Received: " + rx.getStringValue(0));
    }

    @Override
    public void onDeviceFound(BluetoothDevice device) {
        // Called when a UART device is discovered (after calling startScan).
        writeLine("Found device : " + device.getAddress());
        writeLine("Waiting for a connection ...");
    }

    @Override
    public void onDeviceInfoAvailable() {
        writeLine(uart.getDeviceInfo());
    }

}
