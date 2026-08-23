package ru.trucker.money;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
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

public class AddActivity extends BaseActivity {

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
    private LinearLayout fuelBox, discountRow;
    private EditText litersEt, pricePerLiterEt, fuelMileageEt, discountEt;
    private TextView fuelHint;
    private Button save, unlockBtn, del;
    private boolean loading, manualAmount, autoFilling, locked;
    private String lastAuto;
    private String initialState = "";

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
        root.setBackgroundColor(Ui.bg(this));
        sv.addView(root);

        date = System.currentTimeMillis();
        dateBtn = new Button(this);
        dateBtn.setAllCaps(false);
        dateBtn.setText("Дата: " + Util.date(date));
        dateBtn.setTextColor(Ui.accentText(this));
        dateBtn.setBackground(Ui.round(this, Ui.card(this), 8));
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
            zoneSpinner.setBackground(Ui.round(this, Ui.field(this), 8));
            root.addView(zoneSpinner, spinnerLp());

            returnCb = new CheckBox(this);
            returnCb.setText("Был возврат (+50% к оплате)");
            returnCb.setTextSize(16);
            returnCb.setTextColor(Ui.primary(this));
            LinearLayout.LayoutParams cbP = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cbP.topMargin = Util.dp(this, 8);
            root.addView(returnCb, cbP);

            payPreview = new TextView(this);
            payPreview.setTextSize(18);
            payPreview.setTypeface(payPreview.getTypeface(), android.graphics.Typeface.BOLD);
            payPreview.setTextColor(Ui.income(this));
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
            catSpinner.setBackground(Ui.round(this, Ui.field(this), 8));
            root.addView(catSpinner, spinnerLp());

            fuelBox = new LinearLayout(this);
            fuelBox.setOrientation(LinearLayout.HORIZONTAL);
            fuelBox.setPadding(0, Util.dp(this, 8), 0, 0);
            litersEt = fuelField(fuelBox, "Литров");
            pricePerLiterEt = fuelField(fuelBox, "Цена/л");
            fuelMileageEt = fuelField(fuelBox, "Пробег, км");
            fuelBox.setVisibility(View.GONE);
            root.addView(fuelBox);

