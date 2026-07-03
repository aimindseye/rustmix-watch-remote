package io.github.aimindseye.rustmixremote.ble;

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
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.List;
import java.util.Locale;

public final class BleConnectionManager {
    public interface Listener {
        void onStatus(String status);
        void onDeviceFound(String name, String address);
        void onConnected(String name, String address);
        void onDisconnected();
        void onWriteSent(String label);
    }

    private static final String TAG = "RustmixRemoteBLE";
    private static final String PREFS = "rustmix_remote";
    private static final String KEY_DEVICE_ADDRESS = "device_address";
    private static final String DEFAULT_RUSTMIX_ADDRESS = "9C:13:9E:B1:3D:66";

    private final Context context;
    private final Listener listener;
    private final SharedPreferences prefs;
    private final BluetoothAdapter adapter;

    private BluetoothLeScanner scanner;
    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic commandCharacteristic;
    private boolean scanning;
    private int sequence;

    public BleConnectionManager(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        this.prefs = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        BluetoothManager manager =
                (BluetoothManager) this.context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.adapter = manager != null ? manager.getAdapter() : null;
    }

    public String getSavedDeviceAddress() {
        String saved = prefs.getString(KEY_DEVICE_ADDRESS, "");
        if (saved == null || saved.trim().isEmpty()) {
            return DEFAULT_RUSTMIX_ADDRESS;
        }
        return saved.trim().toUpperCase(Locale.US);
    }

    public void saveDeviceAddress(String address) {
        String normalized = normalizeAddress(address);
        if (normalized.isEmpty()) {
            listener.onStatus("No address entered");
            return;
        }
        prefs.edit().putString(KEY_DEVICE_ADDRESS, normalized).apply();
        listener.onStatus("Saved device: " + normalized);
    }

    public boolean isConnected() {
        return gatt != null && commandCharacteristic != null;
    }

