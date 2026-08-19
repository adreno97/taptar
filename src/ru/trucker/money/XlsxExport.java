package ru.trucker.money;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class XlsxExport {

    public static File exportReport(Context ctx, DbHelper db) throws Exception {
        StringBuilder trips = new StringBuilder();
        String[] hT = {"№", "Номер рейса", "Дата", "Зона", "Возврат", "Выгрузка", "Сумма, руб"};
        trips.append(sheetHeader(hT));
        int i = 1;
        for (DbHelper.Record r : db.getRecords(true, 1)) {
            trips.append(sheetRow(new String[]{
                    String.valueOf(i++), r.number, Util.date(r.date), Zones.shortName(r.zone),
                    r.isReturn ? "Да" : "Нет", String.valueOf(r.numPoints),
                    rub(r.amount)}));
        }

        StringBuilder exp = new StringBuilder();
        String[] hE = {"№", "Дата", "Категория", "Описание", "Сумма, руб"};
        exp.append(sheetHeader(hE));
        i = 1;
        for (DbHelper.Record r : db.getRecords(true, 2)) {
            exp.append(sheetRow(new String[]{
                    String.valueOf(i++), Util.date(r.date), r.title, r.note, rub(r.amount)}));
        }

        StringBuilder maint = new StringBuilder();
        String[] hM = {"№", "Дата", "Пробег, км", "Выполненные работы"};
        maint.append(sheetHeader(hM));
        i = 1;
        for (DbHelper.Maint m : db.getMaintAll()) {
            maint.append(sheetRow(new String[]{
                    String.valueOf(i++), Util.date(m.date), String.valueOf(m.mileage), m.works}));
        }

        byte[] xlsx = buildXlsx(
                new String[][]{{"Рейсы", trips.toString()}, {"Расходы", exp.toString()}, {"Обслуживание", maint.toString()}});

        File dir = new File(ctx.getFilesDir(), "pdf");
        dir.mkdirs();
        File f = new File(dir, "excel_" + System.currentTimeMillis() + ".xlsx");
        FileOutputStream fos = new FileOutputStream(f);
        fos.write(xlsx);
        fos.close();
        return f;
    }

    private static String rub(long kopecks) {
        return String.format(java.util.Locale.US, "%.2f", kopecks / 100.0);
    }

    private static String sheetHeader(String[] cols) {
        return sheetRow(cols);
    }

    private static String sheetRow(String[] vals) {
        StringBuilder sb = new StringBuilder("<row>");
        for (String v : vals) {
            sb.append("<c t=\"inlineStr\"><is><t>").append(escape(v == null ? "" : v)).append("</t></is></c>");
        }
        sb.append("</row>");
        return sb.toString();
    }

    private static String escape(String s) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&': out.append("&amp;"); break;
                case '<': out.append("&lt;"); break;
                case '>': out.append("&gt;"); break;
                case '"': out.append("&quot;"); break;
                default:
                    if (c < 0x20) out.append("&#").append((int) c).append(';');
                    else out.append(c);
            }
        }
        return out.toString();
    }

    public static byte[] buildXlsx(String[][] sheets) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(bos);

        put(zip, "[Content_Types].xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
                "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
                "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
                "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" +
                "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" +
                "<Override PartName=\"/xl/worksheets/sheet2.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" +
                "<Override PartName=\"/xl/worksheets/sheet3.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" +
                "</Types>");

        put(zip, "_rels/.rels",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>" +
                "</Relationships>");

        StringBuilder wb = new StringBuilder();
        wb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        wb.append("<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" ");
        wb.append("xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>");
        for (int i = 0; i < sheets.length; i++) {
            wb.append("<sheet name=\"").append(escape(sheets[i][0])).append("\" sheetId=\"").append(i + 1)
                    .append("\" r:id=\"rId").append(i + 1).append("\"/>");
        }
        wb.append("</sheets></workbook>");
        put(zip, "xl/workbook.xml", wb.toString());

        StringBuilder rels = new StringBuilder();
        rels.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        rels.append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
        for (int i = 0; i < sheets.length; i++) {
            rels.append("<Relationship Id=\"rId").append(i + 1).append("\" ")
                    .append("Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" ")
                    .append("Target=\"worksheets/sheet").append(i + 1).append(".xml\"/>");
        }
        rels.append("</Relationships>");
        put(zip, "xl/_rels/workbook.xml.rels", rels.toString());

        for (int i = 0; i < sheets.length; i++) {
            put(zip, "xl/worksheets/sheet" + (i + 1) + ".xml",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                    "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>"
                    + sheets[i][1] + "</sheetData></worksheet>");
        }

        zip.close();
        return bos.toByteArray();
    }

    private static void put(ZipOutputStream zip, String name, String content) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        byte[] data = content.getBytes("UTF-8");
        zip.write(data, 0, data.length);
        zip.closeEntry();
    }
}
