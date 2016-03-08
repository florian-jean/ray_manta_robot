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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;
import android.os.Handler;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    final int MESSAGE_READ = 10;

    private static Context context;
    private static Handler mHandler;
    private static BluetoothDevice mDevice;
    private static BluetoothAdapter mBluetoothAdapter;
    private static SensorManager senSensorManager;
    private static Sensor senAccelerometer;

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

        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MESSAGE_READ: {
                    }
                }
            };
        };


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

    public BluetoothDevice get_device(){
        return mDevice;
    }

    public BluetoothAdapter get_bluetooth_adapter(){
        return mBluetoothAdapter;
    }

    public Context get_context(){
        return context;
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

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket,
            // because mmServerSocket is final
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("Ray_manta", UUID.fromString("639a941b-84f4-4397-853e-1c1ab4ab6359"));
            } catch (IOException e) { }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    break;
                }
                // If a connection was accepted
                if (socket != null) {
                    // Do work to manage the connection (in a separate thread)
                    //manageConnectedSocket(socket);
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                    }
                    break;
                }
            }
        }

        /** Will cancel the listening socket, and cause the thread to finish */
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) { }
        }
    }


    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(UUID.fromString("639a941b-84f4-4397-853e-1c1ab4ab6359"));
            } catch (IOException e) { }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) { }
                return;
            }

            // Do work to manage the connection (in a separate thread)
            //manageConnectedSocket(mmSocket);
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

}
