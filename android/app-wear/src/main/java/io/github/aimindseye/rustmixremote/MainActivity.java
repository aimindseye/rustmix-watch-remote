package io.github.aimindseye.rustmixremote;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import io.github.aimindseye.rustmixremote.ble.BleConnectionManager;

public class MainActivity extends Activity {
    private BleConnectionManager ble;

    private ViewFlipper flipper;

    private TextView remoteStatusText;
    private TextView deviceStatusText;
    private TextView savedDeviceText;

    private EditText addressEdit;

    private Button prevButton;
    private Button nextButton;
    private Button disconnectButton;
    private Button connectSavedButton;
    private Button scanButton;

    private float touchDownX;
    private float touchDownY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ble = new BleConnectionManager(this, new BleConnectionManager.Listener() {
            @Override
            public void onStatus(String status) {
                runOnUiThread(() -> {
                    remoteStatusText.setText(status);
                    deviceStatusText.setText(status);
                });
            }

            @Override
            public void onDeviceFound(String name, String address) {
                runOnUiThread(() -> {
                    savedDeviceText.setText("Saved\n" + address);
                    addressEdit.setText(address);
                });
            }

            @Override
            public void onConnected(String name, String address) {
                runOnUiThread(() -> {
                    savedDeviceText.setText("Connected\n" + address);
                    addressEdit.setText(address);
                    remoteStatusText.setText("● Connected");
                    deviceStatusText.setText("Connected");
                    setConnectedUi(true);
                    showRemoteScreen();
                });
            }

            @Override
            public void onDisconnected() {
                runOnUiThread(() -> {
                    remoteStatusText.setText("○ Disconnected");
                    deviceStatusText.setText("Disconnected");
                    setConnectedUi(false);
                });
            }

            @Override
            public void onWriteSent(String label) {
                runOnUiThread(() -> {
                    String text = "Write sent: " + label;
                    remoteStatusText.setText(text);
                    deviceStatusText.setText(text);
                });
            }
        });

        flipper = new ViewFlipper(this);
        flipper.addView(createRemoteScreen());
        flipper.addView(createDeviceScreen());

        setContentView(flipper);
        setConnectedUi(false);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchDownX = event.getX();
                touchDownY = event.getY();
                break;

            case MotionEvent.ACTION_UP:
                float dx = event.getX() - touchDownX;
                float dy = event.getY() - touchDownY;

                boolean horizontalSwipe =
                        Math.abs(dx) > dp(45) && Math.abs(dx) > Math.abs(dy) * 1.25f;

                if (horizontalSwipe) {
                    if (dx < 0) {
                        showDeviceScreen();
                    } else {
                        showRemoteScreen();
                    }
                    return true;
                }
                break;

