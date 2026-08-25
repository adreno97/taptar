package ru.trucker.money;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

/** Ежедневный бэкап в облако (по умолчанию 21:00, настраивается). */
public class DailyBackup {

    private static final String PREFS = "daily_backup";
    public static final String ACTION_BACKUP = "ru.trucker.money.DAILY_BACKUP";

    private DailyBackup() {}

    public static boolean isEnabled(Context c) {
        return c.getSharedPreferences(PREFS, 0).getBoolean("enabled", true);
    }

    public static void setEnabled(Context c, boolean on) {
        c.getSharedPreferences(PREFS, 0).edit().putBoolean("enabled", on).apply();
    }

    public static int hour(Context c) {
        return c.getSharedPreferences(PREFS, 0).getInt("hour", 21);
    }

    public static int minute(Context c) {
        return c.getSharedPreferences(PREFS, 0).getInt("minute", 0);
    }

    public static void setTime(Context c, int hour, int minute) {
        c.getSharedPreferences(PREFS, 0).edit().putInt("hour", hour).putInt("minute", minute).apply();
    }

    public static String timeText(Context c) {
        return String.format(java.util.Locale.US, "%02d:%02d", hour(c), minute(c));
    }

    public static void schedule(Context c) {
        if (!isEnabled(c)) return;
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour(c));
        cal.set(Calendar.MINUTE, minute(c));
        cal.set(Calendar.SECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        PendingIntent pi = PendingIntent.getBroadcast(c, 0,
                new Intent(c, DailyBackupReceiver.class).setAction(ACTION_BACKUP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        try {
            am.cancel(pi);
            am.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY, pi);
        } catch (Exception ignored) {
        }
    }

    public static void cancel(Context c) {
        PendingIntent pi = PendingIntent.getBroadcast(c, 0,
                new Intent(c, DailyBackupReceiver.class).setAction(ACTION_BACKUP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            try {
                am.cancel(pi);
            } catch (Exception ignored) {
            }
        }
    }
}
