package de.invisibletower.footnavi;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.support.v4.content.res.TypedArrayUtils;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ArrayBlockingQueue;

import de.invisibletower.footnavi.ble.BleManager;
import de.invisibletower.footnavi.ble.BleUtils;

/**
 * Created by daniel on 10.09.17.
 */

class TheFoot implements BleManager.BleManagerListener {
    // Log
    private final static String TAG = TheFoot.class.getSimpleName();

    // region Public Interface

    /// Connects
    public TheFoot(Context context) {
        setState("TheFoot()");
        mContext = context;
        mBleManager = BleManager.getInstance(mContext);
        mBuffer = new ArrayList<Byte>(100);


        mShowDataInHexFormat = true;
        mIsEchoEnabled = true;
        mIsEolEnabled = true;

        onServicesDiscovered();
    }

    public class Orientation {
        int qualiGyro;
        int qualiAcc;
        int qualiMag;
        float[] quat;
        float heading;
    }

    /// Get Last Orientation Data
    public Orientation getLastOrientation() {
        return mLastOrientationData;
    }

    public String getState() {
        return mState;
    }

    private void setState(String log) {
        Log.d(TAG, log);
        mState = log;
    }

    static final String CMD_LED = "2";                     // 2,0,1,0,0; // 0 or 1
    static final String CMD_MONITORENABLE = "3";               // 3;
    static final String CMD_MONITORRESULT = "30";        // 30,winkel:float,system:int,gyro:int,accel:int,mag:int;
    static final String CMD_SETVIBRATIONENABLE = "4";      // 4,1; // 0 or 1
    static final String CMD_SETMAXINTENSITY = "5";         // 5,152; // max: 255
    static final String CMD_SETTHRESHOLDFORVIBRATE = "6";  // 6,0.63; // max: 1.0
    static final String CMD_SETVIBRATIONLOOPTIME = "7";    // 7,20; // in millis
    static final String CMD_LLSETVIBRATION = "8";          // 8,152,0,0,12; // max each: 255

    public void setMonitoring(boolean enable) {
        String cmd =  CMD_MONITORENABLE + ',' + (enable ? 1 : 0) + ";";
        setState(cmd);
        sendData(cmd.getBytes());
    }

    public void setLeds(boolean a, boolean b, boolean c, boolean d) {
        String cmd =  CMD_LED + ',' + (a ? 1 : 0) + ','
                + (b ? 1 : 0) + ',' + (c ? 1 : 0) + ',' + (d ? 1 : 0) + ";";
        setState(cmd);
        sendData(cmd.getBytes());
    }

    public void setVibrate(boolean vibrate) {
        String cmd =  CMD_SETVIBRATIONENABLE + ',' + (vibrate ? 1 : 0) + ";";
        setState(cmd);
        sendData(cmd.getBytes());
    }

    public void setVibThreshold(float threshold) { // 0..1
        String cmd =  CMD_SETTHRESHOLDFORVIBRATE + ',' + String.valueOf (threshold) + ";";
        setState(cmd);
        sendData(cmd.getBytes());
    }

    public void setManualVibrations(int f, int r, int b, int l) { // 0..255
        String cmd =  CMD_LLSETVIBRATION + ',' + f + ',' + r + ',' + b + ',' + l + ";";
        setState(cmd);
        sendData(cmd.getBytes());
    }

    public void setVibLoopTime(int time) { // 0..100
        String cmd =  CMD_SETVIBRATIONLOOPTIME + ',' + time + ";";
        setState(cmd);
        sendData(cmd.getBytes());
    }

    public void setMaxVibIntensity(int max) { // 0..255
        String cmd =  CMD_SETMAXINTENSITY + ',' + max + ";";
        setState(cmd);
        sendData(cmd.getBytes());
    }

    public void onPause() {
        mBleManager.disconnect();
    }

    public void onResume() {
        mBleManager.setBleListener(this);

        BluetoothAdapter bluetoothAdapter = BleUtils.getBluetoothAdapter(mContext);
        // TODO: Scan instead of hardcode
        connect(bluetoothAdapter.getRemoteDevice("E4:8C:88:C5:C2:2E"));

    }

    // endregion

    // region constants

    // Service Constants
    public static final String UUID_SERVICE = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String UUID_RX = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String UUID_TX = "6e400002-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String UUID_DFU = "00001530-1212-EFDE-1523-785FEABCD123";
    public static final int kTxMaxCharacters = 20;

