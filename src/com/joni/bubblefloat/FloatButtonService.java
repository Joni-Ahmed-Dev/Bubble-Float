package com.joni.bubblefloat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

public class FloatButtonService extends Service {
    private WindowManager wm;
    private View button;
    private WindowManager.LayoutParams params;
    private static final int NOTIF_ID = 2;
    private static final String CHANNEL = "float_button";

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        TextView tv = new TextView(this);
        tv.setText("\u2605");
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(26);
        tv.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColors(new int[]{0xFF5B8DEF, 0xFF8B5CF6});
        bg.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        bg.setOrientation(GradientDrawable.Orientation.TL_BR);
        tv.setBackground(bg);
        button = tv;

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int size = (int) (56 * dm.density);
        params = new WindowManager.LayoutParams(
                size, size,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = dm.widthPixels - size - (int) (16 * dm.density);
        params.y = dm.heightPixels / 3;

        final float[] down = new float[2];
        final int[] start = new int[2];
        final boolean[] moved = new boolean[1];
        tv.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        down[0] = e.getRawX();
                        down[1] = e.getRawY();
                        start[0] = params.x;
                        start[1] = params.y;
                        moved[0] = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) (e.getRawX() - down[0]);
                        int dy = (int) (e.getRawY() - down[1]);
                        if (Math.abs(dx) > 12 || Math.abs(dy) > 12) {
                            moved[0] = true;
                        }
                        params.x = start[0] + dx;
                        params.y = start[1] + dy;
                        try {
                            wm.updateViewLayout(button, params);
                        } catch (Exception ignored) {
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!moved[0]) {
                            Intent i = new Intent(FloatButtonService.this, MainActivity.class);
                            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            startActivity(i);
                        }
                        return true;
                    default:
                        return false;
                }
            }
        });
        try {
            wm.addView(button, params);
        } catch (Exception ignored) {
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    && nm.getNotificationChannel(CHANNEL) == null) {
                NotificationChannel ch = new NotificationChannel(CHANNEL, "Floating button",
                        NotificationManager.IMPORTANCE_LOW);
                ch.setDescription("Shows while the floating button is on");
                nm.createNotificationChannel(ch);
            }
            Intent i = new Intent(this, MainActivity.class);
            PendingIntent pi = PendingIntent.getActivity(this, 0, i,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Notification n = new Notification.Builder(this, CHANNEL)
                    .setSmallIcon(R.drawable.ic_small)
                    .setContentTitle("Bubble Float")
                    .setContentText("Floating button is on")
                    .setContentIntent(pi)
                    .setOngoing(true)
                    .build();
            startForeground(NOTIF_ID, n);
        } catch (Exception ignored) {
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        try {
            if (button != null) {
                wm.removeView(button);
            }
        } catch (Exception ignored) {
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
