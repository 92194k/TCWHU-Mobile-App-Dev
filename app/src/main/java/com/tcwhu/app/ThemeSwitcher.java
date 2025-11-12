package com.tcwhu.app;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class ThemeSwitcher {

    private static final String PREFS_NAME = "ThemePrefs";
    private static final String KEY_THEME = "theme_mode";

    // Theme constants
    public static final int THEME_LIGHT = AppCompatDelegate.MODE_NIGHT_NO;
    public static final int THEME_DARK = AppCompatDelegate.MODE_NIGHT_YES;
    public static final int THEME_SYSTEM = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;

    // Call this from TCWHUApplication.java to set the theme on startup
    public static void applyTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int themeMode = prefs.getInt(KEY_THEME, THEME_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(themeMode);
    }

    // Call this from StudentProfileFragment to save the user's choice
    public static void setTheme(Context context, int themeMode) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putInt(KEY_THEME, themeMode);
        editor.apply();

        AppCompatDelegate.setDefaultNightMode(themeMode);
    }

    // Helper to get the currently selected theme index for the dialog
    public static int getTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_THEME, THEME_SYSTEM);
    }

    public static int getThemeIndex(Context context) {
        int themeMode = getTheme(context);
        if (themeMode == THEME_LIGHT) return 0;
        if (themeMode == THEME_DARK) return 1;
        return 2; // Default to System
    }
}