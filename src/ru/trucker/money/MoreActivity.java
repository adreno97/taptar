package ru.trucker.money;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MoreActivity extends BaseActivity {

    private DbHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new DbHelper(this);
        setTitle("Ещё");

        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Util.dp(this, 12), Util.dp(this, 12), Util.dp(this, 12), Util.dp(this, 20));
        root.setBackgroundColor(Ui.bg(this));
        sv.addView(root);

        row(root, "📜 История", "Все рейсы и расходы, поиск по периодам", new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(MoreActivity.this, HistoryActivity.class));
            }
        });
        row(root, "📥 Импорт Excel", "Загрузить рейсы из таблицы логиста (.xls / .xlsx)", new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(MoreActivity.this, ImportActivity.class));
            }
        });
        row(root, "📤 Экспорт Excel", "Выгрузить рейсы, расходы и ТО в файл .xlsx", new View.OnClickListener() {
            @Override public void onClick(View v) { exportExcel(); }
        });
        row(root, "📄 Отчёт PDF", "Полный отчёт: рейсы, расходы, обслуживание ТС", new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    java.io.File f = PdfExport.exportReport(MoreActivity.this, db);
                    PdfExport.share(MoreActivity.this, f);
                } catch (Exception e) {
                    Toast.makeText(MoreActivity.this, "Ошибка PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
        row(root, "☁ Облако и бэкапы", "Резервная копия в Supabase, ежедневный бэкап, восстановление по дате", new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(MoreActivity.this, CloudActivity.class));
            }
        });
        row(root, "⚙ Настройки", "Зоны, точки выгрузки, напоминания о ТО", new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(MoreActivity.this, SettingsActivity.class));
            }
        });

        TextView footer = new TextView(this);
        footer.setText("Разработчик: adreno97 · adreno97@mail.ru");
        footer.setTextSize(12);
        footer.setTextColor(Ui.sub(this));
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, Util.dp(this, 20), 0, Util.dp(this, 4));
        root.addView(footer);

        setContentView(sv);
    }

    private void exportExcel() {
        try {
            java.io.File f = XlsxExport.exportReport(this, db);
            PdfExport.share(this, f);
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка Excel: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void row(LinearLayout root, String title, String desc, View.OnClickListener onClick) {
        Button b = new Button(this);
        b.setBackground(Ui.round(this, Ui.card(this), 10));
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        b.setTextColor(Ui.primary(this));
        b.setPadding(Util.dp(this, 16), 0, Util.dp(this, 16), 0);
        b.setText(title + "\n" + desc);
        b.setTextSize(15);
        b.setLineSpacing(0f, 1.1f);
        b.setOnClickListener(onClick);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.bottomMargin = Util.dp(this, 8);
        root.addView(b, p);
    }
}
