package ru.trucker.money;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

/** Импорт подписи и очистка белого фона. */
public class SignatureActivity extends BaseActivity {

    private static final int REQ_PICK = 1;
    private static final int REQ_IMPORT = 2;

    private ImageView preview;
    private TextView threshTv;
    private SeekBar thresh;
    private Bitmap source;
    private int threshold = 230;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Подпись");

        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Util.dp(this, 16), Util.dp(this, 16), Util.dp(this, 16), Util.dp(this, 24));
        root.setBackgroundColor(Ui.bg(this));
        sv.addView(root);

        TextView hint = new TextView(this);
        hint.setText("Выберите фото подписи на белом фоне. Приложение уберёт белый фон — останется прозрачная подпись, которую можно ставить на любые документы.");
        hint.setTextSize(13);
        hint.setTextColor(Ui.label(this));
        root.addView(hint);

        Button pick = new Button(this);
        pick.setText("📷 Выбрать фото подписи");
        pick.setAllCaps(false);
        pick.setTextSize(15);
        pick.setTextColor(Ui.buttonText(this));
        pick.setBackground(Ui.round(this, Ui.accent(this), 8));
        root.addView(pick, lpWrap());
        pick.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { pickImage(); }
        });

        preview = new ImageView(this);
        preview.setBackgroundColor(0xFFE0E0E0);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Util.dp(this, 220));
        plp.topMargin = Util.dp(this, 12);
        preview.setLayoutParams(plp);
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        root.addView(preview);

        threshTv = new TextView(this);
        threshTv.setTextSize(13);
        threshTv.setTextColor(Ui.label(this));
        threshTv.setPadding(0, Util.dp(this, 10), 0, 0);
        root.addView(threshTv);

        thresh = new SeekBar(this);
        thresh.setMax(255);
        thresh.setProgress(threshold);
        root.addView(thresh, lpWrap());
        thresh.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar b, int v, boolean fromUser) {
                threshold = v;
                updateThreshText();
                updatePreview();
            }
            @Override public void onStartTrackingTouch(SeekBar b) {}
            @Override public void onStopTrackingTouch(SeekBar b) {}
        });

        Button saveBtn = new Button(this);
        saveBtn.setText("💾 Сохранить подпись");
        saveBtn.setAllCaps(false);
        saveBtn.setTextSize(16);
        saveBtn.setTextColor(Ui.buttonText(this));
        saveBtn.setBackground(Ui.round(this, Ui.income(this), 8));
        root.addView(saveBtn, lpWrap());
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { saveSignature(); }
        });

        LinearLayout ioRow = new LinearLayout(this);
        ioRow.setOrientation(LinearLayout.HORIZONTAL);
        ioRow.setPadding(0, Util.dp(this, 10), 0, 0);
        Button expBtn = new Button(this);
        expBtn.setText("📤 Экспорт");
        expBtn.setAllCaps(false);
        expBtn.setTextSize(14);
        expBtn.setTextColor(Ui.buttonText(this));
        expBtn.setBackground(Ui.round(this, Ui.navBtn(this), 8));
        Button impBtn = new Button(this);
        impBtn.setText("📥 Импорт");
        impBtn.setAllCaps(false);
        impBtn.setTextSize(14);
        impBtn.setTextColor(Ui.buttonText(this));
        impBtn.setBackground(Ui.round(this, Ui.navBtn(this), 8));
        ioRow.addView(expBtn, new LinearLayout.LayoutParams(0, Util.dp(this, 46), 1f));
        ioRow.addView(impBtn, new LinearLayout.LayoutParams(0, Util.dp(this, 46), 1f));
        root.addView(ioRow);

        expBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { exportSignature(); }
        });
        impBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.setType("image/*");
                startActivityForResult(i, REQ_IMPORT);
            }
        });

        TextView status = new TextView(this);
        status.setTextSize(13);
        status.setTextColor(Ui.label(this));
        status.setPadding(0, Util.dp(this, 10), 0, 0);
        root.addView(status);
        status.setText(SignatureStore.exists(this)
                ? "Подпись уже сохранена. Новый импорт заменит её."
                : "Подпись ещё не сохранена.");

        setContentView(sv);
        updateThreshText();
    }

    private void pickImage() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        startActivityForResult(i, REQ_PICK);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_PICK && res == RESULT_OK && data != null && data.getData() != null) {
            try {
                Uri uri = data.getData();
                Bitmap bmp = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
                if (bmp == null) throw new Exception();
                source = scaleDown(bmp, 1400);
                if (source != bmp) bmp.recycle();
                updatePreview();
            } catch (Exception e) {
                Toast.makeText(this, "Не удалось открыть изображение", Toast.LENGTH_LONG).show();
            }
        } else if (req == REQ_IMPORT && res == RESULT_OK && data != null && data.getData() != null) {
            try {
                Uri uri = data.getData();
                Bitmap bmp = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
                if (bmp == null) throw new Exception();
                SignatureStore.save(this, bmp);
                if (bmp != null) bmp.recycle();
                Toast.makeText(this, "Подпись импортирована", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Не удалось импортировать подпись", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void exportSignature() {
        if (!SignatureStore.exists(this)) {
            Toast.makeText(this, "Подпись не загружена", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            java.io.File dir = new java.io.File(getFilesDir(), "pdf");
            if (!dir.exists()) dir.mkdirs();
            java.io.File copy = new java.io.File(dir, "signature.png");
            java.io.FileInputStream in = new java.io.FileInputStream(SignatureStore.file(this));
            java.io.FileOutputStream out = new java.io.FileOutputStream(copy);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            in.close();
            out.close();

            Uri uri = Uri.parse("content://ru.trucker.money.pdf/signature.png");
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("image/png");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, "Экспорт подписи"));
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка экспорта: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private Bitmap scaleDown(Bitmap b, int max) {
        int w = b.getWidth(), h = b.getHeight();
        float f = Math.max(w, h);
        if (f <= max) return b;
        float s = max / f;
        return Bitmap.createScaledBitmap(b, Math.round(w * s), Math.round(h * s), true);
    }

    private void updateThreshText() {
        threshTv.setText("Чистота фона: " + threshold + "  (ниже — сохранит больше, выше — чище)");
    }

    private void updatePreview() {
        if (source == null) {
            preview.setImageBitmap(null);
            return;
        }
        preview.setImageBitmap(removeBackground(source, threshold));
    }

    private Bitmap removeBackground(Bitmap src, int t) {
        int w = src.getWidth(), h = src.getHeight();
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        int[] px = new int[w * h];
        src.getPixels(px, 0, w, 0, 0, w, h);
        int lo = t - 48;
        for (int i = 0; i < px.length; i++) {
            int c = px[i];
            int r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
            int min = Math.min(r, Math.min(g, b));
            if (min >= t) {
                px[i] = 0x00000000;
            } else if (min >= lo) {
                int a = (int) (255f * (t - min) / 48f);
                px[i] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }
        out.setPixels(px, 0, w, 0, 0, w, h);
        return out;
    }

    private void saveSignature() {
        if (source == null) {
            Toast.makeText(this, "Сначала выберите фото", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            SignatureStore.save(this, removeBackground(source, threshold));
            Toast.makeText(this, "Подпись сохранена", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка сохранения: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private LinearLayout.LayoutParams lpWrap() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = Util.dp(this, 10);
        return p;
    }
}
