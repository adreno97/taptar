package ru.trucker.money;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.util.List;

public class ImportActivity extends Activity {

    private static final int REQ_PICK = 100;

    private DbHelper db;
    private TextView status;
    private Button importBtn;
    private List<XlsxImport.Row> parsed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new DbHelper(this);
        setTitle("Импорт рейсов из Excel");

        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Util.dp(this, 16), Util.dp(this, 16), Util.dp(this, 16), Util.dp(this, 24));
        root.setBackgroundColor(0xFFF0F2F5);
        sv.addView(root);

        TextView hint = new TextView(this);
        hint.setText("Выберите файл .xlsx от логиста. Приложение прочитает колонки «Номер СЛ», «Дата погрузки», «Маршрут» (зона), «Рейс» (ставка), «Доп.магазин», «Возврат» и «Сумма всего с НДС». Рейсы с уже существующим номером будут пропущены.");
        hint.setTextSize(13);
        hint.setTextColor(0xFF607D8B);
        root.addView(hint);

        Button pick = new Button(this);
        pick.setText("Выбрать файл .xlsx");
        pick.setTextSize(16);
        pick.setTextColor(0xFFFFFFFF);
        pick.setBackgroundColor(0xFF1565C0);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Util.dp(this, 50));
        pp.topMargin = Util.dp(this, 12);
        root.addView(pick, pp);
        pick.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                startActivityForResult(i, REQ_PICK);
            }
        });

        status = new TextView(this);
        status.setText("Файл ещё не выбран.");
        status.setTextSize(14);
        status.setTextColor(0xFF37474F);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sp.topMargin = Util.dp(this, 14);
        root.addView(status, sp);

        importBtn = new Button(this);
        importBtn.setText("Импортировать");
        importBtn.setTextSize(16);
        importBtn.setTextColor(0xFFFFFFFF);
        importBtn.setBackgroundColor(0xFF2E7D32);
        importBtn.setEnabled(false);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Util.dp(this, 50));
        ip.topMargin = Util.dp(this, 12);
        root.addView(importBtn, ip);
        importBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { doImport(); }
        });

        setContentView(sv);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri == null) return;
            String name = fileName(uri);
            try {
                InputStream is = getContentResolver().openInputStream(uri);
                if (name != null && name.toLowerCase(java.util.Locale.US).endsWith(".xls")) {
                    parsed = XlsImport.parse(is);
                } else {
                    parsed = XlsxImport.parse(is);
                }
                is.close();
                long total = 0;
                long fuelTotal = 0;
                int dups = 0;
                for (XlsxImport.Row r : parsed) {
                    if (db.hasTripNumber(r.number)) dups++;
                    else total += revOf(r);
                    fuelTotal += r.fuelKop;
                }
                StringBuilder sb = new StringBuilder();
                sb.append("Файл: ").append(name).append("\n");
                sb.append("Найдено рейсов: ").append(parsed.size()).append("\n");
                if (dups > 0) sb.append("Уже есть в базе (пропустятся): ").append(dups).append("\n");
                sb.append("К импорту: ").append(parsed.size() - dups).append("\n");
                if (parsed.size() - dups > 0) {
                    sb.append("Итоговая сумма рейсов: ").append(Util.rub(total)).append("\n");
                    if (fuelTotal > 0) {
                        sb.append("Топливо (из реестра): ").append(Util.rub(fuelTotal))
                                .append(" — добавится расходом\n");
                    }
                    sb.append("\n");
                    int show = Math.min(parsed.size(), 5);
                    for (int i = 0; i < show; i++) {
                        XlsxImport.Row r = parsed.get(i);
                        sb.append("• ").append(r.number).append(" · ").append(Util.date(r.date))
                                .append(" · зона ").append(r.zone + 1).append(" · ")
                                .append(Util.rub(revOf(r))).append("\n");
                    }
                    if (parsed.size() > show) sb.append("… ещё ").append(parsed.size() - show).append(" рейсов\n");
                }
                status.setText(sb.toString());
                importBtn.setEnabled(parsed.size() - dups > 0);
                importBtn.setText("Импортировать " + (parsed.size() - dups) + " рейсов");
            } catch (Exception e) {
                status.setText("Ошибка чтения файла:\n" + e.getMessage());
                Toast.makeText(this, "Не удалось прочитать файл", Toast.LENGTH_LONG).show();
            }
        }
    }

    private String fileName(Uri uri) {
        String name = uri.getLastPathSegment();
        try {
            Cursor c = getContentResolver().query(uri, null, null, null, null);
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) name = c.getString(idx);
            }
            if (c != null) c.close();
        } catch (Exception ignored) {
        }
        return name == null ? "файл" : name;
    }

    private long revOf(XlsxImport.Row r) {
        long rev = r.baseKop + (r.isReturn ? r.baseKop / 2 : 0);
        if (r.extraCount > 0) {
            long ep = r.extraPriceKop > 0 ? r.extraPriceKop : Zones.getExtraPrice(this);
            rev += r.extraCount * ep;
        }
        return rev;
    }

    private void doImport() {
        if (parsed == null) return;
        int added = 0, skipped = 0;
        long fuelTotal = 0;
        long lastDate = 0;
        for (XlsxImport.Row r : parsed) {
            if (db.hasTripNumber(r.number)) {
                skipped++;
                continue;
            }
            long rev = revOf(r);
            db.addTrip(r.number, r.date, r.zone, r.isReturn, r.baseKop, r.numPoints, rev, "");
            fuelTotal += r.fuelKop;
            if (r.date > lastDate) lastDate = r.date;
            added++;
        }
        StringBuilder msg = new StringBuilder("Импортировано: " + added);
        if (skipped > 0) msg.append(", дублей пропущено: ").append(skipped);
        if (fuelTotal > 0) {
            db.addExpense(lastDate > 0 ? lastDate : System.currentTimeMillis(),
                    "Заправка", fuelTotal, "Топливо (из реестра)");
            msg.append(".\nТопливо добавлено расходом: ").append(Util.rub(fuelTotal));
        }
        Toast.makeText(this, msg.toString(), Toast.LENGTH_LONG).show();
        finish();
    }
}
