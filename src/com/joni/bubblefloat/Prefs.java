package com.joni.bubblefloat;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Prefs {
    private static final String NAME = "bubble_float";
    private static final String PINS = "pins";
    private static final String RECENTS = "recents";
    private static final String SEEN = "onboarding_seen";
    private static final String FLOAT_BTN = "float_button";

    private final SharedPreferences sp;

    public Prefs(Context ctx) {
        sp = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public boolean onboardingSeen() {
        return sp.getBoolean(SEEN, false);
    }

    public void setOnboardingSeen() {
        sp.edit().putBoolean(SEEN, true).apply();
    }

    public Set<String> pins() {
        return new LinkedHashSet<>(sp.getStringSet(PINS, new HashSet<String>()));
    }

    public boolean isPinned(String key) {
        return pins().contains(key);
    }

    public void togglePin(String key) {
        Set<String> s = pins();
        if (s.contains(key)) {
            s.remove(key);
        } else {
            s.add(key);
        }
        sp.edit().putStringSet(PINS, s).apply();
    }

    public List<String> recents() {
        String raw = sp.getString(RECENTS, "");
        List<String> out = new ArrayList<>();
        if (!raw.isEmpty()) {
            for (String p : raw.split("\n")) {
                if (!p.isEmpty()) {
                    out.add(p);
                }
            }
        }
        return out;
    }

    public void addRecent(String key) {
        List<String> r = recents();
        r.remove(key);
        r.add(0, key);
        while (r.size() > 8) {
            r.remove(r.size() - 1);
        }
        StringBuilder sb = new StringBuilder();
        for (String s : r) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(s);
        }
        sp.edit().putString(RECENTS, sb.toString()).apply();
    }

    public boolean floatButton() {
        return sp.getBoolean(FLOAT_BTN, false);
    }

    public void setFloatButton(boolean on) {
        sp.edit().putBoolean(FLOAT_BTN, on).apply();
    }
}
