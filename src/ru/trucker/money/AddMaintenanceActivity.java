package ru.trucker.money;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import java.util.Calendar;

public class AddMaintenanceActivity extends BaseActivity {

    private DbHelper db;
    private long editId = -1;
    private long date;
    private Button dateBtn;
    private EditText mileageEt, worksEt;
    private String initialState = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new DbHelper(this);
        editId = getIntent().getLongExtra("edit_id", -1);
        setTitle(editId >= 0 ? "Обслуживание · редактирование" : "Обслуживание ТС");

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
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        dp.topMargin = Util.dp(this, 8);
        root.addView(dateBtn, dp);
        dateBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { pickDate(); }
        });

        mileageEt = new EditText(this);
        mileageEt.setHint("Пробег, км");
        mileageEt.setInputType(InputType.TYPE_CLASS_NUMBER);
        mileageEt.setTextSize(16);
        mileageEt.setBackground(Ui.round(this, Ui.field(this), 8));
        mileageEt.setPadding(Util.dp(this, 10), Util.dp(this, 12), Util.dp(this, 10), Util.dp(this, 12));
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mp.topMargin = Util.dp(this, 8);
        root.addView(mileageEt, mp);

        worksEt = new EditText(this);
        worksEt.setHint("Что сделано (какие работы)");
        worksEt.setTextSize(15);
        worksEt.setGravity(android.view.Gravity.TOP);
        worksEt.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        worksEt.setMinLines(4);
        worksEt.setBackground(Ui.round(this, Ui.field(this), 8));
        worksEt.setPadding(Util.dp(this, 10), Util.dp(this, 12), Util.dp(this, 10), Util.dp(this, 12));
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        wp.topMargin = Util.dp(this, 8);
        root.addView(worksEt, wp);

        Button save = new Button(this);
        save.setText(editId >= 0 ? "Сохранить изменения" : "Добавить запись");
        save.setTextSize(16);
        save.setTextColor(Ui.buttonText(this));
        save.setBackground(Ui.round(this, Ui.accent(this), 8));
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
            del.setTextColor(Ui.expense(this));
            del.setBackground(Ui.round(this, Ui.dangerBg(this), 8));
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, Util.dp(this, 44));
            dlp.topMargin = Util.dp(this, 8);
            root.addView(del, dlp);
            del.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    db.deleteMaint(editId);
                    Toast.makeText(AddMaintenanceActivity.this, "Запись удалена", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        }

        setContentView(sv);

        if (editId >= 0) {
            DbHelper.Maint m = db.getMaintById(editId);
            if (m != null) {
                date = m.date;
                dateBtn.setText("Дата: " + Util.date(date));
                mileageEt.setText(String.valueOf(m.mileage));
                worksEt.setText(m.works);
            }
        }
        initialState = String.valueOf(date) + '|' + mileageEt.getText() + '|' + worksEt.getText();
    }

    @Override
    public void onBackPressed() {
        String cur = String.valueOf(date) + '|' + mileageEt.getText() + '|' + worksEt.getText();
        if (!initialState.equals(cur)) {
            new AlertDialog.Builder(this)
                    .setTitle("Закрыть без сохранения?")
                    .setMessage("В форме есть изменения. Они не будут сохранены.")
                    .setPositiveButton("Закрыть", new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface d, int w) { AddMaintenanceActivity.super.onBackPressed(); }
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        } else {
            super.onBackPressed();
        }
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
        String mStr = mileageEt.getText().toString().trim();
        if (mStr.isEmpty()) {
            Toast.makeText(this, "Укажите пробег", Toast.LENGTH_SHORT).show();
            return;
        }
        long mileage;
        try {
            mileage = Long.parseLong(mStr);
        } catch (Exception e) {
            Toast.makeText(this, "Некорректный пробег", Toast.LENGTH_SHORT).show();
            return;
        }
        String works = worksEt.getText().toString().trim();
        if (works.isEmpty()) {
            Toast.makeText(this, "Опишите выполненные работы", Toast.LENGTH_SHORT).show();
            return;
        }
        if (editId >= 0) db.updateMaint(editId, date, mileage, works);
        else db.addMaint(date, mileage, works);
        Toast.makeText(this, "Сохранено", Toast.LENGTH_SHORT).show();
        finish();
    }
}
