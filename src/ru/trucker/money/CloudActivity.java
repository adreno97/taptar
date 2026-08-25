package ru.trucker.money;

import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/** Облачная синхронизация: вход, синк, восстановление по дате, ежедневный бэкап. */
public class CloudActivity extends BaseActivity {

    private EditText emailEt, passEt;
    private TextView statusTv;
    private Button syncBtn, restoreBtn;
    private android.widget.Switch dailyCb;
    private Button timeBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Облако и бэкапы");

        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Util.dp(this, 16), Util.dp(this, 16), Util.dp(this, 16), Util.dp(this, 24));
        root.setBackgroundColor(Ui.bg(this));
        sv.addView(root);

        TextView hint = new TextView(this);
        hint.setText("Резервная копия в Supabase. Каждый синк сохраняет отдельную копию с датой — старые не теряются. Синхронизация также происходит при закрытии приложения и по расписанию.");
        hint.setTextSize(13);
        hint.setTextColor(Ui.label(this));
        root.addView(hint);

        emailEt = makeRow(root, "Email аккаунта (Supabase)");
        emailEt.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        passEt = makeRow(root, "Пароль");
        passEt.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        statusTv = new TextView(this);
        statusTv.setTextSize(13);
        statusTv.setTextColor(Ui.label(this));
        statusTv.setPadding(0, Util.dp(this, 8), 0, 0);
        root.addView(statusTv);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, Util.dp(this, 8), 0, 0);
        syncBtn = new Button(this);
        syncBtn.setText("Синхронизировать");
        syncBtn.setAllCaps(false);
        syncBtn.setTextSize(14);
        syncBtn.setTextColor(Ui.buttonText(this));
        syncBtn.setBackground(Ui.round(this, Ui.accent(this), 8));
        restoreBtn = new Button(this);
        restoreBtn.setText("Восстановить");
        restoreBtn.setAllCaps(false);
        restoreBtn.setTextSize(14);
        restoreBtn.setTextColor(Ui.buttonText(this));
        restoreBtn.setBackground(Ui.round(this, Ui.navBtn(this), 8));
        btnRow.addView(syncBtn, new LinearLayout.LayoutParams(0, Util.dp(this, 48), 1f));
        btnRow.addView(restoreBtn, new LinearLayout.LayoutParams(0, Util.dp(this, 48), 1f));
        root.addView(btnRow);

        TextView h = new TextView(this);
        h.setText("Ежедневный бэкап");
        h.setTextSize(15);
        h.setTextColor(Ui.title(this));
        h.setPadding(0, Util.dp(this, 18), 0, 0);
        root.addView(h);

        dailyCb = new android.widget.Switch(this);
        dailyCb.setText("Автоматическая копия каждый день");
        dailyCb.setTextSize(15);
        root.addView(dailyCb);

        timeBtn = new Button(this);
        timeBtn.setAllCaps(false);
        timeBtn.setTextSize(13);
        timeBtn.setTextColor(Ui.accentText(this));
        timeBtn.setBackground(Ui.round(this, Ui.card(this), 8));
        root.addView(timeBtn, lpWrap());
        timeBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { pickTime(); }
        });

        Button save = new Button(this);
        save.setText("Сохранить");
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

        setContentView(sv);

        emailEt.setText(SyncManager.email(this));
        passEt.setText(SyncManager.password(this));
        updateStatus();
        dailyCb.setChecked(DailyBackup.isEnabled(this));
        timeBtn.setText("Время: " + DailyBackup.timeText(this));

        syncBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                saveCredentials();
                if (!SyncManager.hasCredentials(CloudActivity.this)) {
                    Toast.makeText(CloudActivity.this, "Укажите email и пароль аккаунта", Toast.LENGTH_SHORT).show();
                    return;
                }
                SyncManager.sync(CloudActivity.this, new SyncManager.Callback() {
                    @Override public void done(boolean ok, String msg) {
                        updateStatus();
                        Toast.makeText(CloudActivity.this, msg, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        restoreBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                saveCredentials();
                if (!SyncManager.hasCredentials(CloudActivity.this)) {
                    Toast.makeText(CloudActivity.this, "Укажите email и пароль аккаунта", Toast.LENGTH_SHORT).show();
                    return;
                }
                SyncManager.listBackups(CloudActivity.this, new SyncManager.ListCallback() {
                    @Override public void done(boolean ok, String msg, String listJson) {
                        if (!ok) {
                            Toast.makeText(CloudActivity.this, "Ошибка: " + msg, Toast.LENGTH_LONG).show();
                            return;
                        }
                        showRestorePicker(listJson);
                    }
                });
            }
        });

        dailyCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton b, boolean checked) {
                DailyBackup.setEnabled(CloudActivity.this, checked);
                if (checked) DailyBackup.schedule(CloudActivity.this);
                else DailyBackup.cancel(CloudActivity.this);
            }
        });
    }

    private void onSave() {
        saveCredentials();
        DailyBackup.setEnabled(this, dailyCb.isChecked());
        if (dailyCb.isChecked()) DailyBackup.schedule(this);
        else DailyBackup.cancel(this);
        Toast.makeText(this, "Сохранено", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void pickTime() {
        android.app.TimePickerDialog d = new android.app.TimePickerDialog(this,
                new android.app.TimePickerDialog.OnTimeSetListener() {
                    @Override public void onTimeSet(android.widget.TimePicker v, int h, int m) {
                        DailyBackup.setTime(CloudActivity.this, h, m);
                        timeBtn.setText("Время: " + DailyBackup.timeText(CloudActivity.this));
                        if (DailyBackup.isEnabled(CloudActivity.this)) DailyBackup.schedule(CloudActivity.this);
                    }
                },
                DailyBackup.hour(this), DailyBackup.minute(this), true);
        d.show();
    }

    private void showRestorePicker(String listJson) {
        try {
            org.json.JSONArray arr = new org.json.JSONArray(listJson);
            final List<String> names = new ArrayList<>();
            final List<String> labels = new ArrayList<>();
            names.add(SyncManager.OBJECT);
            labels.add("Последняя (текущая)");
            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject o = arr.getJSONObject(i);
                names.add(o.optString("name"));
                labels.add(o.optString("label"));
            }
            if (names.size() == 1) {
                Toast.makeText(this, "В облаке пока нет копий с датой", Toast.LENGTH_LONG).show();
                return;
            }
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Восстановить из облака")
                    .setSingleChoiceItems(labels.toArray(new String[0]), 0, new android.content.DialogInterface.OnClickListener() {
                        @Override public void onClick(android.content.DialogInterface d, int which) {
                            d.dismiss();
                            confirmRestore(names.get(which));
                        }
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка загрузки списка копий", Toast.LENGTH_LONG).show();
        }
    }

    private void confirmRestore(final String objectName) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Восстановить")
                .setMessage("Все данные на устройстве будут заменены выбранной копией. Продолжить?")
                .setPositiveButton("Восстановить", new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface d, int w) {
                        SyncManager.restoreObject(CloudActivity.this, objectName, new SyncManager.Callback() {
                            @Override public void done(boolean ok, String msg) {
                                updateStatus();
                                Toast.makeText(CloudActivity.this, msg, Toast.LENGTH_LONG).show();
                                recreate();
                            }
                        });
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void saveCredentials() {
        String e = emailEt.getText().toString().trim();
        String p = passEt.getText().toString();
        if (!e.isEmpty() && !p.isEmpty()) {
            SyncManager.setCredentials(this, e, p);
        }
    }

    private void updateStatus() {
        String last = SyncManager.lastSyncText(this);
        statusTv.setText(last.isEmpty() ? "Ещё не синхронизировано"
                : "Последняя синхронизация: " + last);
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
        ed.setTextSize(15);
        ed.setSingleLine(true);
        ed.setBackground(Ui.round(this, Ui.field(this), 8));
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
}
