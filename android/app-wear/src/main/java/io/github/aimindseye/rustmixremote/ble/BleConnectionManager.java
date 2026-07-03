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

import java.util.Collections;
import java.util.List;

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

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        scanning = true;
        listener.onStatus("Scanning for Rustmix Remote...");
        android.util.Log.i("RustmixRemoteBLE", "startScan broad null-filter");
        scanner.startScan(null, settings, scanCallback);

        // ESP32-S3 firmware log reports BLE MAC 9C:13:9E:B1:3D:66.
        // If Wear OS scanning does not deliver callbacks, try direct GATT connect.
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (scanning && gatt == null) {
                android.util.Log.i("RustmixRemoteBLE", "scan fallback: direct connect to known Rustmix MAC");
                try {
                    BluetoothDevice known = adapter.getRemoteDevice("9C:13:9E:B1:3D:66");
                    listener.onDeviceFound("Rustmix Remote", known.getAddress());
                    try {
                        scanner.stopScan(scanCallback);
                    } catch (Exception ignored) {
                    }
                    scanning = false;
                    listener.onStatus("Connecting to Rustmix Remote...");
                    gatt = known.connectGatt(context, false, gattCallback);
                } catch (Exception e) {
                    android.util.Log.e("RustmixRemoteBLE", "direct connect fallback failed", e);
                    listener.onStatus("Rustmix Remote not found");
                }
            }
        }, 5000);
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

            String scanName = null;
            if (result.getScanRecord() != null) {
                scanName = result.getScanRecord().getDeviceName();
            }

            String deviceName = null;
            try {
                deviceName = device.getName();
            } catch (SecurityException ignored) {
                // BLUETOOTH_CONNECT may be denied on some Wear OS builds.
            }

            String name = scanName != null
                    ? scanName
                    : (deviceName != null ? deviceName : "Rustmix device");

            String address = device.getAddress();

            boolean hasRustmixService = false;
            if (result.getScanRecord() != null && result.getScanRecord().getServiceUuids() != null) {
                for (android.os.ParcelUuid uuid : result.getScanRecord().getServiceUuids()) {
                    if (RustmixBleUuids.SERVICE.equals(uuid.getUuid())) {
                        hasRustmixService = true;
                        break;
                    }
                }
            }

            boolean hasRustmixName =
                    (scanName != null && scanName.toLowerCase().contains("rustmix"))
                            || (deviceName != null && deviceName.toLowerCase().contains("rustmix"));

            // Firmware log shows BLE MAC 9c:13:9e:b1:3d:66.
            boolean hasKnownRustmixAddress =
                    "9C:13:9E:B1:3D:66".equalsIgnoreCase(address);

            android.util.Log.i("RustmixRemoteBLE",
                    "scan name=" + name
                            + " scanName=" + scanName
                            + " deviceName=" + deviceName
                            + " address=" + address
                            + " service=" + hasRustmixService
                            + " rustmixName=" + hasRustmixName
                            + " knownAddress=" + hasKnownRustmixAddress);

            if (!hasRustmixService && !hasRustmixName && !hasKnownRustmixAddress) {
                return;
            }

            listener.onDeviceFound(name, address);

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
