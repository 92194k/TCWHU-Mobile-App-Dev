package com.tcwhu.app;

import android.app.Application;
import com.cloudinary.android.MediaManager;
import java.util.HashMap;
import java.util.Map;

public class TCWHUApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // --- CONFIGURE CLOUDINARY ---
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dggeonpfw");
        config.put("api_key", "147481881754886");
        config.put("api_secret", "583Dz7vp2y6TRaDBuCj8HbHoQX4");
        MediaManager.init(this, config);

        // --- CRITICAL FIX: APPLY THE SAVED THEME ON APP STARTUP ---
        ThemeSwitcher.applyTheme(this);
    }
}