            discountRow = new LinearLayout(this);
            discountRow.setOrientation(LinearLayout.HORIZONTAL);
            discountRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
            discountRow.setPadding(0, Util.dp(this, 8), 0, 0);
            TextView dLabel = new TextView(this);
            dLabel.setText("Скидка, %");
            dLabel.setTextSize(15);
            dLabel.setTextColor(Ui.title(this));
            discountRow.addView(dLabel, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            discountEt = new EditText(this);
            discountEt.setText("0");
            discountEt.setTextSize(15);
            discountEt.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            discountEt.setSingleLine(true);
            discountEt.setBackground(Ui.round(this, Ui.field(this), 8));
            discountEt.setPadding(Util.dp(this, 10), Util.dp(this, 8), Util.dp(this, 10), Util.dp(this, 8));
            discountRow.addView(discountEt, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            discountRow.setVisibility(View.GONE);
            root.addView(discountRow);

            fuelHint = new TextView(this);
            fuelHint.setTextSize(13);
            fuelHint.setTextColor(Ui.accentText(this));
            fuelHint.setVisibility(View.GONE);
            root.addView(fuelHint, lpWrap());

            amountEt = addField(root, "Сумма расхода (руб)", false);
            amountEt.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                    if (loading) return;
                    if (autoFilling && lastAuto != null && s.toString().equals(lastAuto)) {
                        lastAuto = null;
                        return;
                    }
                    manualAmount = true;
                }
                @Override public void afterTextChanged(android.text.Editable s) {}
            });

            catSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                    if (editId < 0) manualAmount = false;
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
            fuelMileageEt.addTextChangedListener(tw);
            discountEt.addTextChangedListener(tw);
        }

        noteEt = addField(root, "Заметка (необязательно)", true);

        if (editId >= 0) {
            unlockBtn = new Button(this);
            unlockBtn.setText("✏ Внести изменения");
            unlockBtn.setTextSize(16);
            unlockBtn.setTextColor(Ui.buttonText(this));
            unlockBtn.setBackground(Ui.round(this, Ui.accent(this), 8));
            LinearLayout.LayoutParams ulp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, Util.dp(this, 52));
            ulp.topMargin = Util.dp(this, 16);
            root.addView(unlockBtn, ulp);
            unlockBtn.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { applyLocked(false); }
            });
        }

        save = new Button(this);
        save.setText(editId >= 0 ? "Сохранить изменения" : "Добавить запись");
        save.setTextSize(16);
        save.setTextColor(Ui.buttonText(this));
        save.setBackground(Ui.round(this, income ? Ui.income(this) : Ui.expense(this), 8));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Util.dp(this, 52));
        slp.topMargin = Util.dp(this, 16);
        root.addView(save, slp);
        save.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { onSave(); }
        });

        if (editId >= 0) {
            del = new Button(this);
            del.setText("Удалить запись");
            del.setTextColor(Ui.expense(this));
            del.setBackground(Ui.round(this, Ui.dangerBg(this), 8));
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, Util.dp(this, 44));
            dlp.topMargin = Util.dp(this, 8);
            root.addView(del, dlp);
            del.setVisibility(View.GONE);
            del.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    new AlertDialog.Builder(AddActivity.this)
                            .setTitle("Удалить запись?")
                            .setMessage("Запись будет удалена без возможности восстановления.")
                            .setPositiveButton("Удалить", new DialogInterface.OnClickListener() {
                                @Override public void onClick(DialogInterface d, int w) {
                                    db.deleteRecord(editId, income);
                                    Toast.makeText(AddActivity.this, "Запись удалена", Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                            })
                            .setNegativeButton("Отмена", null)
                            .show();
                }
            });
        }

        setContentView(sv);

        if (!income && editId < 0) {
            String presetCat = getIntent().getStringExtra("category");
            if (presetCat != null) {
                int idx = indexOf(presetCat);
                catSpinner.setSelection(idx);
            }
            if (isFuel()) {
                String p = getSharedPreferences("app", 0).getString("last_price", null);
                if (p != null) pricePerLiterEt.setText(p);
                String d = getSharedPreferences("app", 0).getString("last_discount", null);
                if (d != null) discountEt.setText(d);
            }
            toggleFuel();
        }

        setupIme();

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
                    toggleFuel();
                    if (isFuel()) {
                        loading = true;
                        if (r.liters > 0) litersEt.setText(Util.num(r.liters));
                        if (r.pricePerLiter > 0) pricePerLiterEt.setText(Util.num(r.pricePerLiter));
                        if (r.mileage > 0) fuelMileageEt.setText(String.valueOf(r.mileage));
                        discountEt.setText(Util.num(r.discount));
                        loading = false;
                        amountEt.setText(String.valueOf(r.amount / 100.0).replace(".0", ""));
                        double expected = r.liters > 0 && r.pricePerLiter > 0
                                ? Math.round(r.liters * r.pricePerLiter * (1.0 - r.discount / 100.0) * 100.0)
                                : 0;
                        manualAmount = Math.abs(expected - r.amount) > 1;
                        recomputeFuel();
                    } else {
                        amountEt.setText(String.valueOf(r.amount / 100.0).replace(".0", ""));
                    }
                }
                noteEt.setText(r.note);
            }
        }
        if (editId >= 0) applyLocked(true);
        if (income) updatePreview();
        initialState = state();
        if (!income && editId < 0 && isFuel()) {
            litersEt.requestFocus();
        }
    }

    private void setupIme() {
        if (income) {
            chain(numberEt, extraEt);
            chain(extraEt, noteEt);
        } else {
            chain(litersEt, pricePerLiterEt);
            chain(pricePerLiterEt, fuelMileageEt);
            chain(fuelMileageEt, discountEt);
            chain(discountEt, amountEt);
            chain(amountEt, noteEt);
        }
        noteEt.setImeOptions(EditorInfo.IME_ACTION_DONE);
    }

    private void chain(final EditText from, final EditText to) {
        from.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        from.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    to.requestFocus();
                    return true;
                }
                return false;
            }
        });
    }

    private void applyLocked(boolean on) {
        int fieldBg = on ? Ui.fieldLocked(this) : Ui.field(this);
        int cardBg = on ? Ui.cardLocked(this) : Ui.card(this);
        int muted = on ? Ui.textLocked(this) : Ui.primary(this);

        dateBtn.setEnabled(!on);
        dateBtn.setBackground(Ui.round(this, on ? cardBg : Ui.card(this), 8));
        dateBtn.setTextColor(on ? muted : Ui.accentText(this));

        if (income) {
            numberEt.setEnabled(!on); numberEt.setBackground(Ui.round(this, fieldBg, 8));
            zoneSpinner.setEnabled(!on); zoneSpinner.setBackground(Ui.round(this, fieldBg, 8));
            returnCb.setEnabled(!on); returnCb.setTextColor(on ? muted : Ui.primary(this));
            extraEt.setEnabled(!on); extraEt.setBackground(Ui.round(this, fieldBg, 8));
        } else {
            catSpinner.setEnabled(!on); catSpinner.setBackground(Ui.round(this, fieldBg, 8));
            litersEt.setEnabled(!on); litersEt.setBackground(Ui.round(this, fieldBg, 8));
            pricePerLiterEt.setEnabled(!on); pricePerLiterEt.setBackground(Ui.round(this, fieldBg, 8));
            fuelMileageEt.setEnabled(!on); fuelMileageEt.setBackground(Ui.round(this, fieldBg, 8));
            discountEt.setEnabled(!on); discountEt.setBackground(Ui.round(this, fieldBg, 8));
            amountEt.setEnabled(!on); amountEt.setBackground(Ui.round(this, fieldBg, 8));
        }
        noteEt.setEnabled(!on); noteEt.setBackground(Ui.round(this, fieldBg, 8));

        save.setEnabled(!on);
        save.setBackground(Ui.round(this, on ? Ui.fieldLocked(this) : (income ? Ui.income(this) : Ui.expense(this)), 8));
        save.setTextColor(on ? Ui.textLocked(this) : Ui.buttonText(this));
        if (unlockBtn != null) unlockBtn.setVisibility(on ? View.VISIBLE : View.GONE);
        if (del != null) del.setVisibility(on ? View.GONE : View.VISIBLE);
        locked = on;
    }

    private String state() {
        StringBuilder s = new StringBuilder();
        s.append(date).append('|');
        if (income) {
            s.append(numberEt.getText()).append('|')
             .append(extraEt.getText()).append('|')
             .append(returnCb.isChecked()).append('|')
             .append(zoneSpinner.getSelectedItemPosition());
        } else {
            s.append(catSpinner.getSelectedItemPosition()).append('|')
             .append(amountEt.getText()).append('|')
             .append(litersEt.getText()).append('|')
             .append(pricePerLiterEt.getText()).append('|')
             .append(fuelMileageEt.getText()).append('|')
             .append(discountEt.getText());
        }
        s.append('|').append(noteEt.getText());
        return s.toString();
    }

    @Override
    public void onBackPressed() {
        if (!initialState.equals(state())) {
            new AlertDialog.Builder(this)
                    .setTitle("Закрыть без сохранения?")
                    .setMessage("В форме есть изменения. Они не будут сохранены.")
                    .setPositiveButton("Закрыть", new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface d, int w) { AddActivity.super.onBackPressed(); }
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        } else {
            super.onBackPressed();
        }
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
        et.setBackground(Ui.round(this, Ui.field(this), 8));
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
        if (discountRow != null) {
            discountRow.setVisibility(fuel ? View.VISIBLE : View.GONE);
        }
        if (fuel) recomputeFuel();
    }

    private void recomputeFuel() {
        if (fuelBox == null || !isFuel() || loading) return;
        double liters = 0, price = 0, disc = 0;
        long mileage = 0;
        try {
            liters = Double.parseDouble(litersEt.getText().toString().replace(',', '.'));
        } catch (Exception ignored) {}
        try {
            price = Double.parseDouble(pricePerLiterEt.getText().toString().replace(',', '.'));
        } catch (Exception ignored) {}
        try {
            disc = Double.parseDouble(discountEt.getText().toString().replace(',', '.'));
        } catch (Exception ignored) {}
        if (disc < 0) disc = 0;
        if (disc > 100) disc = 100;
        try {
            mileage = Long.parseLong(fuelMileageEt.getText().toString().replace(",", "").trim());
        } catch (Exception ignored) {}
        if (liters > 0 && price > 0 && !manualAmount) {
            long kop = Math.round(liters * price * (1.0 - disc / 100.0) * 100.0);
            lastAuto = String.format(java.util.Locale.US, "%.2f", kop / 100.0);
            autoFilling = true;
            amountEt.setText(lastAuto);
            autoFilling = false;
        }
        if (fuelHint != null) {
            StringBuilder hint = new StringBuilder();
            if (liters > 0 && price > 0 && disc > 0) {
                hint.append("Со скидкой ").append(String.format(java.util.Locale.US, "%.1f", disc)).append("%");
            }
            if (mileage > 0) {
                DbHelper.Record prev = db.getLastFuelBefore(mileage);
                if (prev != null && prev.mileage < mileage) {
                    long dist = mileage - prev.mileage;
                    if (liters > 0 && dist > 0) {
                        double costKm = liters * price / dist;
                        if (hint.length() > 0) hint.append("\n");
                        hint.append("Расход: ").append(String.format(java.util.Locale.US, "%.1f", liters / dist * 100))
                                .append(" л/100 км · ").append(String.format(java.util.Locale.US, "%.2f", costKm)).append(" ₽/км");
                    }
                }
            }
            fuelHint.setText(hint.toString());
            fuelHint.setVisibility(hint.length() > 0 ? View.VISIBLE : View.GONE);
        }
    }

    private EditText addField(LinearLayout root, String hint, boolean multi) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setTextSize(16);
        if (!multi) {
            et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        }
        et.setBackground(Ui.round(this, Ui.field(this), 8));
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
            double fuelLiters = 0, fuelPrice = 0, fuelDisc = 0;
            long fuelMileage = 0;
            if (isFuel()) {
                try {
                    fuelLiters = Double.parseDouble(litersEt.getText().toString().replace(',', '.'));
                } catch (Exception ignored) {}
                try {
                    fuelPrice = Double.parseDouble(pricePerLiterEt.getText().toString().replace(',', '.'));
                } catch (Exception ignored) {}
                try {
                    fuelDisc = Double.parseDouble(discountEt.getText().toString().replace(',', '.'));
                } catch (Exception ignored) {}
                if (fuelDisc < 0) fuelDisc = 0;
                if (fuelDisc > 100) fuelDisc = 100;
                try {
                    fuelMileage = Long.parseLong(fuelMileageEt.getText().toString().replace(",", "").trim());
                } catch (Exception ignored) {}
                if (fuelLiters > 0 && fuelPrice > 0 && !manualAmount) {
                    amount = Math.round(fuelLiters * fuelPrice * (1.0 - fuelDisc / 100.0) * 100.0);
                } else if (fuelLiters <= 0 || fuelPrice <= 0) {
                    amount = Util.parseKopecks(amountEt.getText().toString());
                    if (amount > 0 && fuelDisc > 0) {
                        amount = Math.round(amount * (1.0 - fuelDisc / 100.0));
                    }
                } else {
                    amount = Util.parseKopecks(amountEt.getText().toString());
                }
            } else {
                amount = Util.parseKopecks(amountEt.getText().toString());
            }
            if (amount <= 0) {
                Toast.makeText(this, "Укажите сумму больше нуля", Toast.LENGTH_SHORT).show();
                return;
            }
            String cat = DbHelper.CATEGORIES[catSpinner.getSelectedItemPosition()];
            String note = noteEt.getText().toString().trim();
            android.content.SharedPreferences.Editor e = getSharedPreferences("app", 0).edit();
            if (isFuel()) {
                e.putString("last_price", Util.num(fuelPrice));
                e.putString("last_discount", Util.num(fuelDisc));
            }
            e.apply();
            if (editId >= 0) db.updateExpense(editId, date, cat, amount, note, fuelLiters, fuelPrice, fuelMileage, fuelDisc);
            else db.addExpense(date, cat, amount, note, fuelLiters, fuelPrice, fuelMileage, fuelDisc);
        }
        Toast.makeText(this, "Сохранено", Toast.LENGTH_SHORT).show();
        finish();
    }
}
