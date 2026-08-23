package ru.trucker.money;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Map;

public class StatsActivity extends BaseActivity {

    private static final int PER_CUR = 0, PER_PREV = 1, PER_MONTH = 2, PER_RANGE = 3, PER_ALL = 4;

    private DbHelper db;
    private LinearLayout body, rangeRow;
    private Button fromBtn, toBtn;
    private long start = -1, end = -1; // start<0 → all time
    private String periodLabel = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new DbHelper(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.bg(this));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setPadding(Util.dp(this, 8), Util.dp(this, 8), Util.dp(this, 8), Util.dp(this, 8));
        top.setBackgroundColor(Ui.card(this));
        Spinner period = new Spinner(this);
        period.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,
                new String[]{"Текущий месяц", "Прошлый месяц", "Выбрать месяц…", "Произвольный период", "Всё время"}));
        top.addView(period, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(top);

        rangeRow = new LinearLayout(this);
        rangeRow.setOrientation(LinearLayout.HORIZONTAL);
        rangeRow.setBackgroundColor(Ui.card(this));
        fromBtn = rangeBtn("От");
        toBtn = rangeBtn("До");
        rangeRow.addView(fromBtn, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        rangeRow.addView(toBtn, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        rangeRow.setVisibility(View.GONE);
        root.addView(rangeRow);

        ScrollView sv = new ScrollView(this);
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(Util.dp(this, 14), Util.dp(this, 10), Util.dp(this, 14), Util.dp(this, 20));
        sv.addView(body);
        root.addView(sv, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);

        period.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                onPeriod(pos);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        fromBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { pickDate(true); }
        });
        toBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { pickDate(false); }
        });
    }

    private Button rangeBtn(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(13);
        b.setTextColor(Ui.accentText(this));
        b.setBackgroundColor(Ui.bg(this));
        return b;
    }

    private void onPeriod(int pos) {
        Calendar now = Calendar.getInstance();
        switch (pos) {
            case PER_CUR: {
                long[] r = db.currentMonthRange();
                start = r[0]; end = r[1];
                periodLabel = "за " + Util.monthYear();
                rangeRow.setVisibility(View.GONE);
                render();
                break;
            }
            case PER_PREV: {
                Calendar c = Calendar.getInstance();
                c.add(Calendar.MONTH, -1);
                long[] r = monthRangeOf(c.get(Calendar.YEAR), c.get(Calendar.MONTH));
                start = r[0]; end = r[1];
                periodLabel = "за прошлый месяц (" + android.text.format.DateFormat.format("MMMM yyyy", c).toString() + ")";
                rangeRow.setVisibility(View.GONE);
                render();
                break;
            }
            case PER_MONTH: {
                rangeRow.setVisibility(View.GONE);
                pickMonth();
                break;
            }
            case PER_RANGE: {
                Calendar c = Calendar.getInstance();
                Calendar first = (Calendar) c.clone();
                first.set(Calendar.DAY_OF_MONTH, 1);
                first.set(Calendar.HOUR_OF_DAY, 0); first.set(Calendar.MINUTE, 0);
                first.set(Calendar.SECOND, 0); first.set(Calendar.MILLISECOND, 0);
                start = first.getTimeInMillis();
                Calendar today = (Calendar) c.clone();
                today.set(Calendar.HOUR_OF_DAY, 0); today.set(Calendar.MINUTE, 0);
                today.set(Calendar.SECOND, 0); today.set(Calendar.MILLISECOND, 0);
                end = today.getTimeInMillis() + 86400000L;
                updateRangeButtons();
                rangeRow.setVisibility(View.VISIBLE);
                periodLabel = "за выбранный период";
                render();
                break;
            }
            default: {
                start = -1; end = -1;
                periodLabel = "за всё время";
                rangeRow.setVisibility(View.GONE);
                render();
                break;
            }
        }
    }

    private void pickMonth() {
        Calendar c = Calendar.getInstance();
        DatePickerDialog d = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override public void onDateSet(android.widget.DatePicker v, int y, int m, int d) {
                        long[] r = monthRangeOf(y, m);
                        start = r[0]; end = r[1];
                        Calendar cc = Calendar.getInstance();
                        cc.set(y, m, 1);
                        periodLabel = "за " + android.text.format.DateFormat.format("MMMM yyyy", cc).toString();
                        render();
                    }
                },
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), 1);
        d.show();
    }

    private void pickDate(final boolean isFrom) {
        Calendar c = Calendar.getInstance();
        if (isFrom) {
            if (start > 0) c.setTimeInMillis(start);
        } else {
            if (end > 0) c.setTimeInMillis(end - 86400000L);
        }
        DatePickerDialog d = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override public void onDateSet(android.widget.DatePicker v, int y, int m, int d) {
                        Calendar nc = Calendar.getInstance();
                        nc.set(y, m, d, 0, 0, 0);
                        nc.set(Calendar.MILLISECOND, 0);
                        if (isFrom) {
                            start = nc.getTimeInMillis();
                        } else {
                            end = nc.getTimeInMillis() + 86400000L;
                        }
                        updateRangeButtons();
                        render();
                    }
                },
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        d.show();
    }

    private void updateRangeButtons() {
        fromBtn.setText("От: " + Util.date(start));
        toBtn.setText("До: " + Util.date(end - 86400000L));
    }

    private static long[] monthRangeOf(int year, int month) {
        Calendar c = Calendar.getInstance();
        c.set(year, month, 1, 0, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        long a = c.getTimeInMillis();
        c.add(Calendar.MONTH, 1);
        return new long[]{a, c.getTimeInMillis()};
    }

    private void render() {
        body.removeAllViews();
        TextView h = new TextView(this);
        h.setText("Статистика " + periodLabel);
        h.setTextSize(16);
        h.setTextColor(Ui.title(this));
        h.setTypeface(h.getTypeface(), android.graphics.Typeface.BOLD);
        body.addView(h);

        long[] t = db.getTotals(start, end);
        long profit = t[0] - t[1];
        TextView summary = new TextView(this);
        summary.setTextSize(15);
        summary.setText("Доход: " + Util.rub(t[0]) + "\n"
                + "Расход: " + Util.rub(t[1]) + "\n"
                + "Прибыль: " + Util.rub(profit));
        summary.setTextColor(profit >= 0 ? Ui.income(this) : Ui.expense(this));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        slp.topMargin = Util.dp(this, 10);
        slp.bottomMargin = Util.dp(this, 6);
        body.addView(summary, slp);

        int trips = db.getTripCount(start, end);
        TextView tripTv = new TextView(this);
        tripTv.setText("Выполнено рейсов: " + trips);
        tripTv.setTextSize(14);
        tripTv.setTextColor(Ui.gray(this));
        body.addView(tripTv);

        double[] fuel = db.getFuelStats(start, end);
        if (fuel[3] > 0) {
            TextView hF = new TextView(this);
            hF.setText("Топливо");
            hF.setTextSize(16);
            hF.setTextColor(Ui.title(this));
            hF.setTypeface(hF.getTypeface(), android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams hFp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            hFp.topMargin = Util.dp(this, 14);
            body.addView(hF, hFp);

            StringBuilder fuelTxt = new StringBuilder();
            fuelTxt.append("Заправок: ").append((int) fuel[3]).append("\n");
            fuelTxt.append("Литров: ").append(String.format(java.util.Locale.US, "%.1f", fuel[0])).append("\n");
            fuelTxt.append("Потрачено: ").append(Util.rub((long) fuel[1])).append("\n");
            if (fuel[0] > 0) {
                double avgPrice = (fuel[1] / 100.0) / fuel[0];
                fuelTxt.append("Средняя цена: ").append(String.format(java.util.Locale.US, "%.2f", avgPrice)).append(" ₽/л\n");
            }
            if (fuel[2] > 0) {
                fuelTxt.append("Расход: ").append(String.format(java.util.Locale.US, "%.1f", fuel[0] / fuel[2] * 100))
                        .append(" л/100 км\n");
                fuelTxt.append("Стоимость: ").append(String.format(java.util.Locale.US, "%.2f", (fuel[1] / 100.0) / fuel[2]))
                        .append(" ₽/км\n");
            }
            if (trips > 0) {
                fuelTxt.append("Литров на рейс: ").append(String.format(java.util.Locale.US, "%.1f", fuel[0] / trips))
                        .append(" л\n");
            }
            TextView fuelTv = new TextView(this);
            fuelTv.setText(fuelTxt.toString());
            fuelTv.setTextSize(14);
            fuelTv.setTextColor(Ui.gray(this));
            body.addView(fuelTv);
        }

        TextView h2 = new TextView(this);
        h2.setText("Расходы по категориям");
        h2.setTextSize(16);
        h2.setTextColor(Ui.title(this));
        h2.setTypeface(h2.getTypeface(), android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams h2p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        h2p.topMargin = Util.dp(this, 18);
        body.addView(h2, h2p);

        Map<String, Long> cats = db.getCategoryTotals(start, end);
        long total = 0;
        for (long v : cats.values()) total += v;

        if (cats.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Пока нет записей о расходах за этот период.");
            empty.setTextColor(Ui.sub(this));
            empty.setPadding(0, Util.dp(this, 8), 0, 0);
            body.addView(empty);
            return;
        }

        for (Map.Entry<String, Long> e : cats.entrySet()) {
            long val = e.getValue();
            float frac = total > 0 ? (float) val / (float) total : 0f;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, Util.dp(this, 6), 0, Util.dp(this, 6));

            TextView name = new TextView(this);
            name.setText(e.getKey());
            name.setTextSize(14);
            name.setTextColor(Ui.primary(this));
            row.addView(name, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView valTv = new TextView(this);
            valTv.setText(Util.rub(val) + "  " + Math.round(frac * 100) + "%");
            valTv.setTextSize(14);
            valTv.setTextColor(Ui.expense(this));
            row.addView(valTv);
            body.addView(row);

            LinearLayout bar = new LinearLayout(this);
            bar.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, Util.dp(this, 8));
            bp.bottomMargin = Util.dp(this, 4);
            body.addView(bar, bp);

            View fill = new View(this);
            fill.setBackgroundColor(Ui.barFill(this));
            bar.addView(fill, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT,
                    frac > 0 ? frac : 0.001f));
            View rest = new View(this);
            rest.setBackgroundColor(Ui.barRest(this));
            bar.addView(rest, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT,
                    frac > 0 ? 1f - frac : 0.999f));
        }

        TextView totalTv = new TextView(this);
        totalTv.setText("Итого расходов: " + Util.rub(total));
        totalTv.setTextSize(15);
        totalTv.setTextColor(Ui.title(this));
        totalTv.setTypeface(totalTv.getTypeface(), android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tp.topMargin = Util.dp(this, 10);
        body.addView(totalTv, tp);
    }
}
