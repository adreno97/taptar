package ru.trucker.money;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private DbHelper db;
    private TextView incomeTv, expenseTv, profitTv, periodTv;
    private ListView listView;
    private RecordsAdapter adapter;
    private List<DbHelper.Record> data = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new DbHelper(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Util.dp(this, 12), Util.dp(this, 12), Util.dp(this, 12), 0);
        root.setBackgroundColor(0xFFF0F2F5);

        periodTv = new TextView(this);
        periodTv.setText("Сводка за " + Util.monthYear());
        periodTv.setTextSize(16);
        periodTv.setTypeface(periodTv.getTypeface(), android.graphics.Typeface.BOLD);
        periodTv.setTextColor(0xFF37474F);
        root.addView(periodTv);

        LinearLayout card = card();
        incomeTv = tv(18);
        expenseTv = tv(18);
        profitTv = tv(18);
        card.addView(labelRow("Доход (за рейсы)", incomeTv, 0xFF2E7D32));
        card.addView(labelRow("Расход", expenseTv, 0xFFC62828));
        card.addView(labelRow("Прибыль", profitTv, 0xFF1565C0));
        root.addView(card);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, Util.dp(this, 12), 0, 0);
        Button addIncome = btn("Добавить рейс", 0xFF2E7D32);
        Button addExpense = btn("+ Расход", 0xFFC62828);
        btnRow.addView(addIncome);
        btnRow.addView(addExpense);
        root.addView(btnRow);

        LinearLayout btnRow2 = new LinearLayout(this);
        btnRow2.setOrientation(LinearLayout.HORIZONTAL);
        btnRow2.setPadding(0, Util.dp(this, 8), 0, 0);
        Button history = btn("История", 0xFF546E7A);
        Button stats = btn("Статистика", 0xFF546E7A);
        btnRow2.addView(history);
        btnRow2.addView(stats);
        root.addView(btnRow2);

        LinearLayout btnRow3 = new LinearLayout(this);
        btnRow3.setOrientation(LinearLayout.HORIZONTAL);
        btnRow3.setPadding(0, Util.dp(this, 8), 0, 0);
        Button maint = btn("🚗 Обслуживание ТС", 0xFF37474F);
        Button more = btn("⚙ Ещё", 0xFF6D4C41);
        btnRow3.addView(maint);
        btnRow3.addView(more);
        root.addView(btnRow3);

        TextView recentTv = new TextView(this);
        recentTv.setText("Последние записи");
        recentTv.setTextSize(16);
        recentTv.setTypeface(recentTv.getTypeface(), android.graphics.Typeface.BOLD);
        recentTv.setTextColor(0xFF37474F);
        recentTv.setPadding(0, Util.dp(this, 16), 0, Util.dp(this, 4));
        root.addView(recentTv);

        listView = new ListView(this);
        listView.setDividerHeight(0);
        listView.setBackgroundColor(0xFFFFFFFF);
        root.addView(listView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        TextView footer = new TextView(this);
        footer.setText("Разработчик: adreno97\nadreno97@mail.ru");
        footer.setTextSize(12);
        footer.setTextColor(0xFF90A4AE);
        footer.setGravity(android.view.Gravity.CENTER);
        footer.setPadding(0, Util.dp(this, 10), 0, Util.dp(this, 10));
        root.addView(footer);

        setContentView(root);

        addIncome.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, AddActivity.class);
                i.putExtra("mode", "income");
                startActivity(i);
            }
        });
        addExpense.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, AddActivity.class);
                i.putExtra("mode", "expense");
                startActivity(i);
            }
        });
        history.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { startActivity(new Intent(MainActivity.this, HistoryActivity.class)); }
        });
        stats.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { startActivity(new Intent(MainActivity.this, StatsActivity.class)); }
        });
        maint.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { startActivity(new Intent(MainActivity.this, MaintenanceActivity.class)); }
        });
        more.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { startActivity(new Intent(MainActivity.this, MoreActivity.class)); }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> p, View v, int pos, long id) {
                Object item = adapter.getItem(pos);
                if (!(item instanceof DbHelper.Record)) return;
                DbHelper.Record r = (DbHelper.Record) item;
                Intent i = new Intent(MainActivity.this, AddActivity.class);
                i.putExtra("mode", r.income ? "income" : "expense");
                i.putExtra("edit_id", r.id);
                i.putExtra("edit_income", r.income);
                startActivity(i);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
        if (Reminders.isEnabled(this)) {
            if (android.os.Build.VERSION.SDK_INT >= 33
                    && android.content.pm.PackageManager.PERMISSION_GRANTED
                    != checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
            Reminders.schedule(this);
            Reminders.check(this);
        }
    }

    private void refresh() {
        long[] t = db.getMonthTotals(false);
        incomeTv.setText(Util.rub(t[0]));
        expenseTv.setText(Util.rub(t[1]));
        profitTv.setText(Util.rub(t[0] - t[1]));
        profitTv.setTextColor(t[0] - t[1] >= 0 ? 0xFF2E7D32 : 0xFFC62828);

        data = db.getRecent(40);
        adapter = new RecordsAdapter(this, groupByPeriod(data));
        listView.setAdapter(adapter);
    }

    private List<Object> groupByPeriod(List<DbHelper.Record> records) {
        boolean numbering = getSharedPreferences("app", 0).getBoolean("num_trips", false);
        List<Object> out = new ArrayList<>();
        int i = 0;
        while (i < records.size()) {
            DbHelper.Record first = records.get(i);
            long[] range = periodRange(first.date);

            List<DbHelper.Record> period = new ArrayList<>();
            while (i < records.size()) {
                DbHelper.Record r = records.get(i);
                if (periodRange(r.date)[0] != range[0]) break;
                period.add(r);
                i++;
            }

            long[] totals = db.getTotals(range[0], range[1]);
            int tripTotal = db.getTripCount(range[0], range[1]);
            StringBuilder totalsTxt = new StringBuilder("Доход: ").append(Util.rub(totals[0]))
                    .append("  ·  Расход: ").append(Util.rub(totals[1]));
            if (numbering) {
                totalsTxt.append("  ·  Рейсов: ").append(tripTotal);
            }
            out.add(new RecordsAdapter.Header(periodLabel(first.date), totalsTxt.toString()));

            if (numbering) {
                int idx = tripTotal;
                for (DbHelper.Record r : period) {
                    if (r.income) {
                        r.seq = idx;
                        idx--;
                    } else {
                        r.seq = 0;
                    }
                }
            } else {
                for (DbHelper.Record r : period) r.seq = 0;
            }
            out.addAll(period);
        }
        return out;
    }

    private long[] periodRange(long date) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(date);
        int day = c.get(java.util.Calendar.DAY_OF_MONTH);
        boolean firstHalf = day <= 15;
        c.set(java.util.Calendar.DAY_OF_MONTH, firstHalf ? 1 : 16);
        c.set(java.util.Calendar.HOUR_OF_DAY, 0);
        c.set(java.util.Calendar.MINUTE, 0);
        c.set(java.util.Calendar.SECOND, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);
        long start = c.getTimeInMillis();
        java.util.Calendar endC = (java.util.Calendar) c.clone();
        if (firstHalf) {
            endC.set(java.util.Calendar.DAY_OF_MONTH, 16);
        } else {
            endC.add(java.util.Calendar.MONTH, 1);
            endC.set(java.util.Calendar.DAY_OF_MONTH, 1);
        }
        return new long[]{start, endC.getTimeInMillis()};
    }

    private String periodLabel(long date) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(date);
        int day = c.get(java.util.Calendar.DAY_OF_MONTH);
        String mn = android.text.format.DateFormat.format("MMM", c).toString();
        int lastDay = c.getActualMaximum(java.util.Calendar.DAY_OF_MONTH);
        String label = day <= 15 ? "1–15 " + mn : "16–" + lastDay + " " + mn;
        int year = c.get(java.util.Calendar.YEAR);
        if (year != java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)) {
            label += " " + year;
        }
        return label;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(0xFFFFFFFF);
        card.setPadding(Util.dp(this, 14), Util.dp(this, 12), Util.dp(this, 14), Util.dp(this, 12));
        card.setElevation(Util.dp(this, 2));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = Util.dp(this, 8);
        card.setLayoutParams(p);
        return card;
    }

    private LinearLayout labelRow(String label, TextView value, int color) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, Util.dp(this, 3), 0, Util.dp(this, 3));
        TextView l = new TextView(this);
        l.setText(label);
        l.setTextSize(16);
        l.setTextColor(0xFF607D8B);
        row.addView(l, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        value.setTextColor(color);
        value.setGravity(android.view.Gravity.END);
        row.addView(value);
        return row;
    }

    private TextView tv(float sp) {
        TextView t = new TextView(this);
        t.setTextSize(sp);
        t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        return t;
    }

    private Button btn(String text, int color) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(0xFFFFFFFF);
        b.setBackgroundColor(color);
        b.setTextSize(14);
        b.setLayoutParams(new LinearLayout.LayoutParams(0, Util.dp(this, 48), 1f));
        return b;
    }
}
