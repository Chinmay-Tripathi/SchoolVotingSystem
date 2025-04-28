package com.example.teacherapp;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MainActivity extends AppCompatActivity {

    private static final String APP_NAME = "VotingApp";
    private static final UUID MY_UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    private static final int REQUEST_PERMISSION = 1;
    private static final int REQUEST_DISCOVERABLE = 2;

    private BluetoothAdapter bluetoothAdapter;
    private VoteDatabaseHelper dbHelper;
    private TextView statusTextView, resultsTextView;
    private Button showResultsButton, addCandidateButton;
    private EditText candidateNameEditText;
    private LinearLayout candidatesLayout;
    private Handler handler = new Handler(Looper.getMainLooper());
    private BluetoothServerSocket serverSocket;
    private List<String> candidates = new ArrayList<>();
    private ConcurrentHashMap<BluetoothSocket, OutputStream> connectedClients = new ConcurrentHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusTextView = findViewById(R.id.statusTextView);
        resultsTextView = findViewById(R.id.resultsTextView);
        showResultsButton = findViewById(R.id.showResultsButton);
        addCandidateButton = findViewById(R.id.addCandidateButton);
        candidateNameEditText = findViewById(R.id.candidateNameEditText);
        candidatesLayout = findViewById(R.id.candidatesLayout);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        dbHelper = new VoteDatabaseHelper(this);

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

        addCandidateButton.setOnClickListener(v -> addCandidate());
        showResultsButton.setOnClickListener(v -> {
            showResults();
            broadcastResults();
        });

        // Make device discoverable
        if (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300); // 5 minutes
            startActivityForResult(discoverableIntent, REQUEST_DISCOVERABLE);
        } else {
            startServer();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_DISCOVERABLE) {
            if (resultCode == RESULT_OK) {
                startServer();
            } else {
                Toast.makeText(this, "Device must be discoverable to accept votes", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
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
                // Make device discoverable
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
                if (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                    Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                    startActivityForResult(discoverableIntent, REQUEST_DISCOVERABLE);
                } else {
                    startServer();
                }
            } else {
                Toast.makeText(this, "All permissions are required to use this app", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void addCandidate() {
        String candidateName = candidateNameEditText.getText().toString().trim();
        if (!candidateName.isEmpty() && !candidates.contains(candidateName)) {
            candidates.add(candidateName);
            updateCandidatesDisplay();
            candidateNameEditText.setText("");
            broadcastCandidateList();
        }
    }

    private void updateCandidatesDisplay() {
        candidatesLayout.removeAllViews();
        for (String candidate : candidates) {
            TextView candidateView = new TextView(this);
            candidateView.setText(candidate);
            candidateView.setPadding(0, 8, 0, 8);
            candidatesLayout.addView(candidateView);
        }
    }

    private void broadcastCandidateList() {
        String candidateList = String.join(",", candidates);
        String message = "CANDIDATES:" + candidateList;
        broadcastMessage(message);
    }

    private void broadcastResults() {
        String results = resultsTextView.getText().toString();
        broadcastMessage("RESULTS:" + results);
    }

    private void broadcastMessage(String message) {
        for (OutputStream outputStream : connectedClients.values()) {
            try {
                outputStream.write(message.getBytes());
                outputStream.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void startServer() {
        new Thread(() -> {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, MY_UUID);
                handler.post(() -> statusTextView.setText("Server Started. Waiting for votes..."));

                while (true) {
                    BluetoothSocket socket = serverSocket.accept();
                    if (socket != null) {
                        OutputStream outputStream = socket.getOutputStream();
                        connectedClients.put(socket, outputStream);
                        // Send current candidate list to new client
                        broadcastCandidateList();
                        // Start a new thread to handle this client
                        new Thread(() -> handleClient(socket)).start();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                handler.post(() -> statusTextView.setText("Error Starting Server: " + e.getMessage()));
            }
        }).start();
    }

    private void handleClient(BluetoothSocket socket) {
        try {
            InputStream inputStream = socket.getInputStream();
            byte[] buffer = new byte[1024];
            int bytes;

            while ((bytes = inputStream.read(buffer)) != -1) {
                String message = new String(buffer, 0, bytes);
                if (message.startsWith("VOTE:")) {
                    String[] parts = message.substring(5).split(",");
                    if (parts.length == 2) {
                        String student = parts[0].trim();
                        String candidate = parts[1].trim();

                        if (candidates.contains(candidate)) {
                            boolean success = dbHelper.addVote(student, candidate);
                            handler.post(() -> {
                                if (success) {
                                    statusTextView.setText("Vote received from " + student);
                                } else {
                                    statusTextView.setText("Duplicate vote from " + student);
                                }
                            });
                        } else {
                            handler.post(() -> statusTextView.setText("Invalid candidate from " + student));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            handler.post(() -> statusTextView.setText("Error handling vote: " + e.getMessage()));
        } finally {
            connectedClients.remove(socket);
            try {
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void showResults() {
        Cursor cursor = dbHelper.getAllVotes();
        HashMap<String, Integer> voteCounts = new HashMap<>();

        if (cursor.moveToFirst()) {
            do {
                String candidate = cursor.getString(1);
                voteCounts.put(candidate, voteCounts.getOrDefault(candidate, 0) + 1);
            } while (cursor.moveToNext());
        }
        cursor.close();

        StringBuilder results = new StringBuilder();
        for (String candidate : voteCounts.keySet()) {
            results.append(candidate).append(": ").append(voteCounts.get(candidate)).append(" votes\n");
        }

        resultsTextView.setText(results.toString());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (BluetoothSocket socket : connectedClients.keySet()) {
            try {
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        connectedClients.clear();
    }
}
