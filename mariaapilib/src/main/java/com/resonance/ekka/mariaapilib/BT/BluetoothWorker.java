package com.resonance.ekka.mariaapilib.BT;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;





import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

public class BluetoothWorker {
    private final String DBG_TAG = "DBG_TAG";
    private static final int STATE_NONE = 0;       // we're doing nothing
    private static final int STATE_CONNECTING = 1; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 2;  // now connected to a remote device

    public static final int MSG_NOT_CONNECTED = 1;
    public static final int MSG_CONNECTING = 2;
    public static final int MSG_CONNECTED = 3;
    public static final int MSG_CONNECTION_FAILED = 4;
    public static final int MSG_CONNECTION_ERROR = 5;
    public static final int MSG_CONNECTION_REJECT = 10;

    private static String BLUETOOTH_DEVICE_ADRESS = "";
    private static String tmpAdressBluetooth = "";

    private BluetoothAdapter mBluetoothAdapter = null;
    private final BluetoothSocket btSocket = null;
    private static InputStream InStream = null;
    private static OutputStream OutStream = null;
    private boolean btConnect = false;
    private int StateBT = -1;
    private OpenSocketThread thOpenSocketThread = null;
    private Handler ParentHandler = null;
    private Context mContext;

    /*
    * НЕОБХОДИМО РАЗОБРАТЬСЯ с ситуацией, когда выключен BT
    * */


    public BluetoothWorker(Context context, Handler handler, String BlueToothDevAdress)
    {

      //  Log.d(DBG_TAG, "BluetoothWorker ");
        BLUETOOTH_DEVICE_ADRESS = BlueToothDevAdress;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        StateBT = STATE_NONE;
        ParentHandler = handler;
        mContext = context;


        //зарегистрируем приемник событий от Bluetooth
        IntentFilter filterBT = new IntentFilter();
        //filterBT.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filterBT.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        context.registerReceiver(mBroadcastReceiverExtend, filterBT);

        //зарегистрируем приемник событий от Bluetooth
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        context.registerReceiver(mBroadcastReceiverBT, filter);

        if (!mBluetoothAdapter.isEnabled())
        {
            mBluetoothAdapter.enable();
            Log.e(DBG_TAG, "Worker mBluetoothAdapter is not Enabled");
        } else
            StartSocket();
    }

    public void DestructBluetoothWorker() {

        Log.d(DBG_TAG, "DestructBluetoothWorker");
        //if (mBroadcastReceiverBT.isInitialStickyBroadcast())
            mContext.unregisterReceiver(mBroadcastReceiverBT);
            mContext.unregisterReceiver(mBroadcastReceiverExtend);
     /*   if(BluetoothAdapter.getDefaultAdapter()!=null){
            if (BluetoothAdapter.getDefaultAdapter().isEnabled())
                BluetoothAdapter.getDefaultAdapter().disable();
        }*/

        if (thOpenSocketThread != null) {
            thOpenSocketThread.cancel();
            thOpenSocketThread = null;
        }


    }
    public void SetupBluetoothDevAdress(String BlueToothDevAdress)
    {
        BLUETOOTH_DEVICE_ADRESS = BlueToothDevAdress;
    }

