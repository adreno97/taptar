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

    public static class Header {
        public String title;
        public String totals;
        public Header(String title, String totals) {
            this.title = title;
            this.totals = totals;
        }
    }

    private final Context ctx;
    private final List<Object> items;

    public RecordsAdapter(Context ctx, List<Object> items) {
        this.ctx = ctx;
        this.items = items;
    }

    @Override public int getCount() { return items.size(); }

    @Override public Object getItem(int i) { return items.get(i); }

    @Override public long getItemId(int i) { return i; }

    @Override
    public View getView(int pos, View convert, ViewGroup parent) {
        Object item = items.get(pos);
        if (item instanceof Header) {
            return headerView((Header) item);
        }
        LinearLayout row;
        if (convert == null || !(convert.getTag() instanceof Boolean)) {
            row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(Util.dp(ctx, 14), Util.dp(ctx, 10), Util.dp(ctx, 14), Util.dp(ctx, 10));
            row.setTag(Boolean.TRUE);
        } else {
            row = (LinearLayout) convert;
            row.removeAllViews();
        }

        DbHelper.Record r = (DbHelper.Record) item;
        int accent = r.income ? Ui.income(ctx) : Ui.expense(ctx);

        LinearLayout top = new LinearLayout(ctx);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(ctx);
        if (r.income) {
            if (r.seq > 0) {
                title.setText("🚚 " + r.seq + ". Рейс " + r.number);
            } else {
                title.setText("🚚 Рейс №" + r.number);
            }
        } else {
            title.setText(r.title);
        }
        title.setTextSize(15);
        title.setTextColor(Ui.primary(ctx));
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
        subTv.setTextColor(Ui.sub(ctx));
        row.addView(subTv);

        View divider = new View(ctx);
        divider.setBackgroundColor(Ui.divider(ctx));
        row.addView(divider, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1, 0));

        return row;
    }

    private View headerView(Header h) {
        LinearLayout head = new LinearLayout(ctx);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setPadding(Util.dp(ctx, 14), Util.dp(ctx, 8), Util.dp(ctx, 14), Util.dp(ctx, 6));
        head.setBackgroundColor(Ui.headerBg(ctx));

        TextView title = new TextView(ctx);
        title.setText(h.title);
        title.setTextSize(14);
        title.setTextColor(Ui.headerText(ctx));
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        head.addView(title);

        if (h.totals != null && !h.totals.isEmpty()) {
            TextView totals = new TextView(ctx);
            totals.setText(h.totals);
            totals.setTextSize(12);
            totals.setTextColor(Ui.gray(ctx));
            head.addView(totals);
        }

        View div = new View(ctx);
        div.setBackgroundColor(Ui.headerDiv(ctx));
        head.addView(div, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1, 0));
        return head;
    }
}
