package ru.trucker.money;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

public class RecordsAdapter extends BaseAdapter {

    private final Context ctx;
    private final List<DbHelper.Record> items;

    public RecordsAdapter(Context ctx, List<DbHelper.Record> items) {
        this.ctx = ctx;
        this.items = items;
    }

    @Override public int getCount() { return items.size(); }

    @Override public Object getItem(int i) { return items.get(i); }

    @Override public long getItemId(int i) { return i; }

    @Override
    public View getView(int pos, View convert, ViewGroup parent) {
        LinearLayout row;
        if (convert == null) {
            row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(Util.dp(ctx, 14), Util.dp(ctx, 10), Util.dp(ctx, 14), Util.dp(ctx, 10));
        } else {
            row = (LinearLayout) convert;
            row.removeAllViews();
        }

        DbHelper.Record r = items.get(pos);
        int accent = r.income ? 0xFF2E7D32 : 0xFFC62828;

        LinearLayout top = new LinearLayout(ctx);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(ctx);
        if (r.income) {
            title.setText("🚚 Рейс №" + r.number);
        } else {
            title.setText(r.title);
        }
        title.setTextSize(15);
        title.setTextColor(0xFF263238);
        title.setSingleLine(true);
        top.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView amt = new TextView(ctx);
        amt.setText((r.income ? "+" : "−") + Util.rub(r.amount));
        amt.setTextSize(15);
        amt.setTextColor(accent);
        amt.setTypeface(amt.getTypeface(), android.graphics.Typeface.BOLD);
        top.addView(amt);
        row.addView(top);

        StringBuilder sub = new StringBuilder(Util.date(r.date));
        if (r.sub != null && !r.sub.isEmpty()) sub.append(" · ").append(r.sub);
        TextView subTv = new TextView(ctx);
        subTv.setText(sub.toString());
        subTv.setTextSize(12);
        subTv.setTextColor(0xFF90A4AE);
        row.addView(subTv);

        View divider = new View(ctx);
        divider.setBackgroundColor(0xFFECEFF1);
        row.addView(divider, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1, 0));

        return row;
    }
}
