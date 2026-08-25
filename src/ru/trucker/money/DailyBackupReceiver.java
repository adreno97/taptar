package ru.trucker.money;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class DailyBackupReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            DailyBackup.schedule(context);
            return;
        }
        DailyBackup.schedule(context);
        if (!DailyBackup.isEnabled(context)) return;
        if (!SyncManager.hasCredentials(context)) return;
        final Context app = context.getApplicationContext();
        SyncManager.sync(app, new SyncManager.Callback() {
            @Override public void done(boolean ok, String msg) {
                showNotification(app, ok, msg);
            }
        });
    }

    private static void showNotification(Context c, boolean ok, String msg) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        String chId = "backup";
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(chId, "Резервная копия",
                    ok ? NotificationManager.IMPORTANCE_LOW : NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(ch);
        }
        Notification.Builder b = android.os.Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(c, chId)
                : new Notification.Builder(c);
        Notification n = b.setContentTitle(ok ? "Резервная копия сохранена" : "Резервная копия не удалась")
                .setContentText(msg)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true)
                .build();
        nm.notify(2, n);
    }
}
