package com.joni.bubblefloat;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Person;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.util.Log;

/**
 * Opens any installed app in an Android system bubble.
 *
 * Technique: a long-lived shortcut per app whose intent is the app's main
 * activity, plus a MessagingStyle notification on that shortcut carrying
 * BubbleMetadata that expands at once and hides its own notification.
 * The system only checks that the bubbled activity is resizable, not
 * whose it is. This follows the approach documented in Launchii
 * (https://github.com/deciocrytek/Launchii, GPL-3.0), reimplemented
 * here in Java for this app.
 */
public class BubbleLauncher {
    private static final String TAG = "BubbleLauncher";
    private static final String CHANNEL = "app_bubbles";
    private static final int ID = 1;
    public static final int REQUEST_NOTIFICATIONS = 4203;

    public interface Callback {
        void onShown();
        void onFallback();
    }

    /** Notifications allowed, bubbles on, every conversation may bubble. */
    public static boolean ready(Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return false;
        }
        try {
            NotificationManager nm = ctx.getSystemService(NotificationManager.class);
            if (nm == null || !nm.areNotificationsEnabled()) {
                return false;
            }
            if (!nm.areBubblesEnabled()) {
                return false;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                return nm.getBubblePreference() == NotificationManager.BUBBLE_PREFERENCE_ALL;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** What is still missing, for the status line (null once ready). */
    public static String missing(Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return "Bubbles need Android 11 or newer";
        }
        try {
            NotificationManager nm = ctx.getSystemService(NotificationManager.class);
            if (nm == null || !nm.areNotificationsEnabled()) {
                return "Allow notifications for Bubble Float to use bubbles";
            }
            if (!nm.areBubblesEnabled()) {
                return "Turn on bubbles for Bubble Float";
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    && nm.getBubblePreference() != NotificationManager.BUBBLE_PREFERENCE_ALL) {
                return "Set bubbles for Bubble Float to All conversations can bubble";
            }
        } catch (Exception e) {
            return "Bubble settings could not be checked";
        }
        return null;
    }

    /** One step of setup: notification permission, then bubble settings. */
    public static void setUp(Activity activity) {
        try {
            NotificationManager nm = activity.getSystemService(NotificationManager.class);
            String perm = "android.permission.POST_NOTIFICATIONS";
            if (Build.VERSION.SDK_INT >= 33
                    && activity.checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(new String[]{perm}, REQUEST_NOTIFICATIONS);
                return;
            }
            if (nm != null && !nm.areNotificationsEnabled()) {
                open(activity, new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS));
            } else if (!ready(activity)) {
                open(activity, new Intent(Settings.ACTION_APP_NOTIFICATION_BUBBLE_SETTINGS));
            }
        } catch (Exception e) {
            open(activity, new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", activity.getPackageName(), null)));
        }
    }

    public static void onPermissionResult(Activity activity, boolean granted) {
        if (granted && !ready(activity)) {
            open(activity, new Intent(Settings.ACTION_APP_NOTIFICATION_BUBBLE_SETTINGS));
        }
    }

    private static void open(Activity activity, Intent intent) {
        try {
            activity.startActivity(intent.putExtra(Settings.EXTRA_APP_PACKAGE,
                    activity.getPackageName()));
        } catch (Exception e) {
            try {
                activity.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", activity.getPackageName(), null)));
            } catch (Exception ignored) {
            }
        }
    }

    private static String key(AppInfo app) {
        return "bubble:" + app.key();
    }

    /**
     * Whether a start from Home would take the app out of its bubble: its
     * activity is singleTask or singleInstance, so it has one task, and
     * that is the bubble's. Such an app's bubble is opened instead.
     */
    public static boolean inSingleTaskBubble(Context ctx, AppInfo app) {
        if (app.launchMode != ActivityInfo.LAUNCH_SINGLE_TASK
                && app.launchMode != ActivityInfo.LAUNCH_SINGLE_INSTANCE) {
            return false;
        }
        try {
            NotificationManager nm = ctx.getSystemService(NotificationManager.class);
            String k = key(app);
            for (StatusBarNotification sbn : nm.getActiveNotifications()) {
                if (k.equals(sbn.getTag()) && sbn.getId() == ID
                        && (sbn.getNotification().flags & Notification.FLAG_BUBBLE) != 0) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    /**
     * Posts the app's bubble, opened at once unless expand is false.
     * If the system turns it down (the app is not resizable, bubbles got
     * switched off) the notification is withdrawn and onFallback runs.
     * Exactly one of onShown / onFallback runs.
     */
    public static void show(final Context ctx, final AppInfo app,
                            final boolean expand, final Callback cb) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            cb.onFallback();
            return;
        }
        final NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        final String k = key(app);
        final String label = app.label;
        try {
            final Icon icon = adaptiveIcon(ctx, app);
            final Intent launch = new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setComponent(app.component);
            Person person = new Person.Builder().setName(label).setIcon(icon).setKey(k).build();
            ctx.getSystemService(ShortcutManager.class).pushDynamicShortcut(
                    new ShortcutInfo.Builder(ctx, k)
                            .setShortLabel(label)
                            .setIcon(icon)
                            .setIntent(launch)
                            .setPerson(person)
                            .setLongLived(true)
                            .build());
            if (nm.getNotificationChannel(CHANNEL) == null) {
                NotificationChannel ch = new NotificationChannel(CHANNEL, "App bubbles",
                        NotificationManager.IMPORTANCE_DEFAULT);
                ch.setDescription("Apps you open in a bubble");
                ch.setSound(null, null);
                ch.enableVibration(false);
                ch.setShowBadge(false);
                nm.createNotificationChannel(ch);
            }
            // A bubble the user dismissed can leave its notification behind,
            // and that one will not bubble again.
            for (StatusBarNotification sbn : nm.getActiveNotifications()) {
                if (k.equals(sbn.getTag()) && sbn.getId() == ID
                        && (sbn.getNotification().flags & Notification.FLAG_BUBBLE) == 0) {
                    nm.cancel(k, ID);
                    break;
                }
            }
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                flags |= PendingIntent.FLAG_MUTABLE;
            }
            PendingIntent pi = PendingIntent.getActivity(ctx, k.hashCode(), launch, flags);
            Notification.BubbleMetadata metadata = new Notification.BubbleMetadata.Builder(pi, icon)
                    .setDesiredHeight(ctx.getResources().getDisplayMetrics().heightPixels)
                    .setAutoExpandBubble(expand)
                    .setSuppressNotification(true)
                    .build();
            Person me = new Person.Builder().setName("You").build();
            nm.notify(k, ID, new Notification.Builder(ctx, CHANNEL)
                    // The small icon is what Android can show in the
                    // corner of the bubble head. Use the bubbled app's
                    // own icon here, so no Bubble Float logo sits
                    // next to it. The bubble head itself is already
                    // the app's icon, from the BubbleMetadata above.
                    .setSmallIcon(Icon.createWithBitmap(adaptiveBitmap(ctx, app)))
                    .setBadgeIconType(Notification.BADGE_ICON_NONE)
                    .setContentTitle(label)
                    .setContentText("Open in a bubble")
                    .setContentIntent(pi)
                    .setShortcutId(k)
                    .setStyle(new Notification.MessagingStyle(me)
                            .addMessage("Open in a bubble", System.currentTimeMillis(), person))
                    .setBubbleMetadata(metadata)
                    .setOnlyAlertOnce(true)
                    .build());
        } catch (Exception e) {
            Log.w(TAG, "bubble " + label + ": could not post (" + e + ")");
            cb.onFallback();
            return;
        }
        // getActiveNotifications also returns enqueued notifications before
        // the ranking has set FLAG_BUBBLE, so only judge after a delay.
        final Handler handler = new Handler(Looper.getMainLooper());
        final int[] tries = {0};
        final Runnable[] checkRef = new Runnable[1];
        checkRef[0] = new Runnable() {
            @Override
            public void run() {
                StatusBarNotification found = null;
                try {
                    for (StatusBarNotification sbn : nm.getActiveNotifications()) {
                        if (k.equals(sbn.getTag()) && sbn.getId() == ID) {
                            found = sbn;
                            break;
                        }
                    }
                } catch (Exception ignored) {
                }
                if (found != null
                        && (found.getNotification().flags & Notification.FLAG_BUBBLE) != 0) {
                    cb.onShown();
                } else if (++tries[0] < 8) {
                    handler.postDelayed(checkRef[0], 150);
                } else if (found != null) {
                    Log.w(TAG, "bubble " + label + ": not allowed to bubble, opening normally");
                    nm.cancel(k, ID);
                    cb.onFallback();
                } else {
                    Log.w(TAG, "bubble " + label + ": never posted, opening normally");
                    cb.onFallback();
                }
            }
        };
        handler.postDelayed(checkRef[0], 150);
    }

    /** The app's icon as an adaptive bitmap (108dp, both layers). */
    private static Bitmap adaptiveBitmap(Context ctx, AppInfo app) {
        float density = ctx.getResources().getDisplayMetrics().density;
        int size = (int) (108 * density);
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Drawable d = app.icon;
        if (d instanceof AdaptiveIconDrawable) {
            AdaptiveIconDrawable ad = (AdaptiveIconDrawable) d;
            if (ad.getBackground() != null) {
                ad.getBackground().setBounds(0, 0, size, size);
                ad.getBackground().draw(c);
            }
            if (ad.getForeground() != null) {
                ad.getForeground().setBounds(0, 0, size, size);
                ad.getForeground().draw(c);
            }
        } else {
            c.drawColor(Color.WHITE);
            int inset = size * 22 / 108;
            d.setBounds(inset, inset, size - inset, size - inset);
            d.draw(c);
        }
        return bmp;
    }

    private static Icon adaptiveIcon(Context ctx, AppInfo app) {
        return Icon.createWithAdaptiveBitmap(adaptiveBitmap(ctx, app));
    }
}
