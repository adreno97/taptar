package ru.trucker.money;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends BaseActivity {

    private LinearLayout zonesBox;
    private Spinner countSpinner;
    private List<EditText> priceEds = new ArrayList<>();
    private EditText extraPriceEt, extraStartEt;
    private android.widget.Switch remindCb;
    private android.widget.Switch numTripsCb;
    private android.widget.Switch darkCb;
    private boolean restoring;
    private EditText mileageEt, intervalEt;
    private Button nextToBtn;
    private long nextToDate = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Настройки зон");

        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Util.dp(this, 16), Util.dp(this, 16), Util.dp(this, 16), Util.dp(this, 24));
        root.setBackgroundColor(Ui.bg(this));
        sv.addView(root);

        TextView hint = new TextView(this);
        hint.setText("Оплата за рейс зависит от зоны. Здесь можно изменить количество зон и цены за каждую. Возврат оплачивается дополнительно +50% от стоимости рейса.");
        hint.setTextSize(13);
        hint.setTextColor(Ui.label(this));
        root.addView(hint);

        TextView h = new TextView(this);
        h.setText("Количество зон");
        h.setTextSize(15);
        h.setTextColor(Ui.title(this));
        h.setPadding(0, Util.dp(this, 12), 0, 0);
        root.addView(h);

        countSpinner = new Spinner(this);
        String[] opts = new String[Zones.MAX - Zones.MIN + 1];
        for (int i = 0; i < opts.length; i++) opts[i] = (i + Zones.MIN) + " зоны";
        countSpinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, opts));
        countSpinner.setBackgroundColor(Ui.field(this));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Util.dp(this, 48));
        root.addView(countSpinner, sp);

        zonesBox = new LinearLayout(this);
        zonesBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(zonesBox);

        TextView h2 = new TextView(this);
        h2.setText("Количество точек выгрузки");
        h2.setTextSize(15);
        h2.setTextColor(Ui.title(this));
        h2.setPadding(0, Util.dp(this, 18), 0, 0);
        root.addView(h2);

        TextView hint2 = new TextView(this);
        hint2.setText("Точки выгрузки до 3-й включительно входят в стоимость зоны, с 4-й каждая точка оплачивается отдельно.");
        hint2.setTextSize(12);
        hint2.setTextColor(Ui.sub(this));
        root.addView(hint2);

        extraPriceEt = makeRow(root, "Оплата за точку выгрузки (руб)");
        extraStartEt = makeRow(root, "Оплата точек выгрузки начиная с №");

        TextView h3 = new TextView(this);
        h3.setText("Напоминание о ТО");
        h3.setTextSize(15);
        h3.setTextColor(Ui.title(this));
        h3.setPadding(0, Util.dp(this, 18), 0, 0);
        root.addView(h3);

        remindCb = new android.widget.Switch(this);
        remindCb.setText("Включить напоминания о ТО");
        remindCb.setTextSize(15);
        root.addView(remindCb);

        mileageEt = makeRow(root, "Текущий пробег ТС, км");
        intervalEt = makeRow(root, "Интервал ТО, км (напр. 15000)");
        nextToBtn = new Button(this);
        nextToBtn.setAllCaps(false);
        nextToBtn.setText("Дата следующего ТО: не задана");
        nextToBtn.setTextSize(13);
        nextToBtn.setTextColor(Ui.accentText(this));
        nextToBtn.setBackgroundColor(Ui.card(this));
        root.addView(nextToBtn, lpWrap());
        nextToBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { pickNextDate(); }
        });

        TextView h4 = new TextView(this);
        h4.setText("Список записей");
        h4.setTextSize(15);
        h4.setTextColor(Ui.title(this));
        h4.setPadding(0, Util.dp(this, 18), 0, 0);
        root.addView(h4);

        numTripsCb = new android.widget.Switch(this);
        numTripsCb.setText("Нумеровать рейсы по периодам (1–15 / 16–31)");
        numTripsCb.setTextSize(14);
        root.addView(numTripsCb);

        TextView h5 = new TextView(this);
        h5.setText("Внешний вид");
        h5.setTextSize(15);
        h5.setTextColor(Ui.title(this));
        h5.setPadding(0, Util.dp(this, 18), 0, 0);
        root.addView(h5);

        darkCb = new android.widget.Switch(this);
        darkCb.setText("Тёмная тема");
        darkCb.setTextSize(15);
        root.addView(darkCb);

        Button save = new Button(this);
        save.setText("Сохранить");
        save.setTextSize(16);
        save.setTextColor(Ui.buttonText(this));
        save.setBackgroundColor(Ui.accent(this));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Util.dp(this, 52));
        slp.topMargin = Util.dp(this, 16);
        root.addView(save, slp);
        save.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { onSave(); }
        });

        TextView footer = new TextView(this);
        footer.setText("Разработчик: adreno97 · adreno97@mail.ru");
        footer.setTextSize(12);
        footer.setTextColor(Ui.sub(this));
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, Util.dp(this, 14), 0, Util.dp(this, 4));
        root.addView(footer);

        setContentView(sv);

        extraPriceEt.setText(String.valueOf(Zones.getExtraPrice(this) / 100.0).replace(".0", ""));
        extraStartEt.setText(String.valueOf(Zones.getExtraStart(this)));

        remindCb.setChecked(Reminders.isEnabled(this));
        numTripsCb.setChecked(getSharedPreferences("app", 0).getBoolean("num_trips", false));
        restoring = true;
        darkCb.setChecked(getSharedPreferences("app", 0).getBoolean("dark_theme", false));
        restoring = false;
        mileageEt.setText(String.valueOf(Reminders.currentMileage(this)));
        intervalEt.setText(String.valueOf(Reminders.intervalKm(this)));
        nextToDate = Reminders.nextDate(this);
        nextToBtn.setText(nextToDate > 0 ? "Дата следующего ТО: " + Util.date(nextToDate) : "Дата следующего ТО: не задана");

        countSpinner.setSelection(Zones.getCount(this) - Zones.MIN);
        countSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                buildZones(pos + Zones.MIN);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        darkCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton b, boolean checked) {
                if (restoring) return;
                getSharedPreferences("app", 0).edit().putBoolean("dark_theme", checked).apply();
                recreate();
            }
        });
    }

    private void buildZones(int count) {
        zonesBox.removeAllViews();
        priceEds.clear();
        long[] prices = Zones.getPrices(this);
        long[] current = new long[count];
        for (int i = 0; i < count; i++) {
            current[i] = i < prices.length ? prices[i] : 0;
        }
        for (int i = 0; i < count; i++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, Util.dp(this, 6), 0, Util.dp(this, 6));

            TextView name = new TextView(this);
            name.setText(Zones.name(i));
            name.setTextSize(14);
            name.setTextColor(Ui.title(this));
            row.addView(name, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            EditText price = new EditText(this);
            price.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            price.setTextSize(15);
            price.setText(String.valueOf(current[i] / 100.0).replace(".0", ""));
            price.setSingleLine(true);
            price.setBackgroundColor(Ui.field(this));
            price.setPadding(Util.dp(this, 10), Util.dp(this, 8), Util.dp(this, 10), Util.dp(this, 8));
            LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            row.addView(price, pp);

            TextView cur = new TextView(this);
            cur.setText("  руб");
            cur.setTextColor(Ui.label(this));
            row.addView(cur);

            zonesBox.addView(row);
            priceEds.add(price);
        }
    }

    private EditText makeRow(LinearLayout root, String label) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, Util.dp(this, 6), 0, Util.dp(this, 6));

        TextView name = new TextView(this);
        name.setText(label);
        name.setTextSize(14);
        name.setTextColor(Ui.title(this));
        row.addView(name, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        EditText ed = new EditText(this);
        ed.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        ed.setTextSize(15);
        ed.setSingleLine(true);
        ed.setBackgroundColor(Ui.field(this));
        ed.setPadding(Util.dp(this, 10), Util.dp(this, 8), Util.dp(this, 10), Util.dp(this, 8));
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(ed, pp);
        root.addView(row);
        return ed;
    }

    private LinearLayout.LayoutParams lpWrap() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = Util.dp(this, 8);
        return p;
    }

    private void pickNextDate() {
        java.util.Calendar c = java.util.Calendar.getInstance();
        if (nextToDate > 0) c.setTimeInMillis(nextToDate);
        android.app.DatePickerDialog d = new android.app.DatePickerDialog(this,
                new android.app.DatePickerDialog.OnDateSetListener() {
                    @Override public void onDateSet(android.widget.DatePicker v, int y, int m, int d) {
                        java.util.Calendar nc = java.util.Calendar.getInstance();
                        nc.set(y, m, d, 0, 0, 0);
                        nc.set(java.util.Calendar.MILLISECOND, 0);
                        nextToDate = nc.getTimeInMillis();
                        nextToBtn.setText("Дата следующего ТО: " + Util.date(nextToDate));
                    }
                },
                c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH), c.get(java.util.Calendar.DAY_OF_MONTH));
        d.show();
    }

    private void onSave() {
        int count = countSpinner.getSelectedItemPosition() + Zones.MIN;
        long[] prices = new long[count];
        for (int i = 0; i < count; i++) {
            prices[i] = Util.parseKopecks(priceEds.get(i).getText().toString());
            if (prices[i] <= 0) {
                Toast.makeText(this, "Укажите цену для " + Zones.name(i), Toast.LENGTH_SHORT).show();
                return;
            }
        }
        long extraPrice = Util.parseKopecks(extraPriceEt.getText().toString());
        if (extraPrice <= 0) {
            Toast.makeText(this, "Укажите оплату за точку выгрузки", Toast.LENGTH_SHORT).show();
            return;
        }
        int extraStart;
        try {
            extraStart = Integer.parseInt(extraStartEt.getText().toString().trim());
        } catch (Exception e) {
            extraStart = -1;
        }
        if (extraStart < 1) {
            Toast.makeText(this, "Укажите номер точки, с которой идёт оплата (от 1)", Toast.LENGTH_SHORT).show();
            return;
        }
        Zones.setCount(this, count);
        Zones.setPrices(this, prices);
        Zones.setExtraPrice(this, extraPrice);
        Zones.setExtraStart(this, extraStart);

        Reminders.setEnabled(this, remindCb.isChecked());
        try {
            Reminders.setCurrentMileage(this, Long.parseLong(mileageEt.getText().toString().trim()));
        } catch (Exception ignored) {
            Reminders.setCurrentMileage(this, 0);
        }
        try {
            long iv = Long.parseLong(intervalEt.getText().toString().trim());
            Reminders.setIntervalKm(this, iv > 0 ? iv : 15000);
        } catch (Exception ignored) {
            Reminders.setIntervalKm(this, 15000);
        }
        Reminders.setNextDate(this, nextToDate);
        if (remindCb.isChecked()) Reminders.schedule(this);
        else Reminders.cancel(this);

        getSharedPreferences("app", 0).edit()
                .putBoolean("num_trips", numTripsCb.isChecked()).apply();

        Toast.makeText(this, "Настройки сохранены", Toast.LENGTH_SHORT).show();
        finish();
    }
}
