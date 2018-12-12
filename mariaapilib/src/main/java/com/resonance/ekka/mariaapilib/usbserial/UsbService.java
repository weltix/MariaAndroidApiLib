package com.resonance.ekka.mariaapilib.usbserial;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;



import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.resonance.ekka.mariaapilib.iMariaCommands.DBG_TAG;

public class UsbService extends Service{

    public static final int MSG_USB_ATTACHED = 21;
    public static final int MSG_USB_DETACHED = 22;
    public static final int MSG_USB_READY = 23;
    public static final int MSG_NO_USB = 24;


    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    public static final String ACTION_USB_READY = "com.felhr.connectivityservices.USB_READY";
    public static final String ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";//"android.hardware.usb.action.USB_DEVICE_ATTACHED";
    public static final String ACTION_USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";
    public static final String ACTION_USB_NOT_SUPPORTED = "com.felhr.usbservice.USB_NOT_SUPPORTED";
    public static final String ACTION_NO_USB = "com.felhr.usbservice.NO_USB";
    public static final String ACTION_USB_PERMISSION_GRANTED = "com.felhr.usbservice.USB_PERMISSION_GRANTED";
    public static final String ACTION_USB_PERMISSION_NOT_GRANTED = "com.felhr.usbservice.USB_PERMISSION_NOT_GRANTED";
    public static final String ACTION_USB_DISCONNECTED = "com.felhr.usbservice.USB_DISCONNECTED";
    public static final String ACTION_USB_DEVICE_NOT_WORKING = "com.felhr.connectivityservices.ACTION_USB_DEVICE_NOT_WORKING";
   // public static final String ACTION_DATA_RECEIVED = "ACTION_DATA_RECEIVED";


    public static final int CTS_CHANGE = 1;
    public static final int DSR_CHANGE = 2;

    public static boolean SERVICE_CONNECTED = false;

    private static int BAUD_RATE = 115200;
    private static int DATA_BITS = UsbSerialInterface.DATA_BITS_8;
    private static int STOP_BITS = UsbSerialInterface.STOP_BITS_1;
    private static int PARITY    = UsbSerialInterface.PARITY_EVEN;

    private static int deviceVID = 4292;
    private static int devicePID = 60000;


    private Context mContext;
    private Handler mHandler = null;
    private UsbManager usbManager;
    private UsbDevice device;
    private UsbDeviceConnection connection;
    private UsbSerialDevice serialPort;
    private boolean serialPortConnected = false;


    private IBinder binder = new UsbBinder();


    private BlockingQueue<byte[]> itemsReceivedFromUSB ;
    private BlockingQueue<Integer> messageQueue ;



    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    public class UsbBinder extends Binder {
        public UsbService getService() {
            return UsbService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

       // Log.d(DBG_TAG, "USB Service onStartCommand");
        try {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Set<String> keys = bundle.keySet();
                Iterator<String> it = keys.iterator();
                while (it.hasNext()) {
                    String key = it.next();
                }
            }

            BAUD_RATE = Integer.decode((String) bundle.get("BAUD_RATE"));
            DATA_BITS = Integer.decode((String) bundle.get("DATA_BITS"));
            STOP_BITS = Integer.decode((String) bundle.get("STOP_BITS"));
            PARITY = Integer.decode((String) bundle.get("PARITY"));
            deviceVID  = Integer.decode((String) bundle.get("deviceVID"));
            devicePID = Integer.decode((String) bundle.get("devicePID"));

        }catch (Exception e)
        {};

        new MessageThread().start();
        findSerialPortDevice();
        return Service.START_NOT_STICKY;
    }


