package com.resonance.ekka.mariaapilib.usbserial;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;





import java.io.IOException;
import java.util.Set;

import static com.resonance.ekka.mariaapilib.iMariaCommands.DBG_TAG;

public class UsbWorker {

    private static int BAUD_RATE = 115200;
    private static int DATA_BITS = UsbSerialInterface.DATA_BITS_8;
    private static int STOP_BITS = UsbSerialInterface.STOP_BITS_1;
    private static int PARITY    = UsbSerialInterface.PARITY_EVEN;

    private static int deviceVID = 4292;
    private static int devicePID = 60000;


    private Context mContext;
    private UsbService usbService;
    private Handler mHandler;
    private ServiceConnection usbConnection;


    public UsbWorker(Context context,Handler handler) {
        Log.d(DBG_TAG, "Run UsbWorker");
        this.mContext = context;
        this.mHandler = handler;

        usbConnection = new ServiceConnection()
        {
        @Override
            public void onServiceConnected(ComponentName arg0, IBinder arg1) {
              //  Log.d(DBG_TAG, "*****************onServiceConnected");
                usbService = ((UsbService.UsbBinder) arg1).getService();
                usbService.setHandler(mHandler);
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
               // usbService = null;
               // Log.d(DBG_TAG, "UsbWorker onServiceDisconnected");
            }
        };
        StartServiceUSB();
    }
    private void StartServiceUSB()
    {
        Bundle mBundle = new Bundle();
        mBundle.putString("BAUD_RATE", String.valueOf(this.BAUD_RATE));
        mBundle.putString("DATA_BITS", String.valueOf(this.DATA_BITS));
        mBundle.putString("STOP_BITS", String.valueOf(this.STOP_BITS));
        mBundle.putString("PARITY", String.valueOf(this.PARITY));
        mBundle.putString("deviceVID", String.valueOf(this.deviceVID));
        mBundle.putString("devicePID", String.valueOf(this.devicePID));


        Intent intent = new Intent(this.mContext, UsbService.class);
        if (mBundle != null && !mBundle.isEmpty()) {
            Set<String> keys = mBundle.keySet();
            for (String key : keys) {
                String extra = mBundle.getString(key);
                intent.putExtra(key, extra);
            }
        }
       // Log.d(DBG_TAG, "UsbWorker StartServiceUSB");
        mContext.bindService(intent, usbConnection, Context.BIND_AUTO_CREATE);
        mContext.startService(intent);

    }

    public void StopServiceUSB()
    {
        Log.d(DBG_TAG, "StopServiceUSB()");
        try
        {
            Intent intent = new Intent(this.mContext, UsbService.class);
            mContext.stopService(intent);
            mContext.unbindService(usbConnection);
            UsbService.SERVICE_CONNECTED = false;
            usbService = null;
        }catch(Exception ex){
            Log.d(DBG_TAG, "Ошибка остановки сервиса USB : "+ex.getMessage());
        }
    }
    public boolean write(byte[] Buf)
    {
        boolean res= false;
        if (usbService != null) {
           // Log.d(DBG_TAG, "write USB ... ");
            usbService.write(Buf);
           return true ;
        }
        return false;
    }
    public byte[] read(long TimeOut)
    {
        if (usbService == null) return null;
           return usbService.read(TimeOut*10);

    }



}