    private final BroadcastReceiver mBroadcastReceiverBT = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED))
            {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.i(DBG_TAG, "------------- mBroadcastReceiverBT STATE_OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.i(DBG_TAG, "-------------mBroadcastReceiverBT STATE_TURNING_OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.i(DBG_TAG, "-------------mBroadcastReceiverBT STATE_ON");

                         StartSocket();
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.i(DBG_TAG, "-------------mBroadcastReceiverBT STATE_TURNING_ON");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.i(DBG_TAG, "-------------mBroadcastReceiverBT STATE_CONNECTED");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.i(DBG_TAG, "-------------mBroadcastReceiverBT STATE_CONNECTING");
                        break;
                    case BluetoothAdapter.STATE_DISCONNECTING:
                        Log.i(DBG_TAG, "-------------mBroadcastReceiverBT STATE_DISCONNECTING");
                        break;
                    case BluetoothAdapter.STATE_DISCONNECTED:
                        Log.i(DBG_TAG, "-------------mBroadcastReceiverBT STATE_DISCONNECTED");
                        StartSocket();
                        break;
                }
            }
        }
    };


    private final BroadcastReceiver mBroadcastReceiverExtend = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            switch (action){
                case BluetoothDevice.ACTION_ACL_CONNECTED:
                    // Log.i(DBG_TAG, "------------- ACTION_ACL_CONNECTED");
                    break;
                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    connectionFailed("");
                    //Log.i(DBG_TAG, "------------- ACTION_ACL_DISCONNECTED");
                    break;
            }
        }
    };


    private synchronized boolean StartSocket() {


        Log.d(DBG_TAG, "BluetoothWorker StartSocket ");
        if (tmpAdressBluetooth.equals(BLUETOOTH_DEVICE_ADRESS) == false)//сменили устройство
        {
            //Log.i(DBG_TAG, "TMP ADR:" + tmpAdressBluetooth + " SET ADR:" + BLUETOOTH_DEVICE_ADRESS);
            btConnect = false;
            tmpAdressBluetooth = BLUETOOTH_DEVICE_ADRESS;
        }


        if (BLUETOOTH_DEVICE_ADRESS.trim().length() == 0) {

           // SendMsgToParent( REQUEST_SETTING, "");
            return false;
        }

        if (btConnect)
            return true;

        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(BLUETOOTH_DEVICE_ADRESS);
        Log.d(DBG_TAG, "Открытие сокета..." + BLUETOOTH_DEVICE_ADRESS);
        connect(device);
        return true;
    }
    /**
     * Start the OpenSocketThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
       // Log.d(DBG_TAG, "THREAD connect : " + device);
        // Cancel any thread attempting to make a connection
        if (StateBT == STATE_CONNECTING) {
            if (thOpenSocketThread != null) {
                thOpenSocketThread.cancel();
                thOpenSocketThread = null;
            }
        }

        try {
            thOpenSocketThread = new OpenSocketThread(device);
            thOpenSocketThread.start();
            setState(STATE_CONNECTING);

        } catch (SecurityException e) {
            Log.e(DBG_TAG, "[1] "+e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            Log.e(DBG_TAG, "[2] "+e.getMessage(), e);
        } catch (NoSuchMethodException e) {
            Log.e(DBG_TAG, "[3] "+e.getMessage(), e);
        } catch (IllegalAccessException e) {
            Log.e(DBG_TAG, "[4] "+e.getMessage(), e);
        } catch (InvocationTargetException e) {
            Log.e(DBG_TAG, "[5] "+e.getMessage(), e);
        }
    }


    /**
     * Set the current state of the connection
     *
     * @param state An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        StateBT = state;
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
        return StateBT;
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     */

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class OpenSocketThread extends Thread {
        private  BluetoothSocket mmSocket;
        private  BluetoothDevice mmDevice;
        boolean bAttach = false;


        public OpenSocketThread(BluetoothDevice device) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
            mmDevice = device;
            InStream = null;
            OutStream = null;

            boolean err = false;
            try {
                try {
                    try {
                      //  Log.i(DBG_TAG, "Device Name: " + mmDevice.getName());
                        mmSocket = mmDevice.createRfcommSocketToServiceRecord(mmDevice.getUuids()[0].getUuid());
                        bAttach = true;
                    }catch (IOException e) {
                        err = true;
                        Log.e(DBG_TAG, "#1 IOException: "+e.getLocalizedMessage());
                    }catch(NullPointerException ne){
                        err = true;
                        Log.e(DBG_TAG, "NullPointerException: " + ne.getLocalizedMessage());};

                    if (err) {
                        Log.i(DBG_TAG, "createRfcommSocket: " + mmDevice.getName());

                        Method m = device.getClass().getMethod("createRfcommSocket",new Class[] { int.class });
                        mmSocket = (BluetoothSocket) m.invoke(device,Integer.valueOf(1));
                        bAttach = true;
                    }

                } catch (IllegalArgumentException e) {
                    Log.e(DBG_TAG, "IllegalArgumentException: "+e.getLocalizedMessage());
                }
            } catch (SecurityException e) {
                Log.e(DBG_TAG, "SecurityException: "+e.getLocalizedMessage());
            }
        }

        public void run() {
            boolean err = true;
            if (bAttach) {
                mBluetoothAdapter.cancelDiscovery();
                try {
                    Thread.sleep(500);
                }
                catch(InterruptedException ee){};
                try
                {
                    Log.d(DBG_TAG, "Try to connect " + mmDevice.getName());
                    sendMessage(MSG_CONNECTING, mmDevice.getName());
                    mmSocket.connect();
                   // Log.d(DBG_TAG, mmDevice.getName() + " - connected  , " + bAttach);
                    InStream = mmSocket.getInputStream();
                    OutStream = mmSocket.getOutputStream();
                    err = false;

                } catch (IOException e)
                {
                    Log.e(DBG_TAG, mmDevice.getName() + " - not connected");
                    try {
                         /* if(BluetoothAdapter.getDefaultAdapter()!=null)
                                BluetoothAdapter.getDefaultAdapter().disable();*/

                    } catch (Exception closeException) { };

                    connectionError(mmDevice.getName());
                }

                if (!err) {
                    connectionEstablished(mmDevice.getName());

                }

                } else {
                connectionAbsent(mmDevice.getName());

                }
            }


        public void cancel() {
            try {
                if (InStream != null) {
                    try {
                        InStream.close();
                        InStream = null;
                    } catch (IOException e) {
                        Log.e(DBG_TAG, "close() of InStream failed.");
                    }
                }
                if (OutStream != null) {
                    try {
                        OutStream.close();
                        OutStream = null;
                    } catch (IOException e) {
                        Log.e(DBG_TAG, "close() of OutStream failed.");
                    }
                }
                mmSocket.close();
                mmSocket = null;
               // Log.d(DBG_TAG, "cancel()");
            } catch (IOException e) {
                Log.e(DBG_TAG, "close() of connect socket failed", e);
            }
        }
    }




        /*public int read(byte[] BufForRead) {
            int cntRead = 0;
                try{
                    cntRead = InStream.read(BufForRead);
                } catch (IOException e) {
                    connectionFailed("");
            }
            return cntRead;
        }*/

    public synchronized int getAvailable(){

        if (StateBT==STATE_NONE){
            return 0;
        }
        int available = -1;
        try {
            if (InStream != null)
                available = InStream.available();
        } catch (IOException e) {
                connectionFailed("");
        }


        return available;
    }


    public synchronized byte[] read(long TimeOut) {


        if (StateBT==STATE_NONE){
            return null;
        }
       // Log.d(DBG_TAG, "read "+StateBT);

        long lStopTime = System.currentTimeMillis()+TimeOut;

        byte[] buf = null;

        while(lStopTime > System.currentTimeMillis())
        {
            if (InStream == null) {
                setState(STATE_NONE);
               // Log.e(DBG_TAG, "read ##1");
                break;
            }
            try {

                if (InStream.available()>0)
                {
                    buf = new byte[InStream.available()];
                    int CntRead  = InStream.read(buf);
                  // Log.d(DBG_TAG, "read "+CntRead+" bytes");
                    if (CntRead>0)break;

                }

            } catch (IOException e) {
                Log.e(DBG_TAG, "read IOException");
                connectionFailed("");
                buf = null;
                break;
            }
        }
        return buf;
    }


        /**
     * Write to the connected OutStream.
     *
     * @param bytes The bytes to write
     */
    public synchronized boolean write(byte[] bytes) {

        if (StateBT==STATE_NONE){
            return false;
        }

        boolean res = false;
        if (OutStream == null) return res;

        try {

            OutStream.write(bytes);
            OutStream.flush();
            res = true;
        } catch (IOException e) {

            connectionFailed("");
            Log.e(DBG_TAG, "Exception during write", e);
        }
        return res;
    }

    private void connectionFailed(String deviceName) {
        setState(STATE_NONE);
        sendMessage(MSG_CONNECTION_FAILED,deviceName);
    }
    private void connectionError(String deviceName) {
        setState(STATE_NONE);
        sendMessage(MSG_CONNECTION_ERROR,deviceName);
    }
    private void connectionEstablished(String deviceName) {
        setState(STATE_CONNECTED);
        sendMessage(MSG_CONNECTED,deviceName);
    }
    private void connectionAbsent(String deviceName) {
        setState(STATE_NONE);
        sendMessage(MSG_NOT_CONNECTED,deviceName);
    }

    private void sendMessage(int messageId, String deviceName) {
        ParentHandler.obtainMessage(messageId, -1, -1, deviceName).sendToTarget();
    }

}
