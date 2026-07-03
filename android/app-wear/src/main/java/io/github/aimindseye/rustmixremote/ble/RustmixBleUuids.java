package io.github.aimindseye.rustmixremote.ble;

import java.util.UUID;

public final class RustmixBleUuids {
    public static final UUID SERVICE = UUID.fromString("8f7a0000-6b8f-4a91-9e2c-8d2f5c000001");
    public static final UUID COMMAND = UUID.fromString("8f7a0001-6b8f-4a91-9e2c-8d2f5c000001");
    public static final UUID STATUS = UUID.fromString("8f7a0002-6b8f-4a91-9e2c-8d2f5c000001");
    public static final UUID CAPABILITIES = UUID.fromString("8f7a0003-6b8f-4a91-9e2c-8d2f5c000001");

    private RustmixBleUuids() {
    }
}
