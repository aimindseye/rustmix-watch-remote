package io.github.aimindseye.rustmixremote.profiles;

public enum DeviceProfile {
    RUSTMIX_WAVE_READER("Rustmix-Wave Reader"),
    RUSTMIX_X4_READER("Rustmix-X4 Reader");

    private final String label;

    DeviceProfile(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
