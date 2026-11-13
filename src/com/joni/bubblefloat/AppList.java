package com.joni.bubblefloat;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AppList {
    public static List<AppInfo> load(Context ctx) {
        PackageManager pm = ctx.getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> ris = pm.queryIntentActivities(main, 0);
        List<AppInfo> out = new ArrayList<>();
        String self = ctx.getPackageName();
        for (ResolveInfo ri : ris) {
            ActivityInfo ai = ri.activityInfo;
            if (ai == null || ai.packageName == null) {
                continue;
            }
            if (ai.packageName.equals(self)) {
                continue;
            }
            String label = ri.loadLabel(pm).toString();
            ComponentName cn = new ComponentName(ai.packageName, ai.name);
            out.add(new AppInfo(ai.packageName, cn, label, ri.loadIcon(pm), ai.launchMode));
        }
        Collections.sort(out, new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo a, AppInfo b) {
                return a.label.compareToIgnoreCase(b.label);
            }
        });
        return out;
    }

    public static AppInfo findByKey(List<AppInfo> apps, String key) {
        for (AppInfo a : apps) {
            if (a.key().equals(key)) {
                return a;
            }
        }
        return null;
    }
}
