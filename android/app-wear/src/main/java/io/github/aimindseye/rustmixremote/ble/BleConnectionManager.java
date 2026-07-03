package io.github.aimindseye.rustmixremote.ble;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.ParcelUuid;

import java.util.Collections;

import io.github.aimindseye.rustmixremote.protocol.RemoteCommand;
import io.github.aimindseye.rustmixremote.protocol.RemotePacket;

public final class BleConnectionManager {
    public interface Listener {
        void onStatus(String status);
        void onDeviceFound(String name, String address);
        void onConnected();
        void onDisconnected();
    }

    private final Context context;
    private final Listener listener;
    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic commandCharacteristic;
    private byte sequence = 0;
    private boolean scanning = false;

    public BleConnectionManager(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
    }

    public boolean hasRuntimePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                    && context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    @SuppressLint("MissingPermission")
    public void scanAndConnect() {
        if (!hasRuntimePermissions()) {
            listener.onStatus("Bluetooth permissions needed");
            return;
        }

        BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (manager == null) {
            listener.onStatus("Bluetooth unavailable");
            return;
        }

        BluetoothAdapter adapter = manager.getAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            listener.onStatus("Bluetooth is off");
            return;
        }

        BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();
        if (scanner == null) {
            listener.onStatus("BLE scanner unavailable");
            return;
        }

        if (scanning) {
            listener.onStatus("Already scanning");
            return;
        }

        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(RustmixBleUuids.SERVICE))
                .build();
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        scanning = true;
        listener.onStatus("Scanning for Rustmix Remote...");
        scanner.startScan(Collections.singletonList(filter), settings, scanCallback);
    }

    @SuppressLint("MissingPermission")
    public void disconnect() {
        if (gatt != null) {
            gatt.disconnect();
            gatt.close();
            gatt = null;
        }
        commandCharacteristic = null;
        listener.onDisconnected();
        listener.onStatus("Disconnected");
    }

    @SuppressLint("MissingPermission")
    public boolean send(RemoteCommand command) {
        if (!hasRuntimePermissions()) {
            listener.onStatus("Bluetooth permissions needed");
            return false;
        }
        if (gatt == null || commandCharacteristic == null) {
            listener.onStatus("Not connected");
            return false;
        }

        byte[] packet = RemotePacket.command(sequence++, command);

        if (Build.VERSION.SDK_INT >= 33) {
            int result = gatt.writeCharacteristic(
                    commandCharacteristic,
                    packet,
                    BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            );
            boolean ok = result == BluetoothGatt.GATT_SUCCESS;
            listener.onStatus(ok ? "Sent " + command.name() : "Send failed: " + result);
            return ok;
        } else {
            commandCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            commandCharacteristic.setValue(packet);
            boolean ok = gatt.writeCharacteristic(commandCharacteristic);
            listener.onStatus(ok ? "Sent " + command.name() : "Send failed");
            return ok;
        }
    }

    @SuppressLint("MissingPermission")
    private void connectTo(BluetoothDevice device) {
        listener.onStatus("Connecting...");
        gatt = device.connectGatt(context, false, gattCallback);
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String name = device.getName() != null ? device.getName() : "Rustmix device";
            listener.onDeviceFound(name, device.getAddress());

            BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            if (manager != null && manager.getAdapter() != null && scanning) {
                BluetoothLeScanner scanner = manager.getAdapter().getBluetoothLeScanner();
                if (scanner != null) {
                    scanner.stopScan(this);
                }
            }
            scanning = false;
            connectTo(device);
        }

        @Override
        public void onScanFailed(int errorCode) {
            scanning = false;
            listener.onStatus("Scan failed: " + errorCode);
        }
    };

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                listener.onStatus("Discovering services...");
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                commandCharacteristic = null;
                listener.onDisconnected();
                listener.onStatus("Disconnected");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                listener.onStatus("Service discovery failed: " + status);
                return;
            }

            BluetoothGattService service = gatt.getService(RustmixBleUuids.SERVICE);
            if (service == null) {
                listener.onStatus("Rustmix Remote service not found");
                return;
            }

            commandCharacteristic = service.getCharacteristic(RustmixBleUuids.COMMAND);
            if (commandCharacteristic == null) {
                listener.onStatus("Command characteristic not found");
                return;
            }

            listener.onConnected();
            listener.onStatus("Connected to Rustmix Remote");
        }
    };
}