    // Constants
    private final static String kPreferences = "UartActivity_prefs";
    private final static String kPreferences_eol = "eol";
    private final static String kPreferences_echo = "echo";
    private final static String kPreferences_asciiMode = "ascii";
    private final static String kPreferences_timestampDisplayMode = "timestampdisplaymode";

    // Activity request codes (used for onActivityResult)
    private static final int kActivityRequestCode_ConnectedSettingsActivity = 0;
    private static final int kActivityRequestCode_MqttSettingsActivity = 1;

    // endregion

    // region configuration

    // Configuration
    private final static boolean kUseColorsForData = true;
    public final static int kDefaultMaxPacketsToPaintAsText = 500;
    private final static int kInfoColor = Color.parseColor("#F21625");

    // endregion

    // region preferences (echo, eol)

    // Data
    private boolean mShowDataInHexFormat;
    private boolean mIsTimestampDisplayMode;
    private boolean mIsEchoEnabled;
    private boolean mIsEolEnabled;

    // endregion

    // region fields (GattServicem BleManager), state machines

    protected Context mContext;

    // Data
    protected BleManager mBleManager;
    protected BluetoothGattService mUartService;
    private boolean isRxNotificationEnabled = false;
    private Orientation mLastOrientationData = null;

    private String mState = "";

    // endregion

    // region statistics

    private volatile int mSentBytes;
    private volatile int mReceivedBytes;

    // endregion

    // region Methods: Connecting

    private void connect(BluetoothDevice device) {
        boolean isConnecting = mBleManager.connect(mContext, device.getAddress());
        if (isConnecting) {
        }
    }

    @Override
    public void onServicesDiscovered() {
        mUartService = mBleManager.getGattService(UUID_SERVICE);
        enableRxNotifications();
    }

    protected void enableRxNotifications() {
        isRxNotificationEnabled = true;
        mBleManager.enableNotification(mUartService, UUID_RX, true);
    }

    // endregion


    // region Send Data to UART
    protected void sendData(String text) {
        final byte[] value = text.getBytes(Charset.forName("UTF-8"));
        sendData(value);
    }

    protected void sendData(byte[] data) {
        if (mUartService != null) {
            mSentBytes += data.length;
            //setState("sendData()");

            // Split the value into chunks (UART service has a maximum number of characters that can be written )
            for (int i = 0; i < data.length; i += kTxMaxCharacters) {
                final byte[] chunk = Arrays.copyOfRange(data, i, Math.min(i + kTxMaxCharacters, data.length));
                mBleManager.writeService(mUartService, UUID_TX, chunk);
            }
        } else {
            Log.w(TAG, "Uart Service not discovered. Unable to send data");
        }
    }

    // Send data to UART and add a byte with a custom CRC
    protected void sendDataWithCRC(byte[] data) {

        // Calculate checksum
        byte checksum = 0;
        for (byte aData : data) {
            checksum += aData;
        }
        checksum = (byte) (~checksum);       // Invert

        // Add crc to data
        byte dataCrc[] = new byte[data.length + 1];
        System.arraycopy(data, 0, dataCrc, 0, data.length);
        dataCrc[data.length] = checksum;

        // Send it
        Log.d(TAG, "Send to UART: " + BleUtils.bytesToHexWithSpaces(dataCrc));
        sendData(dataCrc);
    }
    // endregion

    // region SendDataWithCompletionHandler
    protected interface SendDataCompletionHandler {
        void sendDataResponse(String data);
    }

    final private Handler sendDataTimeoutHandler = new Handler();
    private Runnable sendDataRunnable = null;
    private UartInterfaceActivity.SendDataCompletionHandler sendDataCompletionHandler = null;

    protected void sendData(byte[] data, UartInterfaceActivity.SendDataCompletionHandler completionHandler) {

        if (completionHandler == null) {
            sendData(data);
            return;
        }

        if (!isRxNotificationEnabled) {
            Log.w(TAG, "sendData warning: RX notification not enabled. completionHandler will not be executed");
        }

        if (sendDataRunnable != null || sendDataCompletionHandler != null) {
            Log.d(TAG, "sendData error: waiting for a previous response");
            return;
        }

        Log.d(TAG, "sendData");
        sendDataCompletionHandler = completionHandler;
        sendDataRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "sendData timeout");
                final UartInterfaceActivity.SendDataCompletionHandler dataCompletionHandler = sendDataCompletionHandler;