            default:
                break;
        }

        return super.dispatchTouchEvent(event);
    }

    @Override
    protected void onDestroy() {
        ble.disconnect();
        super.onDestroy();
    }

    private LinearLayout createRemoteScreen() {
        LinearLayout root = baseScreen();

        TextView title = title("Rustmix Remote");
        root.addView(title, fullWidth());

        remoteStatusText = smallText("○ Disconnected");
        root.addView(remoteStatusText, fullWidth());

        LinearLayout arrows = new LinearLayout(this);
        arrows.setOrientation(LinearLayout.HORIZONTAL);
        arrows.setGravity(Gravity.CENTER);

        prevButton = new Button(this);
        prevButton.setText("◀\nPrev");
        prevButton.setTextSize(16);
        prevButton.setOnClickListener(v -> ble.sendPrevious());
        arrows.addView(prevButton, halfWidthTall());

        nextButton = new Button(this);
        nextButton.setText("▶\nNext");
        nextButton.setTextSize(16);
        nextButton.setOnClickListener(v -> ble.sendNext());
        arrows.addView(nextButton, halfWidthTall());

        root.addView(arrows, fullWidth());

        disconnectButton = new Button(this);
        disconnectButton.setText("Disconnect");
        disconnectButton.setTextSize(12);
        disconnectButton.setOnClickListener(v -> ble.disconnect());
        root.addView(disconnectButton, fullWidth());

        Button deviceButton = new Button(this);
        deviceButton.setText("Device →");
        deviceButton.setTextSize(12);
        deviceButton.setOnClickListener(v -> showDeviceScreen());
        root.addView(deviceButton, fullWidth());

        TextView hint = smallText("Swipe left for Device");
        root.addView(hint, fullWidth());

        return root;
    }

    private ScrollView createDeviceScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);

        LinearLayout root = baseScreen();

        TextView title = title("Device");
        root.addView(title, fullWidth());

        deviceStatusText = smallText("Disconnected");
        root.addView(deviceStatusText, fullWidth());

        savedDeviceText = smallText("Saved\n" + ble.getSavedDeviceAddress());
        root.addView(savedDeviceText, fullWidth());

        addressEdit = new EditText(this);
        addressEdit.setSingleLine(true);
        addressEdit.setText(ble.getSavedDeviceAddress());
        addressEdit.setTextSize(12);
        addressEdit.setHint("BLE MAC");
        root.addView(addressEdit, fullWidth());

        Button saveButton = new Button(this);
        saveButton.setText("Save Address");
        saveButton.setTextSize(12);
        saveButton.setOnClickListener(v -> {
            ble.saveDeviceAddress(addressEdit.getText().toString());
            savedDeviceText.setText("Saved\n" + ble.getSavedDeviceAddress());
        });
        root.addView(saveButton, fullWidth());

        connectSavedButton = new Button(this);
        connectSavedButton.setText("Connect Saved");
        connectSavedButton.setTextSize(12);
        connectSavedButton.setOnClickListener(v -> {
            ble.saveDeviceAddress(addressEdit.getText().toString());
            savedDeviceText.setText("Saved\n" + ble.getSavedDeviceAddress());
            ble.connectToAddress(ble.getSavedDeviceAddress());
        });
        root.addView(connectSavedButton, fullWidth());

        scanButton = new Button(this);
        scanButton.setText("Scan / Fallback");
        scanButton.setTextSize(12);
        scanButton.setOnClickListener(v -> {
            ble.saveDeviceAddress(addressEdit.getText().toString());
            savedDeviceText.setText("Saved\n" + ble.getSavedDeviceAddress());
            ble.startScanOrConnect(ble.getSavedDeviceAddress());
        });
        root.addView(scanButton, fullWidth());

        Button remoteButton = new Button(this);
        remoteButton.setText("← Remote");
        remoteButton.setTextSize(12);
        remoteButton.setOnClickListener(v -> showRemoteScreen());
        root.addView(remoteButton, fullWidth());

        TextView hint = smallText("Swipe right for Remote");
        root.addView(hint, fullWidth());

        scroll.addView(root);
        return scroll;
    }

    private void showRemoteScreen() {
        flipper.setDisplayedChild(0);
    }

    private void showDeviceScreen() {
        flipper.setDisplayedChild(1);
    }

    private void setConnectedUi(boolean connected) {
        if (prevButton != null) {
            prevButton.setEnabled(connected);
        }
        if (nextButton != null) {
            nextButton.setEnabled(connected);
        }
        if (disconnectButton != null) {
            disconnectButton.setEnabled(connected);
        }
        if (connectSavedButton != null) {
            connectSavedButton.setEnabled(!connected);
        }
        if (scanButton != null) {
            scanButton.setEnabled(!connected);
        }
    }

    private LinearLayout baseScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = dp(8);
        root.setPadding(pad, pad, pad, pad);
        return root;
    }

    private TextView title(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(17);
        v.setGravity(Gravity.CENTER);
        return v;
    }

    private TextView smallText(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(11);
        v.setGravity(Gravity.CENTER);
        return v;
    }

    private LinearLayout.LayoutParams fullWidth() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        p.setMargins(0, dp(2), 0, dp(2));
        return p;
    }

    private LinearLayout.LayoutParams halfWidthTall() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                0,
                dp(92),
                1.0f
        );
        p.setMargins(dp(3), dp(4), dp(3), dp(4));
        return p;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
