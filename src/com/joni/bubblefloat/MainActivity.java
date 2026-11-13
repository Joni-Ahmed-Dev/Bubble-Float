package com.joni.bubblefloat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final int COLOR_BG = 0xFFF6F7F9;
    private static final int COLOR_CARD = 0xFFFFFFFF;
    private static final int COLOR_ACCENT = 0xFF6366F1;
    private static final int COLOR_ACCENT2 = 0xFF8B5CF6;
    private static final int COLOR_TEXT = 0xFF111111;
    private static final int COLOR_SUB = 0xFF5F6368;
    private static final int COLOR_GOLD = 0xFFE8A100;
    private static final int COLOR_GREEN = 0xFF1E8E3E;
    private static final int COLOR_AMBER = 0xFFB06000;

    private Prefs prefs;
    private List<AppInfo> allApps = new ArrayList<>();
    private LinearLayout drawerView;
    private LinearLayout onboardingView;
    private LinearLayout sections;
    private EditText searchBox;
    private TextView statusText;
    private String filter = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new Prefs(this);
        allApps = AppList.load(this);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(COLOR_BG);

        onboardingView = buildOnboarding();
        drawerView = buildDrawer();
        root.addView(onboardingView);
        root.addView(drawerView);
        setContentView(root);
        showOnboarding(!prefs.onboardingSeen());
    }

    @Override
    protected void onResume() {
        super.onResume();
        allApps = AppList.load(this);
        rebuildSections();
        updateStatus();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == BubbleLauncher.REQUEST_NOTIFICATIONS) {
            boolean granted = grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            BubbleLauncher.onPermissionResult(this, granted);
            updateStatus();
        }
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setCornerRadius(dp(radiusDp));
        d.setColor(color);
        return d;
    }

    private GradientDrawable oval(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        return d;
    }

    // Every app icon in this app is drawn as a circle, like the
    // screenshots: white round disc, app artwork clipped to the circle,
    // transparent corners. No Bubble Float logo is added to any app icon.
    private Drawable roundIcon(Drawable d, int sizePx) {
        if (d == null) {
            return null;
        }
        Bitmap bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Paint white = new Paint(Paint.ANTI_ALIAS_FLAG);
        white.setColor(Color.WHITE);
        c.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, white);
        Path circle = new Path();
        circle.addCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, Path.Direction.CW);
        c.save();
        c.clipPath(circle);
        if (d instanceof AdaptiveIconDrawable) {
            AdaptiveIconDrawable ad = (AdaptiveIconDrawable) d;
            if (ad.getBackground() != null) {
                ad.getBackground().setBounds(0, 0, sizePx, sizePx);
                ad.getBackground().draw(c);
            }
            if (ad.getForeground() != null) {
                ad.getForeground().setBounds(0, 0, sizePx, sizePx);
                ad.getForeground().draw(c);
            }
        } else {
            int inset = sizePx * 8 / 100;
            d.setBounds(inset, inset, sizePx - inset, sizePx - inset);
            d.draw(c);
        }
        c.restore();
        return new BitmapDrawable(getResources(), bmp);
    }

    private void showOnboarding(boolean show) {
        onboardingView.setVisibility(show ? View.VISIBLE : View.GONE);
        drawerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    // ---------- Onboarding ----------

    private LinearLayout buildOnboarding() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BG);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(28), dp(48), dp(28), dp(28));
        root.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        ImageView logo = new ImageView(this);
        logo.setImageDrawable(roundIcon(getDrawable(R.drawable.ic_launcher), dp(96)));
        logo.setElevation(dp(4));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(96), dp(96));
        lp.bottomMargin = dp(28);
        root.addView(logo, lp);

        TextView title = new TextView(this);
        title.setText("Every app, one tap away");
        title.setTextColor(COLOR_TEXT);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        title.setTypeface(Typeface.DEFAULT_BOLD, Typeface.BOLD_ITALIC);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, dp(12));
        root.addView(title);

        TextView desc = new TextView(this);
        desc.setText("Tap an app and it opens in a floating window over whatever you were doing. "
                + "Two apps on screen at once, and switching between them is one tap.");
        desc.setTextColor(COLOR_SUB);
        desc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        desc.setGravity(Gravity.CENTER);
        desc.setLineSpacing(0, 1.25f);
        desc.setPadding(0, 0, 0, dp(28));
        root.addView(desc);

        root.addView(featureRow("\u2912", "Tap an app to float it",
                "It joins the row of heads at the top of the screen. Tap a head to switch, drag it away to close."));
        root.addView(featureRow("\u2605", "Pin the ones you use most",
                "Long-press any app to keep it at the front of the drawer."));
        root.addView(featureRow("\u25A3", "Android draws the window",
                "The floating frame, the row of heads and the close button belong to the system, "
                        + "not to Bubble Float, which is why it works without any special permission."));

        Button cta = new Button(this);
        cta.setText("\u25CF  Show my apps");
        cta.setTextColor(Color.WHITE);
        cta.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        cta.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp(28));
        bg.setColors(new int[]{0xFF5B8DEF, COLOR_ACCENT2});
        bg.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        bg.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
        cta.setBackground(bg);
        LinearLayout.LayoutParams ctaLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(56));
        ctaLp.topMargin = dp(32);
        cta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prefs.setOnboardingSeen();
                showOnboarding(false);
                if (Build.VERSION.SDK_INT >= 33
                        && checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                                != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},
                            BubbleLauncher.REQUEST_NOTIFICATIONS);
                }
            }
        });
        root.addView(cta, ctaLp);

        TextView foot = new TextView(this);
        foot.setText("Bubble Float will ask to post notifications the first time you float an app. "
                + "That is how Android is asked to open one.");
        foot.setTextColor(COLOR_SUB);
        foot.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        foot.setGravity(Gravity.CENTER);
        foot.setPadding(0, dp(16), 0, 0);
        root.addView(foot);
        return root;
    }

    private LinearLayout featureRow(String glyph, String title, String body) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 0, 0, dp(20));

        TextView icon = new TextView(this);
        icon.setText(glyph);
        icon.setTextColor(0xFF5B8DEF);
        icon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        icon.setBackground(oval(0xFFE8F0FE));
        icon.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(48), dp(48));
        ip.rightMargin = dp(16);
        row.addView(icon, ip);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(COLOR_TEXT);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        texts.addView(t);
        TextView b = new TextView(this);
        b.setText(body);
        b.setTextColor(COLOR_SUB);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        b.setLineSpacing(0, 1.2f);
        texts.addView(b);
        row.addView(texts, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return row;
    }

    // ---------- Drawer ----------

    private LinearLayout buildDrawer() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BG);
        root.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(16), dp(16), dp(8));

        ImageView logo = new ImageView(this);
        logo.setImageDrawable(roundIcon(getDrawable(R.drawable.ic_launcher), dp(40)));
        logo.setElevation(dp(2));
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(dp(40), dp(40));
        llp.rightMargin = dp(10);
        header.addView(logo, llp);

        TextView title = new TextView(this);
        title.setText("Bubble Float");
        title.setTextColor(COLOR_TEXT);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        header.addView(title, tlp);

        Button menu = new Button(this);
        menu.setText("\u22EE");
        menu.setTextColor(COLOR_TEXT);
        menu.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        menu.setBackgroundColor(Color.TRANSPARENT);
        menu.setMinWidth(0);
        menu.setMinHeight(0);
        menu.setPadding(dp(12), dp(4), dp(12), dp(4));
        menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMenu(v);
            }
        });
        header.addView(menu);
        root.addView(header);

        statusText = new TextView(this);
        statusText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        statusText.setPadding(dp(18), 0, dp(18), dp(8));
        root.addView(statusText);

        searchBox = new EditText(this);
        searchBox.setHint("Search apps");
        searchBox.setHintTextColor(COLOR_SUB);
        searchBox.setTextColor(COLOR_TEXT);
        searchBox.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        searchBox.setBackground(rounded(COLOR_CARD, 28));
        searchBox.setElevation(dp(2));
        searchBox.setPadding(dp(20), dp(12), dp(20), dp(12));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.setMargins(dp(16), 0, dp(16), dp(12));
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                filter = s.toString();
                rebuildSections();
            }
        });
        root.addView(searchBox, slp);

        ScrollView scroll = new ScrollView(this);
        sections = new LinearLayout(this);
        sections.setOrientation(LinearLayout.VERTICAL);
        sections.setPadding(dp(16), 0, dp(16), dp(24));
        scroll.addView(sections, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return root;
    }

    private void updateStatus() {
        if (statusText == null) {
            return;
        }
        String miss = BubbleLauncher.missing(this);
        if (miss == null) {
            statusText.setText("Bubbles ready. Tap any app to float it.");
            statusText.setTextColor(COLOR_GREEN);
        } else {
            statusText.setText(miss + "  (menu > Bubble setup)");
            statusText.setTextColor(COLOR_AMBER);
        }
    }

    private void showMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "Bubble setup");
        popup.getMenu().add(0, 2, 0, "Floating button: " + (prefs.floatButton() ? "on" : "off"));
        popup.getMenu().add(0, 3, 0, "About");
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == 1) {
                    BubbleLauncher.setUp(MainActivity.this);
                    Toast.makeText(MainActivity.this,
                            "Follow the settings, then come back here", Toast.LENGTH_LONG).show();
                } else if (item.getItemId() == 2) {
                    toggleFloatButton();
                } else if (item.getItemId() == 3) {
                    showAbout();
                }
                return true;
            }
        });
        popup.show();
    }

    private void toggleFloatButton() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Allow display over other apps first", Toast.LENGTH_LONG).show();
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())));
            } catch (Exception ignored) {
            }
            return;
        }
        Intent i = new Intent(this, FloatButtonService.class);
        if (prefs.floatButton()) {
            stopService(i);
            prefs.setFloatButton(false);
            Toast.makeText(this, "Floating button off", Toast.LENGTH_SHORT).show();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(i);
            } else {
                startService(i);
            }
            prefs.setFloatButton(true);
            Toast.makeText(this, "Floating button on. Tap it from any screen to open your apps.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void showAbout() {
        new AlertDialog.Builder(this)
                .setTitle("Bubble Float 1.1")
                .setMessage("Open any app in a floating bubble over whatever you are doing. "
                        + "Two apps on screen at once, switching is one tap.\n\n"
                        + "Android itself draws the floating frame, the row of heads and the "
                        + "close button. This app only asks Android to open the app as a bubble.\n\n"
                        + "Some apps refuse to bubble and open full screen instead. "
                        + "On MIUI the system bubble behaviour can differ from stock Android.\n\n"
                        + "Bubble technique follows Launchii (GPL-3.0). This app is GPL-3.0.")
                .setPositiveButton("OK", null)
                .show();
    }

    // ---------- App list ----------

    private void rebuildSections() {
        if (sections == null) {
            return;
        }
        sections.removeAllViews();
        String q = filter.trim().toLowerCase();

        List<AppInfo> pinned = new ArrayList<>();
        List<AppInfo> recent = new ArrayList<>();
        List<AppInfo> rest = new ArrayList<>();
        for (AppInfo a : allApps) {
            if (!q.isEmpty() && !a.label.toLowerCase().contains(q)) {
                continue;
            }
            if (prefs.isPinned(a.key())) {
                pinned.add(a);
            } else {
                rest.add(a);
            }
        }
        for (String k : prefs.recents()) {
            AppInfo a = AppList.findByKey(allApps, k);
            if (a == null || prefs.isPinned(k)) {
                continue;
            }
            if (!q.isEmpty() && !a.label.toLowerCase().contains(q)) {
                continue;
            }
            recent.add(a);
        }

        if (!pinned.isEmpty()) {
            sections.addView(sectionTitle("Pinned"));
            sections.addView(chipRow(pinned, true));
        }
        if (!recent.isEmpty()) {
            sections.addView(sectionTitle("Recent"));
            sections.addView(chipRow(recent, false));
        }
        sections.addView(sectionTitle(q.isEmpty() ? "All apps" : "Results"));
        if (rest.isEmpty() && pinned.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No apps match that search.");
            empty.setTextColor(COLOR_SUB);
            empty.setPadding(0, dp(8), 0, 0);
            sections.addView(empty);
        }
        for (AppInfo a : rest) {
            sections.addView(appRow(a));
        }
    }

    private TextView sectionTitle(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(COLOR_SUB);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, dp(12), 0, dp(8));
        return t;
    }

    private LinearLayout chipRow(List<AppInfo> apps, final boolean pinnedRow) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (final AppInfo a : apps) {
            LinearLayout chip = new LinearLayout(this);
            chip.setOrientation(LinearLayout.VERTICAL);
            chip.setGravity(Gravity.CENTER_HORIZONTAL);
            chip.setPadding(dp(4), dp(4), dp(4), dp(4));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                    dp(76), ViewGroup.LayoutParams.WRAP_CONTENT);
            cp.rightMargin = dp(8);

            ImageView icon = new ImageView(this);
            icon.setImageDrawable(roundIcon(a.icon, dp(56)));
            icon.setElevation(dp(2));
            chip.addView(icon, new LinearLayout.LayoutParams(dp(56), dp(56)));

            TextView label = new TextView(this);
            label.setText(a.label);
            label.setTextColor(COLOR_TEXT);
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            label.setGravity(Gravity.CENTER);
            label.setMaxLines(1);
            label.setPadding(0, dp(6), 0, 0);
            chip.addView(label);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openApp(a);
                }
            });
            chip.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (pinnedRow) {
                        prefs.togglePin(a.key());
                        rebuildSections();
                        Toast.makeText(MainActivity.this, a.label + " unpinned",
                                Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    return false;
                }
            });
            row.addView(chip, cp);
        }
        return row;
    }

    private LinearLayout appRow(final AppInfo a) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(rounded(COLOR_CARD, 28));
        row.setElevation(dp(2));
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rp.bottomMargin = dp(8);
        row.setLayoutParams(rp);

        ImageView icon = new ImageView(this);
        icon.setImageDrawable(roundIcon(a.icon, dp(48)));
        icon.setElevation(dp(2));
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(48), dp(48));
        ip.rightMargin = dp(12);
        row.addView(icon, ip);

        TextView label = new TextView(this);
        label.setText(a.label);
        label.setTextColor(COLOR_TEXT);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(label, lp);

        final TextView star = new TextView(this);
        star.setText(prefs.isPinned(a.key()) ? "\u2605" : "\u2606");
        star.setTextColor(prefs.isPinned(a.key()) ? COLOR_GOLD : COLOR_SUB);
        star.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        star.setPadding(dp(8), dp(4), dp(8), dp(4));
        star.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prefs.togglePin(a.key());
                rebuildSections();
            }
        });
        row.addView(star);

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openApp(a);
            }
        });
        row.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                prefs.togglePin(a.key());
                rebuildSections();
                Toast.makeText(MainActivity.this,
                        prefs.isPinned(a.key()) ? a.label + " pinned" : a.label + " unpinned",
                        Toast.LENGTH_SHORT).show();
                return true;
            }
        });
        return row;
    }

    // ---------- Launch ----------

    private void openApp(final AppInfo app) {
        if (BubbleLauncher.inSingleTaskBubble(this, app)) {
            BubbleLauncher.show(this, app, true, new BubbleLauncher.Callback() {
                @Override
                public void onShown() {
                    prefs.addRecent(app.key());
                    rebuildSections();
                }

                @Override
                public void onFallback() {
                    launchNormally(app);
                }
            });
            return;
        }
        if (!BubbleLauncher.ready(this)) {
            Toast.makeText(this, "Bubbles need setup first", Toast.LENGTH_LONG).show();
            BubbleLauncher.setUp(this);
            updateStatus();
            return;
        }
        BubbleLauncher.show(this, app, true, new BubbleLauncher.Callback() {
            @Override
            public void onShown() {
                prefs.addRecent(app.key());
                rebuildSections();
            }

            @Override
            public void onFallback() {
                Toast.makeText(MainActivity.this,
                        app.label + " cannot open in a bubble, opening normally",
                        Toast.LENGTH_LONG).show();
                launchNormally(app);
            }
        });
    }

    private void launchNormally(AppInfo app) {
        try {
            Intent launch = new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setComponent(app.component);
            startActivity(launch);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open " + app.label, Toast.LENGTH_SHORT).show();
        }
    }
}
