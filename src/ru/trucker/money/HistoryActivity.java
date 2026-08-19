package ru.trucker.money;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;

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
        super.onCreate(savedInstanceState);
        db = new DbHelper(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFF0F2F5);

        LinearLayout filters = new LinearLayout(this);
        filters.setOrientation(LinearLayout.HORIZONTAL);
        filters.setPadding(Util.dp(this, 8), Util.dp(this, 8), Util.dp(this, 8), Util.dp(this, 8));
        filters.setBackgroundColor(0xFFFFFFFF);

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

    private void reload() {
        data = db.getRecords(allTime, typeFilter);
        adapter = new RecordsAdapter(this, data);
        listView.setAdapter(adapter);
    }
}
