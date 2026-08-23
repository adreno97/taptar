package ru.trucker.money;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MaintenanceActivity extends BaseActivity {

    private DbHelper db;
    private ListView listView;
    private List<DbHelper.Maint> data = new ArrayList<>();
    private MaintAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new DbHelper(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Util.dp(this, 12), Util.dp(this, 12), Util.dp(this, 12), 0);
        root.setBackgroundColor(Ui.bg(this));

        TextView h = new TextView(this);
        h.setText("Обслуживание ТС");
        h.setTextSize(18);
        h.setTextColor(Ui.title(this));
        h.setTypeface(h.getTypeface(), Typeface.BOLD);
        root.addView(h);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, Util.dp(this, 10), 0, 0);
        Button add = btn("+ Запись", Ui.accent(this));
        Button pdf = btn("Экспорт PDF", Ui.brown(this));
        btnRow.addView(add);
        btnRow.addView(pdf);
        root.addView(btnRow);

        listView = new ListView(this);
        listView.setDivider(new android.graphics.drawable.ColorDrawable(Ui.bg(this)));
        listView.setDividerHeight(Util.dp(this, 6));
        listView.setBackgroundColor(Ui.bg(this));
        listView.setPadding(Util.dp(this, 6), Util.dp(this, 4), Util.dp(this, 6), Util.dp(this, 4));
        listView.setClipToPadding(false);
        root.addView(listView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);

        add.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(MaintenanceActivity.this, AddMaintenanceActivity.class));
            }
        });
        pdf.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { exportPdf(); }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> p, View v, int pos, long id) {
                Intent i = new Intent(MaintenanceActivity.this, AddMaintenanceActivity.class);
                i.putExtra("edit_id", data.get(pos).id);
                startActivity(i);
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override public boolean onItemLongClick(AdapterView<?> p, View v, int pos, long id) {
                final DbHelper.Maint m = data.get(pos);
                new AlertDialog.Builder(MaintenanceActivity.this)
                        .setTitle("Удалить запись?")
                        .setMessage(Util.date(m.date) + " · " + m.mileage + " км")
                        .setPositiveButton("Удалить", new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface d, int w) {
                                db.deleteMaint(m.id);
                                reload();
                            }
                        })
                        .setNegativeButton("Отмена", null)
                        .show();
                return true;
            }
        });
    }

    private void exportPdf() {
        try {
            File f = PdfExport.exportMaintenance(this, db);
            PdfExport.share(this, f);
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка создания PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private Button btn(String text, int color) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(Ui.buttonText(this));
        b.setBackground(Ui.round(MaintenanceActivity.this, color, 8));
        b.setTextSize(14);
        b.setLayoutParams(new LinearLayout.LayoutParams(0, Util.dp(this, 46), 1f));
        return b;
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        data = db.getMaintAll();
        adapter = new MaintAdapter(data);
        listView.setAdapter(adapter);
    }

    private class MaintAdapter extends BaseAdapter {
        private final List<DbHelper.Maint> items;
        MaintAdapter(List<DbHelper.Maint> items) { this.items = items; }
        @Override public int getCount() { return items.size(); }
        @Override public Object getItem(int i) { return items.get(i); }
        @Override public long getItemId(int i) { return i; }
        @Override public View getView(int pos, View convert, ViewGroup parent) {
            LinearLayout row;
            if (convert == null) {
                row = new LinearLayout(MaintenanceActivity.this);
                row.setOrientation(LinearLayout.VERTICAL);
                row.setPadding(Util.dp(MaintenanceActivity.this, 14), Util.dp(MaintenanceActivity.this, 10),
                        Util.dp(MaintenanceActivity.this, 14), Util.dp(MaintenanceActivity.this, 10));
                row.setBackground(Ui.round(MaintenanceActivity.this, Ui.card(MaintenanceActivity.this), 10));
            } else {
                row = (LinearLayout) convert;
                row.removeAllViews();
            }
            DbHelper.Maint m = items.get(pos);

            LinearLayout top = new LinearLayout(MaintenanceActivity.this);
            top.setOrientation(LinearLayout.HORIZONTAL);
            top.setGravity(Gravity.CENTER_VERTICAL);
            TextView title = new TextView(MaintenanceActivity.this);
            title.setText(Util.date(m.date) + "  ·  " + m.mileage + " км");
            title.setTextSize(15);
            title.setTextColor(Ui.primary(MaintenanceActivity.this));
            title.setTypeface(title.getTypeface(), Typeface.BOLD);
            top.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(top);

            TextView works = new TextView(MaintenanceActivity.this);
            works.setText(m.works == null || m.works.isEmpty() ? "—" : m.works);
            works.setTextSize(13);
            works.setTextColor(Ui.gray(MaintenanceActivity.this));
            row.addView(works);

            return row;
        }
    }
}
