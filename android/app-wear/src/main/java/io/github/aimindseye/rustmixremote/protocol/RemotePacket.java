package io.github.aimindseye.rustmixremote.protocol;

public final class RemotePacket {
    public static final byte PROTOCOL_VERSION = 0x01;

    private RemotePacket() {
    }

    public static byte[] command(byte sequence, RemoteCommand command) {
        return command(sequence, command, (byte) 0x00, (byte) 0x00);
    }

    public static byte[] command(byte sequence, RemoteCommand command, byte flags, byte parameter) {
        return new byte[] {
                PROTOCOL_VERSION,
                sequence,
                command.code(),
                flags,
                parameter,
                0x00
        };
    }
}
