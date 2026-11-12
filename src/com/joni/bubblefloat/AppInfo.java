package com.joni.bubblefloat;

import android.content.ComponentName;
import android.graphics.drawable.Drawable;

public class AppInfo {
    public final String packageName;
    public final ComponentName component;
    public final String label;
    public final Drawable icon;
    public final int launchMode;

    public AppInfo(String packageName, ComponentName component, String label,
                   Drawable icon, int launchMode) {
        this.packageName = packageName;
        this.component = component;
        this.label = label;
        this.icon = icon;
        this.launchMode = launchMode;
    }

    public String key() {
        return component.flattenToShortString();
    }
}
