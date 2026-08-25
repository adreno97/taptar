package ru.trucker.money;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Подпись PDF: открыть, поставить подпись, переместить, изменить размер, сохранить. */
public class SignActivity extends BaseActivity {

    private static final int REQ_PDF = 1;
    private static final int REQ_SAVE = 2;

    private Uri pdfUri;
    private ParcelFileDescriptor pfd;
    private PdfRenderer renderer;
    private int pageCount;
    private int current;

    private final Map<Integer, List<PdfSign.Place>> places = new HashMap<>();

    private TextView pageTv;
    private Button prevBtn, nextBtn, delBtn, clearBtn;
    private SignPageView pageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PdfSign.init(this);
        setTitle("Подпись в PDF");
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.bg(this));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setPadding(Util.dp(this, 8), Util.dp(this, 8), Util.dp(this, 8), Util.dp(this, 4));
        Button openBtn = btn("📄 Открыть PDF", Ui.accent(this));
        Button sigBtn = btn("✍ Подпись", Ui.brown(this));
        top.addView(openBtn, new LinearLayout.LayoutParams(0, Util.dp(this, 48), 1f));
        top.addView(sigBtn, new LinearLayout.LayoutParams(0, Util.dp(this, 48), 1f));
        root.addView(top);

        openBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { pickPdf(); }
        });
        sigBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(SignActivity.this, SignatureActivity.class));
            }
        });

        pageView = new SignPageView(this);
        root.addView(pageView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(Util.dp(this, 8), Util.dp(this, 6), Util.dp(this, 8), Util.dp(this, 6));
        prevBtn = navBtn("◀");
        nextBtn = navBtn("▶");
        pageTv = new TextView(this);
        pageTv.setText("Нет PDF");
        pageTv.setTextSize(15);
        pageTv.setTextColor(Ui.title(this));
        pageTv.setGravity(Gravity.CENTER);
        nav.addView(prevBtn, new LinearLayout.LayoutParams(0, Util.dp(this, 44), 1f));
        nav.addView(pageTv, new LinearLayout.LayoutParams(0, Util.dp(this, 44), 1.4f));
        nav.addView(nextBtn, new LinearLayout.LayoutParams(0, Util.dp(this, 44), 1f));
        root.addView(nav);

        prevBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { go(current - 1); }
        });
        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { go(current + 1); }
        });

        TextView hint = new TextView(this);
        hint.setText("Тап по странице — поставить подпись · перетащить — переместить · щипок — размер");
        hint.setTextSize(12);
        hint.setTextColor(Ui.sub(this));
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(Util.dp(this, 8), 0, Util.dp(this, 8), Util.dp(this, 4));
        root.addView(hint);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(Util.dp(this, 8), 0, Util.dp(this, 8), Util.dp(this, 8));
        delBtn = actionBtn("🗑 Удалить");
        clearBtn = actionBtn("Очистить стр.");
        Button clearAllBtn = actionBtn("Очистить всё");
        actions.addView(delBtn, new LinearLayout.LayoutParams(0, Util.dp(this, 44), 1f));
        actions.addView(clearBtn, new LinearLayout.LayoutParams(0, Util.dp(this, 44), 1f));
        actions.addView(clearAllBtn, new LinearLayout.LayoutParams(0, Util.dp(this, 44), 1f));
        root.addView(actions);

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setPadding(Util.dp(this, 8), 0, Util.dp(this, 8), Util.dp(this, 8));
        Button saveBtn = btn("💾 Сохранить PDF", Ui.income(this));
        bottom.addView(saveBtn, new LinearLayout.LayoutParams(0, Util.dp(this, 50), 1f));
        root.addView(bottom);

        setContentView(root);

        delBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (pageView.sel >= 0) {
                    pageView.places.remove(pageView.sel);
                    pageView.sel = -1;
                    pageView.invalidate();
                } else {
                    Toast.makeText(SignActivity.this,
                            "Сначала коснитесь подписи, которую нужно удалить", Toast.LENGTH_SHORT).show();
                }
            }
        });
        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                pageView.places.clear();
                pageView.sel = -1;
                pageView.invalidate();
            }
        });
        clearAllBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                places.clear();
                pageView.places = placesFor(current);
                pageView.sel = -1;
                pageView.invalidate();
            }
        });
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { createPdf(); }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (pageView != null) {
            pageView.sig = SignatureStore.load(this);
            pageView.invalidate();
        }
    }

    @Override
    protected void onDestroy() {
        closeRenderer();
        super.onDestroy();
    }

    // ---------- открытие PDF ----------

    private void pickPdf() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/pdf");
        startActivityForResult(i, REQ_PDF);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_PDF && res == RESULT_OK && data != null && data.getData() != null) {
            openPdf(data.getData());
        } else if (req == REQ_SAVE && res == RESULT_OK && data != null && data.getData() != null) {
            savePdf(data.getData());
        }
    }

    private void openPdf(Uri uri) {
        try {
            closeRenderer();
            pdfUri = uri;
            pfd = getContentResolver().openFileDescriptor(uri, "r");
            renderer = new PdfRenderer(pfd);
            pageCount = renderer.getPageCount();
            places.clear();
            current = 0;
            pageView.sig = SignatureStore.load(this);
            renderPage(0);
            Toast.makeText(this, "Страниц: " + pageCount, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка открытия PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void closeRenderer() {
        try {
            if (renderer != null) renderer.close();
            if (pfd != null) pfd.close();
        } catch (Exception ignored) {
        }
        renderer = null;
        pfd = null;
    }

    private void go(int idx) {
        if (renderer == null) return;
        if (idx < 0 || idx >= pageCount) return;
        current = idx;
        renderPage(idx);
    }

    private void renderPage(int idx) {
        try {
            if (pageView.page != null) {
                pageView.page.recycle();
                pageView.page = null;
            }
            PdfRenderer.Page pg = renderer.openPage(idx);
            float pw = pg.getWidth();
            float ph = pg.getHeight();
            float targetW = getResources().getDisplayMetrics().widthPixels * 1.2f;
            float scale = targetW / pw;
            int w = Math.max(1, Math.round(pw * scale));
            int h = Math.max(1, Math.round(ph * scale));
            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            pg.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            pg.close();
            pageView.page = bmp;
            pageView.places = placesFor(idx);
            pageView.sel = -1;
            pageView.invalidate();
            pageTv.setText((idx + 1) + " / " + pageCount);
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка рендера страницы: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private List<PdfSign.Place> placesFor(int idx) {
        List<PdfSign.Place> l = places.get(idx);
        if (l == null) {
            l = new ArrayList<>();
            places.put(idx, l);
        }
        return l;
    }

    // ---------- сохранение ----------

    private void confirmDeleteSignature(final int idx) {
        if (idx < 0 || idx >= pageView.places.size()) return;
        final PdfSign.Place p = pageView.places.get(idx);
        new android.app.AlertDialog.Builder(this)
                .setTitle("Удалить подпись")
                .setMessage("Удалить выбранную подпись?")
                .setPositiveButton("Удалить", new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface d, int w) {
                        pageView.places.remove(p);
                        pageView.sel = -1;
                        pageView.invalidate();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void createPdf() {
        if (renderer == null) {
            Toast.makeText(this, "Сначала откройте PDF", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!SignatureStore.exists(this)) {
            Toast.makeText(this, "Сначала загрузите подпись (✍ Подпись)", Toast.LENGTH_LONG).show();
            return;
        }
        int placed = 0;
        for (List<PdfSign.Place> l : places.values()) placed += l.size();
        if (placed == 0) {
            Toast.makeText(this, "Подписи не расставлены", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/pdf");
        i.putExtra(Intent.EXTRA_TITLE, "подписан-" + System.currentTimeMillis() + ".pdf");
        startActivityForResult(i, REQ_SAVE);
    }

    private void savePdf(final Uri dst) {
        Toast.makeText(this, "Сохранение…", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override public void run() {
                String msg;
                boolean ok = false;
                try {
                    Bitmap sig = SignatureStore.load(SignActivity.this);
                    OutputStream out = getContentResolver().openOutputStream(dst);
                    if (out == null) throw new Exception("Не удалось создать файл");
                    PdfSign.save(SignActivity.this, pdfUri, out, places, sig);
                    out.close();
                    ok = true;
                    msg = "PDF сохранён";
                } catch (Exception e) {
                    msg = "Ошибка: " + e.getMessage();
                }
                final boolean fOk = ok;
                final String fMsg = msg;
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        Toast.makeText(SignActivity.this, fMsg, Toast.LENGTH_LONG).show();
                        if (fOk) sharePdf(dst);
                    }
                });
            }
        }).start();
    }

    private void sharePdf(Uri uri) {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("application/pdf");
        share.putExtra(Intent.EXTRA_STREAM, uri);
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(Intent.createChooser(share, "Отправить подписанный PDF"));
        } catch (Exception e) {
            Toast.makeText(this, "Нет приложений для отправки", Toast.LENGTH_SHORT).show();
        }
    }

    // ---------- UI ----------

    private Button btn(String text, int color) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(Ui.buttonText(this));
        b.setBackground(Ui.round(this, color, 8));
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setPadding(0, 0, 0, 0);
        return b;
    }

    private Button navBtn(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(Ui.buttonText(this));
        b.setBackground(Ui.round(this, Ui.navBtn(this), 8));
        b.setAllCaps(false);
        b.setTextSize(16);
        return b;
    }

    private Button actionBtn(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(Ui.primary(this));
        b.setBackground(Ui.round(this, Ui.card(this), 8));
        b.setAllCaps(false);
        b.setTextSize(12);
        b.setPadding(Util.dp(this, 4), 0, Util.dp(this, 4), 0);
        return b;
    }

    // ---------- просмотр страницы ----------

    private class SignPageView extends View {

        Bitmap page;
        Bitmap sig;
        List<PdfSign.Place> places = new ArrayList<>();
        int sel = -1;

        private final Paint border = new Paint();
        private final Paint empty = new Paint();

        private float downX, downY;
        private float origNX, origNY;
        private boolean moved;
        private float initDist, initNW;
        private long downTime;

        private static final float OVERFLOW = 0.02f;

        SignPageView(Context ctx) {
            super(ctx);
            border.setStyle(Paint.Style.STROKE);
            border.setStrokeWidth(Util.dp(ctx, 2));
            border.setColor(0xFF1565C0);
            empty.setTextSize(Util.dp(ctx, 14));
            empty.setColor(Ui.sub(SignActivity.this));
            empty.setTextAlign(Paint.Align.CENTER);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawColor(Ui.bg(SignActivity.this));
            if (page == null) {
                canvas.drawText("Откройте PDF, чтобы начать", getWidth() / 2f, getHeight() / 2f, empty);
                return;
            }
            RectF r = fitRect(page.getWidth(), page.getHeight(), getWidth(), getHeight());
            canvas.drawBitmap(page, null, r, null);
            if (sig != null) {
                float aspect = (float) sig.getHeight() / (float) sig.getWidth();
                for (int i = 0; i < places.size(); i++) {
                    PdfSign.Place p = places.get(i);
                    float w = p.nw * r.width();
                    float h = w * aspect;
                    RectF sr = new RectF(r.left + p.nx * r.width(), r.top + p.ny * r.height(),
                            r.left + p.nx * r.width() + w, r.top + p.ny * r.height() + h);
                    canvas.drawBitmap(sig, null, sr, null);
                    if (i == sel) canvas.drawRect(sr, border);
                }
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            if (page == null || sig == null) return true;
            RectF r = fitRect(page.getWidth(), page.getHeight(), getWidth(), getHeight());
            int action = ev.getActionMasked();
            int pc = ev.getPointerCount();
            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    float nx = normX(ev.getX(0), r);
                    float ny = normY(ev.getY(0), r);
                    sel = hit(nx, ny);
                    downX = ev.getX(0);
                    downY = ev.getY(0);
                    if (sel >= 0) {
                        origNX = places.get(sel).nx;
                        origNY = places.get(sel).ny;
                    }
                    moved = false;
                    initDist = 0;
                    initNW = 0;
                    downTime = System.currentTimeMillis();
                    invalidate();
                    break;
                }
                case MotionEvent.ACTION_POINTER_DOWN: {
                    if (pc == 2 && sel >= 0) {
                        initDist = dist(ev);
                        initNW = places.get(sel).nw;
                    }
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    if (pc >= 2 && sel >= 0 && initDist > 0) {
                        float d = dist(ev);
                        if (d > 0) {
                            places.get(sel).nw = clamp(initNW * (d / initDist), 0.02f, 0.6f);
                            invalidate();
                        }
                    } else if (pc == 1 && sel >= 0) {
                        float dx = ev.getX(0) - downX;
                        float dy = ev.getY(0) - downY;
                        if (Math.abs(dx) > Util.dp(SignActivity.this, 3)
                                || Math.abs(dy) > Util.dp(SignActivity.this, 3)) moved = true;
                        if (moved) {
                            PdfSign.Place p = places.get(sel);
                            p.nx = clamp(origNX + dx / r.width(), -OVERFLOW, 1f - p.nw + OVERFLOW);
                            p.ny = clamp(origNY + dy / r.height(), -OVERFLOW,
                                    1f - p.nw * ((float) sig.getHeight() / (float) sig.getWidth()) + OVERFLOW);
                            invalidate();
                        }
                    }
                    break;
                }
                case MotionEvent.ACTION_UP: {
                    if (pc == 1 && !moved && initDist == 0) {
                        boolean longPress = System.currentTimeMillis() - downTime >= 500L;
                        if (sel >= 0) {
                            if (longPress) confirmDeleteSignature(sel);
                        } else if (!longPress) {
                            float nx = normX(ev.getX(0), r);
                            float ny = normY(ev.getY(0), r);
                            PdfSign.Place p = new PdfSign.Place(
                                    clamp(nx - 0.1f, -OVERFLOW, 1f - 0.2f + OVERFLOW),
                                    clamp(ny - 0.06f, -OVERFLOW, 1f - 0.2f + OVERFLOW),
                                    0.2f);
                            places.add(p);
                            sel = places.size() - 1;
                            invalidate();
                        }
                    }
                    initDist = 0;
                    initNW = 0;
                    break;
                }
                case MotionEvent.ACTION_CANCEL: {
                    initDist = 0;
                    initNW = 0;
                    break;
                }
            }
            return true;
        }

        private int hit(float nx, float ny) {
            float aspect = (float) sig.getHeight() / (float) sig.getWidth();
            for (int i = places.size() - 1; i >= 0; i--) {
                PdfSign.Place p = places.get(i);
                float w = p.nw, h = p.nw * aspect;
                if (nx >= p.nx && nx <= p.nx + w && ny >= p.ny && ny <= p.ny + h) return i;
            }
            return -1;
        }

        private float normX(float sx, RectF r) { return (sx - r.left) / r.width(); }
        private float normY(float sy, RectF r) { return (sy - r.top) / r.height(); }

        private float dist(MotionEvent ev) {
            float dx = ev.getX(0) - ev.getX(1);
            float dy = ev.getY(0) - ev.getY(1);
            return (float) Math.sqrt(dx * dx + dy * dy);
        }
    }

    private static RectF fitRect(int bw, int bh, int vw, int vh) {
        float s = Math.min(vw / (float) bw, vh / (float) bh);
        float w = bw * s, h = bh * s;
        float left = (vw - w) / 2f, top = (vh - h) / 2f;
        return new RectF(left, top, left + w, top + h);
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
