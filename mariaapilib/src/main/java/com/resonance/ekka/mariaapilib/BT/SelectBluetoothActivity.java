package com.resonance.ekka.mariaapilib.BT;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.preference.ListPreference;
import android.support.annotation.NonNull;

import android.support.v7.app.AppCompatActivity;
import android.app.LoaderManager.LoaderCallbacks;

import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;


import com.resonance.ekka.mariaapilib.MariaAPI;
import com.resonance.ekka.mariaapilib.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static android.Manifest.permission.READ_CONTACTS;
import static com.resonance.ekka.mariaapilib.iMariaCommands.DBG_TAG;


public class SelectBluetoothActivity extends Activity {

    public static final String CANCELSTR = "#NODEVSELECT#";
    RadioGroup radiogroup;
    Handler mHandler = null;

    Button btnselDev;
    Button btnCancelDev;

    private BluetoothAdapter mBluetoothAdapter = null;

    private String SelectedDev = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_bluetooth);

        Bundle  b = getIntent().getExtras();
        if (b!=null) {
            SelectedDev = b.getString("DEVNAME");
            //MariaAPI.ClassName model = (MariaAPI.ClassName) getIntent().getSerializableExtra("Editing");
          //  mHandler = model.handler;

        }
      //  Log.i(DBG_TAG, "SelectedDev: " + SelectedDev);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //зарегистрируем приемник событий от Bluetooth
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        this.registerReceiver(mBroadcastReceiverBT, filter);

        radiogroup = (RadioGroup) findViewById(R.id.rg_choosedev);

        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
          //  Log.i(DBG_TAG, "Worker mBluetoothAdapter is not Enabled");
        }else
            setListPairedDevices(radiogroup);




        btnselDev = (Button) findViewById(R.id.btnSelectDev);
        btnselDev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //((RadioButton)radiogroup.getChildAt(1)).setChecked(true);
                int checkedRadioButtonId = radiogroup.getCheckedRadioButtonId();
                if (checkedRadioButtonId != -1)
                {
                    unregisterReceiver(mBroadcastReceiverBT);
                    final String value = ((RadioButton) findViewById(radiogroup.getCheckedRadioButtonId())).getText().toString();
                   // Log.d("SSS", "radiogroup: " + value);
                    int indexFirst = value.indexOf("[")+1;
                    int indexLast = value.indexOf("]");
                    String Mac = value.substring(indexFirst,indexLast);
                    Intent intent = new Intent(MariaAPI.BROADCAST_ACTION);
                    intent.putExtra("MACBT", Mac);
                    intent.putExtra("DEVNAME", value.substring(indexLast+1).trim());
                    SelectedDev = Mac;
                    sendBroadcast(intent);
                    //setResult(RESULT_OK, intent);
                    finish();
                }
            }
        });


        btnCancelDev = (Button) findViewById(R.id.btnCancelSelDev);
        btnCancelDev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(MariaAPI.BROADCAST_ACTION);
                intent.putExtra("MACBT", CANCELSTR);
                intent.putExtra("DEVNAME", CANCELSTR);
                sendBroadcast(intent);
                unregisterReceiver(mBroadcastReceiverBT);
                finish();  };
           });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if ((keyCode == KeyEvent.KEYCODE_BACK))
        {
            return false; //I have tried here true also
        }
        return super.onKeyDown(keyCode, event);
    }

    protected  void setListPairedDevices(RadioGroup radiogroup) {

      //  Log.i(DBG_TAG, "setListPairedDevices");
        if (mBluetoothAdapter!=null) {

            mBluetoothAdapter.cancelDiscovery();
            //Получим подключенные устройства
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

            int i = 0;
            if (pairedDevices != null && !pairedDevices.isEmpty()) {

                for (BluetoothDevice device : pairedDevices) {
                    RadioButton radiobutton = new RadioButton(this);
                    radiobutton.setHeight(120);
                    radiobutton.setText( "[" + device.getAddress()+"] \t"+ device.getName() );
                    //Log.d(DBG_TAG, ""+device.getAddress());
                    radiobutton.setId(i);
                    radiogroup.addView(radiobutton);
                    i++;
                }

            }
            radiogroup.invalidate();
        }
        else
            Log.i(DBG_TAG, "mBluetoothAdapter=null");


        if (!TextUtils.isEmpty(SelectedDev))
        {
           // Log.i(DBG_TAG, "begin: "+SelectedDev+" radiogroup.getChildCount(): "+radiogroup.getChildCount());
            for (int i=0;i<radiogroup.getChildCount();i++)
            {
                RadioButton radiobutton = (RadioButton)radiogroup.getChildAt(i);
                int indexFirst = radiobutton.getText().toString().indexOf("[")+1;
                int indexLast = radiobutton.getText().toString().indexOf("]");
                String Mac = radiobutton.getText().toString().substring(indexFirst,indexLast);
                if (Mac.equals(SelectedDev))
                    ((RadioButton) radiogroup.getChildAt(i)).setChecked(true);
            }
        }

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
                      //  Log.i(DBG_TAG, "------------- mBroadcastReceiverBT STATE_OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                     //   Log.i(DBG_TAG, "-------------mBroadcastReceiverBT STATE_TURNING_OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                      //  Log.i(DBG_TAG, "-------------mBroadcastReceiverBT STATE_ON");
                        setListPairedDevices(radiogroup);
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                     //   Log.i(DBG_TAG, "-------------mBroadcastReceiverBT STATE_TURNING_ON");

                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                      //  Log.i(DBG_TAG, "-------------mBroadcastReceiverBT STATE_CONNECTED");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                      //  Log.i(DBG_TAG, "-------------mBroadcastReceiverBT STATE_CONNECTING");
                        break;
                    case BluetoothAdapter.STATE_DISCONNECTING:
                       // Log.i(DBG_TAG, "-------------mBroadcastReceiverBT STATE_DISCONNECTING");
                        break;
                    case BluetoothAdapter.STATE_DISCONNECTED:
                      //  Log.i(DBG_TAG, "-------------mBroadcastReceiverBT STATE_DISCONNECTED");
                        break;
                }
            }
        }
    };


}

