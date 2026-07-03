package io.github.aimindseye.rustmixremote.settings;

import android.content.Context;
import android.content.SharedPreferences;

public final class AppSettingsStore {
    private static final String PREFS = "rustmix_remote_settings";
    private static final String KEY_LAST_PROFILE = "last_profile";

    private final SharedPreferences prefs;

    public AppSettingsStore(Context context) {
        this.prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public String lastProfile() {
        return prefs.getString(KEY_LAST_PROFILE, "Rustmix-Wave Reader");
    }

    public void setLastProfile(String profile) {
        prefs.edit().putString(KEY_LAST_PROFILE, profile).apply();
    }
}
