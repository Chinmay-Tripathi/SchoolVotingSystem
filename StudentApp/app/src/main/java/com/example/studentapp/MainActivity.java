package com.example.studentapp;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSION = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final UUID MY_UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<BluetoothDevice> devices = new ArrayList<>();
    private ArrayAdapter<String> arrayAdapter;
    private ListView deviceListView;
    private Button scanButton, voteButton;
    private EditText nameEditText;
    private RadioGroup candidateRadioGroup;
    private TextView resultsTextView;
    private List<String> candidates = new ArrayList<>();

    private BluetoothDevice selectedDevice = null;
    private BluetoothSocket socket = null;
    private boolean isConnected = false;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                if (device != null && device.getName() != null) {
                    devices.add(device);
                    arrayAdapter.add(device.getName() + "\n" + device.getAddress());
                    arrayAdapter.notifyDataSetChanged();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        nameEditText = findViewById(R.id.nameEditText);
        candidateRadioGroup = findViewById(R.id.candidateRadioGroup);
        deviceListView = findViewById(R.id.deviceListView);
        scanButton = findViewById(R.id.scanButton);
        voteButton = findViewById(R.id.voteButton);
        resultsTextView = findViewById(R.id.resultsTextView);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Request necessary permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                    new String[]{
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    }, 
                    REQUEST_PERMISSION);
                return;
            }
        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 
                    REQUEST_PERMISSION);
                return;
            }
        }

        // Enable Bluetooth if not already enabled
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_single_choice);
        deviceListView.setAdapter(arrayAdapter);
        deviceListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        deviceListView.setOnItemClickListener((parent, view, position, id) -> {
            selectedDevice = devices.get(position);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            Toast.makeText(MainActivity.this, "Selected: " + selectedDevice.getName(), Toast.LENGTH_SHORT).show();
            connectToDevice();
        });

        scanButton.setOnClickListener(v -> {
            scanDevices();
        });

        voteButton.setOnClickListener(v -> {
            if (selectedDevice != null && validateInput()) {
                sendVote();
            } else {
                Toast.makeText(MainActivity.this, "Fill all fields and select a device", Toast.LENGTH_SHORT).show();
            }
        });

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
    }

    private void connectToDevice() {
        new Thread(() -> {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                socket = selectedDevice.createRfcommSocketToServiceRecord(MY_UUID);
                socket.connect();
                isConnected = true;

                // Start listening for messages
                startListening();

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to connect: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void startListening() {
        new Thread(() -> {
            try {
                InputStream inputStream = socket.getInputStream();
                byte[] buffer = new byte[1024];
                int bytes;

                while (isConnected && (bytes = inputStream.read(buffer)) != -1) {
                    String message = new String(buffer, 0, bytes);
                    handleMessage(message);
                }
            } catch (Exception e) {
                e.printStackTrace();
                isConnected = false;
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Connection lost: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void handleMessage(String message) {
        if (message.startsWith("CANDIDATES:")) {
            String[] candidateArray = message.substring(11).split(",");
            candidates.clear();
            for (String candidate : candidateArray) {
                candidates.add(candidate.trim());
            }

            runOnUiThread(() -> {
                updateCandidateRadioGroup();
                Toast.makeText(MainActivity.this, "Connected to teacher's device", Toast.LENGTH_SHORT).show();
            });
        } else if (message.startsWith("RESULTS:")) {
            final String results = message.substring(8);
            runOnUiThread(() -> {
                resultsTextView.setText(results);
                Toast.makeText(MainActivity.this, "Results updated", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void updateCandidateRadioGroup() {
        candidateRadioGroup.removeAllViews();
        for (String candidate : candidates) {
            RadioButton radioButton = new RadioButton(this);
            radioButton.setText(candidate);
            radioButton.setPadding(0, 8, 0, 8);
            candidateRadioGroup.addView(radioButton);
        }
    }

    private void sendVote() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_PERMISSION);
            return;
        }

        new Thread(() -> {
            try {
                String name = nameEditText.getText().toString().trim();
                int selectedId = candidateRadioGroup.getCheckedRadioButtonId();
                RadioButton selectedButton = findViewById(selectedId);
                String candidate = selectedButton.getText().toString();

                String voteData = "VOTE:" + name + "," + candidate;

                if (socket == null || !socket.isConnected()) {
                    socket = selectedDevice.createRfcommSocketToServiceRecord(MY_UUID);
                    socket.connect();
                }

                OutputStream outputStream = socket.getOutputStream();
                outputStream.write(voteData.getBytes());
                outputStream.flush();

                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Vote Sent Successfully!", Toast.LENGTH_SHORT).show();
                    // Clear input fields after successful vote
                    nameEditText.setText("");
                    candidateRadioGroup.clearCheck();
                });

            } catch (Exception e) {
                e.printStackTrace();
                final String errorMessage = e.getMessage() != null ? e.getMessage() : "Failed to Send Vote!";
                runOnUiThread(() -> Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private boolean validateInput() {
        String name = nameEditText.getText().toString().trim();
        int selectedId = candidateRadioGroup.getCheckedRadioButtonId();
        return !name.isEmpty() && selectedId != -1;
    }

    private void scanDevices() {
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Please enable Bluetooth first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        arrayAdapter.clear();
        devices.clear();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, REQUEST_PERMISSION);
                return;
            }
        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION);
                return;
            }
        }

        if (bluetoothAdapter.startDiscovery()) {
            Toast.makeText(this, "Scanning for devices...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to start scanning", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isConnected = false;
        unregisterReceiver(receiver);
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Bluetooth must be enabled to use this app", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (allPermissionsGranted) {
                scanDevices();
            } else {
                Toast.makeText(this, "All permissions are required to use this app", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}
