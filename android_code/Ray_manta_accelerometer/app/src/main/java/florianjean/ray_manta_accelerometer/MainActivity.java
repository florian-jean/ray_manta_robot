package florianjean.ray_manta_accelerometer;

import android.bluetooth.*;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.SensorEventListener;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;


public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private Context context;
    private BluetoothDevice mDevice;
    private BluetoothAdapter mBluetoothAdapter;
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;

    boolean data_acquisition = false;

    private long lastUpdate = 0;
    private float last_x, last_y, last_z;
    private static final int SHAKE_THRESHOLD = 600;

    private float x,y,z,delay,ratio, lastSentRatio, lastSentDelay;

    private float triggerDelay = 50;
    private float triggerRatio = (float) 0.05;

    private TextView textAxisX,textAxisY,textAxisZ,textRatio,textDelay;
    private CheckBox checkManually;
    private ListView listDevice;
    private LinearLayout deviceLayout;
    private TableLayout dataLayout;
    private Button refreshDeviceButton, selectDeviceButton;

    private ArrayAdapter<String> mArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);

        context = getApplicationContext();
        dataLayout = (TableLayout) findViewById(R.id.dataLayout);
        deviceLayout = (LinearLayout) findViewById(R.id.deviceLayout);
        refreshDeviceButton = (Button) findViewById(R.id.refreshDeviceButton);
        selectDeviceButton = (Button) findViewById(R.id.selectDeviceButton);

        textAxisX = (TextView) findViewById(R.id.textViewAxisX);
        textAxisY = (TextView) findViewById(R.id.textViewAxisY);
        textAxisZ = (TextView) findViewById(R.id.textViewAxisZ);
        textRatio = (TextView) findViewById(R.id.textViewRatio);
        textDelay = (TextView) findViewById(R.id.textViewDelay);
        checkManually = (CheckBox) findViewById(R.id.checkBoxManually);
        listDevice = (ListView) findViewById(R.id.listViewDevice);
        listDevice.setAdapter(mArrayAdapter);

        textAxisX.setText("Ready");
        textAxisY.setText("Ready");
        textAxisZ.setText("Ready");

        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }

        refreshDeviceButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                refreshBluetoothList();
            }
        });

        listDevice.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                                           int pos, long arg3) {
                displayDataLayout();
                return true;
            }
        });

        selectDeviceButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                displayDataLayout();
            }
        });
    }

    public void refreshBluetoothList(){
        mArrayAdapter.clear();
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }

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
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        if(data_acquisition) {
            Sensor mySensor = event.sensor;

            if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                x = event.values[0];
                y = event.values[1];
                z = event.values[2];

                textAxisX.setText("" + String.valueOf(x));
                textAxisY.setText("" + String.valueOf(y));
                textAxisZ.setText("" + String.valueOf(z));

                ratio = (x + 10) / 20;
                textRatio.setText("" + String.valueOf(ratio));

                delay = (500 + 100 * y);
                if (delay < 1) {
                    delay = 1;
                }
                textDelay.setText("" + String.valueOf(delay));

                if (delay > (lastSentDelay + triggerDelay) || delay < (lastSentDelay - triggerDelay) || ratio > (lastSentRatio + triggerRatio) || ratio < (lastSentRatio - triggerRatio)) {
                    lastSentDelay = delay;
                    lastSentRatio = ratio;
                    Toast.makeText(context, "Sending data", Toast.LENGTH_SHORT).show();
                }
            }
        }
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




}
