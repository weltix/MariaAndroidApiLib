package com.resonance.ekka.mariaapilib;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;

import android.os.AsyncTask;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.resonance.ekka.mariaapilib.BT.BluetoothWorker;
import com.resonance.ekka.mariaapilib.BT.SelectBluetoothActivity;
import com.resonance.ekka.mariaapilib.usbserial.UsbService;
import com.resonance.ekka.mariaapilib.usbserial.UsbWorker;

public class MariaAPI {
  public interface MariaAPICallback{
        void onReceiveHardwareMessage(Message msg);
    }
    public final static int BLUETOOTH = 1;
    public final static int USB = 2;

    private BroadcastReceiver br;
    private StorageSharedPreference preference;


    private static final String DBG_TAG = "DBG_TAG";
    private static String thisClassName = "";

    private static String MAC_REMOTE_DEVICE = "20:17:09:28:83:49";

    public final static String BROADCAST_ACTION = "com.resonance.ekka.mariaandroidapi.servicebackbroadcast";

    private int TypeConnection = USB;
    private boolean istConnectEKKA = false;
    private Context mContext;
    public MariaCommands MariaCmd;

    private static MariaAPICallback callback_onMessage;

    private static int DefineTransport = 0;
    private BluetoothWorker blueToothWorker = null;
    private UsbWorker usbWorker = null;


    public void onMessageCallBack(MariaAPICallback cback) {
        callback_onMessage = cback;
    }



    private void do_onMsgParent(int msg, String Value) {
        Message message = new Message();
        message.what = msg;
        message.obj = (String) Value;
        callback_onMessage.onReceiveHardwareMessage(message);

    }



    public MariaAPI(Context context) {
        mContext = context;
        thisClassName = "[" + this.getClass().getSimpleName() + "] ";

        DefineTransport = MariaAPI.BLUETOOTH;
        blueToothWorker = null;
        usbWorker = null;


        preference = new StorageSharedPreference(mContext);
        MAC_REMOTE_DEVICE = preference.getMAC_BT();

//        if (SetTypeConnection(BLUETOOTH) < 0) //установим по умолчанию
//            SetTypeConnection(USB);
        MariaCmd = new MariaCommands(this, mContext);

        CreateBroadcastReceiverForSelectBT();
    }

    /*
     * Текущая версия сборки
     * <br>
     * <br>
     *
     * @return                        "VERSION_NAME:["..."] VERSION_CODE :["..."]"
     */
    public String GetVersion(){
        return ("vn:"+ BuildConfig.VERSION_NAME+" vc:"+BuildConfig.VERSION_CODE);
    }

    /*
     * Получить текущий канал связи
     * <br>
     * <br>
     *
     * @return                         1/2  BLUETOOTH/USB
     */
    public int getTypeConnection() {
        return TypeConnection;
    }

