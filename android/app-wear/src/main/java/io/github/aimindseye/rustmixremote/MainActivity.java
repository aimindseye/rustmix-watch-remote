package io.github.aimindseye.rustmixremote;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.github.aimindseye.rustmixremote.ble.BleConnectionManager;
import io.github.aimindseye.rustmixremote.haptics.HapticFeedbackController;
import io.github.aimindseye.rustmixremote.protocol.RemoteCommand;

public final class MainActivity extends Activity implements BleConnectionManager.Listener {
    private static final int REQUEST_BLUETOOTH = 1001;

    private TextView statusView;
    private TextView deviceView;
    private BleConnectionManager ble;
    private HapticFeedbackController haptics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ble = new BleConnectionManager(this, this);
        haptics = new HapticFeedbackController(this);
        setContentView(buildUi());
        requestBluetoothPermissionsIfNeeded();
    }

    @Override
    protected void onDestroy() {
        ble.disconnect();
        super.onDestroy();
    }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(18, 12, 18, 12);
        root.setBackgroundColor(Color.BLACK);
        root.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        TextView title = new TextView(this);
        title.setText("Rustmix Remote");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        statusView = new TextView(this);
        statusView.setText("Disconnected");
        statusView.setTextColor(Color.LTGRAY);
        statusView.setTextSize(12);
        statusView.setGravity(Gravity.CENTER);
        root.addView(statusView);

        deviceView = new TextView(this);
        deviceView.setText("No device");
        deviceView.setTextColor(Color.GRAY);
        deviceView.setTextSize(10);
        deviceView.setGravity(Gravity.CENTER);
        root.addView(deviceView);

        Button connect = makeButton("Scan / Connect");
        connect.setOnClickListener(v -> ble.scanAndConnect());
        root.addView(connect);

        Button next = makeButton("Next Page →");
        next.setOnClickListener(v -> send(RemoteCommand.PAGE_NEXT));
        root.addView(next);

        Button previous = makeButton("← Previous");
        previous.setOnClickListener(v -> send(RemoteCommand.PAGE_PREVIOUS));
        root.addView(previous);

        Button menu = makeButton("Menu");
        menu.setOnClickListener(v -> send(RemoteCommand.MENU));
        root.addView(menu);

        Button back = makeButton("Back");
        back.setOnClickListener(v -> send(RemoteCommand.BACK));
        root.addView(back);

        return root;
    }

    private Button makeButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(13);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 4, 0, 4);
        button.setLayoutParams(params);
        return button;
    }

    private void send(RemoteCommand command) {
        if (ble.send(command)) {
            haptics.tick();
        }
    }

    private void requestBluetoothPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            boolean needsScan = checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED;
            boolean needsConnect = checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED;
            if (needsScan || needsConnect) {
                requestPermissions(new String[] {
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT
                }, REQUEST_BLUETOOTH);
            }
        }
    }

    @Override
    public void onStatus(String status) {
        runOnUiThread(() -> statusView.setText(status));
    }

    @Override
    public void onDeviceFound(String name, String address) {
        runOnUiThread(() -> deviceView.setText(name + "\n" + address));
    }

    @Override
    public void onConnected() {
        runOnUiThread(() -> statusView.setText("Connected"));
        haptics.tick();
    }

    @Override
    public void onDisconnected() {
        runOnUiThread(() -> statusView.setText("Disconnected"));
    }
}
