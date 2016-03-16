package florianjean.ray_manta_accelerometer;

import android.bluetooth.*;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.SensorEventListener;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;

import android.os.Message;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import android.os.Handler;
import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private final static String string_my_UUID = "d2642200-e5de-11e5-9730-9a79f06e9478";

    final int MESSAGE_READ = 10;
    private static boolean use_app_UUID;
    private static boolean manual_data;


    private static Context context;
    private static Handler mHandler;
    private static BluetoothDevice mDevice;
    private static BluetoothDevice mDevice_choosen;
    private static BluetoothAdapter mBluetoothAdapter;
    private static SensorManager senSensorManager;
    private static Sensor senAccelerometer;
    private static String[] device_selected;
    private static BluetoothSocket mSocket;

    private static InputStream mInputStream;
    private static OutputStream mOutputStream;

    private static ParcelUuid[] ParcelUUID_used;
    private static UUID UUID_used;
    private static UUID my_UUID;

    boolean data_acquisition = false;
    private boolean is_new_data = false;

    private float x,y,z,delay,ratio;

    private Handler handler_send;
    private Runnable runnableCode;

    private TextView textAxisX,textAxisY,textAxisZ;
    private EditText textRatio,textDelay;
    private CheckBox checkManually;
    private ListView listDevice;
    private LinearLayout deviceLayout;
    private LinearLayout dataLayout;
    private Button refreshDeviceButton, sendData;
    private RadioButton radioDetectUUID, radioAppUUID;

    private ArrayAdapter<String> mArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init_graphical_compenents();
        init_sensor();
        init_bluetooth_components();
        init_callback();
        refreshBluetoothList();
        init_handler_data();
        handler_send.post(runnableCode);
    }


    private void init_graphical_compenents(){
        mArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);

        context = getApplicationContext();
        dataLayout = (LinearLayout) findViewById(R.id.dataLayout);
        deviceLayout = (LinearLayout) findViewById(R.id.deviceLayout);
        refreshDeviceButton = (Button) findViewById(R.id.refreshDeviceButton);

        textAxisX = (TextView) findViewById(R.id.textViewAxisX);
        textAxisY = (TextView) findViewById(R.id.textViewAxisY);
        textAxisZ = (TextView) findViewById(R.id.textViewAxisZ);
        textRatio = (EditText) findViewById(R.id.textViewRatio);
        textDelay = (EditText) findViewById(R.id.textViewDelay);
        radioDetectUUID = (RadioButton) findViewById(R.id.radioButtonDetectUUID);
        radioAppUUID = (RadioButton) findViewById(R.id.radioButtonAppUUID);
        checkManually = (CheckBox) findViewById(R.id.checkBoxSendManually);
        sendData = (Button) findViewById(R.id.buttonSendData);
        listDevice = (ListView) findViewById(R.id.listViewDevice);
        listDevice.setAdapter(mArrayAdapter);

        textAxisX.setText("Ready");
        textAxisY.setText("Ready");
        textAxisZ.setText("Ready");

    }

    private void init_sensor(){
        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

    }

    private void init_handler_data(){
        handler_send = new Handler();
        // Define the code block to be executed
        runnableCode = new Runnable() {
            @Override
            public void run() {
                if(data_acquisition&&is_new_data){
                    is_new_data=false;
                    send_data(float2ByteArray(ratio));
                    send_data(float2ByteArray(delay));
                    showToast("Data send");
                }
                handler_send.postDelayed(runnableCode, 1000);
            }
        };
    }

    private void init_bluetooth_components(){
        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MESSAGE_READ: {
                    }
                }
            };
        };

        manual_data = false;
        use_app_UUID = false;
        my_UUID = UUID.fromString(string_my_UUID);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }
    }

    private void init_callback() {

        textRatio.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (!textRatio.hasFocus()) {
                    if (checkManually.isChecked()) {
                        if(!(textDelay.getText().toString()=="")){
                            float value = Float.valueOf(textRatio.getText().toString());
                            if (value < 0) {
                                textRatio.setText("0");
                                showToast("Min value is 0");
                            }
                            if (value > 1) {
                                textRatio.setText("1");
                                showToast("Max value is 1");
                            }
                        }
                        else {
                            textRatio.setText("0.5");
                        }
                    }
                    ratio = Float.valueOf(textRatio.getText().toString());
                }
            }
        });

        textDelay.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (!textDelay.hasFocus()) {
                    if (checkManually.isChecked()) {
                        if(!(textDelay.getText().toString()=="")){
                            float value = Float.valueOf(textDelay.getText().toString());
                            if (value < 10) {
                                textDelay.setText("10");
                                showToast("Min value is 10");
                            }
                        }
                        else{
                            textDelay.setText("100");
                        }
                    }
                    ratio = Float.valueOf(textDelay.getText().toString());
                }
            }
        });

        refreshDeviceButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                refreshBluetoothList();
            }
        });

        radioAppUUID.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                radioAppUUID.setChecked(true);
                radioDetectUUID.setChecked(false);
                use_app_UUID = true;
            }
        });

        radioDetectUUID.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                radioAppUUID.setChecked(false);
                radioDetectUUID.setChecked(true);
                use_app_UUID = false;
            }
        });


        sendData.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean res = send_data(float2ByteArray(delay));
                if (res) {
                    res = send_data(float2ByteArray(ratio));
                }
                if(!res){
                    stop_data();
                }
                else {
                    showToast("Data sent");
                }
            }
        });

        checkManually.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                manual_data = checkManually.isChecked();
                if (manual_data){
                    textRatio.setEnabled(true);
                    textDelay.setEnabled(true);
                }
                else {
                    textRatio.setEnabled(false);
                    textDelay.setEnabled(false);
                }
            }
        });

        listDevice.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                                           int pos, long arg3) {
                setDeviceLayoutInvisible();
                device_selected = mArrayAdapter.getItem(pos).split(System.getProperty("line.separator"));

                if (connection_client(device_selected[1])) {
                    if (set_IO_bluetooth()) {
                        displayDataLayout();
                    } else {
                        displayDeviceLayout();
                    }
                } else {
                    displayDeviceLayout();
                }
                return true;
            }

            ;
        });

    }

    protected boolean connection_client(String Address){
        boolean res=false;
        mDevice_choosen = mBluetoothAdapter.getRemoteDevice(Address);
        showToast(mDevice_choosen.getName());

        if(use_app_UUID){
            UUID_used = my_UUID;
            showToast("Using the apps UUID");
        }
        else {
            // get the UUID's
            showToast("Detecting the UUID");
            ParcelUUID_used = mDevice_choosen.getUuids();
            UUID_used = ParcelUUID_used[0].getUuid();
        }


        //connection management
        try {
            // MY_UUID is the app's UUID string, also used by the server code
            mSocket = mDevice_choosen.createRfcommSocketToServiceRecord(UUID_used);
            showToast("Socket generated");
            mBluetoothAdapter.cancelDiscovery();
            mSocket.connect();
            showToast("Connected");
            res = true;
        } catch (IOException e) {
            showToast("UNABLE TO CONNECT");
        }
        return res;
    }


    protected boolean set_IO_bluetooth(){
        boolean res = false;
        try {
            mInputStream = mSocket.getInputStream();
            mOutputStream = mSocket.getOutputStream();
            showToast("Ready to send/read data");
            res = true;
        } catch (IOException e) {
            showToast("Error Generating the IO");
        }
        return res;
    }

    protected void refreshBluetoothList(){

        mBluetoothAdapter.cancelDiscovery();
        mArrayAdapter.clear();
        showToast("Refreshing the list");

        // Create a BroadcastReceiver for ACTION_FOUND
        final BroadcastReceiver mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                // When discovery finds a device
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Get the BluetoothDevice object from the Intent
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    // Add the name and address to an array adapter to show in a ListView
                    mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            }
        };

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothAdapter.startDiscovery();
    }

    protected boolean send_data(byte[] bytes){
        boolean res = false;
        try {
            mOutputStream.write(bytes);
            res = true;
        } catch (IOException e) {
        }
        return res;
    }



    protected void displayDataLayout() {
        MainActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                deviceLayout.setVisibility(View.INVISIBLE);
                dataLayout.setVisibility(View.VISIBLE);
            }
        });
        data_acquisition = true;
    }

    protected void displayDeviceLayout() {
        MainActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                deviceLayout.setVisibility(View.VISIBLE);
                dataLayout.setVisibility(View.INVISIBLE);
            }
        });
        data_acquisition = true;
    }

    protected void setDeviceLayoutInvisible(){
        MainActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                deviceLayout.setVisibility(View.INVISIBLE);
            }
        });
    }




    @Override
    public void onSensorChanged(SensorEvent event) {
        if(data_acquisition&&!manual_data) {
            Sensor mySensor = event.sensor;

            if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                x = event.values[0];
                y = event.values[1];
                z = event.values[2];

                textAxisX.setText("" + String.valueOf(x));
                textAxisY.setText("" + String.valueOf(y));
                textAxisZ.setText("" + String.valueOf(z));

                float new_ratio = ((float)((int)(((x + 10) / 20)*10)))/10;
                if(new_ratio!=ratio){
                    ratio=new_ratio;
                    is_new_data=true;
                }
                textRatio.setText("" + String.valueOf(ratio));

                float new_delay = (float)(500 + 100*((int)y));
                if (new_delay < 10) {
                    new_delay = 10;
                }
                if(new_delay!=delay){
                    delay=new_delay;
                    is_new_data=true;
                }
                textDelay.setText("" + String.valueOf(delay));
            }
        }
    }

    public static byte [] float2ByteArray (float value)
    {
        return ByteBuffer.allocate(4).putFloat(value).array();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    protected void onPause() {
        super.onPause();
        senSensorManager.unregisterListener(this);
    }

    protected void onResume() {
        super.onResume();
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    protected void stop_data(){
        data_acquisition=false;
        displayDeviceLayout();
    }



    protected void showToast(String str){
        Toast.makeText(context, str, Toast.LENGTH_SHORT).show();
    }
}
