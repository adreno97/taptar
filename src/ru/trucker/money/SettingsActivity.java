package ru.trucker.money;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends Activity {

    private LinearLayout zonesBox;
    private Spinner countSpinner;
    private List<EditText> priceEds = new ArrayList<>();
    private EditText extraPriceEt, extraStartEt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Настройки зон");

        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Util.dp(this, 16), Util.dp(this, 16), Util.dp(this, 16), Util.dp(this, 24));
        root.setBackgroundColor(0xFFF0F2F5);
        sv.addView(root);

        TextView hint = new TextView(this);
        hint.setText("Оплата за рейс зависит от зоны. Здесь можно изменить количество зон и цены за каждую. Возврат оплачивается дополнительно +50% от стоимости рейса.");
        hint.setTextSize(13);
        hint.setTextColor(0xFF607D8B);
        root.addView(hint);

        TextView h = new TextView(this);
        h.setText("Количество зон");
        h.setTextSize(15);
        h.setTextColor(0xFF37474F);
        h.setPadding(0, Util.dp(this, 12), 0, 0);
        root.addView(h);

        countSpinner = new Spinner(this);
        String[] opts = new String[Zones.MAX - Zones.MIN + 1];
        for (int i = 0; i < opts.length; i++) opts[i] = (i + Zones.MIN) + " зоны";
        countSpinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, opts));
        countSpinner.setBackgroundColor(0xFFFFFFFF);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Util.dp(this, 48));
        root.addView(countSpinner, sp);

        zonesBox = new LinearLayout(this);
        zonesBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(zonesBox);

        TextView h2 = new TextView(this);
        h2.setText("Количество точек выгрузки");
        h2.setTextSize(15);
        h2.setTextColor(0xFF37474F);
        h2.setPadding(0, Util.dp(this, 18), 0, 0);
        root.addView(h2);

        TextView hint2 = new TextView(this);
        hint2.setText("Точки выгрузки до указанного номера входят в стоимость зоны, с этого номера каждая точка оплачивается отдельно.");
        hint2.setTextSize(12);
        hint2.setTextColor(0xFF90A4AE);
        root.addView(hint2);

        extraPriceEt = makeRow(root, "Оплата за точку выгрузки (руб)");
        extraStartEt = makeRow(root, "Оплата точек выгрузки начиная с №");

        Button save = new Button(this);
        save.setText("Сохранить");
        save.setTextSize(16);
        save.setTextColor(0xFFFFFFFF);
        save.setBackgroundColor(0xFF1565C0);
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
        footer.setTextColor(0xFF90A4AE);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, Util.dp(this, 14), 0, Util.dp(this, 4));
        root.addView(footer);

        setContentView(sv);

        extraPriceEt.setText(String.valueOf(Zones.getExtraPrice(this) / 100.0).replace(".0", ""));
        extraStartEt.setText(String.valueOf(Zones.getExtraStart(this)));

        countSpinner.setSelection(Zones.getCount(this) - Zones.MIN);
        countSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                buildZones(pos + Zones.MIN);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
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
            name.setTextColor(0xFF37474F);
            row.addView(name, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            EditText price = new EditText(this);
            price.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            price.setTextSize(15);
            price.setText(String.valueOf(current[i] / 100.0).replace(".0", ""));
            price.setSingleLine(true);
            price.setBackgroundColor(0xFFFFFFFF);
            price.setPadding(Util.dp(this, 10), Util.dp(this, 8), Util.dp(this, 10), Util.dp(this, 8));
            LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            row.addView(price, pp);

            TextView cur = new TextView(this);
            cur.setText("  руб");
            cur.setTextColor(0xFF607D8B);
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
        name.setTextColor(0xFF37474F);
        row.addView(name, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        EditText ed = new EditText(this);
        ed.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        ed.setTextSize(15);
        ed.setSingleLine(true);
        ed.setBackgroundColor(0xFFFFFFFF);
        ed.setPadding(Util.dp(this, 10), Util.dp(this, 8), Util.dp(this, 10), Util.dp(this, 8));
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(ed, pp);
        root.addView(row);
        return ed;
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
        Toast.makeText(this, "Настройки сохранены", Toast.LENGTH_SHORT).show();
        finish();
    }
}
