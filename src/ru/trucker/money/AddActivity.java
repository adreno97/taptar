package ru.trucker.money;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;

public class AddActivity extends Activity {

    private DbHelper db;
    private boolean income;
    private long editId = -1;
    private boolean editIncome;

    private long date;
    private Button dateBtn;
    private EditText numberEt, amountEt, noteEt, extraEt;
    private Spinner catSpinner, zoneSpinner;
    private CheckBox returnCb;
    private TextView payPreview;
    private LinearLayout fuelBox;
    private EditText litersEt, pricePerLiterEt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new DbHelper(this);
        income = "income".equals(getIntent().getStringExtra("mode"));
        editId = getIntent().getLongExtra("edit_id", -1);
        editIncome = getIntent().getBooleanExtra("edit_income", true);
        if (editId >= 0) income = editIncome;

        setTitle(income ? (editId >= 0 ? "Рейс · редактирование" : "Новый рейс")
                        : (editId >= 0 ? "Расход · редактирование" : "Новый расход"));

        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Util.dp(this, 16), Util.dp(this, 16), Util.dp(this, 16), Util.dp(this, 24));
        root.setBackgroundColor(0xFFF0F2F5);
        sv.addView(root);

        date = System.currentTimeMillis();
        dateBtn = new Button(this);
        dateBtn.setAllCaps(false);
        dateBtn.setText("Дата: " + Util.date(date));
        dateBtn.setTextColor(0xFF1565C0);
        dateBtn.setBackgroundColor(0xFFFFFFFF);
        root.addView(dateBtn, lpWrap());
        dateBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { pickDate(); }
        });

        if (income) {
            numberEt = addField(root, "Номер рейса", true);
            zoneSpinner = new Spinner(this);
            ArrayAdapter<String> za = new ArrayAdapter<String>(this,
                    android.R.layout.simple_spinner_item, zoneNames());
            za.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            zoneSpinner.setAdapter(za);
            zoneSpinner.setBackgroundColor(0xFFFFFFFF);
            root.addView(zoneSpinner, spinnerLp());

            returnCb = new CheckBox(this);
            returnCb.setText("Был возврат (+50% к оплате)");
            returnCb.setTextSize(16);
            LinearLayout.LayoutParams cbP = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cbP.topMargin = Util.dp(this, 8);
            root.addView(returnCb, cbP);

            payPreview = new TextView(this);
            payPreview.setTextSize(18);
            payPreview.setTypeface(payPreview.getTypeface(), android.graphics.Typeface.BOLD);
            payPreview.setTextColor(0xFF2E7D32);
            LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            pp.topMargin = Util.dp(this, 8);
            root.addView(payPreview, pp);

            AdapterViewListener avl = new AdapterViewListener();
            zoneSpinner.setOnItemSelectedListener(avl);
            returnCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override public void onCheckedChanged(CompoundButton b, boolean checked) { updatePreview(); }
            });

            extraEt = addField(root, "Кол-во точек выгрузки (0–25)", false);
            extraEt.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) { updatePreview(); }
                @Override public void afterTextChanged(android.text.Editable s) {}
            });
        } else {
            catSpinner = new Spinner(this);
            ArrayAdapter<String> a = new ArrayAdapter<String>(this,
                    android.R.layout.simple_spinner_item, DbHelper.CATEGORIES);
            a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            catSpinner.setAdapter(a);
            catSpinner.setBackgroundColor(0xFFFFFFFF);
            root.addView(catSpinner, spinnerLp());

            fuelBox = new LinearLayout(this);
            fuelBox.setOrientation(LinearLayout.HORIZONTAL);
            fuelBox.setPadding(0, Util.dp(this, 8), 0, 0);
            litersEt = fuelField(fuelBox, "Литров");
            pricePerLiterEt = fuelField(fuelBox, "Цена за литр, руб");
            fuelBox.setVisibility(View.GONE);
            root.addView(fuelBox);

            amountEt = addField(root, "Сумма расхода (руб)", false);

            catSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                    toggleFuel();
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
            });
            android.text.TextWatcher tw = new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) { recomputeFuel(); }
                @Override public void afterTextChanged(android.text.Editable s) {}
            };
            litersEt.addTextChangedListener(tw);
            pricePerLiterEt.addTextChangedListener(tw);
        }

        noteEt = addField(root, "Заметка (необязательно)", true);

        Button save = new Button(this);
        save.setText(editId >= 0 ? "Сохранить изменения" : "Добавить запись");
        save.setTextSize(16);
        save.setTextColor(0xFFFFFFFF);
        save.setBackgroundColor(income ? 0xFF2E7D32 : 0xFFC62828);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Util.dp(this, 52));
        slp.topMargin = Util.dp(this, 16);
        root.addView(save, slp);
        save.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { onSave(); }
        });

        if (editId >= 0) {
            Button del = new Button(this);
            del.setText("Удалить запись");
            del.setTextColor(0xFFC62828);
            del.setBackgroundColor(0xFFFFEBEE);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, Util.dp(this, 44));
            dlp.topMargin = Util.dp(this, 8);
            root.addView(del, dlp);
            del.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    db.deleteRecord(editId, income);
                    Toast.makeText(AddActivity.this, "Запись удалена", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        }

        setContentView(sv);

        if (editId >= 0) {
            DbHelper.Record r = db.getRecord(editId, income);
            if (r != null) {
                date = r.date;
                dateBtn.setText("Дата: " + Util.date(date));
                if (income) {
                    numberEt.setText(r.number);
                    if (r.zone >= 0 && r.zone < zoneSpinner.getAdapter().getCount()) {
                        zoneSpinner.setSelection(r.zone);
                    }
                    returnCb.setChecked(r.isReturn);
                    if (r.numPoints > 0) extraEt.setText(String.valueOf(r.numPoints));
                } else {
                    int idx = indexOf(r.category);
                    if (idx >= 0) catSpinner.setSelection(idx);
                    amountEt.setText(String.valueOf(r.amount / 100.0).replace(".0", ""));
                }
                noteEt.setText(r.note);
            }
        }
        if (income) updatePreview();
    }

    private String[] zoneNames() {
        int c = Zones.getCount(this);
        String[] names = new String[c];
        for (int i = 0; i < c; i++) names[i] = Zones.name(i) + "  ·  " + Util.rub(Zones.getPrices(this)[i]);
        return names;
    }

    private class AdapterViewListener implements android.widget.AdapterView.OnItemSelectedListener {
        @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
            updatePreview();
        }
        @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
    }

    private void updatePreview() {
        long[] prices = Zones.getPrices(this);
        int pos = zoneSpinner != null ? zoneSpinner.getSelectedItemPosition() : 0;
        long base = pos >= 0 && pos < prices.length ? prices[pos] : 0;
        boolean ret = returnCb != null && returnCb.isChecked();
        long totalBase = ret ? Zones.priceWithReturn(base) : base;

        int points = 0;
        if (extraEt != null) {
            try {
                points = Integer.parseInt(extraEt.getText().toString().trim());
            } catch (Exception ignored) {
            }
        }
        long extra = Zones.extraCost(this, points);
        long total = totalBase + extra;

        StringBuilder txt = new StringBuilder("Оплата: ").append(Util.rub(total));
        if (ret) txt.append("\n(вкл. возврат +50%: ").append(Util.rub(base)).append(" + ").append(Util.rub(totalBase - base)).append(")");
        if (extra > 0) txt.append("\n(точки выгрузки: ").append(Zones.paidExtraPoints(points, Zones.getExtraStart(this)))
                .append(" × ").append(Util.rub(Zones.getExtraPrice(this))).append(" = ").append(Util.rub(extra)).append(")");
        payPreview.setText(txt.toString());
    }

    private int indexOf(String cat) {
        for (int i = 0; i < DbHelper.CATEGORIES.length; i++) {
            if (DbHelper.CATEGORIES[i].equals(cat)) return i;
        }
        return 0;
    }

    private EditText fuelField(LinearLayout box, String hint) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setTextSize(14);
        et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        et.setBackgroundColor(0xFFFFFFFF);
        et.setPadding(Util.dp(this, 8), Util.dp(this, 10), Util.dp(this, 8), Util.dp(this, 10));
        box.addView(et, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return et;
    }

    private boolean isFuel() {
        return !income && catSpinner != null
                && catSpinner.getSelectedItemPosition() == 0;
    }

    private void toggleFuel() {
        boolean fuel = isFuel();
        if (fuelBox != null) {
            fuelBox.setVisibility(fuel ? View.VISIBLE : View.GONE);
        }
        if (amountEt != null) amountEt.setEnabled(!fuel);
        if (fuel) recomputeFuel();
    }

    private void recomputeFuel() {
        if (fuelBox == null || !isFuel()) return;
        double liters = 0, price = 0;
        try {
            liters = Double.parseDouble(litersEt.getText().toString().replace(',', '.'));
        } catch (Exception ignored) {}
        try {
            price = Double.parseDouble(pricePerLiterEt.getText().toString().replace(',', '.'));
        } catch (Exception ignored) {}
        if (liters > 0 && price > 0) {
            long kop = Math.round(liters * price * 100.0);
            amountEt.setText(String.format(java.util.Locale.US, "%.2f", kop / 100.0));
        }
    }

    private EditText addField(LinearLayout root, String hint, boolean multi) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setTextSize(16);
        if (!multi) {
            et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        }
        et.setBackgroundColor(0xFFFFFFFF);
        et.setPadding(Util.dp(this, 10), Util.dp(this, 12), Util.dp(this, 10), Util.dp(this, 12));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = Util.dp(this, 8);
        root.addView(et, p);
        return et;
    }

    private LinearLayout.LayoutParams spinnerLp() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Util.dp(this, 48));
        p.topMargin = Util.dp(this, 8);
        return p;
    }

    private LinearLayout.LayoutParams lpWrap() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = Util.dp(this, 8);
        return p;
    }

    private void pickDate() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(date);
        DatePickerDialog d = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override public void onDateSet(android.widget.DatePicker v, int y, int m, int d) {
                        Calendar nc = Calendar.getInstance();
                        nc.set(y, m, d, 0, 0, 0);
                        nc.set(Calendar.MILLISECOND, 0);
                        date = nc.getTimeInMillis();
                        dateBtn.setText("Дата: " + Util.date(date));
                    }
                },
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        d.show();
    }

    private void onSave() {
        if (income) {
            String number = numberEt.getText().toString().trim();
            if (number.isEmpty()) {
                Toast.makeText(this, "Укажите номер рейса", Toast.LENGTH_SHORT).show();
                return;
            }
            long[] prices = Zones.getPrices(this);
            int zone = zoneSpinner.getSelectedItemPosition();
            long base = zone >= 0 && zone < prices.length ? prices[zone] : 0;
            if (base <= 0) {
                Toast.makeText(this, "Для этой зоны не задана цена. Проверьте настройки", Toast.LENGTH_SHORT).show();
                return;
            }
            boolean ret = returnCb.isChecked();
            int points = 0;
            try {
                points = Integer.parseInt(extraEt.getText().toString().trim());
            } catch (Exception ignored) {
            }
            if (points < 0 || points > Zones.MAX_EXTRA) {
                Toast.makeText(this, "Точек выгрузки: от 0 до " + Zones.MAX_EXTRA, Toast.LENGTH_SHORT).show();
                return;
            }
            long total = (ret ? Zones.priceWithReturn(base) : base) + Zones.extraCost(this, points);
            String note = noteEt.getText().toString().trim();
            if (editId >= 0) db.updateTrip(editId, number, date, zone, ret, base, points, total, note);
            else db.addTrip(number, date, zone, ret, base, points, total, note);
        } else {
            long amount;
            if (isFuel()) {
                double liters = 0, price = 0;
                try {
                    liters = Double.parseDouble(litersEt.getText().toString().replace(',', '.'));
                } catch (Exception ignored) {}
                try {
                    price = Double.parseDouble(pricePerLiterEt.getText().toString().replace(',', '.'));
                } catch (Exception ignored) {}
                if (liters <= 0 || price <= 0) {
                    Toast.makeText(this, "Укажите литры и цену за литр", Toast.LENGTH_SHORT).show();
                    return;
                }
                amount = Math.round(liters * price * 100.0);
            } else {
                amount = Util.parseKopecks(amountEt.getText().toString());
            }
            if (amount <= 0) {
                Toast.makeText(this, "Укажите сумму больше нуля", Toast.LENGTH_SHORT).show();
                return;
            }
            String cat = DbHelper.CATEGORIES[catSpinner.getSelectedItemPosition()];
            String note = noteEt.getText().toString().trim();
            if (editId >= 0) db.updateExpense(editId, date, cat, amount, note);
            else db.addExpense(date, cat, amount, note);
        }
        Toast.makeText(this, "Сохранено", Toast.LENGTH_SHORT).show();
        finish();
    }
}
