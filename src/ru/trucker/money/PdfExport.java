package ru.trucker.money;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class PdfExport {

    private static final float M = 36f;
    private static final float BOTTOM = 806f;

    public static byte[] fontBytes(Context ctx) throws Exception {
        InputStream is = ctx.getAssets().open("fonts/DejaVuSans.ttf");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
        is.close();
        return bos.toByteArray();
    }

    public static File exportMaintenance(Context ctx, DbHelper db) throws Exception {
        MiniPdf pdf = new MiniPdf(fontBytes(ctx));
        pdf.setTitle("Обслуживание ТС");
        String[] headers = {"№", "Дата", "Пробег, км", "Выполненные работы"};
        float[] w = {24f, 72f, 78f, 370f};
        List<String[]> rows = new ArrayList<>();
        int i = 1;
        for (DbHelper.Maint m : db.getMaintAll()) {
            rows.add(new String[]{String.valueOf(i++), Util.date(m.date), String.valueOf(m.mileage), m.works});
        }
        drawTable(pdf, "Обслуживание ТС", headers, w, rows);
        return save(ctx, pdf, "to_");
    }

    public static File exportReport(Context ctx, DbHelper db) throws Exception {
        MiniPdf pdf = new MiniPdf(fontBytes(ctx));
        pdf.setTitle("Отчёт Таптар");

        String[] hT = {"№", "Номер рейса", "Дата", "Зона", "Возврат", "Выгрузка", "Сумма, руб"};
        float[] wT = {22f, 66f, 64f, 90f, 46f, 40f, 60f};
        List<String[]> rT = new ArrayList<>();
        int i = 1;
        for (DbHelper.Record r : db.getRecords(true, 1)) {
            rT.add(new String[]{String.valueOf(i++), r.number, Util.date(r.date),
                    Zones.shortName(r.zone), r.isReturn ? "Да" : "Нет", String.valueOf(r.numPoints),
                    String.format(java.util.Locale.US, "%.2f", r.amount / 100.0)});
        }
        drawTable(pdf, "Рейсы", hT, wT, rT);

        String[] hE = {"№", "Дата", "Категория", "Описание", "Сумма, руб"};
        float[] wE = {24f, 72f, 110f, 250f, 70f};
        List<String[]> rE = new ArrayList<>();
        i = 1;
        for (DbHelper.Record r : db.getRecords(true, 2)) {
            rE.add(new String[]{String.valueOf(i++), Util.date(r.date), r.title, r.note,
                    String.format(java.util.Locale.US, "%.2f", r.amount / 100.0)});
        }
        drawTable(pdf, "Расходы", hE, wE, rE);

        String[] hM = {"№", "Дата", "Пробег, км", "Выполненные работы"};
        float[] wM = {24f, 72f, 78f, 370f};
        List<String[]> rM = new ArrayList<>();
        i = 1;
        for (DbHelper.Maint m : db.getMaintAll()) {
            rM.add(new String[]{String.valueOf(i++), Util.date(m.date), String.valueOf(m.mileage), m.works});
        }
        drawTable(pdf, "Обслуживание ТС", hM, wM, rM);

        return save(ctx, pdf, "report_");
    }

    private static void drawTable(MiniPdf pdf, String title, String[] headers, float[] w, List<String[]> rows) {
        pdf.addPage();
        float y = 40f;
        pdf.setColor(0f, 0f, 0f);
        pdf.text(title, M, y, 16);
        y += 20;
        pdf.text("Сформировано: " + Util.date(System.currentTimeMillis()), M, y, 9);
        y += 14;

        y = header(pdf, headers, w, y);

        for (String[] row : rows) {
            List<List<String>> cells = new ArrayList<>();
            int maxLines = 1;
            for (int c = 0; c < headers.length; c++) {
                List<String> lines = pdf.wrap(row[c], w[c] - 8, 9);
                cells.add(lines);
                if (lines.size() > maxLines) maxLines = lines.size();
            }
            float lh = pdf.lineHeight(9);
            float rowH = maxLines * lh + 6;
            if (y + rowH > BOTTOM) {
                pdf.addPage();
                y = 40f;
                y = header(pdf, headers, w, y);
            }
            float x = M;
            for (int c = 0; c < headers.length; c++) {
                pdf.setColor(0f, 0f, 0f);
                pdf.rect(x, y, w[c], rowH, 0.5f);
                List<String> lines = cells.get(c);
                float ly = y + 3;
                for (String line : lines) {
                    pdf.text(line, x + 4, ly, 9);
                    ly += lh;
                }
                x += w[c];
            }
            y += rowH;
        }
    }

    private static float header(MiniPdf pdf, String[] headers, float[] w, float y) {
        float x = M;
        pdf.setColor(0.85f, 0.85f, 0.85f);
        pdf.fillRect(M, y, total(w), pdf.lineHeight(9) + 6);
        pdf.setColor(0f, 0f, 0f);
        for (int c = 0; c < headers.length; c++) {
            pdf.rect(x, y, w[c], pdf.lineHeight(9) + 6, 0.5f);
            pdf.text(headers[c], x + 4, y + 2, 9);
            x += w[c];
        }
        return y + pdf.lineHeight(9) + 6;
    }

    private static float total(float[] w) {
        float t = 0;
        for (float v : w) t += v;
        return t;
    }

    private static File save(Context ctx, MiniPdf pdf, String prefix) throws Exception {
        File dir = new File(ctx.getFilesDir(), "pdf");
        dir.mkdirs();
        File f = new File(dir, prefix + System.currentTimeMillis() + ".pdf");
        FileOutputStream fos = new FileOutputStream(f);
        fos.write(pdf.build());
        fos.close();
        return f;
    }

    public static void share(Context ctx, File f) {
        Uri uri = Uri.parse("content://ru.trucker.money.pdf/" + f.getName());
        Intent i = new Intent(Intent.ACTION_SEND);
        if (f.getName().endsWith(".xlsx")) {
            i.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        } else {
            i.setType("application/pdf");
        }
        i.putExtra(Intent.EXTRA_STREAM, uri);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        ctx.startActivity(Intent.createChooser(i, "Поделиться файлом"));
    }
}