    private void CreateBroadcastReceiverForSelectBT() {
        // создаем BroadcastReceiver для приема сообщений от activity выбора BT устройства
        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String MAC = intent.getStringExtra("MACBT");
                final String DEVNAME = intent.getStringExtra("DEVNAME");
                Log.d(DBG_TAG, thisClassName + "MACBT: " + MAC + "  DEVNAME: {" + DEVNAME + "}");
               /* MAC_REMOTE_DEVICE = MAC;
                preference.putMAC_BT(MAC);
                SetBluetooth(MAC);*/

              if (DEVNAME.contains(SelectBluetoothActivity.CANCELSTR)){//Cancel
                //  Log.d(DBG_TAG, thisClassName + "BluetoothWorker.MSG_CONNECTION_REJECT: "+DEVNAME);

                  Thread t = new Thread(new Runnable() {
                      public void run() {
                          do_onMsgParent(BluetoothWorker.MSG_CONNECTION_REJECT, DEVNAME);
                      }
                  });

                  t.start();



               }
               else {//Apply
                   MAC_REMOTE_DEVICE = MAC;
                   preference.putMAC_BT(MAC);
                   SetBluetooth(MAC);
               }


            }
        };
        IntentFilter intFilt = new IntentFilter(BROADCAST_ACTION);
        mContext.registerReceiver(br, intFilt);
    }

    /*
     * Деинициализация класса
     * <br>
     * <br>
     */
    public void DestructMariaAPI() {
        if (br != null) {
            mContext.unregisterReceiver(br);
        }
        istConnectEKKA = false;
        DestructUsb();
        DestructBluetooth();

    }

    /*
     * Установка канала связи
     * <br>
     * <br>
     * @param  <typeConnection>        1/2  BLUETOOTH/USB
     *
     * @return                        0/1
     */
    public int SetTypeConnection(int typeConnection) {
        if ((typeConnection != BLUETOOTH) && (typeConnection != USB))
            return 0;
        if ((BluetoothAdapter.getDefaultAdapter() == null) && (typeConnection == BLUETOOTH)) {
            return 0;
        }

        TypeConnection = typeConnection;

        if (TypeConnection == BLUETOOTH) {
            DestructUsb();

            if ((MAC_REMOTE_DEVICE.trim().length() == 0)||(MAC_REMOTE_DEVICE.trim().indexOf(SelectBluetoothActivity.CANCELSTR)>=0)) {
                ShowPairedBluetoothDevices();
            } else {
                SetBluetooth(MAC_REMOTE_DEVICE);
            }
        } else {
            DestructBluetooth();
            SetUsbDevice();
        }
        return 1;
    }

    private void SetUsbDevice() {
        DefineTransport = MariaAPI.USB;

        if (usbWorker != null) {
            usbWorker.StopServiceUSB();
        }
        usbWorker = new UsbWorker(mContext, usbHandler);
    }

    private void SetBluetooth(String BlueToothMAC) {
        DefineTransport = MariaAPI.BLUETOOTH;
        Log.d(DBG_TAG, "SetRemoteBluetoothDevice " + BlueToothMAC);
        if (blueToothWorker != null) {
            blueToothWorker.DestructBluetoothWorker();
        }
        blueToothWorker = new BluetoothWorker(mContext, bluetoothHandler, BlueToothMAC);
    }

    private void DestructUsb() {
        Log.d(DBG_TAG, "DestructUsb ");
        if (usbWorker != null) {
            istConnectEKKA = false;
            usbWorker.StopServiceUSB();
            usbWorker = null;
        }
    }

    private void DestructBluetooth() {
        Log.d(DBG_TAG, "DestructBluetooth ");
        if (blueToothWorker != null) {
            istConnectEKKA = false;
            blueToothWorker.DestructBluetoothWorker();
            blueToothWorker = null;
        }
    }

    public boolean ResetConnection(long TimeOut) {
        Log.w(DBG_TAG, "ResetConnection...");
        if (DefineTransport == MariaAPI.BLUETOOTH) {
            DestructBluetooth();
            Utils.sleep(3000);
            SetBluetooth(MAC_REMOTE_DEVICE);
            Utils.sleep(2000);
        } else {
            DestructUsb();
            Utils.sleep(2000);
            SetUsbDevice();
            Utils.sleep(2000);
        }

        long lStopTime = System.currentTimeMillis() + TimeOut;
        while (lStopTime > System.currentTimeMillis()) {
            if (istConnectEKKA)break;
            Utils.sleep(20);
        }
        return istConnectEKKA;
    }

    public synchronized boolean write(byte[] bytes) {
        boolean res = false;

        if (!istConnectEKKA) return res;

        if (DefineTransport == MariaAPI.BLUETOOTH) {

            if (blueToothWorker == null) {
                return res;
            }
            res = blueToothWorker.write(bytes);
        } else {
            res = usbWorker.write(bytes);
        }
        return res;
    }

    public synchronized byte[] read(long TimeOut) {
        if (!istConnectEKKA) return null;

        //Log.d(DBG_TAG, "read transport "+DefineTransport);
        if (DefineTransport == MariaAPI.BLUETOOTH) {

            if (blueToothWorker == null) {
                Log.e(DBG_TAG, "blueToothWorker == null ");
                return null;
            }
            return blueToothWorker.read(TimeOut);
        } else {
            return usbWorker.read(TimeOut);
        }
    }

    public synchronized int GetAvailable() {
        if (DefineTransport == MariaAPI.BLUETOOTH)
            return blueToothWorker.getAvailable();
        else
            return (int) 0;//заглушка для USB
    }

    private final Handler bluetoothHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {

            if (TypeConnection != BLUETOOTH) {
                Log.e(DBG_TAG, thisClassName + "handleMessage BT :" + TypeConnection + " msg.what:" + msg.what);
                return;
            }

            switch (msg.what) {
                case BluetoothWorker.MSG_CONNECTED:
                  //  Log.i(DBG_TAG, thisClassName + "***MSG_CONNECTED:" + String.valueOf(msg.obj));
                    do_onMsgParent(BluetoothWorker.MSG_CONNECTED, String.valueOf(msg.obj));

                    istConnectEKKA = true;
                    break;
                case BluetoothWorker.MSG_CONNECTING:
                    //Log.i(DBG_TAG, thisClassName + "***MSG_CONNECTING:" + String.valueOf(msg.obj));
                    do_onMsgParent(BluetoothWorker.MSG_CONNECTING, String.valueOf(msg.obj));
                    break;
                case BluetoothWorker.MSG_CONNECTION_ERROR:
                    //Log.i(DBG_TAG, thisClassName + "***MSG_CONNECTION_ERROR");
                    do_onMsgParent(BluetoothWorker.MSG_CONNECTION_ERROR, String.valueOf(msg.obj));
                    ShowPairedBluetoothDevices();
                    istConnectEKKA = false;
                    break;
                case BluetoothWorker.MSG_CONNECTION_FAILED:
                    //Log.e(DBG_TAG, thisClassName + "**MSG_CONNECTION_FAILED " + String.valueOf(msg.what));
                    do_onMsgParent(BluetoothWorker.MSG_CONNECTION_FAILED, String.valueOf(msg.obj));
                    ShowPairedBluetoothDevices();
                    istConnectEKKA = false;
                    break;
                case BluetoothWorker.MSG_NOT_CONNECTED:
                   // Log.i(DBG_TAG, thisClassName + "***MSG_NOT_CONNECTED:" + String.valueOf(msg.obj));
                    do_onMsgParent(BluetoothWorker.MSG_NOT_CONNECTED, String.valueOf(msg.obj));
                    istConnectEKKA = false;
                    break;

                default:
                    Log.e(DBG_TAG, thisClassName + "**Handler default message:" + String.valueOf(msg.what));
                    break;

            }
        }
    };


    private final Handler usbHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            //  Log.i(DBG_TAG, thisClassName+"handleMessage USB :"+TypeConnection+" msg.what:"+msg.what);
            if (TypeConnection != USB)
                return;
            switch (msg.what) {
                case UsbService.MSG_USB_READY:
                   // Log.w(DBG_TAG, thisClassName + "usbHandler MSG_USB_READY");
                    do_onMsgParent(UsbService.MSG_USB_READY, String.valueOf(msg.obj));
                    istConnectEKKA = true;
                    break;
                case UsbService.MSG_USB_DETACHED:
                   // Log.w(DBG_TAG, thisClassName + "usbHandler MSG_USB_DETACHED");
                    do_onMsgParent(UsbService.MSG_USB_DETACHED, String.valueOf(msg.obj));
                    istConnectEKKA = false;
                    break;
                case UsbService.MSG_USB_ATTACHED:
                   // Log.w(DBG_TAG, thisClassName + "usbHandler MSG_USB_ATTACHED");
                    do_onMsgParent(UsbService.MSG_USB_ATTACHED, String.valueOf(msg.obj));
                    istConnectEKKA = false;
                    break;
                case UsbService.MSG_NO_USB:
                   // Log.w(DBG_TAG, thisClassName + "usbHandler MSG_NO_USB");
                    do_onMsgParent(UsbService.MSG_NO_USB, String.valueOf(msg.obj));
                    istConnectEKKA = false;
                    break;
                default:
                    Log.e(DBG_TAG, thisClassName + "**usb Handler default message:" + String.valueOf(msg.what));
                    break;

            }
        }
    };

    private void ShowPairedBluetoothDevices() {
       /*   if(BluetoothAdapter.getDefaultAdapter()!=null){
                BluetoothAdapter.getDefaultAdapter().disable();
              sleep(2000);
           }*/
        Intent intent = new Intent(mContext, SelectBluetoothActivity.class);
        intent.putExtra("DEVNAME", MAC_REMOTE_DEVICE);
        mContext.startActivity(intent);
    }





}

/*
 class BackgroundTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                return null;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void res) {
            super.onPostExecute(res);
        }


    }

*/



class StorageSharedPreference {

    private static String MAC_REMOTE_DEVICE ="";
    private final String MAC_BT ="MACBT";
    private  Context mContext;

    public StorageSharedPreference(Context ctx){
        mContext = ctx;
    }

    private SharedPreferences getSharedPreferences (String spName) {
        return mContext.getSharedPreferences(spName, Context.MODE_PRIVATE);
    }
    public void putMAC_BT(String MAC){
        SharedPreferences sharedPreferences = getSharedPreferences(MAC_BT);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(MAC_BT, MAC);
        editor.commit();
    }

    public String getMAC_BT() {
        SharedPreferences readData = getSharedPreferences(MAC_BT);
        String mac = readData.getString(MAC_BT, "");
        return mac;
    }
}