    public void startScanOrConnect(String preferredAddress) {
        if (adapter == null) {
            listener.onStatus("Bluetooth unavailable");
            return;
        }

        String normalized = normalizeAddress(preferredAddress);
        if (!normalized.isEmpty()) {
            prefs.edit().putString(KEY_DEVICE_ADDRESS, normalized).apply();
        }

        String fallbackAddress = getSavedDeviceAddress();

        scanner = adapter.getBluetoothLeScanner();
        if (scanner == null) {
            listener.onStatus("BLE scanner unavailable; using saved address");
            connectToAddress(fallbackAddress);
            return;
        }

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        scanning = true;
        listener.onStatus("Scanning for Rustmix Remote...");
        android.util.Log.i(TAG, "startScan broad null-filter fallback=" + fallbackAddress);

        try {
            scanner.startScan(null, settings, scanCallback);
        } catch (SecurityException e) {
            android.util.Log.e(TAG, "scan permission denied", e);
            listener.onStatus("BLE scan permission denied; using saved address");
            connectToAddress(fallbackAddress);
            return;
        } catch (Exception e) {
            android.util.Log.e(TAG, "scan failed immediately", e);
            listener.onStatus("BLE scan failed; using saved address");
            connectToAddress(fallbackAddress);
            return;
        }

        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (scanning && gatt == null) {
                android.util.Log.i(TAG, "scan fallback: direct connect to saved Rustmix MAC " + getSavedDeviceAddress());
                listener.onStatus("Fallback connecting to saved device...");
                connectToAddress(getSavedDeviceAddress());
            }
        }, 5000);
    }

    public void connectToAddress(String address) {
        if (adapter == null) {
            listener.onStatus("Bluetooth unavailable");
            return;
        }

        String normalized = normalizeAddress(address);
        if (normalized.isEmpty()) {
            listener.onStatus("No saved device address");
            return;
        }

        stopScanQuietly();

        try {
            BluetoothDevice device = adapter.getRemoteDevice(normalized);
            listener.onDeviceFound("Rustmix Remote", normalized);
            listener.onStatus("Connecting to Rustmix Remote...");
            android.util.Log.i(TAG, "direct connect address=" + normalized);

            closeGattQuietly();
            gatt = device.connectGatt(context, false, gattCallback);
        } catch (SecurityException e) {
            android.util.Log.e(TAG, "connect permission denied", e);
            listener.onStatus("BLE connect permission denied");
        } catch (Exception e) {
            android.util.Log.e(TAG, "direct connect failed", e);
            listener.onStatus("Connect failed: " + e.getMessage());
        }
    }

    public void disconnect() {
        stopScanQuietly();

        if (gatt == null) {
            commandCharacteristic = null;
            listener.onStatus("Disconnected");
            listener.onDisconnected();
            return;
        }

        try {
            listener.onStatus("Disconnecting...");
            android.util.Log.i(TAG, "disconnect requested");
            gatt.disconnect();
        } catch (SecurityException e) {
            android.util.Log.e(TAG, "disconnect permission denied", e);
            closeGattQuietly();
            listener.onDisconnected();
        }
    }

    public boolean sendNext() {
        return sendCommand((byte) 0x01, "next");
    }

    public boolean sendPrevious() {
        return sendCommand((byte) 0x02, "previous");
    }

    private boolean sendCommand(byte command, String label) {
        if (gatt == null || commandCharacteristic == null) {
            listener.onStatus("Not connected");
            return false;
        }

        byte[] packet = new byte[] {
                0x01,
                (byte) (sequence++ & 0xFF),
                command,
                0x00,
                0x00,
                0x00
        };

        try {
            commandCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            commandCharacteristic.setValue(packet);

            boolean ok = gatt.writeCharacteristic(commandCharacteristic);
            if (ok) {
                listener.onStatus("Write sent: " + label);
                listener.onWriteSent(label);
                android.util.Log.i(TAG, "write sent label=" + label + " bytes=" + bytesToHex(packet));
            } else {
                listener.onStatus("Write failed: " + label);
                android.util.Log.w(TAG, "writeCharacteristic returned false label=" + label);
            }
            return ok;
        } catch (SecurityException e) {
            android.util.Log.e(TAG, "write permission denied", e);
            listener.onStatus("BLE write permission denied");
            return false;
        }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanFailed(int errorCode) {
            android.util.Log.e(TAG, "scan failed errorCode=" + errorCode);
            listener.onStatus("BLE scan failed: " + errorCode);
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            handleScanResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            if (results == null) {
                return;
            }
            for (ScanResult result : results) {
                handleScanResult(result);
            }
        }
    };

    private void handleScanResult(ScanResult result) {
        if (result == null || result.getDevice() == null) {
            return;
        }

        BluetoothDevice device = result.getDevice();
        String address = device.getAddress();

        ScanRecord record = result.getScanRecord();
        String scanName = record != null ? record.getDeviceName() : null;

        String deviceName = null;
        try {
            deviceName = device.getName();
        } catch (SecurityException ignored) {
        }

        String name = scanName != null
                ? scanName
                : (deviceName != null ? deviceName : "BLE device");

        boolean hasRustmixService = false;
        if (record != null && record.getServiceUuids() != null) {
            for (android.os.ParcelUuid uuid : record.getServiceUuids()) {
                if (RustmixBleUuids.SERVICE.equals(uuid.getUuid())) {
                    hasRustmixService = true;
                    break;
                }
            }
        }

        boolean hasRustmixName =
                containsRustmix(scanName) || containsRustmix(deviceName);

        boolean hasSavedAddress =
                getSavedDeviceAddress().equalsIgnoreCase(address);

        android.util.Log.i(TAG,
                "scan name=" + name
                        + " scanName=" + scanName
                        + " deviceName=" + deviceName
                        + " address=" + address
                        + " service=" + hasRustmixService
                        + " rustmixName=" + hasRustmixName
                        + " savedAddress=" + hasSavedAddress);

        if (!hasRustmixService && !hasRustmixName && !hasSavedAddress) {
            return;
        }

        saveDeviceAddress(address);
        listener.onDeviceFound(name, address);
        listener.onStatus("Found Rustmix Remote; connecting...");
        connectToAddress(address);
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt bluetoothGatt, int status, int newState) {
            android.util.Log.i(TAG, "connection state status=" + status + " newState=" + newState);

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt = bluetoothGatt;
                listener.onStatus("Connected; discovering services...");
                try {
                    bluetoothGatt.discoverServices();
                } catch (SecurityException e) {
                    android.util.Log.e(TAG, "discoverServices permission denied", e);
                    listener.onStatus("Service discovery permission denied");
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                commandCharacteristic = null;
                closeGattQuietly();
                listener.onStatus("Disconnected");
                listener.onDisconnected();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt bluetoothGatt, int status) {
            android.util.Log.i(TAG, "services discovered status=" + status);

            if (status != BluetoothGatt.GATT_SUCCESS) {
                listener.onStatus("Service discovery failed: " + status);
                return;
            }

            BluetoothGattService service = bluetoothGatt.getService(RustmixBleUuids.SERVICE);
            if (service == null) {
                listener.onStatus("Rustmix service not found");
                android.util.Log.w(TAG, "Rustmix service not found");
                return;
            }

            BluetoothGattCharacteristic characteristic =
                    service.getCharacteristic(RustmixBleUuids.COMMAND);

            if (characteristic == null) {
                listener.onStatus("Rustmix command characteristic not found");
                android.util.Log.w(TAG, "Rustmix command characteristic not found");
                return;
            }

            commandCharacteristic = characteristic;
            String address = bluetoothGatt.getDevice() != null ? bluetoothGatt.getDevice().getAddress() : getSavedDeviceAddress();
            saveDeviceAddress(address);
            listener.onConnected("Rustmix Remote", address);
            listener.onStatus("Connected");
        }

        @Override
        public void onCharacteristicWrite(
                BluetoothGatt bluetoothGatt,
                BluetoothGattCharacteristic characteristic,
                int status
        ) {
            android.util.Log.i(TAG, "write callback status=" + status);
        }
    };

    private void stopScanQuietly() {
        if (!scanning) {
            return;
        }
        scanning = false;

        if (scanner == null) {
            return;
        }

        try {
            scanner.stopScan(scanCallback);
        } catch (SecurityException ignored) {
        } catch (Exception ignored) {
        }
    }

    private void closeGattQuietly() {
        if (gatt != null) {
            try {
                gatt.close();
            } catch (SecurityException ignored) {
            } catch (Exception ignored) {
            }
        }
        gatt = null;
        commandCharacteristic = null;
    }

    private static String normalizeAddress(String address) {
        if (address == null) {
            return "";
        }
        return address.trim().toUpperCase(Locale.US);
    }

    private static boolean containsRustmix(String value) {
        return value != null && value.toLowerCase(Locale.US).contains("rustmix");
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(String.format(Locale.US, "%02X", b));
        }
        return sb.toString();
    }
}
