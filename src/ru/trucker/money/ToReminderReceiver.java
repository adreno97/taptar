package ru.trucker.money;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ToReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Reminders.schedule(context);
            return;
        }
        Reminders.check(context);
        Reminders.schedule(context);
    }
}