                TheFoot.this.sendDataRunnable = null;
                TheFoot.this.sendDataCompletionHandler = null;

                dataCompletionHandler.sendDataResponse(null);
            }
        };

        sendDataTimeoutHandler.postDelayed(sendDataRunnable, 2 * 1000);
        sendData(data);

    }

    protected boolean isWaitingForSendDataResponse() {
        return sendDataRunnable != null;
    }

    // endregion

    // region Rx

    ArrayList<Byte> mBuffer;

    @Override
    public void onDataAvailable(BluetoothGattCharacteristic characteristic) {

        // Check if there is a pending sendDataRunnable
        if (characteristic.getService().getUuid().toString().equalsIgnoreCase(UUID_SERVICE)) {
            if (characteristic.getUuid().toString().equalsIgnoreCase(UUID_RX)) {

                if (sendDataRunnable != null) {
                    // call the callback that was given while sending

                    setState(" -> callback()");

                    Log.d(TAG, "sendData received data");
                    sendDataTimeoutHandler.removeCallbacks(sendDataRunnable);
                    sendDataRunnable = null;

                    if (sendDataCompletionHandler != null) {
                        final byte[] bytes = characteristic.getValue();
                        final String data = new String(bytes, Charset.forName("UTF-8"));

                        final UartInterfaceActivity.SendDataCompletionHandler dataCompletionHandler = sendDataCompletionHandler;
                        sendDataCompletionHandler = null;
                        dataCompletionHandler.sendDataResponse(data);
                    }
                } else {
                    final byte[] bytes = characteristic.getValue();
                    mReceivedBytes += bytes.length;
                    int i = 0;

                    while(i != bytes.length) {
                        if (bytes[i] == ';') {
                            // bytes[i] is now ';', so skip it
                            i++;
                            mLastOrientationData = parseLine(toByteArray(mBuffer));
                            mBuffer.clear();
                        } else {
                            mBuffer.add(bytes[i]);
                            i++;
                        }
                    }
                }
            }
        }
    }

    public static byte[] toByteArray(ArrayList<Byte> in) {
        final int n = in.size();
        byte ret[] = new byte[n];
        for (int i = 0; i < n; i++) {
            ret[i] = in.get(i);
        }
        return ret;
    }

    @Override
    public void onDataAvailable(BluetoothGattDescriptor descriptor) {

    }


    public Orientation parseLine(byte[] bytes) {

        ///3 x int für Calibrationsqualität: Gyro | Accel | Mag
        ///4 x float quaternionen w y x z
        ///1 x float ausgerechnete heading(Kompass)

        // 30,winkel:float,system:int,gyro:int,accel:int,mag:int,q1,q2,q3,q4:float;

        final String data = new String(bytes, Charset.forName("UTF-8")).trim();
        setState("rx: " + data);

        String[] parts = data.split(",");
        if (parts.length == 10 && parts[0].equals(CMD_MONITORRESULT)) {
            Orientation result = new Orientation();
            result.heading = Float.parseFloat(parts[1]);
            result.qualiGyro = Integer.parseInt(parts[3]);
            result.qualiAcc = Integer.parseInt(parts[4]);
            result.qualiMag = Integer.parseInt(parts[5]);
            float quat[] = { Float.parseFloat(parts[6]),
                    Float.parseFloat(parts[7]),
                    Float.parseFloat(parts[8]),
                    Float.parseFloat(parts[9])};
            result.quat = quat;
            return result;
        } else {
            Log.d(TAG, "Parsing: got " + parts.length + " parts instead of 10: " + data);
//            if (parts.length >= 1) {
//                Log.d(TAG, "Cmd id is '" + parts[0] + "' " + (parts.length == 10) + " & "
//                + (parts[0] == CMD_MONITORRESULT) + ". '" + CMD_MONITORRESULT + "'" +
//                CMD_MONITORRESULT.);
//            }
        }
        return null;
    }

    // endregion

    // region BleManagerListener  (used to implement sendData with completionHandler)

    @Override
    public void onConnected() {
        setState("onConnected()");
    }

    @Override
    public void onConnecting() {
        setState("onConnecting()");
    }

    @Override
    public void onDisconnected() {
        setState("onDisconnected()");
    }

    @Override
    public void onReadRemoteRssi(int rssi) {
        setState("onReadRemoteRssi()");
    }

    // endregion
}
