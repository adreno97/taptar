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
        Button settings = btn("⚙ Настройки", 0xFF6D4C41);
        Button export = btn("Экспорт CSV", 0xFF6D4C41);
        btnRow3.addView(settings);
        btnRow3.addView(export);
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
        settings.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { startActivity(new Intent(MainActivity.this, SettingsActivity.class)); }
        });
        export.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { exportCsv(); }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> p, View v, int pos, long id) {
                DbHelper.Record r = data.get(pos);
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
    }

    private void refresh() {
        long[] t = db.getMonthTotals(false);
        incomeTv.setText(Util.rub(t[0]));
        expenseTv.setText(Util.rub(t[1]));
        profitTv.setText(Util.rub(t[0] - t[1]));
        profitTv.setTextColor(t[0] - t[1] >= 0 ? 0xFF2E7D32 : 0xFFC62828);

        data = db.getRecent(30);
        adapter = new RecordsAdapter(this, data);
        listView.setAdapter(adapter);
    }

    private void exportCsv() {
        String csv = db.exportCsv();
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/csv");
        i.putExtra(Intent.EXTRA_SUBJECT, "Учёт ИП — выгрузка");
        i.putExtra(Intent.EXTRA_TEXT, csv);
        startActivity(Intent.createChooser(i, "Отправить CSV"));
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