    /*
     * onCreate will be executed when service is started. It configures an IntentFilter to listen for
     * incoming Intents (USB ATTACHED, USB DETACHED...) and it tries to open a serial port.
     */
    @Override
    public void onCreate() {
        mContext = this;
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(ACTION_USB_ATTACHED);
        filter.addAction(ACTION_USB_DETACHED);
        filter.addAction(ACTION_USB_READY);

        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
       // filter.addAction(ACTION_DATA_RECEIVED);


        mContext.registerReceiver(usbReceiver, filter);
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
       // Log.d(DBG_TAG, "USB Service onCreate");

        itemsReceivedFromUSB = new ArrayBlockingQueue<byte[]>(100);
        messageQueue = new ArrayBlockingQueue<Integer>(10);
        messageQueue.clear();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();



        if( (serialPort != null)&&(serialPortConnected)) {
            serialPort.setDTR(false);
           // serialPort.setRTS(false);
            serialPort.killWorkingThread();
            serialPort.close();
            serialPortConnected = false;
        }
        mHandler=null;
        unregisterReceiver(usbReceiver);

        Log.d(DBG_TAG, "USB Service onDestroy");
    }


    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent arg1) {

            if (arg1.getAction().equals(ACTION_USB_PERMISSION)) {
                synchronized (this) {
                    boolean granted = arg1.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                    if (granted) // User accepted our USB connection. Try to open the device as a serial port
                    {
                        Intent intent = new Intent(ACTION_USB_PERMISSION_GRANTED);
                        arg0.sendBroadcast(intent);

                        if ((deviceVID == device.getVendorId())&&(devicePID == device.getProductId())) {
                            connection = usbManager.openDevice(device);
                            new ConnectionThread().start();
                            Log.d(DBG_TAG, "BroadcastReceiver USB_PERMISSION_GRANTED and Connection thread is start deviceVID:" + device.getVendorId() + ", devicePID:" + device.getProductId());
                        }else {
                            Log.d(DBG_TAG, "BroadcastReceiver USB_PERMISSION_GRANTED  deviceVID:" + device.getVendorId() + ", devicePID:" + device.getProductId() + " *** IGNORED");
                        }

                    } else // User not accepted our USB connection. Send an Intent to the Main Activity
                    {
                        Log.d(DBG_TAG, "BroadcastReceiver ACTION_USB_PERMISSION_NOT_GRANTED");
                        Intent intent = new Intent(ACTION_USB_PERMISSION_NOT_GRANTED);
                        arg0.sendBroadcast(intent);
                    }
                }
            } else if (arg1.getAction().equals(ACTION_USB_ATTACHED)) {
                Log.d(DBG_TAG, "***ACTION_USB_ATTACHED");
                sendMessageToParent(MSG_USB_ATTACHED);

                if (!serialPortConnected)
                    findSerialPortDevice(); // A USB device has been attached. Try to open it as a Serial port
            } else if (arg1.getAction().equals(ACTION_USB_DETACHED)) {
                sendMessageToParent(MSG_USB_DETACHED);

                Log.d(DBG_TAG, "*** ACTION_USB_DETACHED");
                // Usb device was disconnected. send an intent to the Main Activity
                Intent intent = new Intent(ACTION_USB_DISCONNECTED);
                arg0.sendBroadcast(intent);
                if (serialPortConnected) {

                        serialPort.setDTR(false);
                        serialPortConnected = false;
                    }
                    serialPort.close();
                itemsReceivedFromUSB.clear();
                connection = null;
            }
        }
    };

    /*
     * Request user permission. The response will be received in the BroadcastReceiver
     */
    private void requestUserPermission() {
        PendingIntent mPendingIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_USB_PERMISSION), 0);
        usbManager.requestPermission(device, mPendingIntent);
    }


    private void findSerialPortDevice()
    {
        itemsReceivedFromUSB.clear();
        Log.d(DBG_TAG,  "findSerialPortDevice");
        // This snippet will try to open the first encountered usb device connected, excluding usb root hubs
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (usbDevices.isEmpty())
        {
            Log.d(DBG_TAG, "There is no USB devices connected. ");
            Intent intent = new Intent(ACTION_NO_USB);
            mContext.sendBroadcast(intent);
            return;
        }
       // Log.d(DBG_TAG, "Found total "+usbDevices.size()+" devices");

            boolean keep = true;
            device = null;
            boolean reqPermission = false;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet())
            {

                UsbDevice devTmp = entry.getValue();

                if ((deviceVID == devTmp.getVendorId())&&(devicePID == devTmp.getProductId())){
                    Log.d(DBG_TAG,  ">>>> deviceVID:"+devTmp.getVendorId()+", devicePID:"+devTmp.getProductId());
                    device = entry.getValue();
                    if (usbManager.hasPermission(device)!=true) {
                        reqPermission = true;
                        requestUserPermission();
                    }
                    else{
                        Log.d(DBG_TAG, "Нет необходимости проверять права при подключении");
                        connection = usbManager.openDevice(device);
                        new ConnectionThread().start();
                        keep=false;
                    }
                    sendMessageToParent(MSG_USB_ATTACHED);
                }
                if (!keep)
                    break;
            }
            if (keep&&(!reqPermission)) {
                Log.d(DBG_TAG,  "There is no USB devices connected (but usb host were listed). Send an intent to ");
                // There is no USB devices connected (but usb host were listed). Send an intent to 
                Intent intent = new Intent(ACTION_NO_USB);
                mContext.sendBroadcast(intent);

                sendMessageToParent(MSG_NO_USB);
            }
    }


    private UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() {
        @Override
        public void onReceivedData(byte[] arg0) {

          //Log.d(DBG_TAG, "UsbReadCallback onReceivedData:"+new String(arg0) );
              /*  Intent intent = new Intent(ACTION_DATA_RECEIVED);
                  mContext.sendBroadcast(intent);
              */
            try {
                itemsReceivedFromUSB.put(arg0);
            } catch (InterruptedException iex) {
               // Thread.currentThread().interrupt();
               throw new RuntimeException("Unexpected interruption");
            }

        }
    };

    /*
     * State changes in the CTS line will be received here
     */
    private UsbSerialInterface.UsbCTSCallback ctsCallback = new UsbSerialInterface.UsbCTSCallback() {
        @Override
        public void onCTSChanged(boolean state) {
            if(mHandler != null)
                mHandler.obtainMessage(CTS_CHANGE).sendToTarget();
        }
    };

    /*
     * State changes in the DSR line will be received here
     */
    private UsbSerialInterface.UsbDSRCallback dsrCallback = new UsbSerialInterface.UsbDSRCallback() {
        @Override
        public void onDSRChanged(boolean state) {
            if(mHandler != null)
                mHandler.obtainMessage(DSR_CHANGE).sendToTarget();
        }
    };

    /*
     * A simple thread to open a serial port.
     * Although it should be a fast operation. moving usb operations away from UI thread is a good thing.
     */
    private class ConnectionThread extends Thread {
        @Override
        public synchronized void run() {
            if (usbManager.hasPermission(device)!=true)
                return;

            serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
            try {
                if (serialPort != null) {
                    if (serialPort.open()) {
                        Log.d(DBG_TAG,  "ConnectionThread serialPort.open() BAUD_RATE:"+BAUD_RATE+" DATA_BITS:"+DATA_BITS+" STOP_BITS:"+STOP_BITS+" PARITY:"+PARITY);
                        serialPort.setBaudRate(BAUD_RATE);
                        serialPort.setDataBits(DATA_BITS);
                        serialPort.setStopBits(STOP_BITS);
                        serialPort.setParity(PARITY);
                        serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                        serialPort.read(mCallback);
                        serialPort.setDTR(true);
                        Log.d(DBG_TAG,  "sendMessageToParent MSG_USB_READY");
                        //serialPort.getCTS(ctsCallback);
                        //serialPort.getDSR(dsrCallback);
                        // Everything went as expected. Send an intent to MainActivity
                        Intent intent = new Intent(ACTION_USB_READY);
                        mContext.sendBroadcast(intent);
                        serialPortConnected = true;
                        sendMessageToParent(MSG_USB_READY);


                    } else {
                        Log.d(DBG_TAG, "USBSERVICE port NOT open");
                        // Serial port could not be opened, maybe an I/O error or if CDC driver was chosen, it does not really fit
                            Intent intent = new Intent(ACTION_USB_DEVICE_NOT_WORKING);
                            mContext.sendBroadcast(intent);
                    }
                } else {

                    Log.d(DBG_TAG, "USBSERVICE PORT is NULL");
                    // No driver for given device, even generic CDC driver could not be loaded
                    Intent intent = new Intent(ACTION_USB_NOT_SUPPORTED);
                    mContext.sendBroadcast(intent);
                }
            }catch(Exception e)
            {
                Log.d(DBG_TAG,"ОШИБКА открытия порта : \n "+e.getMessage());
            };
        }
    }


    private class MessageThread extends Thread {
        @Override
        public void run() {
          //  Log.d(DBG_TAG,"MessageThread запущен ");
            try {
            while(mHandler == null){
                Thread.sleep(1000);
               // Log.d(DBG_TAG,"MessageThread ........99999 ");
            }
              while(mHandler != null){
                  if ((!messageQueue.isEmpty())&&(mHandler != null))
                  {
                      int msg = messageQueue.take();
                      mHandler.obtainMessage(msg).sendToTarget();
                     // Log.d(DBG_TAG,"MessageThread SEND MESSAGE "+msg);
                  }
                  Thread.sleep(1000);
                 // Log.d(DBG_TAG,"MessageThread ........ ");
              }
            }catch(Exception e)
            {
                Log.e(DBG_TAG,"Exception MessageThread: "+e.getMessage());
            };
           // Log.d(DBG_TAG,"MessageThread завершен ");
        }
    }



    /*
     * This function will be called from MainActivity to write data through Serial Port
     */
    public boolean  write(byte[] data) {
        boolean res = false;
        if (serialPort != null) {
            //Log.d(DBG_TAG, "Usb sernice write USB ... ");
            serialPort.write(data);
            res = true;
        }
        return res;
    }

    public  byte[] read(long TimeOut)
    {
        if (serialPort == null)
            return null;
        byte[] buf = null;

        long lStopTime = System.currentTimeMillis()+TimeOut;

        while(lStopTime > System.currentTimeMillis())
        {

            try {

                if (!itemsReceivedFromUSB.isEmpty())
                {
                buf = new byte[itemsReceivedFromUSB.peek().length];
                buf = itemsReceivedFromUSB.take();

                //Log.i(DBG_TAG, "read item=" + new String(buf));
                break;
                }

            } catch (Exception e) {
                Log.e(DBG_TAG, "read IOException:"+e);

                buf = null;
                break;
            }
        }
       // Log.d(DBG_TAG, "read ****");
        return buf;
    }




    public void setHandler(Handler mHandler) {
       // Log.d(DBG_TAG, "setHandler");
        this.mHandler = mHandler;

    }


    private void sendMessageToParent(int messageId) {
        try {
            messageQueue.put(messageId);
        } catch (InterruptedException iex) {
            // Thread.currentThread().interrupt();
            throw new RuntimeException("Unexpected interruption");
        }

       /* if (this.mHandler!=null)
        this.mHandler.obtainMessage(messageId).sendToTarget();
        else
            Log.e(DBG_TAG, "this.mHandler==null");*/

    }


}
