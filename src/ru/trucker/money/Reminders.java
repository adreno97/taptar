package ru.trucker.money;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.Calendar;
import java.util.List;

public class Reminders {

    private static final String PREFS = "reminders";
    private static final String CHANNEL = "to_remind";
    public static final String ACTION_CHECK = "ru.trucker.money.TO_REMIND";

    public static boolean isEnabled(Context c) {
        return c.getSharedPreferences(PREFS, 0).getBoolean("enabled", false);
    }

    public static void setEnabled(Context c, boolean on) {
        c.getSharedPreferences(PREFS, 0).edit().putBoolean("enabled", on).apply();
    }

    public static long intervalKm(Context c) {
        return c.getSharedPreferences(PREFS, 0).getLong("interval_km", 15000);
    }

    public static void setIntervalKm(Context c, long v) {
        c.getSharedPreferences(PREFS, 0).edit().putLong("interval_km", v).apply();
    }

    public static long currentMileage(Context c) {
        return c.getSharedPreferences(PREFS, 0).getLong("mileage", 0);
    }

    public static void setCurrentMileage(Context c, long v) {
        c.getSharedPreferences(PREFS, 0).edit().putLong("mileage", v).apply();
    }

    public static long nextDate(Context c) {
        return c.getSharedPreferences(PREFS, 0).getLong("next_date", 0);
    }

    public static void setNextDate(Context c, long v) {
        c.getSharedPreferences(PREFS, 0).edit().putLong("next_date", v).apply();
    }

    public static String nextDateText(Context c) {
        long d = nextDate(c);
        return d > 0 ? Util.date(d) : "";
    }

    public static void schedule(Context c) {
        if (!isEnabled(c)) return;
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 9);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        PendingIntent pi = PendingIntent.getBroadcast(c, 0,
                new Intent(c, ToReminderReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        try {
            am.cancel(pi);
            am.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY, pi);
        } catch (Exception ignored) {
        }
    }

    public static void cancel(Context c) {
        PendingIntent pi = PendingIntent.getBroadcast(c, 0,
                new Intent(c, ToReminderReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            try {
                am.cancel(pi);
            } catch (Exception ignored) {
            }
        }
    }

    /** Checks due conditions and shows a notification if a service is due. Returns true if shown. */
    public static boolean check(Context c) {
        if (!isEnabled(c)) return false;

        boolean due = false;
        StringBuilder msg = new StringBuilder();

        long cur = currentMileage(c);
        long interval = intervalKm(c);
        if (cur > 0 && interval > 0) {
            long last = 0;
            List<DbHelper.Maint> list = new DbHelper(c).getMaintAll();
            for (DbHelper.Maint m : list) {
                if (m.mileage > last) last = m.mileage;
            }
            if (last > 0 && cur - last >= interval) {
                due = true;
                msg.append("Пробег достиг интервала ТО: ").append(cur).append(" км.\n");
            }
        }

        long next = nextDate(c);
        if (next > 0 && System.currentTimeMillis() >= next) {
            due = true;
            msg.append("Подошла дата ТО: ").append(Util.date(next)).append(".\n");
        }

        if (!due) return false;
        showNotification(c, msg.toString().trim());
        return true;
    }

    private static void showNotification(Context c, String text) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "Обслуживание ТС",
                    NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Напоминания о техническом обслуживании");
            nm.createNotificationChannel(ch);
        }
        Intent i = new Intent(c, MaintenanceActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(c, CHANNEL)
                : new Notification.Builder(c);
        Notification n = b.setContentTitle("Пора провести ТО")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build();
        nm.notify(1, n);
    }
}
