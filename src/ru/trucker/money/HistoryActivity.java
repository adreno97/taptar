package ru.trucker.money;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends Activity {

    private DbHelper db;
    private boolean allTime = false;
    private int typeFilter = 0;
    private ListView listView;
    private RecordsAdapter adapter;
    private List<DbHelper.Record> data = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Ui.dark(this)) setTheme(android.R.style.Theme_Material);
        super.onCreate(savedInstanceState);
        db = new DbHelper(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.bg(this));

        LinearLayout filters = new LinearLayout(this);
        filters.setOrientation(LinearLayout.HORIZONTAL);
        filters.setPadding(Util.dp(this, 8), Util.dp(this, 8), Util.dp(this, 8), Util.dp(this, 8));
        filters.setBackgroundColor(Ui.card(this));

        final Spinner period = new Spinner(this);
        period.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,
                new String[]{"Текущий месяц", "Все время"}));
        filters.addView(period, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        final Spinner type = new Spinner(this);
        type.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,
                new String[]{"Все", "Доходы", "Расходы"}));
        filters.addView(type, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(filters);

        listView = new ListView(this);
        listView.setDividerHeight(0);
        root.addView(listView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);

        AdapterView.OnItemSelectedListener l = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                allTime = period.getSelectedItemPosition() == 1;
                typeFilter = type.getSelectedItemPosition();
                reload();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        };
        period.setOnItemSelectedListener(l);
        type.setOnItemSelectedListener(l);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> p, View v, int pos, long id) {
                if (pos >= 0 && pos < data.size()) showRecordActions(data.get(pos));
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override public boolean onItemLongClick(AdapterView<?> p, View v, int pos, long id) {
                final DbHelper.Record r = data.get(pos);
                new AlertDialog.Builder(HistoryActivity.this)
                        .setTitle("Удалить запись?")
                        .setMessage(Util.date(r.date) + " · " + r.title + " · " + Util.rub(r.amount))
                        .setPositiveButton("Удалить", new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface d, int w) {
                                db.deleteRecord(r.id, r.income);
                                reload();
                            }
                        })
                        .setNegativeButton("Отмена", null)
                        .show();
                return true;
            }
        });
    }

    private void showRecordActions(final DbHelper.Record r) {
        String title = r.income ? "Рейс " + r.number : r.category;
        new AlertDialog.Builder(HistoryActivity.this)
                .setTitle(title)
                .setMessage(Util.date(r.date) + " · " + Util.rub(r.amount))
                .setItems(new String[]{"Редактировать", "Дублировать"}, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) {
                        if (which == 0) {
                            Intent i = new Intent(HistoryActivity.this, AddActivity.class);
                            i.putExtra("mode", r.income ? "income" : "expense");
                            i.putExtra("edit_id", r.id);
                            i.putExtra("edit_income", r.income);
                            startActivity(i);
                        } else {
                            if (!r.income) {
                                db.addExpense(r.date, r.category, r.amount, r.note, r.liters, r.pricePerLiter, r.mileage, r.discount);
                            } else {
                                String base = r.number;
                                String num = base;
                                int n = 2;
                                while (db.hasTripNumber(num)) {
                                    num = base + " (" + n + ")";
                                    n++;
                                }
                                db.addTrip(num, r.date, r.zone, r.isReturn, r.basePrice, r.numPoints, r.amount, r.note);
                            }
                            Toast.makeText(HistoryActivity.this, "Запись продублирована", Toast.LENGTH_SHORT).show();
                            reload();
                        }
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void reload() {
        data = db.getRecords(allTime, typeFilter);
        adapter = new RecordsAdapter(this, new java.util.ArrayList<Object>(data));
        listView.setAdapter(adapter);
    }
}
