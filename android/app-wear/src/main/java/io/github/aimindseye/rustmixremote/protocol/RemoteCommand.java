package io.github.aimindseye.rustmixremote.protocol;

public enum RemoteCommand {
    PAGE_NEXT(0x01),
    PAGE_PREVIOUS(0x02),
    SELECT(0x03),
    BACK(0x04),
    MENU(0x05),
    SLEEP(0x06),
    WAKE(0x07),
    SCROLL_UP(0x08),
    SCROLL_DOWN(0x09),
    NEXT_CHAPTER(0x0A),
    PREVIOUS_CHAPTER(0x0B),
    TOGGLE_BOOKMARK(0x0C),
    REFRESH(0x0D);

    private final int code;

    RemoteCommand(int code) {
        this.code = code;
    }

    public byte code() {
        return (byte) code;
    }
}
