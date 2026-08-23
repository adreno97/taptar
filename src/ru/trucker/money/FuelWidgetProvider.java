package ru.trucker.money;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class FuelWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) {
            RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget_fuel);
            rv.setOnClickPendingIntent(R.id.btn_fuel, pending(ctx, 1, "Заправка"));
            rv.setOnClickPendingIntent(R.id.btn_expense, pending(ctx, 2, null));
            mgr.updateAppWidget(id, rv);
        }
    }

    private PendingIntent pending(Context ctx, int req, String category) {
        Intent i = new Intent(ctx, AddActivity.class);
        i.putExtra("mode", "expense");
        if (category != null) i.putExtra("category", category);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(ctx, req, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
